package com.pda.distributed.services;

import com.pda.distributed.core.Nodo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class NetworkServiceTest {

    private NetworkService networkService;
    private Nodo nodoFalso;

    @Before
    public void setUp() {
        nodoFalso = new Nodo(1, "127.0.0.1", "NodoTest");
        networkService = new NetworkService(nodoFalso);
    }

    @After
    public void tearDown() throws InterruptedException {
        // Apagamos el servidor después de la prueba para no dejar puertos colgados
        networkService.stop();
    }

    @Test
    public void testStartDirectServer_EncuentraPuertoLibre() throws IOException {
        // Act
        int puertoAsignado = networkService.startDirectServer();

        // Assert
        assertTrue("El servidor debe levantar en un puerto mayor a 0", puertoAsignado > 0);
        System.out.println("Test: Servidor gRPC levantado en el puerto: " + puertoAsignado);
    }
}