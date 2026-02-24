package com.pda.distributed.services;

import com.pda.distributed.network.grpc.*;
import io.grpc.stub.StreamObserver;

// Esta clase es el "Servidor" que escucha lo que otros nodos le envían
public class PdaServiceGrpcImpl extends PdaServiceGrpc.PdaServiceImplBase {

    // Dependencias inyectadas para derivar el trabajo
    private final QuorumService quorumService;
    private final StateSyncService stateSyncService;
    private final StorageCoordinator storageCoordinator;

    public PdaServiceGrpcImpl(QuorumService quorumService, StateSyncService stateSyncService, StorageCoordinator storageCoordinator) {
        this.quorumService = quorumService;
        this.stateSyncService = stateSyncService;
        this.storageCoordinator = storageCoordinator;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        System.out.println("[gRPC Servidor] Ping recibido: " + request.getMensajeSaludo());

        PingResponse response = PingResponse.newBuilder()
                .setExito(true)
                .setRespuesta("¡Hola! Ping recibido correctamente por el nodo P2P.")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sincronizarEstado(PeticionEstado request, StreamObserver<RespuestaEstado> responseObserver) {
        boolean exito = true;
        if (stateSyncService != null) {
            // Aquí el StateSyncService procesará el JSON/String que llegó de otro nodo
            // stateSyncService.procesarEstadoEntrante(request.getDatosEstado());
        }

        RespuestaEstado response = RespuestaEstado.newBuilder().setExito(exito).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void votar(PeticionVoto request, StreamObserver<RespuestaVoto> responseObserver) {
        System.out.println("[gRPC Servidor] Petición de voto recibida para la acción: " + request.getIdAccion());

        boolean acepta = true; // Por defecto acepta, a menos que el QuorumService diga lo contrario
        if (quorumService != null) {
            // acepta = quorumService.evaluarVoto(request.getIdAccion());
        }

        RespuestaVoto response = RespuestaVoto.newBuilder().setAcepta(acepta).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void subirFragmento(PeticionSubida request, StreamObserver<RespuestaSubida> responseObserver) {
        String idArchivo = request.getIdArchivo();
        byte[] datos = request.getFragmento().toByteArray();

        System.out.println("[gRPC Servidor] Recibiendo fragmento por la red -> Archivo: " + idArchivo + " | Tamaño: " + datos.length + " bytes");

        boolean exito = true;
        try {
            if (storageCoordinator != null) {
                // ¡AQUÍ SE CIERRA EL CICLO! Le pasamos el archivo a tu coordinador para que lo guarde en disco
                storageCoordinator.procesarFragmentoEntrante(idArchivo, datos);
            } else {
                System.err.println("[gRPC Servidor] Error: StorageCoordinator no está conectado.");
                exito = false;
            }
        } catch (Exception e) {
            System.err.println("[gRPC Servidor] Error crítico procesando fragmento entrante: " + e.getMessage());
            exito = false;
        }

        // Construimos la respuesta diciendo si logramos guardarlo o no
        RespuestaSubida response = RespuestaSubida.newBuilder().setExito(exito).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}