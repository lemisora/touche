package com.pda.distributed.services;

import com.google.protobuf.ByteString;
import com.pda.distributed.core.Nodo;
// Importaciones de gRPC
import com.pda.distributed.network.grpc.*;
import com.pda.distributed.utils.ConsoleLogger;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;

import com.pda.distributed.network.grpc.PingRequest;
import com.pda.distributed.network.grpc.PingResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkService {
    private final Nodo nodoLocal;
    private QuorumService quorumService;
    private DiscoveryService discoveryService;
    private StorageCoordinator storageCoordinator;
    private StateSyncService stateSyncService;

    private ScheduledExecutorService heartbeatTimer;

    private Server grpcServer;
    // Mapa para mantener las conexiones abiertas hacia otros nodos (K: "IP:Puerto",
    // V: Canal gRPC)
    private final Map<String, ManagedChannel> activeChannels;

    public NetworkService(Nodo nodoLocal) {
        this.nodoLocal = nodoLocal;
        this.activeChannels = new ConcurrentHashMap<>();
    }

    public void setQuorumService(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    public void setDiscoveryService(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public void setStorageCoordinator(StorageCoordinator storageCoordinator) {
        this.storageCoordinator = storageCoordinator;
    }

    public void setStateSyncService(StateSyncService stateSyncService) {
        this.stateSyncService = stateSyncService;
    }

    /**
     * Levanta el servidor gRPC en un puerto disponible.
     *
     * @return El puerto en el que finalmente se levantó el servidor.
     */
    public int startDirectServer() throws IOException {
        int puertoBase = 50000;
        int puertoMaximo = 50050;

        for (int puertoPrueba = puertoBase; puertoPrueba <= puertoMaximo; puertoPrueba++) {
            try {
                String direccionTemporal = nodoLocal.getIp() + ":" + puertoPrueba;
                NodeServiceGrpcImpl grpcService = new NodeServiceGrpcImpl(quorumService, storageCoordinator,
                        direccionTemporal);
                grpcService.setStateSyncService(stateSyncService);
                grpcService.setNetworkService(this);

                grpcServer = ServerBuilder.forPort(puertoPrueba)
                        .addService(grpcService)
                        .build()
                        .start();

                if (discoveryService != null) {
                    discoveryService.iniciar(puertoPrueba);
                }

                return puertoPrueba;
            } catch (IOException e) {
                ConsoleLogger.error(nodoLocal.getName(), "No se pudo iniciar el servidor gRPC en el puerto "
                        + puertoPrueba + ". Intentando con el siguiente");
            }
        }
        throw new IOException("No se encontró ningún puerto libre en el rango "
                + "[" + puertoBase + "-" + puertoMaximo + "]");
    }

    /**
     * Enciende el motor de latidos. Se ejecutará en segundo plano cada 5 segundos.
     */
    public void iniciarHeartbeats() {
        if (heartbeatTimer == null) {
            heartbeatTimer = Executors.newSingleThreadScheduledExecutor();
            // Inicia en 5 segs, y se repite cada 5 segs
            heartbeatTimer.scheduleAtFixedRate(this::revisarSaludNodos, 5, 5, TimeUnit.SECONDS);
            ConsoleLogger.info("Network", "Servicio de Heartbeats (Latidos) iniciado.");
        }
    }

    /**
     * Revisa todos los canales activos. Si alguno no responde, lo elimina.
     */
    private void revisarSaludNodos() {
        List<String> nodosCaidos = new ArrayList<>();

        // Preguntamos a todos si están vivos
        for (String targetAddress : activeChannels.keySet()) {
            boolean isAlive = enviarPing(targetAddress);
            if (!isAlive) {
                nodosCaidos.add(targetAddress);
            }
        }

        // Limpiamos a los caídos
        for (String caido : nodosCaidos) {
            ConsoleLogger.advertencia("Network", "¡Nodo desconectado o muerto detectado!: " + caido);

            // Cerramos la conexión de red
            ManagedChannel canalMuerto = activeChannels.remove(caido);
            if (canalMuerto != null) {
                canalMuerto.shutdown();
            }

            // Le avisamos al QuorumService para que lo borre del mapa y decida qué hacer
            if (quorumService != null) {
                quorumService.removerNodoCaido(caido);
            }
        }
    }

    /**
     * Envía un ping gRPC rápido. Tiene un "timeout" de 2 segundos para no quedarse colgado.
     */
    private boolean enviarPing(String targetAddress) {
        try {
            ManagedChannel channel = activeChannels.get(targetAddress);

            // Usamos .withDeadlineAfter() para que si el nodo se apagó a la fuerza,
            // no nos quedemos esperando infinitamente una respuesta.
            PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(10, TimeUnit.SECONDS);

            PingRequest request = PingRequest.newBuilder()
                    .setDireccionNodo(this.getMiDireccion())
                    .setIdNodo(this.getMiId())
                    .build();

            PingResponse response = stub.heartbeat(request);
            return response.getExito();

        } catch (Exception e) {
            // Cualquier error (Timeout, conexión rechazada, etc) significa que el nodo murió
            return false;
        }
    }

    /**
     * Actúa como cliente gRPC para pedir unirse a un nodo semilla.
     */
    public boolean joinNetwork(String seedAddress, String miDireccion, int miId) {
        try {
            // Parsear el seedAddress para obtener IP y Puerto del semilla.
            String[] ip_parts = seedAddress.split(":");
            String ip = ip_parts[0];
            int port = Integer.parseInt(ip_parts[1]);

            // Normalizar la IP: Si alguien nos manda un 127.0.0.1, lo cambiamos a nuestra IP real
            if (seedAddress.startsWith("127.0.0.1:")) {
                seedAddress = seedAddress.replace("127.0.0.1", nodoLocal.getIp());
            }

            // Prevenir la esquizofrenia de red: No conectarnos a nosotros mismos
            if (seedAddress.equals(miDireccion)) {
                return false; // Nos ignoramos silenciosamente
            }

            // Prevenir duplicados: Si ya tenemos esta dirección normalizada, no hacemos nada
            if (activeChannels.containsKey(seedAddress)) {
                return true;
            }

            // .usePlaintext() para que no intente usar HTTPS/SSL en las pruebas locales.
            ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port)
                    .usePlaintext()
                    .build();

            // Crear un stub (cliente) bloqueante.
            // Bloqueante significa que el código se pausará aquí hasta que el Semilla nos
            // responda.
            PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);

            // Construir el JoinRequest con 'miDireccion' y 'miId'.
            JoinRequest request = JoinRequest.newBuilder()
                    .setDireccionNodo(miDireccion)
                    .setIdNodo(miId)
                    .build();

            ConsoleLogger.info("Network", "Enviando JoinRequest a la semilla: " + seedAddress);

            // Enviar la petición y recibir el JoinResponse.
            JoinResponse response = stub.join(request);

            // Si response.getAceptado() es true, llamar a
            // nodoLocal.actualizarEstadoDesdeSemilla(...).
            if (response.getAceptado()) {
                ConsoleLogger.exito("Network", "¡Unión aceptada! " + response.getMensaje());

                // Actualizamos nuestro estado interno y rol
                nodoLocal.actualizarEstadoDesdeSemilla(response.getAnilloAsignado());

                // Guardamos el canal abierto para futuras comunicaciones con este nodo
                activeChannels.put(seedAddress, channel);

                // Retornar el resultado de la conexión.
                return true;
            } else {
                ConsoleLogger.advertencia("Network", "El nodo semilla rechazó nuestra petición.");
                channel.shutdown(); // Cerramos el canal porque no fuimos aceptados
                return false;
            }

        } catch (Exception e) {
            // Si el nodo semilla está apagado o hay un error de red, gRPC lanzará una
            // excepción aquí.
            ConsoleLogger.error("Network", "No se pudo conectar al nodo semilla " + seedAddress + ". ¿Está encendido?");
            return false;
        }
    }

    public void enviarEstado(String direccionDestino, String jsonEstado) {
        try {
            io.grpc.ManagedChannel channel = this.activeChannels.get(direccionDestino);
            if (channel == null) {
                com.pda.distributed.utils.ConsoleLogger.advertencia("Network",
                        "Canal no encontrado para " + direccionDestino);
                return;
            }

            com.pda.distributed.network.grpc.PdaServiceGrpc.PdaServiceBlockingStub stub = com.pda.distributed.network.grpc.PdaServiceGrpc
                    .newBlockingStub(channel);

            com.pda.distributed.network.grpc.PeticionEstado request = com.pda.distributed.network.grpc.PeticionEstado
                    .newBuilder()
                    .setDireccionOrigen("NetworkService")
                    .setDatosEstado(jsonEstado)
                    .build();

            stub.sincronizarEstado(request);
        } catch (Exception e) {
            com.pda.distributed.utils.ConsoleLogger.error("Network",
                    "Error al enviar estado a " + direccionDestino + ": " + e.getMessage());
        }
    }

    /**
     * Envía una petición de voto a un nodo remoto usando gRPC.
     */
    public boolean enviarPeticionVoto(String direccionDestino, int miId) {
        // Obviamente siempre votamos por nosotros mismos sin usar la red
        if (direccionDestino.equals(this.getMiDireccion())) {
            return true;
        }

        try {
            // Reutilizamos la conexión si ya la tenemos, si no, creamos una nueva
            ManagedChannel channel = activeChannels.get(direccionDestino);
            if (channel == null) {
                String[] partes = direccionDestino.split(":");
                channel = ManagedChannelBuilder.forAddress(partes[0], Integer.parseInt(partes[1]))
                        .usePlaintext()
                        .build();
                activeChannels.put(direccionDestino, channel);
            }

            // Creamos el cliente bloqueante
            PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);

            // Construimos la petición con las clases generadas por Protobuf
            PeticionVoto request = PeticionVoto.newBuilder()
                    .setDireccionCandidato(this.getMiDireccion())
                    .setIdCandidato(miId)
                    .build();

            // Enviamos y esperamos respuesta
            RespuestaVoto response = stub.votar(request);

            return response.getAcepta();

        } catch (Exception e) {
            // Si el otro nodo se apagó o desconectó, asumimos que no nos dio su voto
            ConsoleLogger.error("Network", "Error pidiendo voto a " + direccionDestino + " (Nodo inalcanzable)");
            return false;
        }
    }

    /**
     * Abre y registra un nuevo canal hacia un nodo, en caso de que no exista.
     */
    public void registrarCanal(String direccionDestino) {
        if (!activeChannels.containsKey(direccionDestino)) {
            try {
                String[] partes = direccionDestino.split(":");
                ManagedChannel channel = ManagedChannelBuilder.forAddress(partes[0], Integer.parseInt(partes[1]))
                        .usePlaintext()
                        .build();
                activeChannels.put(direccionDestino, channel);
                ConsoleLogger.info("Network", "Nuevo canal registrado hacia: " + direccionDestino);
            } catch (Exception e) {
                ConsoleLogger.error("Network",
                        "No se pudo registrar canal hacia " + direccionDestino + ": " + e.getMessage());
            }
        }
    }

    /**
     * Registra un canal de comunicación con un nodo sin pedirle unirse a su red.
     * Útil cuando nosotros ya somos parte de una red y descubrimos a un recién llegado.
     */
    public void registrarCanalSilencioso(String targetAddress) {
        if (!activeChannels.containsKey(targetAddress)) {
            String[] partes = targetAddress.split(":");
            io.grpc.ManagedChannel channel = io.grpc.ManagedChannelBuilder.forAddress(partes[0], Integer.parseInt(partes[1]))
                    .usePlaintext()
                    .build();
            activeChannels.put(targetAddress, channel);
            ConsoleLogger.info("Network", "Canal de comunicación abierto con: " + targetAddress);
        }
    }

    public void stop() throws InterruptedException {
        if (this.grpcServer != null) {
            grpcServer.shutdown();
        }

        if (heartbeatTimer != null) {
            heartbeatTimer.shutdownNow();
        }
        for (ManagedChannel channel : activeChannels.values()) {
            channel.shutdown();
        }
    }

    /**
     * Retorna una lista con las direcciones "IP:Puerto" de todos los nodos
     * con los que tenemos un canal de comunicación abierto.
     */
    public List<String> getNodosConectados() {
        // Obtenemos todas las claves (IPs) del mapa de canales activos
        return new ArrayList<>(activeChannels.keySet());
    }

    /**
     * Envía un fragmento de archivo a un nodo específico mediante gRPC.
     */
    public boolean enviarFragmento(String targetAddress, String fileId, int chunkIndex, byte[] chunkData,
            int totalExpected) {
        try {
            // 1. Obtener o crear el canal hacia el nodo destino
            ManagedChannel channel = activeChannels.get(targetAddress);
            if (channel == null) {
                String[] partes = targetAddress.split(":");
                channel = ManagedChannelBuilder.forAddress(partes[0], Integer.parseInt(partes[1]))
                        .usePlaintext()
                        .build();
                activeChannels.put(targetAddress, channel);
            }

            // 2. Crear el cliente gRPC bloqueante
            PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);

            // 3. Convertir el byte[] de Java al ByteString que usa Protobuf (Súper
            // importante)
            ByteString protobufBytes = ByteString.copyFrom(chunkData);

            // 4. Construir el mensaje PeticionSubida
            PeticionSubida request = PeticionSubida.newBuilder()
                    .setIdArchivo(fileId)
                    .setIndiceFragmento(chunkIndex)
                    .setTotalFragmentos(totalExpected)
                    .setFragmento(protobufBytes)
                    .build();

            // 5. Enviar el mensaje por la red y esperar la respuesta
            RespuestaSubida response = stub.subirFragmento(request);

            if (response.getExito()) {
                ConsoleLogger.info("Network", "Fragmento " + chunkIndex + " enviado correctamente a " + targetAddress);
                return true;
            } else {
                ConsoleLogger.error("Network", "El nodo " + targetAddress + " rechazó el fragmento " + chunkIndex);
                return false;
            }

        } catch (Exception e) {
            ConsoleLogger.error("Network",
                    "Fallo al enviar fragmento a " + targetAddress + " (El nodo podría estar caído).");
            return false;
        }
    }

    // Getters y setters
    public String getMiDireccion() {
        return this.nodoLocal.getNodeAddress();
    }

    public int getMiId() {
        return this.nodoLocal.getId();
    }

    public boolean estaConectado(String directorioDestino) {
        return activeChannels.containsKey(directorioDestino);
    }

    public Nodo getNodoLocal() {
        return nodoLocal;
    }
}
