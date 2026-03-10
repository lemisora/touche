package com.pda.distributed.core;

import com.pda.distributed.services.*;
import com.pda.distributed.utils.ConsoleLogger;

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
    /** Dirección del nodo en formato IP:PUERTO */
    private String nodeAddress;
    private final String name;
    private NodeRole currentRole;
    private RingType currentRingID;
    private NodeState currentState;

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
    // private Thread ringWatchdog;
    // private boolean watchdogActivo = false;
    // private String currentRingId = "A";
    // private int ultimoConteoNodos = 0;

    public Nodo(int id, String ip, String name) {
        this.id = id;
        this.ip = ip;
        this.name = name;
        this.currentRole = NodeRole.WORKER;
        this.currentState = NodeState.BLOCKED;
        this.currentRingID = RingType.NONE;

        // Instanciar servicios
        this.networkService = new NetworkService(this);
        this.quorumService = new QuorumService(this.networkService);
        this.discoveryService = new DiscoveryService(this.networkService);
        this.stateSyncService = new StateSyncService(this.networkService);

        this.networkService.setQuorumService(this.quorumService);
        this.networkService.setDiscoveryService(this.discoveryService);

        this.storageManager = new StorageManager("archivos");
        this.storageCoordinator = new StorageCoordinator();
        this.distributedDirectory = new DistributedDirectory();

        this.storageCoordinator.setStorageManager(this.storageManager);
        this.storageCoordinator.setDistributedDirectory(this.distributedDirectory);
        this.storageCoordinator.setNetworkService(this.networkService);
        this.storageCoordinator.setQuorumService(this.quorumService);
        this.storageCoordinator.setStateSyncService(this.stateSyncService);

        this.stateSyncService.setDistributedDirectory(this.distributedDirectory);
        this.stateSyncService.setQuorumService(this.quorumService);
        this.stateSyncService.setNodoLocal(this);
        this.quorumService.setStateSyncService(this.stateSyncService);

        this.networkService.setStorageCoordinator(this.storageCoordinator);
        this.networkService.setStateSyncService(this.stateSyncService);

        this.fileWatcherService = new FileWatcherService("archivos_entrada");
        this.fileWatcherService.setStorageCoordinator(this.storageCoordinator);
    }

    /**
     * Iniciar el nodo, en caso de no existir una dirección de semilla
     * (para ejecución en redes locales)
     */
    public void start() {
        ConsoleLogger.info(this.name, "-- Iniciando el Nodo y sus servicios... --");

    }

    /**
     * Iniciar el nodo con una dirección de semilla, en caso de existir
     *
     * @param seedAddress Dirección de semilla para iniciar el nodo
     */
    public void start(String seedAddress) throws IOException {
        if (seedAddress != null && !seedAddress.trim().isEmpty()) {
            ConsoleLogger.info(this.name, "-- Iniciando conectando a semilla: " + seedAddress + " --");
        } else {
            ConsoleLogger.info(this.name, "-- Iniciando sin semilla. Buscando red local... --");
        }

        // 1. Iniciar servidor gRPC buscando puerto libre
        this.port = networkService.startDirectServer();
        this.nodeAddress = this.ip + ":" + this.port;
        ConsoleLogger.info(this.name, "Servidor gRPC escuchando en: " + this.nodeAddress);

        // Asignamos el ID al QuorumService
        this.quorumService.setMiNodeId(this.id);
        this.storageCoordinator.start();

        // 2. Lógica de conexión
        if (seedAddress != null && !seedAddress.trim().isEmpty()) {
            intentarUnirseARed(seedAddress);
        } else {
            esperarDescubrimientoONacer();
        }

        this.networkService.iniciarHeartbeats();
        this.fileWatcherService.start();
    }

    /** Detener al nodo y sus servicios */
    public void stop() throws InterruptedException {
        ConsoleLogger.info(this.name, "Deteniendo el nodo y sus servicios...");
        if (this.networkService != null) {
            networkService.stop();
        }
    }

    private void esperarDescubrimientoONacer() {
        ConsoleLogger.info(this.name, "Sin semilla. Escuchando UDP por 4 segundos...");
        try {
            // Dormimos el hilo principal para darle tiempo al DiscoveryService de escuchar
            // algo
            Thread.sleep(4000);
        } catch (InterruptedException ignored) {
        }

        // Despertamos. ¿El Discovery logró conectarnos?
        if (this.currentState == NodeState.BLOCKED) {
            ConsoleLogger.advertencia(this.name, "Nadie respondió en la red local.");
            iniciarComoGenesis();
        } else {
            ConsoleLogger.exito(this.name, "¡Auto-descubrimiento exitoso!");
        }
    }

    private void iniciarComoGenesis() {
        ConsoleLogger.advertencia(this.name, "Asumiendo rol de Génesis (Primer Líder).");
        this.currentRole = NodeRole.LEADER;
        this.currentRingID = RingType.RING_A;
        this.currentState = NodeState.READY;

        ConsoleLogger.setRolConfigurado("LIDER");
        // Nos registramos a nosotros mismos en nuestro propio mapa
        quorumService.registrarNodo(this.nodeAddress, this.currentRingID);

        ConsoleLogger.exito(this.name, "Nodo Listo operando como Líder del Anillo A.");
    }

    private void intentarUnirseARed(String seedAddress) {
        ConsoleLogger.info(this.name, "Intentando unirse mediante semilla: " + seedAddress);
        this.currentState = NodeState.BLOCKED;

        // Pedimos al NetworkService que intente la conexión gRPC
        boolean conexionExitosa = networkService.joinNetwork(seedAddress, this.nodeAddress, this.id);

        if (conexionExitosa) {
            ConsoleLogger.exito(this.name, "Conexión exitosa. Asignación completada.");
        } else {
            ConsoleLogger.error(this.name, "No se pudo conectar a la semilla. Permaneciendo bloqueado.");
        }
    }

    /**
     * Llamado por NetworkService/QuorumService cuando el líder semilla responde al
     * 'JoinRequest'
     */
    public void actualizarEstadoDesdeSemilla(String anilloAsignadoStr) {
        try {
            this.currentRingID = RingType.valueOf(anilloAsignadoStr);
            this.currentState = NodeState.READY;

            if (this.currentRingID == RingType.RING_A) {
                this.currentRole = NodeRole.LEADER;
                ConsoleLogger.setRolConfigurado("LIDER");
            } else {
                this.currentRole = NodeRole.WORKER;
                ConsoleLogger.setRolConfigurado("WORKER");
            }

            ConsoleLogger.exito("Nodo", "Asignación completada: " + this.currentRole + " en " + this.currentRingID);
        } catch (IllegalArgumentException e) {
            ConsoleLogger.error("Nodo", "Anillo asignado desconocido: " + anilloAsignadoStr);
        }
    }

    /**
     * Método puente para que la consola (App) mande archivos a la cola del
     * coordinador.
     */
    public void forzarSubidaManual(String rutaAbsoluta) {
        if (this.storageCoordinator != null) {
            this.storageCoordinator.encolarArchivo(rutaAbsoluta);
        } else {
            ConsoleLogger.error(this.name, "El StorageCoordinator no está inicializado.");
        }
    }

    public Map<String, DistributedDirectory.FileMetadata> getArchivosDistribuidos() {
        if (this.distributedDirectory != null) {
            return this.distributedDirectory.getGlobalFileMap();
        }
        return Collections.emptyMap();
    }

    /**
     * Forzar la actualización del rol del nodo según el consenso de la red.
     */
    public void forzarCambioDeAnillo(RingType nuevoAnillo) {
        if (this.currentRingID != nuevoAnillo) {
            ConsoleLogger.advertencia(this.name, "Degradación/Ascenso por red. Pasando de " + this.currentRingID + " a " + nuevoAnillo);

            this.currentRingID = nuevoAnillo;

            if (nuevoAnillo == RingType.RING_A) {
                this.currentRole = NodeRole.LEADER;
                ConsoleLogger.setRolConfigurado("LIDER");
            } else {
                this.currentRole = NodeRole.WORKER;
                ConsoleLogger.setRolConfigurado("WORKER");
            }
        }
    }

    // Getters y setters
    public String getNodeAddress() {
        return this.nodeAddress;
    }

    public int getId() {
        return this.id;
    }

    public NodeState getCurrentState() {
        return currentState;
    }

    public NodeRole getCurrentRole() {
        return currentRole;
    }

    public RingType getCurrentRingID() {
        return currentRingID;
    }

    public List<String> getNodosConectados() {
        return networkService != null ? networkService.getNodosConectados() : Collections.emptyList();
    }

    public Map<String, RingType> getNodeRegistry() {
        return quorumService != null ? quorumService.getNodeRegistry() : Collections.emptyMap();
    }

    public String getIp() {
        return ip;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return this.port;
    }
}
