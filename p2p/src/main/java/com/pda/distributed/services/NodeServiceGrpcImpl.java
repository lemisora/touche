package com.pda.distributed.services;

import com.pda.distributed.network.grpc.*; // Importar clases generadas del .proto
import com.pda.distributed.utils.ConsoleLogger;
import io.grpc.stub.StreamObserver;
import com.pda.distributed.services.StorageCoordinator;

public class NodeServiceGrpcImpl extends PdaServiceGrpc.PdaServiceImplBase {

    private final QuorumService quorumService;
    private final StorageCoordinator storageCoordinator;
    private StateSyncService stateSyncService;
    private NetworkService networkService;
    private final String miDireccion;

    public NodeServiceGrpcImpl(QuorumService quorumService, StorageCoordinator storageCoordinator, String miDireccion) {
        this.quorumService = quorumService;
        this.storageCoordinator = storageCoordinator;
        this.miDireccion = miDireccion;
    }

    public void setStateSyncService(StateSyncService stateSyncService) {
        this.stateSyncService = stateSyncService;
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        // TODO: 1. Extraer request.getDireccionNodo() y request.getIdNodo().
        // TODO: 2. Llamar a quorumManager.evaluarIngresoNuevoNodo(...) y guardar el
        // resultado.
        String anilloAsignado = quorumService.evaluarIngresoNuevoNodo(
                request.getDireccionNodo(),
                request.getIdNodo());
        // TODO: 3. Construir el JoinResponse con el anillo asignado.
        JoinResponse response = JoinResponse.newBuilder()
                .setAnilloAsignado(anilloAsignado)
                .setAceptado(true)
                .setMensaje("Aceptado")
                .build();
        // TODO: 4. Enviar la respuesta con responseObserver.onNext(...) y
        // onCompleted().
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // Aseguramos que nuestro NetworkService registre este nuevo canal
        // para que pueda recibir los broadcasts de estado.
        if (networkService != null) {
            networkService.registrarCanal(request.getDireccionNodo());
        }

        // Al aceptar a un nuevo nodo, enviamos una actualización del directorio a la
        // red
        // para que el nodo recién integrado conozca el estado de los archivos.
        if (stateSyncService != null) {
            stateSyncService.broadcastMapUpdate();
        }
    }

    @Override
    public void heartbeat(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        // TODO: Construir y devolver un PingResponse simple.
        responseObserver.onNext(PingResponse.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void votar(PeticionVoto request, StreamObserver<RespuestaVoto> responseObserver) {
        // TODO: 1. Extraer el request.getIdCandidato().
        // TODO: 2. Preguntar a quorumService.evaluarVotoBully(...).
        boolean votoValido = quorumService.evaluarVotoBully(
                request.getIdCandidato());
        // TODO: 3. Devolver RespuestaVoto con el booleano resultante.
        responseObserver.onNext(
                RespuestaVoto.newBuilder()
                        .setAcepta(votoValido)
                        .build());

        responseObserver.onCompleted();
    }

    @Override
    public void sincronizarEstado(PeticionEstado request, StreamObserver<RespuestaEstado> responseObserver) {
        if (stateSyncService != null) {
            stateSyncService.recibirEstado(request.getDatosEstado(), request.getDireccionOrigen());
        } else {
            ConsoleLogger.advertencia("gRPC", "StateSyncService no inicializado. Se ignora la sincronización.");
        }

        responseObserver.onNext(RespuestaEstado.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subirFragmento(PeticionSubida request, StreamObserver<RespuestaSubida> responseObserver) {
        try {
            // 1. Extraer los metadatos de la petición
            String fileId = request.getIdArchivo();
            int chunkIndex = request.getIndiceFragmento();
            int totalExpected = request.getTotalFragmentos();

            // 2. ¡La magia inversa! Convertir de Protobuf ByteString al byte[] de Java
            byte[] data = request.getFragmento().toByteArray();

            // 3. Pasarle el fragmento al StorageCoordinator para que lo guarde físicamente
            if (storageCoordinator != null) {
                storageCoordinator.handleIncomingChunk(fileId, chunkIndex, data, totalExpected);

                // 4. Responderle al nodo remitente que todo salió bien
                responseObserver.onNext(RespuestaSubida.newBuilder().setExito(true).build());
            } else {
                ConsoleLogger.advertencia("gRPC", "StorageCoordinator apagado. Fragmento rechazado.");
                responseObserver.onNext(RespuestaSubida.newBuilder().setExito(false).build());
            }

        } catch (Exception e) {
            ConsoleLogger.error("gRPC", "Error al procesar el fragmento entrante: " + e.getMessage());
            responseObserver.onNext(RespuestaSubida.newBuilder().setExito(false).build());
        } finally {
            // 5. Siempre cerrar la conexión
            responseObserver.onCompleted();
        }
    }
}
