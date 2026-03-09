package com.pda.distributed.storage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class DistributedDirectory {

    // Clase auxiliar interna para representar los metadatos de un archivo
    public static class FileMetadata {
        public String fileName;
        public long sizeBytes;
        public List<String> nodeAddresses; // Lista de "IP:Puerto" donde está replicado

        public FileMetadata(String fileName, long sizeBytes) {
            this.fileName = fileName;
            this.sizeBytes = sizeBytes;
            this.nodeAddresses = new ArrayList<>();
        }
    }

    // El catálogo global del sistema (K: Nombre del archivo, V: Metadatos)
    private final Map<String, FileMetadata> globalFileMap;

    public DistributedDirectory() {
        this.globalFileMap = new ConcurrentHashMap<>();
    }

    public List<String> getFileLocation(String fileName) {
        // TODO: 1. Buscar en globalFileMap el archivo por su fileName.
        // TODO: 2. Si existe, retornar su lista de nodeAddresses. Si no, retornar una lista vacía.
        return new ArrayList<>(); // Placeholder
    }

    public void updateMap(FileMetadata fileData) {
        // TODO: 1. Insertar o actualizar el fileData en globalFileMap usando su fileName como clave.
    }
}