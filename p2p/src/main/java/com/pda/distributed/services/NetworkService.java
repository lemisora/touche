package com.pda.distributed.services;

import com.pda.distributed.core.Nodo;
// Importaciones de gRPC
import com.pda.distributed.network.grpc.*;
import com.pda.distributed.utils.ConsoleLogger;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkService {

    private final Nodo nodoLocal;
    private QuorumService quorumService;
    private DiscoveryService discoveryService;
    
    private Server grpcServer;
    // Mapa para mantener las conexiones abiertas hacia otros nodos (K: "IP:Puerto", V: Canal gRPC)
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

    /**
     * Levanta el servidor gRPC en un puerto disponible.
     * @return El puerto en el que finalmente se levantó el servidor.
     */
    public int startDirectServer() throws IOException {
        int puertoBase = 50000;
        int puertoMaximo = 50050;

        for (int puertoPrueba = puertoBase; puertoPrueba <= puertoMaximo; puertoPrueba++) {
            try {
                String direccionTemporal = nodoLocal.getIp() + ":" + puertoPrueba;
                grpcServer = ServerBuilder.forPort(puertoPrueba)
                        .addService(new NodeServiceGrpcImpl(quorumService, direccionTemporal))
                        .build()
                        .start();

                if (discoveryService != null) {
                    discoveryService.iniciar(puertoPrueba);
                }

                return puertoPrueba;
            } catch (IOException e) {
                ConsoleLogger.error(nodoLocal.getName(), "No se pudo iniciar el servidor gRPC en el puerto "
                        + puertoPrueba + ". Intentando con el siguiente" );
            }
        }
        throw new IOException("No se encontró ningún puerto libre en el rango "
                + "[" + puertoBase + "-" + puertoMaximo + "]");
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

            // .usePlaintext() para que no intente usar HTTPS/SSL en las pruebas locales.
            ManagedChannel channel = ManagedChannelBuilder.forAddress(ip, port)
                    .usePlaintext()
                    .build();

            // Crear un stub (cliente) bloqueante.
            // Bloqueante significa que el código se pausará aquí hasta que el Semilla nos responda.
            PdaServiceGrpc.PdaServiceBlockingStub stub = PdaServiceGrpc.newBlockingStub(channel);

            // Construir el JoinRequest con 'miDireccion' y 'miId'.
            JoinRequest request = JoinRequest.newBuilder()
                    .setDireccionNodo(miDireccion)
                    .setIdNodo(miId)
                    .build();

            ConsoleLogger.info("Network", "Enviando JoinRequest a la semilla: " + seedAddress);

            // Enviar la petición y recibir el JoinResponse.
            JoinResponse response = stub.join(request);

            // Si response.getAceptado() es true, llamar a nodoLocal.actualizarEstadoDesdeSemilla(...).
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
            // Si el nodo semilla está apagado o hay un error de red, gRPC lanzará una excepción aquí.
            ConsoleLogger.error("Network", "No se pudo conectar al nodo semilla " + seedAddress + ". ¿Está encendido?");
            return false;
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

    public void stop() throws InterruptedException {
        if (this.grpcServer != null) {
            grpcServer.shutdown();
        }

        for (ManagedChannel channel : activeChannels.values()) {
            channel.shutdown();
        }
    }

    // Getters y setters
    public String getMiDireccion(){
        return this.nodoLocal.getNodeAddress();
    }

    public int getMiId() {
        return this.nodoLocal.getId();
    }

    public boolean estaConectado(String directorioDestino) {
        return activeChannels.containsKey(directorioDestino);
    }
}