package com.pda.distributed.core;

import com.pda.distributed.services.NetworkService;
import com.pda.distributed.services.QuorumService;
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

    public Nodo(int id, String ip, int port, String name, NodeRole initialRole) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.currentRole = initialRole;

        // Instanciar servicios principal
        this.networkService = new NetworkService();
        this.quorumService = new QuorumService();

        // Inyectar dependencias (conectar cables)
        this.quorumService.setNetworkService(this.networkService);
        this.networkService.setQuorumService(this.quorumService);
    }

    public void start() throws IOException {
        System.out.println("--- Iniciando Nodo " + name + " ---");
        System.out.println("ID: " + id + " | IP: " + ip + " | Puerto: " + port + " | Rol: " + currentRole);

        // Arrancar el servidor de red
        networkService.startServer(port);
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
        networkService.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        networkService.blockUntilShutdown();
    }

    public void promoteToLeader() {
        this.currentRole = NodeRole.LEADER;
        System.out.println("Nodo ha sido promovido a LIDER");
    }

    public void demoteToWorker() {
        this.currentRole = NodeRole.WORKER;
        System.out.println("Nodo ha sido degradado a TRABAJADOR");
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
