package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

public class StateSyncService {

	private long lastUpdateTimestamp;
	private NetworkService networkService;

	public StateSyncService(NetworkService networkService) {
		this.networkService = networkService;
		this.lastUpdateTimestamp = System.currentTimeMillis();
	}

	public void broadcastMapUpdate(String incrementalData) {
		// TODO: 1. Actualizar lastUpdateTimestamp = System.currentTimeMillis().
		// TODO: 2. Usar networkService para enviar un PeticionEstado a todos los nodos conectados.
		ConsoleLogger.info("Sync", "Haciendo broadcast de actualización de mapa...");
	}

	public void syncNodeStates() {
		// TODO: (Para el futuro) Lógica periódica para enviar latidos y estados de anillo.
	}

	public void recibirEstado(String estado, String direccionOrigen) {
		// TODO: 1. Parsear el 'estado' (podría ser un JSON del DistributedDirectory).
		// TODO: 2. Actualizar el DistributedDirectory local con esta información.
		// TODO: 3. Actualizar lastUpdateTimestamp.
		ConsoleLogger.info("Sync", "Estado recibido desde " + direccionOrigen);
	}
}