package com.pda.distributed.services;

import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StorageCoordinator {

	private final BlockingQueue<String> uploadQueue;
	private StorageManager storageManager;
	private DistributedDirectory directory;

	private final int DEFAULT_REPLICAS = 2;
	private final int CHUNK_SIZE_MB = 2;

	public StorageCoordinator() {
		this.uploadQueue = new LinkedBlockingQueue<>();
	}

	// Setters para inyección de dependencias
	public void setStorageManager(StorageManager storageManager) { this.storageManager = storageManager; }
	public void setDistributedDirectory(DistributedDirectory directory) { this.directory = directory; }

	public void processUploadQueue() {
		// TODO: 1. (Idealmente en un hilo) hacer un loop infinito leyendo uploadQueue.take().
		// TODO: 2. Al tomar una ruta de archivo, calcular cuántos chunks de 2MB saldrán.
		// TODO: 3. Llamar a allocateToNodes() para saber a quiénes enviárselo.
		// TODO: 4. Leer el archivo localmente y enviarlo usando el NetworkService (aún no inyectado aquí, lo agregaremos después).
	}

	public List<String> allocateToNodes(DistributedDirectory.FileMetadata fileMetadata) {
		// TODO: 1. Consultar el QuorumManager o un registro para ver qué nodos están vivos.
		// TODO: 2. Elegir hasta 'DEFAULT_REPLICAS' nodos para guardar el archivo.
		// TODO: 3. Retornar la lista de "IP:Puerto" elegidos.
		return new ArrayList<>(); // Placeholder
	}

	public void handleIncomingChunk(String fileId, int chunkIndex, byte[] data, int totalExpected) {
		// TODO: 1. Delegar a storageManager.saveChunk(fileId, chunkIndex, data, totalExpected).
		// TODO: 2. Si es el último chunk (storageManager nos avisa o lo validamos), podemos notificar éxito.
	}

	// Método extra para que el FileWatcher meta cosas a la cola
	public void encolarArchivo(String rutaAbsoluta) {
		uploadQueue.offer(rutaAbsoluta);
	}
}