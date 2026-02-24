package com.pda.distributed.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class NodeServer {
    private final int port;
    private final Server server;

    public NodeServer(int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService((BindableService) new NetworkServiceImpl())
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Servidor iniciado en el puerto: " + port);
    }

    public void stop() throws InterruptedException {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        NodeServer server = new NodeServer(2000);
        server.start();
        server.blockUntilShutdown();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
