package com.pda.distributed.network;

import com.pda.distributed.network.grpc.P2PNetworkServiceGrpc;
import com.pda.distributed.network.grpc.PingRequest;
import com.pda.distributed.network.grpc.PingResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class NodeClient {
    private final ManagedChannel channel;

    public NodeClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // No encriptar la conexión local
                .build();
    }

    public void ping() {
        P2PNetworkServiceGrpc.P2PNetworkServiceBlockingStub stub = P2PNetworkServiceGrpc.newBlockingStub(channel);
        PingRequest request = PingRequest.newBuilder().setMensajeSaludo("Hola").build();

        boolean conectado = false;

        while (!conectado) {
            try {
                PingResponse response = stub.ping(request);
                System.out.println("Respuesta del otro nodo: " + response.getRespuesta());
                conectado = true; // Si llegamos aquí, ¡funcionó! Salimos del bucle.

            } catch (StatusRuntimeException e) {
                System.out.println("El nodo destino aún no está listo. Reintentando en 3 segundos...");
                try {
                    Thread.sleep(3000); // Esperar 3 segundos antes del próximo intento
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.out.println("Bucle de conexión interrumpido.");
                    break;
                }
            }
        }
    }

    public void shutdown() {
        channel.shutdown();
    }
}
