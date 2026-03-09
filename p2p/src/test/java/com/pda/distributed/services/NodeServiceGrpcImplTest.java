//package com.pda.distributed.services;
//
//import com.pda.distributed.network.grpc.JoinRequest;
//import com.pda.distributed.network.grpc.JoinResponse;
//import io.grpc.stub.StreamObserver;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.util.concurrent.atomic.AtomicReference;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//public class NodeServiceGrpcImplTest {
//
//    private NodeServiceGrpcImpl grpcService;
//    private QuorumService quorumService;
//
//    @Before
//    public void setUp() {
//        // Usamos la lógica real del QuorumService para esta prueba de integración
//        networkService = new NetworkService();
//        quorumService = new QuorumService(networkService);
//        grpcService = new NodeServiceGrpcImpl(quorumService, "127.0.0.1:50051");
//    }
//
//    @Test
//    public void testJoin_GeneraRespuestaCorrecta() {
//        // Arrange: Preparamos la petición
//        JoinRequest request = JoinRequest.newBuilder()
//                .setDireccionNodo("127.0.0.1:50052")
//                .setIdNodo(2)
//                .build();
//
//        // Creamos un observador falso para capturar la respuesta del servidor
//        AtomicReference<JoinResponse> respuestaCapturada = new AtomicReference<>();
//        StreamObserver<JoinResponse> mockObserver = new StreamObserver<JoinResponse>() {
//            @Override
//            public void onNext(JoinResponse value) {
//                respuestaCapturada.set(value);
//            }
//
//            @Override
//            public void onError(Throwable t) {}
//
//            @Override
//            public void onCompleted() {}
//        };
//
//        // Act: Llamamos al método como si fuéramos un cliente gRPC
//        grpcService.join(request, mockObserver);
//
//        // Assert: Validamos que la respuesta capturada contenga los datos correctos
//        JoinResponse response = respuestaCapturada.get();
//        assertTrue("La petición debe ser aceptada", response.getAceptado());
//        assertEquals("Debe asignarse al RING_A por ser el primero", "RING_A", response.getAnilloAsignado());
//    }
//}
