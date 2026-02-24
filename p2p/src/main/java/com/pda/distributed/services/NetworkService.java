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

    // Permite al Nodo inyectar el servicio de StateSync
    private StateSyncService stateSyncService;

    public void setStateSyncService(StateSyncService stateSyncService) {
        this.stateSyncService = stateSyncService;
    }

    // Inicia el servidor escuchar a otros nodos
    public void startServer(int port, StorageCoordinator storageCoordinator) throws IOException {
        this.grpcServer = ServerBuilder.forPort(port)
                .addService(new PdaServiceGrpcImpl(this.quorumService, this.stateSyncService, storageCoordinator))
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

    // Enviar el estado propio a todos los nodos conectados (Gossip)
    public void sincronizarEstado(String miEstado) {
        // System.out.println("NetworkService: Enviando estado a " + channels.size() + "
        // nodos...");

        com.pda.distributed.network.grpc.PeticionEstado peticion = com.pda.distributed.network.grpc.PeticionEstado
                .newBuilder()
                .setDatosEstado(miEstado)
                .build();

        for (Map.Entry<Integer, ManagedChannel> entry : channels.entrySet()) {
            int puertoDestino = entry.getKey();
            ManagedChannel canal = entry.getValue();

            try {
                // Para gossip, enviamos sin bloquear mucho tiempo
                PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(canal);
                com.pda.distributed.network.grpc.RespuestaEstado respuesta = stub.sincronizarEstado(peticion);

                // System.out.println("NetworkService: Estado sincronizado con puerto " +
                // puertoDestino + ". Confirmación: " + respuesta.getConfirmacion());

            } catch (Exception e) {
                System.out.println("NetworkService: Error sincronizando estado con el puerto " + puertoDestino
                        + ". Puede que esté caído.");
            }
        }
    }

    // =========================================================================
    // Métodos para la Gestión de Archivos (Subidas/Descargas)
    // =========================================================================

    /**
     * Envía un fragmento de archivo a un nodo específico (Worker) de la red.
     * @param nodoDestino Formato "ip:puerto" (ej. "localhost:50052")
     * @param idArchivo   El UUID + nombre del archivo original
     * @param datos       Los bytes puros a enviar
     * @return true si el nodo destino lo recibió y guardó, false si falló
     */
    public boolean enviarFragmento(String nodoDestino, String idArchivo, byte[] datos) {
        System.out.println("[NetworkService] Preparando envío de fragmento '" + idArchivo + "' a " + nodoDestino + " (" + datos.length + " bytes)");

        int puertoDestino;
        try {
            String[] partes = nodoDestino.split(":");
            puertoDestino = Integer.parseInt(partes[1]);
        } catch (Exception e) {
            System.err.println("[NetworkService] Error parseando nodo destino: " + nodoDestino);
            return false;
        }

        ManagedChannel canal = channels.get(puertoDestino);
        if (canal == null) {
            System.err.println("[NetworkService] No hay conexión establecida con el puerto " + puertoDestino + ". Asegúrate de hacer Ping primero.");
            return false;
        }

        try {
            // Convertimos tu arreglo de bytes de Java al formato de bytes de Protobuf
            com.google.protobuf.ByteString bytesProto = com.google.protobuf.ByteString.copyFrom(datos);

            // Usamos los nombres exactos de tu .proto
            com.pda.distributed.network.grpc.PeticionSubida peticion = com.pda.distributed.network.grpc.PeticionSubida
                    .newBuilder()
                    .setIdArchivo(idArchivo)
                    .setFragmento(bytesProto) // Coincide con: bytes fragmento = 2;
                    .build();

            PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(canal);

            System.out.println("[NetworkService] Enviando petición gRPC de subida...");

            // Coincide con: rpc subirFragmento(PeticionSubida)
            com.pda.distributed.network.grpc.RespuestaSubida respuesta = stub.subirFragmento(peticion);

            if (respuesta.getExito()) {
                System.out.println("[NetworkService] ¡Éxito! El nodo " + nodoDestino + " guardó el fragmento.");
                return true;
            } else {
                System.err.println("[NetworkService] El nodo " + nodoDestino + " rechazó el fragmento.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("[NetworkService] Error crítico de red al enviar fragmento a " + puertoDestino + ": " + e.getMessage());
            return false;
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
