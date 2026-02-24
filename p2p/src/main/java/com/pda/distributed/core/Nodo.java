package com.pda.distributed.core;

import com.pda.distributed.utils.ConsoleLogger;

import com.pda.distributed.services.NetworkService;
import com.pda.distributed.services.QuorumService;
import com.pda.distributed.services.StateSyncService;
import com.pda.distributed.services.StorageCoordinator;
import com.pda.distributed.services.FileWatcherService;
import java.io.IOException;

// Facade principal del nodo
public class Nodo {
    private final int id;
    private final String ip;
    private final int port;
    private final String name;
    private NodeRole currentRole;

    // Servicios
    private final NetworkService networkService;
    private final QuorumService quorumService;
    private final StateSyncService stateSyncService;
    private final StorageCoordinator storageCoordinator;
    private final FileWatcherService fileWatcherService;

    public Nodo(int id, String ip, int port, String name, NodeRole initialRole) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.currentRole = initialRole;

        // Instanciar servicios principal
        this.networkService = new NetworkService();
        this.quorumService = new QuorumService();
        this.stateSyncService = new StateSyncService();
        this.storageCoordinator = new StorageCoordinator();
        this.fileWatcherService = new FileWatcherService();

        // Inyectar dependencias (conectar cables)
        this.quorumService.setNetworkService(this.networkService);
        this.networkService.setQuorumService(this.quorumService);
        this.stateSyncService.setNetworkService(this.networkService);
        this.networkService.setStateSyncService(this.stateSyncService);

        // Cables de Storage
        this.storageCoordinator.setNetworkService(this.networkService);
        this.storageCoordinator.setQuorumService(this.quorumService);
        this.fileWatcherService.setStorageCoordinator(this.storageCoordinator);
    }

    public void start() throws IOException {
        ConsoleLogger.info("Log", "--- Iniciando Nodo " + name + " ---");
        ConsoleLogger.info("Log", "ID: " + id + " | IP: " + ip + " | Puerto: " + port + " | Rol: " + currentRole);

        // Arrancar el servidor de red pasándole nuestra lógica actual
        networkService.startServer(port, storageCoordinator);

        // Si somos líderes, empezamos a latir y a vigilar archivos
        if (currentRole == NodeRole.LEADER) {
            stateSyncService.iniciarGossip(port);
            fileWatcherService.iniciar();
        }
    }

    public void connectToPeer(String peerIp, int peerPort) {
        networkService.sendPing(peerIp, peerPort);
    }

    // Expone la funcionalidad de proponer una acción al Quorum
    public void proponer(String idAccion, String accion) {
        if (currentRole == NodeRole.LEADER) {
            quorumService.proponerAccion(idAccion, accion);
        } else {
            ConsoleLogger.info("Log", "Nodo: Soy WORKER, no puedo proponer acciones al Quorum.");
        }
    }

    public void stop() throws InterruptedException {
        ConsoleLogger.info("Log", "Deteniendo nodo " + name);
        if (stateSyncService != null) {
            stateSyncService.detenerGossip();
        }
        networkService.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        networkService.blockUntilShutdown();
    }

    public void promoteToLeader() {
        this.currentRole = NodeRole.LEADER;
        ConsoleLogger.info("Log", "Nodo ha sido promovido a LIDER");
    }

    public void demoteToWorker() {
        this.currentRole = NodeRole.WORKER;
        ConsoleLogger.info("Log", "Nodo ha sido degradado a TRABAJADOR");
    }

    public NodeRole getRole() {
        return currentRole;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }
}
