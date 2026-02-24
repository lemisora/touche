package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

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

    // Tolerancia a fallos
    private final Map<Integer, String> hostMap = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> nodosMuertos = ConcurrentHashMap.newKeySet();
    private Thread hiloReconexion;
    private boolean activoReconexion = false;

    public NetworkService() {
        iniciarHiloReconexion();
    }

    private void iniciarHiloReconexion() {
        activoReconexion = true;
        hiloReconexion = new Thread(() -> {
            while (activoReconexion) {
                try {
                    Thread.sleep(10000); // Intenta revivir nodos cada 10 segundos
                    for (Integer puertoMuerto : nodosMuertos) {
                        String host = hostMap.get(puertoMuerto);
                        if (host != null) {
                            try {
                                ManagedChannel channel = ManagedChannelBuilder.forAddress(host, puertoMuerto)
                                        .usePlaintext()
                                        .build();
                                PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);
                                PingRequest request = PingRequest.newBuilder().setMensajeSaludo("Ping de reconexion")
                                        .build();
                                PingResponse response = stub.ping(request);

                                if (response.getExito()) {
                                    System.out.println("NetworkService: ¡Reconexión EXITOSA con el puerto "
                                            + puertoMuerto + " que estaba muerto!");
                                    channels.put(puertoMuerto, channel);
                                    nodosMuertos.remove(puertoMuerto);
                                } else {
                                    channel.shutdown();
                                }
                            } catch (Exception e) {
                                // Sigue muerto, intentará en los próximos 10 segundos
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    ConsoleLogger.info("Log", "NetworkService: Hilo de reconexión interrumpido.");
                    activoReconexion = false;
                }
            }
        });
        hiloReconexion.start();
    }

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
        ConsoleLogger.info("Log", "NetworkService: Servidor gRPC iniciado en puerto " + port);
    }

    // Funcionalidad de ping
    public void sendPing(String host, int port) {
        hostMap.put(port, host); // Guardar host para futura reconexion si muere
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);
        PingRequest request = PingRequest.newBuilder().setMensajeSaludo("Hola desde P2P Nodo Facade").build();

        boolean conectado = false;
        while (!conectado) {
            try {
                PingResponse response = stub.ping(request);
                ConsoleLogger.info("Log", "Respuesta del otro nodo (" + port + "): " + response.getRespuesta());
                conectado = true;
            } catch (io.grpc.StatusRuntimeException e) {
                ConsoleLogger.info("Log", "El nodo destino " + port + " no está listo. Reintentando en 3 segundos...");
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
        ConsoleLogger.info("NetworkService",
                "Enviando petición de voto para '" + idAccion + "' a " + channels.size() + " nodos...");

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

                ConsoleLogger.info("NetworkService", "Respuesta de voto recibida del puerto " + puertoDestino + ": "
                        + (respuesta.getAcepta() ? "Aceptó" : "Rechazó"));

                // Si tuviéramos acceso a QuorumService aquí, le pasaríamos la respuesta
                // inmediatamente
                // Pero lo conectaremos en el Orquestador (Facade) o pasando una referencia

            } catch (Exception e) {
                ConsoleLogger.info("Log", "NetworkService: Error solicitando voto al puerto " + puertoDestino);
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
                // System.out.println("NetworkService: Error sincronizando estado con el puerto
                // " + puertoDestino + ". Puede que esté caído.");
            }
        }
    }

    // Desconecta limpiamente el canal gRPC de un nodo que se considera muerto
    public void desconectarNodo(int puerto) {
        ManagedChannel channel = channels.remove(puerto);
        if (channel != null) {
            channel.shutdown();
            nodosMuertos.add(puerto);
            ConsoleLogger.info("Log", "NetworkService: Canal cerrado y movido a nodos muertos: puerto " + puerto);
        }
    }

    public void stop() throws InterruptedException {
        activoReconexion = false;
        if (hiloReconexion != null) {
            hiloReconexion.interrupt();
        }
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
