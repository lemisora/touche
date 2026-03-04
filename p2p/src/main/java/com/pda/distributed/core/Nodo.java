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
import java.util.Map;

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
    private int ultimoConteoNodos = 0;

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
        this.quorumService.setOnElectionWon(this::promoteToLeader);
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
        if (currentRole == NodeRole.LEADER || "ELECTION".equals(idAccion)) {
            quorumService.proponerAccion(idAccion, accion);
        } else {
            ConsoleLogger.info("Log", "Nodo: Soy WORKER, no puedo proponer esta acción al Quorum.");
        }
    }

    public void stop() throws InterruptedException {
        ConsoleLogger.info("Log", "Deteniendo nodo " + name);
        watchdogActivo = false;
        if (ringWatchdog != null)
            ringWatchdog.interrupt();
        if (stateSyncService != null)
            stateSyncService.detenerGossip();
        if (discoveryService != null)
            discoveryService.detener();
        if (fileWatcherService != null)
            fileWatcherService.detener();
        networkService.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        networkService.blockUntilShutdown();
    }

    public synchronized void promoteToLeader() {
        if (this.currentRole != NodeRole.LEADER) {
            this.currentRole = NodeRole.LEADER;
            ConsoleLogger.setRolConfigurado("LIDER");
            ConsoleLogger.info("Log", "Nodo ha sido promovido a LIDER");
            // Un nuevo Lider debe iniciar su gossip
            stateSyncService.iniciarGossip(this.port);
        }
    }

    public synchronized void demoteToWorker() {
        this.currentRole = NodeRole.WORKER;
        ConsoleLogger.setRolConfigurado("WORKER");
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

    // Lógica de anillos y auto-elección
    private void iniciarWatchdog() {
        watchdogActivo = true;
        ringWatchdog = new Thread(() -> {
            // Jitter inicial para evitar colisiones al arrancar
            try {
                int randomJitter = (int) (Math.random() * 5000);
                Thread.sleep(10000 + randomJitter);
            } catch (InterruptedException ignored) {}

            while (watchdogActivo) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // Usamos un bloque sincronizado para que nadie más toque las variables mientras evaluamos
                synchronized (this) {
                    // Si ya estamos en el Anillo B, este nodo solo es trabajador y no orquesta nada
                    if (!"A".equals(currentRingId)) {
                        continue;
                    }

                    int totalNodos = networkService.getConnectedNodesCount() + 1;

                    if (totalNodos >= 3 && totalNodos != ultimoConteoNodos) {
                        ConsoleLogger.advertencia("Log", "Cantidad de nodos: " + totalNodos + ". Re-evaluando distribución de Comité y Trabajadores...");
                        dividirAnillos();
                        ultimoConteoNodos = totalNodos;
                    }
                    // Si después de la posible división seguimos en el Anillo A y somos Workers, nos promovemos
                    else {
                        if (currentRole == NodeRole.WORKER && "A".equals(currentRingId)) {
                            promoteToLeader();
                        }

                        if (totalNodos != ultimoConteoNodos) {
                            ultimoConteoNodos = totalNodos; // Actualizamos el tracking si alguien se fue
                        }
                    }
                }
            }
        });
        ringWatchdog.start();
    }

    private synchronized void dividirAnillos() {
        List<Integer> todosLosPuertos = networkService.getConnectedPorts();
        todosLosPuertos.add(this.port);
        Collections.sort(todosLosPuertos);

        int miIndice = todosLosPuertos.indexOf(this.port);

        if (miIndice >= 2) {
            this.currentRingId = "B";
            if (this.discoveryService != null) {
                this.discoveryService.setRingId("B");
            }
            ConsoleLogger.info("Log", "Fui reasignado al nuevo Anillo B (Trabajadores).");
            demoteToWorker();
        } else {
            ConsoleLogger.info("Log", "Me mantengo en el Anillo A (Comité).");
            promoteToLeader();
        }

        ConsoleLogger.info("Log", "Reestructuración de anillos completada. Total Nodos: " + todosLosPuertos.size());
    }

    public String getNetworkInfo() {
        return String.format("--- INFO DEL NODO ---\n" +
                "ID: %d\nIP: %s\nPuerto: %d\nRol: %s\nAnillo: %s\nConexiones Activas: %d\nPuertos Conectados: %s\n---------------------",
                id, ip, port, currentRole, currentRingId, networkService.getConnectedNodesCount(),
                networkService.getConnectedPorts().toString());
    }

    public void forzarSubidaManual(String rutaArchivo) {
        if (this.storageCoordinator != null) {
            this.storageCoordinator.manejarNuevoArchivoLocal(rutaArchivo);
        }
    }

    public String getArchivosDistribuidos() {
        if (distributedDirectory == null)
            return "Directorio no inicializado.";
        Map<String, List<String>> catalogo = distributedDirectory.obtenerEstadoCompleto();

        if (catalogo.isEmpty()) {
            return "No hay archivos distribuidos en la red actualmente.";
        }

        StringBuilder sb = new StringBuilder("--- ARCHIVOS DISTRIBUIDOS ---\n");
        for (Map.Entry<String, List<String>> entry : catalogo.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(" -> Guardado en: ").append(entry.getValue()).append("\n");
        }
        sb.append("-----------------------------");
        return sb.toString();
    }
}