package com.pda.distributed.network;

import com.pda.distributed.network.grpc.P2PNetworkServiceGrpc;
import com.pda.distributed.network.grpc.PingRequest;
import com.pda.distributed.network.grpc.PingResponse;

import io.grpc.stub.StreamObserver;

public class NetworkServiceImpl extends P2PNetworkServiceGrpc.P2PNetworkServiceImplBase {

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        System.out.println("Recibido ping: " + request.getMensajeSaludo());
        PingResponse response = PingResponse.newBuilder().setExito(true).setRespuesta("Pong").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}