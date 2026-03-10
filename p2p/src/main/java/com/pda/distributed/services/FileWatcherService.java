package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class FileWatcherService {

	private final BlockingQueue<String> fileQueue;
	private Thread watcherThread;
	private StorageCoordinator storageCoordinator;
	private boolean isRunning = false;
	private String directorioObservado;

	public FileWatcherService(String directorioObservado) {
		this.fileQueue = new LinkedBlockingQueue<>();
		this.directorioObservado = directorioObservado;
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
		Path rutaDirectorioObservado = Paths.get(directorioObservado);

		try {
			if (!Files.exists(rutaDirectorioObservado)) {
				Files.createDirectories(rutaDirectorioObservado);
			}
			WatchService watchService = FileSystems.getDefault().newWatchService();

			rutaDirectorioObservado.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

			ConsoleLogger.info("FileWatcher", "Vigilando la carpeta: " + rutaDirectorioObservado.toAbsolutePath());

			while (isRunning) {
				WatchKey key;
				try {
					// .take() pausa este hilo hasta que ocurra un evento en la carpeta
					key = watchService.take();
				} catch (InterruptedException ex) {
					// Si apagamos el nodo, el hilo se interrumpe y salimos del bucle
					return;
				}

				// Recorremos todos los eventos que ocurrieron (podrían ser varios archivos al mismo tiempo)
				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();

					// Ignorar eventos raros del sistema operativo
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}

					// Obtenemos el nombre del archivo nuevo
					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path nombreArchivoNuevo = ev.context();

					// Unimos la ruta de la carpeta con el nombre del archivo para obtener la ruta real
					Path rutaCompleta = rutaDirectorioObservado.resolve(nombreArchivoNuevo);

					ConsoleLogger.info("FileWatcher", "¡Nuevo archivo detectado!: " + nombreArchivoNuevo);

					if (storageCoordinator != null) {
						// Pasamos la ruta absoluta a la cola del coordinador
						storageCoordinator.encolarArchivo(rutaCompleta.toAbsolutePath().toString());
					} else {
						ConsoleLogger.advertencia("Vigía", "StorageCoordinator no está conectado.");
					}
				}

				// Reiniciar la llave para seguir escuchando futuros eventos
				boolean valid = key.reset();
				if (!valid) {
					break; // Si la carpeta fue borrada, salimos del ciclo
				}
			}
		} catch (IOException e) {
			ConsoleLogger.error("FileWatcher", "Error al iniciar vigilancia. " + e.getMessage());
		}
	}

	public void stop() {
		isRunning = false;
		if (watcherThread != null) watcherThread.interrupt();
	}
}