package com.pda.distributed.core;

import com.pda.distributed.utils.ConsoleLogger;
import com.pda.distributed.services.NetworkService;
import com.pda.distributed.services.QuorumService;
//import com.pda.distributed.services.StateSyncService;
//import com.pda.distributed.services.StorageCoordinator;
//import com.pda.distributed.services.FileWatcherService;
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
    /** Dirección del nodo en formato IP:PUERTO */
    private String nodeAddress;
    private final String name;
    private NodeRole currentRole;
    private RingType currentRingID;
    private NodeState currentState;

    // Servicios de Red y Consenso
    private final NetworkService networkService;
    // private final QuorumService quorumService;
    // private final StateSyncService stateSyncService;
    // private final DiscoveryService discoveryService;

    // Servicios de Almacenamiento
    // private final StorageCoordinator storageCoordinator;
    // private final FileWatcherService fileWatcherService;
    // private final StorageManager storageManager;
    // private final DistributedDirectory distributedDirectory;

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
        // this.quorumService = new QuorumService();
        // this.stateSyncService = new StateSyncService();
        // this.discoveryService = new DiscoveryService();

        // this.storageCoordinator = new StorageCoordinator();
        // this.fileWatcherService = new FileWatcherService();
        // this.storageManager = new StorageManager();
        // this.distributedDirectory = new DistributedDirectory();

        // Inyectar dependencias de Red
        // this.quorumService.setNetworkService(this.networkService);
        // this.quorumService.setOnElectionWon(this::promoteToLeader);
        // this.networkService.setQuorumService(this.quorumService);
        // this.stateSyncService.setNetworkService(this.networkService);
        // this.networkService.setStateSyncService(this.stateSyncService);
        // this.discoveryService.setNetworkService(this.networkService);

        // Dependencias de Storage
        // this.storageCoordinator.setNetworkService(this.networkService);
        // this.storageCoordinator.setQuorumService(this.quorumService);
        // this.storageCoordinator.setStorageManager(this.storageManager);
        // this.storageCoordinator.setDistributedDirectory(this.distributedDirectory);
        // this.fileWatcherService.setStorageCoordinator(this.storageCoordinator);
    }

    /** Iniciar el nodo, en caso de no existir una dirección de semilla
     * (para ejecución en redes locales) */
    public void start(){
        ConsoleLogger.info(this.name, "-- Iniciando el Nodo y sus servicios... --");
        
    }
    
    /** Iniciar el nodo con una dirección de semilla, en caso de existir 
    * 
    * @param seedAddress Dirección de semilla para iniciar el nodo
    */
    public void start(String seedAddress){}
    
    
    /** Detener al nodo y sus servicios */
    public void stop() throws InterruptedException{
        ConsoleLogger.info(this.name, "Deteniendo el nodo y sus servicios...");
        if (this.networkService != null) {
            networkService.stop();
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
}