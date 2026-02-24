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

    // Referencia al servicio de Quorum para enviarle los votos entrantes
    private QuorumService quorumService;

    // Permite al Nodo inyectar el servicio de Quorum
    public void setQuorumService(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    // Inicia el servidor escuchar a otros nodos
    public void startServer(int port) throws IOException {
        this.grpcServer = ServerBuilder.forPort(port)
                .addService(new PdaServiceGrpcImpl(this.quorumService))
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

    // Enviar una propuesta de votación a todos los nodos conectados actualmente
    public void solicitarVotos(String idAccion) {
        System.out.println(
                "NetworkService: Enviando petición de voto para '" + idAccion + "' a " + channels.size() + " nodos...");

        com.pda.distributed.network.grpc.PeticionVoto peticion = com.pda.distributed.network.grpc.PeticionVoto
                .newBuilder()
                .setIdAccion(idAccion)
                .build();

        for (Map.Entry<Integer, ManagedChannel> entry : channels.entrySet()) {
            int puertoDestino = entry.getKey();
            ManagedChannel canal = entry.getValue();

            // Usamos un stub asíncrono o síncrono. Aquí bloqueamos brevemente por
            // simplicidad
            try {
                PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(canal);
                com.pda.distributed.network.grpc.RespuestaVoto respuesta = stub.votar(peticion);

                System.out.println("NetworkService: Respuesta de voto recibida del puerto " + puertoDestino + ": "
                        + (respuesta.getAcepta() ? "Aceptó" : "Rechazó"));

                // Si tuviéramos acceso a QuorumService aquí, le pasaríamos la respuesta
                // inmediatamente
                // Pero lo conectaremos en el Orquestador (Facade) o pasando una referencia

            } catch (Exception e) {
                System.out.println("NetworkService: Error solicitando voto al puerto " + puertoDestino);
            }
        }
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
