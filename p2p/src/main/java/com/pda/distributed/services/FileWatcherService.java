package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileWatcherService {

	private final BlockingQueue<String> fileQueue;
	private Thread watcherThread;
	private StorageCoordinator storageCoordinator;
	private boolean isRunning = false;

	public FileWatcherService() {
		this.fileQueue = new LinkedBlockingQueue<>();
	}

	public void setStorageCoordinator(StorageCoordinator storageCoordinator) {
		this.storageCoordinator = storageCoordinator;
	}

	public void start() {
		isRunning = true;
		watcherThread = new Thread(this::watchArchivosEntrada);
		watcherThread.setDaemon(true);
		watcherThread.start();
		ConsoleLogger.info("Watcher", "Vigilando directorio de entrada...");
	}

	private void watchArchivosEntrada() {
		// TODO: 1. Implementar java.nio.file.WatchService para la carpeta "archivos_entrada".
		// TODO: 2. Cuando detecte un ENTRY_CREATE, obtener la ruta absoluta.
		// TODO: 3. Llamar a storageCoordinator.encolarArchivo(rutaAbsoluta).
	}

	public void stop() {
		isRunning = false;
		if (watcherThread != null) watcherThread.interrupt();
	}
}