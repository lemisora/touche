package com.pda.distributed.services;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class NetworkServiceTest {

    private NetworkService networkService;

    @Before
    public void setUp() {
        networkService = new NetworkService();
    }

    @Test
    public void testEnviarFragmento_NodoInexistente_RetornaFalse() {
        // Arrange
        String nodoFantasma = "localhost:9999"; // Un puerto que definitivamente no está escuchando
        String idArchivo = "test_uuid.txt";
        byte[] datos = "Hola gRPC".getBytes();

        // Act
        // Como no hemos hecho un 'sendPing' previo para establecer el ManagedChannel,
        // el servicio no debería encontrar el canal y debería retornar false elegantemente.
        boolean resultado = networkService.enviarFragmento(nodoFantasma, idArchivo, datos);

        // Assert
        assertFalse("Debería retornar false al intentar enviar a un canal no establecido", resultado);
    }

    @Test
    public void testEnviarFragmento_FormatoNodoInvalido_RetornaFalse() {
        // Arrange
        String nodoMalFormateado = "sololaip_sinpuerto"; // No tiene los dos puntos ':'

        // Act
        boolean resultado = networkService.enviarFragmento(nodoMalFormateado, "test.txt", new byte[]{1,2});

        // Assert
        assertFalse("Debería manejar la excepción de parseo y retornar false", resultado);
    }
}