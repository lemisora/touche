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
        System.out.println("Recibido voto para accion: " + request.getIdAccion());
        responseObserver.onNext(RespuestaVoto.newBuilder().setAcepta(true).build());
        responseObserver.onCompleted();
    }

    @Override
    public void subirFragmento(PeticionSubida request, StreamObserver<RespuestaSubida> responseObserver) {
        System.out.println("Recibida peticion de subir archivo: " + request.getIdArchivo());
        responseObserver.onNext(RespuestaSubida.newBuilder().setExito(true).build());
        responseObserver.onCompleted();
    }
}
