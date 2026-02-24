package com.pda.distributed.services;

import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StorageCoordinatorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private StorageCoordinator coordinator;

    @Before
    public void setUp() {
        // Inicializamos nuestro coordinador puro y limpio antes de cada test
        coordinator = new StorageCoordinator();
    }

    @After
    public void tearDown() {
        // Limpieza si fuera necesario
    }

    @Test
    public void testManejarNuevoArchivoLocal_SinDependencias_NoLanzaExcepcion() throws IOException {
        // Arrange: Creamos un archivo real en la carpeta temporal de JUnit
        File archivoPrueba = folder.newFile("archivo_test_coordinador.txt");
        
        // Le escribimos algo de texto para que tenga un tamaño mayor a 0-bytes
        try (FileWriter writer = new FileWriter(archivoPrueba)) {
            writer.write("Este es un archivo de prueba para el sistema P2P");
        }
        
        String rutaAbsoluta = archivoPrueba.getAbsolutePath();

        // Act & Assert:
        // En JUnit 4, simplemente llamamos al método. Si lanza un NullPointerException
        // u otra excepción, la prueba fallará automáticamente. Si llega al final, pasa.
        coordinator.manejarNuevoArchivoLocal(rutaAbsoluta);
    }

    @Test
    public void testManejarNuevoArchivoLocal_ArchivoInexistente() {
        // Arrange: Inventamos una ruta que claramente no existe en el disco duro
        String rutaFalsa = "/ruta/inventada/que/no/existe.txt";

        // Act & Assert: El coordinador debería imprimir un error de "archivo desaparecido",
        // pero NO debería hacer crashear la aplicación.
        coordinator.manejarNuevoArchivoLocal(rutaFalsa);
    }

    @Test
    public void testProcesarFragmentoEntrante_SinStorageManager_SimulacionExitosa() {
        // Arrange: Simulamos que otro nodo nos mandó un chunk de bytes
        String idArchivoSimulado = "documento_importante_part1";
        byte[] datosFalsos = "1010101010".getBytes();

        // Act & Assert: Validamos que procese el fragmento sin romperse,
        // incluso si el StorageManager aún no ha sido inyectado.
        coordinator.procesarFragmentoEntrante(idArchivoSimulado, datosFalsos);
    }

    @Test
    public void testInyeccionDependencias_Y_Procesamiento() throws IOException {
        // Arrange: Instanciamos dependencias vacías (mocks manuales)
        NetworkService dummyNetwork = new NetworkService();
        QuorumService dummyQuorum = new QuorumService();
        StorageManager dummyStorage = new StorageManager();
        DistributedDirectory dummyDirectory = new DistributedDirectory();

        coordinator.setNetworkService(dummyNetwork);
        coordinator.setQuorumService(dummyQuorum);
        coordinator.setStorageManager(dummyStorage);
        coordinator.setDistributedDirectory(dummyDirectory);

        File archivoPrueba = folder.newFile("otro_test.txt");

        // Act & Assert: Debería ejecutar las "simulaciones" llamando a los servicios reales vacíos.
        // Si hay errores de estado en esas dependencias, la prueba fallará.
        coordinator.manejarNuevoArchivoLocal(archivoPrueba.getAbsolutePath());
        coordinator.procesarFragmentoEntrante("archivo_1", new byte[]{1, 2, 3});
    }
}