package com.pda.distributed.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.DistributedDirectory.FileMetadata;
import com.pda.distributed.utils.ConsoleLogger;

import java.lang.reflect.Type;
import java.util.Map;

public class StateSyncService {

    private long lastUpdateTimestamp;
    private NetworkService networkService;
    private DistributedDirectory distributedDirectory;
    private final Gson gson;

    public StateSyncService(NetworkService networkService) {
        this.networkService = networkService;
        this.lastUpdateTimestamp = System.currentTimeMillis();
        this.gson = new Gson();
    }

    public void setDistributedDirectory(DistributedDirectory distributedDirectory) {
        this.distributedDirectory = distributedDirectory;
    }

    public void broadcastMapUpdate() {
        this.lastUpdateTimestamp = System.currentTimeMillis();
        if (this.distributedDirectory != null) {
            Map<String, FileMetadata> mapaGlobal = this.distributedDirectory.getGlobalFileMap();
            String jsonEstado = gson.toJson(mapaGlobal);
            ConsoleLogger.info("Sync", "Haciendo broadcast de actualización de mapa a nodos conectados...");

            java.util.List<String> nodos = networkService.getNodosConectados();
            for (String direccionNodo : nodos) {
                networkService.enviarEstado(direccionNodo, jsonEstado);
            }
        } else {
            ConsoleLogger.advertencia("Sync", "No hay DistributedDirectory configurado para sincronizar.");
        }
    }

    public void syncNodeStates() {
        // TODO: (Para el futuro) Lógica periódica para enviar latidos y estados de
        // anillo.
    }

    public void recibirEstado(String estado, String direccionOrigen) {
        this.lastUpdateTimestamp = System.currentTimeMillis();
        if (this.distributedDirectory != null) {
            try {
                Type type = new TypeToken<Map<String, FileMetadata>>() {
                }.getType();
                Map<String, FileMetadata> nuevoMapa = gson.fromJson(estado, type);

                for (FileMetadata metadata : nuevoMapa.values()) {
                    this.distributedDirectory.updateMap(metadata);
                }
                ConsoleLogger.info("Sync", "Estado sincronizado correctamente desde " + direccionOrigen);
            } catch (Exception e) {
                ConsoleLogger.error("Sync", "Error al parsear estado de " + direccionOrigen + ": " + e.getMessage());
            }
        } else {
            ConsoleLogger.advertencia("Sync", "No hay DistributedDirectory configurado para recibir sincronización.");
        }
    }
}
