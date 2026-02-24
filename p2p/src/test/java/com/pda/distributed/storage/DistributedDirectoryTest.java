package com.pda.distributed.storage;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.Assert.*;

public class DistributedDirectoryTest {

    private DistributedDirectory directorio;

    @Before
    public void setUp() {
        directorio = new DistributedDirectory();
    }

    @Test
    public void testRegistrarYLocalizarArchivo() {
        // Arrange
        String archivo = "foto_vacaciones.jpg";
        String nodoA = "localhost:8080";
        String nodoB = "localhost:9090";

        // Act
        directorio.registrarUbicacion(archivo, nodoA);
        directorio.registrarUbicacion(archivo, nodoB);
        
        List<String> nodos = directorio.localizarArchivo(archivo);

        // Assert
        assertEquals("El archivo debería estar en 2 nodos", 2, nodos.size());
        assertTrue("El nodo A debe tener el archivo", nodos.contains(nodoA));
        assertTrue("El nodo B debe tener el archivo", nodos.contains(nodoB));
    }

    @Test
    public void testLocalizarArchivoInexistente_RetornaListaVacia() {
        // Act
        List<String> nodos = directorio.localizarArchivo("archivo_fantasma.txt");

        // Assert
        assertNotNull("No debe retornar null para evitar NullPointerExceptions", nodos);
        assertTrue("La lista debe estar vacía", nodos.isEmpty());
    }

    @Test
    public void testSincronizarEstadoCompleto() {
        // Arrange: Simulamos un catálogo que recibimos por red (Gossip/StateSync)
        Map<String, List<String>> estadoRecibido = new HashMap<>();
        estadoRecibido.put("video.mp4", Arrays.asList("nodo1", "nodo2"));
        estadoRecibido.put("texto.txt", Arrays.asList("nodo3"));

        // Act
        directorio.actualizarEstadoCompleto(estadoRecibido);

        // Assert
        Map<String, List<String>> estadoActual = directorio.obtenerEstadoCompleto();
        assertEquals("El catálogo debe tener 2 archivos", 2, estadoActual.size());
        assertEquals("El video debe estar en 2 nodos", 2, estadoActual.get("video.mp4").size());
    }

    @Test
    public void testAssignWorkersToNewFile_AsignaCorrectamente() {
        // Arrange
        // Simulamos nodos reportando su estado (ej. Nodo A tiene 10GB, Nodo B tiene 2GB, Nodo C tiene 5GB)
        long GB = 1024L * 1024L * 1024L;
        directorio.actualizarEstadoNodo("nodo_A_10GB", 10 * GB);
        directorio.actualizarEstadoNodo("nodo_B_2GB", 2 * GB);
        directorio.actualizarEstadoNodo("nodo_C_5GB", 5 * GB);

        String archivoPrueba = "pelicula_nueva.mp4";
        long tamanoArchivo = 3 * GB; // El archivo pesa 3GB

        // Act
        List<String> workersAsignados = directorio.assignWorkersToNewFile(archivoPrueba, tamanoArchivo);

        // Assert
        // El factor de replicación es 2.
        // El nodo_B_2GB NO tiene espacio suficiente para 3GB, así que debe ser ignorado.
        // Solo nodo_A y nodo_C tienen espacio.
        assertEquals("Debería asignar 2 workers según el factor de replicación", 2, workersAsignados.size());
        assertTrue("Debería asignar a nodo_A_10GB (tiene espacio)", workersAsignados.contains("nodo_A_10GB"));
        assertTrue("Debería asignar a nodo_C_5GB (tiene espacio)", workersAsignados.contains("nodo_C_5GB"));
        assertFalse("NO debería asignar a nodo_B_2GB (no tiene espacio)", workersAsignados.contains("nodo_B_2GB"));
    }

    @Test
    public void testAssignWorkersToNewFile_SinEspacioSuficiente_RetornaListaVacia() {
        // Arrange
        long MB = 1024L * 1024L;
        directorio.actualizarEstadoNodo("nodo_pobre", 100 * MB); // Solo tiene 100MB

        String archivoGigante = "juego.iso";
        long tamanoArchivo = 500 * MB; // Pesa 500MB

        // Act
        List<String> workersAsignados = directorio.assignWorkersToNewFile(archivoGigante, tamanoArchivo);

        // Assert
        assertTrue("Debería retornar lista vacía al no haber nodos capaces", workersAsignados.isEmpty());
    }
}