package com.pda.distributed.core;

import com.pda.distributed.utils.ConsoleLogger;

import com.pda.distributed.services.NetworkService;
import com.pda.distributed.services.QuorumService;
import com.pda.distributed.services.StateSyncService;
import com.pda.distributed.services.StorageCoordinator;
import com.pda.distributed.services.FileWatcherService;
import com.pda.distributed.services.DiscoveryService;

import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// Facade principal del nodo
public class Nodo {
    private final int id;
    private final String ip;
    private int port;
    private final String name;
    private NodeRole currentRole;

    // Servicios de Red y Consenso
    private final NetworkService networkService;
    private final QuorumService quorumService;
    private final StateSyncService stateSyncService;
    private final DiscoveryService discoveryService;

    // Servicios de Almacenamiento
    private final StorageCoordinator storageCoordinator;
    private final FileWatcherService fileWatcherService;
    private final StorageManager storageManager;
    private final DistributedDirectory distributedDirectory;

    // Variables del Anillo
    private Thread ringWatchdog;
    private boolean watchdogActivo = false;
    private String currentRingId = "A";

    public Nodo(int id, String ip, String name, NodeRole initialRole) {
        this.id = id;
        this.ip = ip;
        this.name = name;
        this.currentRole = initialRole;

        // Instanciar servicios
        this.networkService = new NetworkService();
        this.quorumService = new QuorumService();
        this.stateSyncService = new StateSyncService();
        this.discoveryService = new DiscoveryService();
        
        this.storageCoordinator = new StorageCoordinator();
        this.fileWatcherService = new FileWatcherService();
        this.storageManager = new StorageManager();
        this.distributedDirectory = new DistributedDirectory();

        // Inyectar dependencias de Red
        this.quorumService.setNetworkService(this.networkService);
        this.networkService.setQuorumService(this.quorumService);
        this.stateSyncService.setNetworkService(this.networkService);
        this.networkService.setStateSyncService(this.stateSyncService);
        this.discoveryService.setNetworkService(this.networkService);

        // Dependencias de Storage
        this.storageCoordinator.setNetworkService(this.networkService);
        this.storageCoordinator.setQuorumService(this.quorumService);
        this.storageCoordinator.setStorageManager(this.storageManager);
        this.storageCoordinator.setDistributedDirectory(this.distributedDirectory);
        this.fileWatcherService.setStorageCoordinator(this.storageCoordinator);
    }

    public void start() throws IOException {
        ConsoleLogger.info("Log", "--- Iniciando Nodo " + name + " ---");
        
        // Arrancar el servidor de red recibiendo el puerto aleatorio libre elegido
        this.port = networkService.startServer(storageCoordinator);
        ConsoleLogger.info("Log", "Puerto asignado: " + this.port + ". Iniciando Discovery UDP...");

        // Iniciamos el servicio de UDP Broadcast
        discoveryService.iniciar(this.port);

        // Registrar nuestro propio almacenamiento local en el directorio
        String miIdNodo = this.ip + ":" + this.port;
        long miEspacioLibre = storageManager.obtenerEspacioDisponible();
        distributedDirectory.actualizarEstadoNodo(miIdNodo, miEspacioLibre);

        // Iniciamos el vigía de archivos para que detecte nuevos documentos
        fileWatcherService.iniciar();

        // Si somos líderes, empezamos a latir
        if (currentRole == NodeRole.LEADER) {
            stateSyncService.iniciarGossip(this.port);
        }

        // Iniciar el vigilante de los anillos
        iniciarWatchdog();
        
        ConsoleLogger.info("Log", "ID: " + id + " | IP: " + ip + " | Puerto: " + port + " | Rol: " + currentRole);
    }

    public void connectToPeer(String peerIp, int peerPort) {
        networkService.sendPing(peerIp, peerPort);
    }

    public void proponer(String idAccion, String accion) {
        if (currentRole == NodeRole.LEADER) {
            quorumService.proponerAccion(idAccion, accion);
        } else {
            ConsoleLogger.info("Log", "Nodo: Soy WORKER, no puedo proponer acciones al Quorum.");
        }
    }

    public void stop() throws InterruptedException {
        ConsoleLogger.info("Log", "Deteniendo nodo " + name);
        watchdogActivo = false;
        if (ringWatchdog != null) ringWatchdog.interrupt();
        if (stateSyncService != null) stateSyncService.detenerGossip();
        if (discoveryService != null) discoveryService.detener();
        if (fileWatcherService != null) fileWatcherService.detener();
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

    public NodeRole getRole() { return currentRole; }
    public int getPort() { return port; }
    public String getIp() { return ip; }

    // Lógica de anillos
    private void iniciarWatchdog() {
        watchdogActivo = true;
        ringWatchdog = new Thread(() -> {
            while (watchdogActivo) {
                try {
                    Thread.sleep(5000); // Revisar cada 5 segundos

                    if (!currentRingId.equals("A") || currentRole == NodeRole.WORKER) {
                        continue;
                    }

                    int totalNodos = networkService.getConnectedNodesCount() + 1;

                    if (totalNodos >= 6) {
                        ConsoleLogger.advertencia("Log", "¡Nivel crítico de nodos (6)! Iniciando Sharding de Anillos...");
                        dividirAnillos();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    watchdogActivo = false;
                }
            }
        });
        ringWatchdog.start();
    }

    private void dividirAnillos() {
        List<Integer> todosLosPuertos = networkService.getConnectedPorts();
        todosLosPuertos.add(this.port);
        Collections.sort(todosLosPuertos);

        int miIndice = todosLosPuertos.indexOf(this.port);

        if (miIndice >= 3) {
            this.currentRingId = "B";
            this.discoveryService.setRingId("B");
            ConsoleLogger.info("Log", "Fui reasignado al nuevo Anillo B.");
        } else {
            ConsoleLogger.info("Log", "Me mantengo en el Anillo A.");
        }

        // Cortamos las conexiones con los nodos del anillo opuesto
        for (Integer p : todosLosPuertos) {
            if (p == this.port) continue;

            int suIndice = todosLosPuertos.indexOf(p);
            boolean esDelAnilloB = (suIndice >= 3);
            boolean soyDelAnilloB = (miIndice >= 3);

            if (esDelAnilloB != soyDelAnilloB) {
                // Somos de anillos diferentes, cerramos el canal gRPC
                ConsoleLogger.info("Log", "Cortando conexión con nodo del anillo opuesto (Puerto " + p + ")");
                networkService.desconectarNodo(p);
            }
        }
    }

    public String getNetworkInfo() {
        return String.format("--- INFO DEL NODO ---\n" +
                "ID: %d\nIP: %s\nPuerto: %d\nRol: %s\nAnillo: %s\nConexiones Activas: %d\nPuertos Conectados: %s\n---------------------",
                id, ip, port, currentRole, currentRingId, networkService.getConnectedNodesCount(),
                networkService.getConnectedPorts().toString());
    }
}