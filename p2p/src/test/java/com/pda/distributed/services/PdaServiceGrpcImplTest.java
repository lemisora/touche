package com.pda.distributed.services;

import com.pda.distributed.network.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class PdaServiceGrpcImplTest {

    private PdaServiceGrpcImpl grpcService;
    private DummyStorageCoordinator dummyCoordinator;

    // Nuestro coordinador espía para ver si el servidor gRPC le pasa el archivo
    class DummyStorageCoordinator extends StorageCoordinator {
        AtomicBoolean fragmentoRecibido = new AtomicBoolean(false);

        @Override
        public void procesarFragmentoEntrante(String idArchivo, byte[] datosFragmento) {
            fragmentoRecibido.set(true);
        }
    }

    // Un "falso" emisor de red para atrapar la respuesta que el servidor nos intenta enviar
    class TestStreamObserver<T> implements StreamObserver<T> {
        T respuestaCapturada;
        boolean finalizado = false;

        @Override
        public void onNext(T value) { this.respuestaCapturada = value; }

        @Override
        public void onError(Throwable t) { }

        @Override
        public void onCompleted() { this.finalizado = true; }
    }

    @Before
    public void setUp() {
        dummyCoordinator = new DummyStorageCoordinator();

        // Inyectamos null en Quorum y StateSync porque en este test solo nos importa subirFragmento
        grpcService = new PdaServiceGrpcImpl(null, null, dummyCoordinator);
    }

    @Test
    public void testSubirFragmento_LlamaAlCoordinator_YRetornaExito() {
        // Arrange: Simulamos un mensaje PeticionSubida llegando desde la red
        PeticionSubida peticionRed = PeticionSubida.newBuilder()
                .setIdArchivo("archivo_de_red.txt")
                .setFragmento(com.google.protobuf.ByteString.copyFromUtf8("hola mundo p2p"))
                .build();

        TestStreamObserver<RespuestaSubida> observadorFalso = new TestStreamObserver<>();

        // Act: Llamamos al método como si fuéramos el motor interno de gRPC
        grpcService.subirFragmento(peticionRed, observadorFalso);

        // Assert:
        // 1. Validamos que el gRPC cerró la conexión correctamente (onCompleted)
        assertTrue("El servidor gRPC debió llamar a onCompleted", observadorFalso.finalizado);

        // 2. Validamos que el servidor respondió con un 'true' (éxito)
        assertTrue("La respuesta enviada por la red debió tener exito = true", observadorFalso.respuestaCapturada.getExito());

        // 3. Validamos que el archivo llegó a tus dominios (el StorageCoordinator)
        assertTrue("El StorageCoordinator debió recibir el fragmento procesado", dummyCoordinator.fragmentoRecibido.get());
    }
}