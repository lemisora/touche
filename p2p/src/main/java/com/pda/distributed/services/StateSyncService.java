package com.pda.distributed.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pda.distributed.core.RingType;
import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.DistributedDirectory.FileMetadata;
import com.pda.distributed.utils.ConsoleLogger;
import com.pda.distributed.core.Nodo;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class StateSyncService {

    private long lastUpdateTimestamp;
    private NetworkService networkService;
    private QuorumService quorumService;
    private DistributedDirectory distributedDirectory;
    private Nodo nodoLocal;
    private final Gson gson;

    public StateSyncService(NetworkService networkService) {
        this.networkService = networkService;
        this.lastUpdateTimestamp = System.currentTimeMillis();
        this.gson = new Gson();
    }

    public void setDistributedDirectory(DistributedDirectory distributedDirectory) {
        this.distributedDirectory = distributedDirectory;
    }

    public void setQuorumService(QuorumService quorumService) {
        this.quorumService = quorumService;
    }
    public void setNodoLocal(Nodo nodoLocal){
        this.nodoLocal = nodoLocal;
    }

    public void broadcastMapUpdate() {
        this.lastUpdateTimestamp = System.currentTimeMillis();
        if (this.distributedDirectory != null) {
            Map<String, FileMetadata> mapaGlobal = this.distributedDirectory.getGlobalFileMap();
            String jsonEstado = "FILES:" + gson.toJson(mapaGlobal);

            List<String> nodos = networkService.getNodosConectados();
            for (String direccionNodo : nodos) {
                networkService.enviarEstado(direccionNodo, jsonEstado);
            }
        }
    }

    public void broadcastQuorum(Map<String, RingType> registry) {
        String jsonEstado = "QUORUM:" + gson.toJson(registry);
        List<String> nodos = networkService.getNodosConectados();
        for (String direccionNodo : nodos) {
            networkService.enviarEstado(direccionNodo, jsonEstado);
        }
        ConsoleLogger.info("Sync", "Haciendo broadcast del Registro Global (Quórum)...");
    }

    public void syncNodeStates() {
        // TODO: (Para el futuro) Lógica periódica para enviar latidos y estados de
        // anillo.
    }

    public void recibirEstado(String estado, String direccionOrigen) {
        this.lastUpdateTimestamp = System.currentTimeMillis();
        try {
            if (estado.startsWith("QUORUM:")) {
                // Sincronizar Quórum
                String json = estado.substring(7); // Quitamos la palabra "QUORUM:"
                Type type = new TypeToken<Map<String, com.pda.distributed.core.RingType>>() {}.getType();
                Map<String, com.pda.distributed.core.RingType> nuevoRegistro = gson.fromJson(json, type);

                if (quorumService != null) {
                    quorumService.setNodeRegistry(nuevoRegistro);
                }

                // Revisamos la lista de nodos que el líder nos mandó.
                for (String ipNodoEnQuorum : nuevoRegistro.keySet()) {
                    // Si el nodo no somos nosotros mismos, abrimos un canal hacia él.
                    if (!ipNodoEnQuorum.equals(this.nodoLocal.getNodeAddress())) {
                        // registrarCanalSilencioso ya tiene un `if(!activeChannels.containsKey)` interno, 
                        // así que es súper seguro llamarlo aquí sin crear canales duplicados.
                        networkService.registrarCanalSilencioso(ipNodoEnQuorum);
                    }
                }
                
                // Resolver el problema del Cerebro Dividido (Split-Brain)
                String miDireccion = networkService.getMiDireccion();
                if (nuevoRegistro.containsKey(miDireccion) && nodoLocal != null) {
                    nodoLocal.forzarCambioDeAnillo(nuevoRegistro.get(miDireccion));
                }
                ConsoleLogger.info("Sync", "Registro Global sincronizado correctamente desde " + direccionOrigen);

            } else if (estado.startsWith("FILES:")) {
                // Sincronizar Archivos
                String json = estado.substring(6); // Quitamos la palabra "FILES:"
                Type type = new TypeToken<Map<String, FileMetadata>>() {}.getType();
                Map<String, FileMetadata> nuevoMapa = gson.fromJson(json, type);

                if (this.distributedDirectory != null) {
                    for (FileMetadata metadata : nuevoMapa.values()) {
                        this.distributedDirectory.updateMap(metadata);
                    }
                    ConsoleLogger.info("Sync", "Catálogo de archivos sincronizado correctamente desde " + direccionOrigen);
                }
            } else {
                // Si el mensaje viene con el formato viejo (sin prefijo), asumimos que son archivos
                Type type = new TypeToken<Map<String, FileMetadata>>() {}.getType();
                Map<String, FileMetadata> nuevoMapa = gson.fromJson(estado, type);
                if (this.distributedDirectory != null) {
                    for (FileMetadata metadata : nuevoMapa.values()) {
                        this.distributedDirectory.updateMap(metadata);
                    }
                }
            }
        } catch (Exception e) {
            ConsoleLogger.error("Sync", "Error al parsear estado de " + direccionOrigen + ": " + e.getMessage());
        }
    }
}
