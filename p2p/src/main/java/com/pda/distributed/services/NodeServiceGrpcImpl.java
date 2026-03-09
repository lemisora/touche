package com.pda.distributed.services;

import com.pda.distributed.network.grpc.*; // Importar clases generadas del .proto
import com.pda.distributed.utils.ConsoleLogger;
import io.grpc.stub.StreamObserver;


public class NodeServiceGrpcImpl extends PdaServiceGrpc.PdaServiceImplBase {

    private final QuorumService quorumService;
    private final String miDireccion;

    public NodeServiceGrpcImpl(QuorumService quorumService, String miDireccion) {
        this.quorumService = quorumService;
        this.miDireccion = miDireccion;
    }

    @Override
    public void join(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        // TODO: 1. Extraer request.getDireccionNodo() y request.getIdNodo().
        // TODO: 2. Llamar a quorumManager.evaluarIngresoNuevoNodo(...) y guardar el resultado.
        String anilloAsignado = quorumService.evaluarIngresoNuevoNodo(
                request.getDireccionNodo(),
                request.getIdNodo()
        );
        // TODO: 3. Construir el JoinResponse con el anillo asignado.
        JoinResponse response = JoinResponse.newBuilder()
                .setAnilloAsignado(anilloAsignado)
                .setAceptado(true)
                .setMensaje("Aceptado")
                .build();
        // TODO: 4. Enviar la respuesta con responseObserver.onNext(...) y onCompleted().
        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
                request.getIdCandidato()
        );
        // TODO: 3. Devolver RespuestaVoto con el booleano resultante.
        responseObserver.onNext(
                RespuestaVoto.newBuilder()
                        .setAcepta(votoValido)
                        .build()
        );

        responseObserver.onCompleted();
    }

    // Los métodos de archivos y sincronización los puedes dejar vacíos por ahora
    @Override
    public void sincronizarEstado(PeticionEstado request, StreamObserver<RespuestaEstado> responseObserver) {
        responseObserver.onNext(RespuestaEstado.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subirFragmento(PeticionSubida request, StreamObserver<RespuestaSubida> responseObserver) {
        responseObserver.onNext(RespuestaSubida.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }
}