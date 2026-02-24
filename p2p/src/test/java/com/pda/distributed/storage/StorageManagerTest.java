package com.pda.distributed.storage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.junit.Assert.*;

public class StorageManagerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private StorageManager storageManager;

    @Before
    public void setUp() throws IOException {
        // En lugar de ensuciar "./archivos", usamos la carpeta efímera de JUnit
        String rutaTemporal = folder.newFolder("pruebas_almacenamiento").getAbsolutePath();
        storageManager = new StorageManager(rutaTemporal);
    }

    @Test
    public void testGuardarYLeerFragmento_Exito() {
        // Arrange
        String nombreFragmento = "documento_test.pdf.part1";
        byte[] datosOriginales = "Hola, esto es un chunk de prueba de sistema distribuido".getBytes();

        // Act - Guardamos
        boolean resultadoGuardado = storageManager.guardarFragmento(nombreFragmento, datosOriginales);

        // Assert - Verificamos que diga true
        assertTrue("El StorageManager debió retornar true al guardar con éxito", resultadoGuardado);

        // Act - Leemos
        byte[] datosLeidos = storageManager.leerFragmento(nombreFragmento);

        // Assert - Verificamos que lo que leemos es exactamente lo mismo que escribimos
        assertNotNull("Los datos leídos no deben ser nulos", datosLeidos);
        assertArrayEquals("Los bytes leídos deben coincidir exactamente con los escritos", datosOriginales, datosLeidos);
    }

    @Test
    public void testLeerFragmento_Inexistente_RetornaNull() {
        // Act
        byte[] resultado = storageManager.leerFragmento("fantasma.part2");

        // Assert
        assertNull("Leer un archivo que no existe debería retornar null", resultado);
    }

    @Test
    public void testObtenerEspacioDisponible_MayorCero() {
        // Act
        long espacio = storageManager.obtenerEspacioDisponible();

        // Assert: Simplemente validamos que el SO pueda devolver un número válido (> 0)
        assertTrue("El espacio disponible en disco debe ser mayor a 0", espacio > 0);
        System.out.println("Prueba: Espacio detectado en disco de prueba: " + (espacio / 1024 / 1024) + " MB");
    }
}