package com.pda.distributed.services;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class FileWatcherServiceTest {

	@Test
	public void testStartAndStop_NoLanzaExcepcion() {
		String directorioObservado = "archivos_entrada";
		FileWatcherService watcher = new FileWatcherService(directorioObservado);
		watcher.start();
		// Le damos 100ms para arrancar el hilo
		try { Thread.sleep(100); } catch (InterruptedException e) {}
		watcher.stop();
		assertTrue("El servicio debe iniciar y detenerse limpiamente", true);
	}
}