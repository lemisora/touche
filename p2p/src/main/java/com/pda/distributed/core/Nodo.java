package com.pda.distributed.core;

import com.pda.distributed.services.NetworkService;
import com.pda.distributed.services.QuorumService;
import com.pda.distributed.services.StateSyncService;
import com.pda.distributed.services.StorageCoordinator;
import com.pda.distributed.services.FileWatcherService;
import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;

import java.io.IOException;

// Facade principal del nodo
public class Nodo {
    private final int id;
    private final String ip;
    private final int port;
    private final String name;
    private NodeRole currentRole;

    // Servicios de Red y Consenso
    private final NetworkService networkService;
    private final QuorumService quorumService;
    private final StateSyncService stateSyncService;

    // Servicios de Almacenamiento (¡Tu subsistema!)
    private final StorageCoordinator storageCoordinator;
    private final FileWatcherService fileWatcherService;
    private final StorageManager storageManager;
    private final DistributedDirectory distributedDirectory;

    public Nodo(int id, String ip, int port, String name, NodeRole initialRole) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.currentRole = initialRole;

        // 1. Instanciar todos los servicios
        this.networkService = new NetworkService();
        this.quorumService = new QuorumService();
        this.stateSyncService = new StateSyncService();
        this.storageCoordinator = new StorageCoordinator();
        this.fileWatcherService = new FileWatcherService();
        this.storageManager = new StorageManager();
        this.distributedDirectory = new DistributedDirectory();

        // 2. Inyectar dependencias (Conectar los cables de Red y Consenso)
        this.quorumService.setNetworkService(this.networkService);
        this.networkService.setQuorumService(this.quorumService);
        this.stateSyncService.setNetworkService(this.networkService);
        this.networkService.setStateSyncService(this.stateSyncService);

        // 3. Inyectar dependencias (Conectar TUS cables de Storage)
        this.storageCoordinator.setNetworkService(this.networkService);
        this.storageCoordinator.setQuorumService(this.quorumService);
        this.storageCoordinator.setStorageManager(this.storageManager);
        this.storageCoordinator.setDistributedDirectory(this.distributedDirectory);

        // El vigía necesita conocer a su coordinador
        this.fileWatcherService.setStorageCoordinator(this.storageCoordinator);
    }

    public void start() throws IOException {
        System.out.println("--- Iniciando Nodo " + name + " ---");
        System.out.println("ID: " + id + " | IP: " + ip + " | Puerto: " + port + " | Rol: " + currentRole);

        // Arrancar el servidor de red pasándole nuestra lógica actual
        networkService.startServer(port, storageCoordinator);

        // Actualizamos el directorio global indicando que este nodo (nosotros mismos) está activo y reportamos el espacio libre
        String miIdNodo = this.ip + ":" + this.port;
        long miEspacioLibre = storageManager.obtenerEspacioDisponible();
        distributedDirectory.actualizarEstadoNodo(miIdNodo, miEspacioLibre);

        // Comportamiento según el rol inicial
        if (currentRole == NodeRole.LEADER) {
            stateSyncService.iniciarGossip(port);
            fileWatcherService.iniciar();
        } else {
            // Un worker también podría iniciar su FileWatcher si permiten subir archivos desde cualquier nodo,
            // pero si la regla actual es que solo el líder recibe archivos, lo dejamos así.
            System.out.println("Nodo: Soy WORKER. Esperando instrucciones y fragmentos...");
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
            System.out.println("Nodo: Soy WORKER, no puedo proponer acciones al Quorum.");
        }
    }

    public void stop() throws InterruptedException {
        System.out.println("Deteniendo nodo " + name);
        if (stateSyncService != null) {
            stateSyncService.detenerGossip();
        }
        if (fileWatcherService != null) {
            fileWatcherService.detener();
        }
        networkService.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        networkService.blockUntilShutdown();
    }

    public void promoteToLeader() {
        this.currentRole = NodeRole.LEADER;
        System.out.println("Nodo ha sido promovido a LIDER");

        // Si me vuelvo líder, arranco los servicios exclusivos de líder
        fileWatcherService.iniciar();
    }

    public void demoteToWorker() {
        this.currentRole = NodeRole.WORKER;
        System.out.println("Nodo ha sido degradado a TRABAJADOR");

        // Si pierdo el liderazgo, detengo la vigilancia de archivos exclusivos
        fileWatcherService.detener();
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