package com.pda.distributed.services;

import com.pda.distributed.core.Nodo;
import com.pda.distributed.utils.ConsoleLogger;
// Importaciones de gRPC
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkService {

    private final Nodo nodoLocal;
    private QuorumService quorumService;
    
    private Server grpcServer;
    // Mapa para mantener las conexiones abiertas hacia otros nodos (K: "IP:Puerto", V: Canal gRPC)
    private final Map<String, ManagedChannel> activeChannels;

    public NetworkService(Nodo nodoLocal) {
        this.nodoLocal = nodoLocal;
        this.activeChannels = new ConcurrentHashMap<>();
    }

    public void setQuorumManager(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    /**
     * Levanta el servidor gRPC en un puerto disponible.
     * @return El puerto en el que finalmente se levantó el servidor.
     */
    public int startDirectServer() throws IOException {
        // TODO: 1. Crear un ServerBuilder (puedes empezar intentando el puerto 50051).
        // TODO: 2. Si el puerto está ocupado, intentar con el siguiente (ej. 50052, 50053).
        // TODO: 3. Añadir el servicio pasándole tu NodeServiceGrpcImpl.
        // TODO: 4. Hacer server.start().
        // TODO: 5. Retornar el puerto que funcionó.
        return 50051; // Placeholder
    }

    /**
     * Actúa como cliente gRPC para pedir unirse a un nodo semilla.
     */
    public boolean joinNetwork(String seedAddress, String miDireccion, int miId) {
        // TODO: 1. Parsear el seedAddress para obtener IP y Puerto del semilla.
        // TODO: 2. Crear un ManagedChannel hacia esa IP y Puerto usando ManagedChannelBuilder.
        // TODO: 3. Crear un stub (cliente) bloqueante de tu PdaServiceGrpc.
        // TODO: 4. Construir el JoinRequest con 'miDireccion' y 'miId'.
        // TODO: 5. Enviar la petición y recibir el JoinResponse.
        // TODO: 6. Si response.getAceptado() es true, llamar a nodoLocal.actualizarEstadoDesdeSemilla(...).
        // TODO: 7. Retornar el resultado de la conexión.
        return false; // Placeholder
    }

    public void stop() throws InterruptedException {
        // TODO: 1. Apagar el grpcServer si no es nulo (server.shutdown()).
        // TODO: 2. Iterar sobre activeChannels y cerrar todos los canales (channel.shutdown()).
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