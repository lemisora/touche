package com.pda.distributed.services;

import com.pda.distributed.network.grpc.PdaServiceGrpc;
import com.pda.distributed.network.grpc.PeticionEstado;
import com.pda.distributed.network.grpc.PeticionSubida;
import com.pda.distributed.network.grpc.PeticionVoto;
import com.pda.distributed.network.grpc.PingRequest;
import com.pda.distributed.network.grpc.PingResponse;
import com.pda.distributed.network.grpc.RespuestaEstado;
import com.pda.distributed.network.grpc.RespuestaSubida;
import com.pda.distributed.network.grpc.RespuestaVoto;
import io.grpc.stub.StreamObserver;

// Implementación de los servicios gRPC
public class PdaServiceGrpcImpl extends PdaServiceGrpc.PdaServiceImplBase {

    private final QuorumService quorumService;

    // Constructor que recibe el QuorumService
    public PdaServiceGrpcImpl(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        System.out.println("Recibido ping: " + request.getMensajeSaludo());
        PingResponse response = PingResponse.newBuilder().setExito(true).setRespuesta("Pong").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void sincronizarEstado(PeticionEstado request, StreamObserver<RespuestaEstado> responseObserver) {
        System.out.println("Recibido estado: " + request.getDatosEstado());
        responseObserver.onNext(RespuestaEstado.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void votar(PeticionVoto request, StreamObserver<RespuestaVoto> responseObserver) {
        String idAccion = request.getIdAccion();
        System.out.println("GRPC: Recibido voto para acción: " + idAccion);

        // Simular que el voto siempre es a favor (para la práctica)
        boolean votoAFavor = true;

        if (quorumService != null) {
            quorumService.recibirVoto(idAccion, votoAFavor);
        } else {
            System.err.println("GRPC: QuorumService no inicializado!");
        }

        responseObserver.onNext(RespuestaVoto.newBuilder().setAcepta(votoAFavor).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subirFragmento(PeticionSubida request, StreamObserver<RespuestaSubida> responseObserver) {
        System.out.println("Recibida peticion de subir archivo: " + request.getIdArchivo());
        responseObserver.onNext(RespuestaSubida.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }
}
