package com.pda.distributed.services;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import com.pda.distributed.network.grpc.PdaServiceGrpc;
import com.pda.distributed.network.grpc.PingRequest;
import com.pda.distributed.network.grpc.PingResponse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

// Abstrae todas las conexiones gRPC
public class NetworkService {
    private Server grpcServer;
    // Guardamos los canales para no crearlos a cada rato
    private final Map<Integer, ManagedChannel> channels = new ConcurrentHashMap<>();

    // Inicia el servidor escuchar a otros nodos
    public void startServer(int port) throws IOException {
        this.grpcServer = ServerBuilder.forPort(port)
                .addService(new PdaServiceGrpcImpl())
                .build();
        this.grpcServer.start();
        System.out.println("NetworkService: Servidor gRPC iniciado en puerto " + port);
    }

    // Funcionalidad de ping
    public void sendPing(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);
        PingRequest request = PingRequest.newBuilder().setMensajeSaludo("Hola desde P2P Nodo Facade").build();

        boolean conectado = false;
        while (!conectado) {
            try {
                PingResponse response = stub.ping(request);
                System.out.println("Respuesta del otro nodo (" + port + "): " + response.getRespuesta());
                conectado = true;
            } catch (io.grpc.StatusRuntimeException e) {
                System.out.println("El nodo destino " + port + " no está listo. Reintentando en 3 segundos...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        channels.put(port, channel);
    }

    public void stop() throws InterruptedException {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
        }
        if (grpcServer != null) {
            grpcServer.shutdown().awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (grpcServer != null) {
            grpcServer.awaitTermination();
        }
    }
}
