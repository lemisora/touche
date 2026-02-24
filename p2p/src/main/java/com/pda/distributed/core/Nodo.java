package com.pda.distributed.core;

import com.pda.distributed.services.NetworkService;
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

    public Nodo(int id, String ip, int port, String name, NodeRole initialRole) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.currentRole = initialRole;

        // Instanciar servicios principal
        this.networkService = new NetworkService();
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
