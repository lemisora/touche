package com.pda.distributed.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class FileWatcherServiceTest {

	private FileWatcherService fileWatcherService;
	private MockStorageCoordinator mockCoordinator;
	private File directorioEntrada;

	// Coordinador espía adaptado a la firma de tu repositorio actual
	class MockStorageCoordinator extends StorageCoordinator {
		AtomicBoolean archivoManejado = new AtomicBoolean(false);

		@Override
		public void manejarNuevoArchivoLocal(String rutaArchivo) {
			archivoManejado.set(true);
		}
	}

	@Before
	public void setUp() {
		mockCoordinator = new MockStorageCoordinator();
		fileWatcherService = new FileWatcherService();

		fileWatcherService.setStorageCoordinator(mockCoordinator);
		fileWatcherService.iniciar();

		// Esta es la carpeta que definió nuestro FileWatcher en su constructor
		directorioEntrada = new File("./archivos_entrada");
	}

	@After
	public void tearDown() {
		fileWatcherService.detener();

		// Limpiamos el archivo de prueba para no ensuciar tu entorno de desarrollo
		File testFile = new File(directorioEntrada, "archivo_prueba_0bytes.txt");
		if (testFile.exists()) {
			testFile.delete();
		}
	}

	@Test
	public void testDetectsNewZeroByteFileInEntrada() throws IOException, InterruptedException {
		// Le damos un respiro minúsculo al hilo observador para que se registre en el OS
		Thread.sleep(200);

		// Act: Creamos el archivo manualmente en el directorio de entrada
		File testFile = new File(directorioEntrada, "archivo_prueba_0bytes.txt");
		testFile.createNewFile();
		System.out.println("Test: Archivo creado manualmente -> " + testFile.getAbsolutePath());

		// Assert: Esperamos como MÁXIMO 1 segundo. ¡Como usamos WatchService suele tardar milisegundos!
		int maxWaitTimeMs = 1000;
		int timeWaitedMs = 0;

		while (!mockCoordinator.archivoManejado.get() && timeWaitedMs < maxWaitTimeMs) {
			Thread.sleep(50); // Revisamos rapidísimo
			timeWaitedMs += 50;
		}

		assertTrue("El StorageCoordinator debió ser notificado del nuevo archivo en archivos_entrada",
				mockCoordinator.archivoManejado.get());
	}
}