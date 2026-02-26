package com.pda.distributed.core;

import com.pda.distributed.utils.ConsoleLogger;

import com.pda.distributed.services.NetworkService;
import com.pda.distributed.services.QuorumService;
import com.pda.distributed.services.StateSyncService;
import com.pda.distributed.services.StorageCoordinator;
import com.pda.distributed.services.FileWatcherService;
import com.pda.distributed.services.DiscoveryService;
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
    private final DiscoveryService discoveryService;

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
        this.discoveryService = new DiscoveryService();

        // Inyectar dependencias (conectar cables)
        this.quorumService.setNetworkService(this.networkService);
        this.networkService.setQuorumService(this.quorumService);
        this.stateSyncService.setNetworkService(this.networkService);
        this.networkService.setStateSyncService(this.stateSyncService);

        // Cables de Discovery
        this.discoveryService.setNetworkService(this.networkService);

        // Cables de Storage
        this.storageCoordinator.setNetworkService(this.networkService);
        this.storageCoordinator.setQuorumService(this.quorumService);
        this.fileWatcherService.setStorageCoordinator(this.storageCoordinator);
    }

    public void start() throws IOException {
        ConsoleLogger.info("Log", "--- Iniciando Nodo " + name + " ---");
        ConsoleLogger.info("Log", "ID: " + id + " | IP: " + ip + " | Puerto: " + port + " | Rol: " + currentRole);

        // Arrancar el servidor de red recibiendo el puerto aleatorio libre elegido
        int assignedPort = networkService.startServer(storageCoordinator);
        ConsoleLogger.info("Log", "Puerto asignado: " + assignedPort + ". Iniciando Discovery UDP...");

        // Iniciamos el servicio de UDP Broadcast indicando en qué puerto TCP debe
        // escuchar la otra gente
        discoveryService.iniciar(assignedPort);

        // Si somos líderes, empezamos a latir y a vigilar archivos
        if (currentRole == NodeRole.LEADER) {
            stateSyncService.iniciarGossip(assignedPort);
            fileWatcherService.iniciar();
        }

        // Iniciar el vigilante de los anillos
        iniciarWatchdog();
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
        watchdogActivo = false;
        if (ringWatchdog != null) {
            ringWatchdog.interrupt();
        }
        if (stateSyncService != null) {
            stateSyncService.detenerGossip();
        }
        if (discoveryService != null) {
            discoveryService.detener();
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

    private Thread ringWatchdog;
    private boolean watchdogActivo = false;
    private String currentRingId = "A";

    private void iniciarWatchdog() {
        watchdogActivo = true;
        ringWatchdog = new Thread(() -> {
            while (watchdogActivo) {
                try {
                    Thread.sleep(5000); // Revisar cada 5 segundos

                    // Si ya nos dividimos o somos worker, no hacemos nada extra por ahora
                    if (!currentRingId.equals("A") || currentRole == NodeRole.WORKER) {
                        continue;
                    }

                    // Total de Nodos en mi anillo = Mis conexiones + yo mismo
                    int totalNodos = networkService.getConnectedNodesCount() + 1;

                    // Si llegamos a 6 nodos en el anillo A, desencadenamos la separación
                    if (totalNodos >= 6) {
                        ConsoleLogger.advertencia("Log",
                                "¡Nivel crítico de nodos (6)! Iniciando Sharding de Anillos...");
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
        // Obtenemos todos los puertos conectados y nos añadimos a la lista
        java.util.List<Integer> todosLosPuertos = networkService.getConnectedPorts();
        todosLosPuertos.add(this.port);

        // Ordenamos la lista para que todos los nodos vean el mismo orden
        // (Determinismo)
        java.util.Collections.sort(todosLosPuertos);

        // Los 3 menores se quedan en el Anillo A, los 3 mayores van al Anillo B
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
            if (p == this.port)
                continue;

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
