package com.pda.distributed.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Sabe dónde está cada archivo en la red P2P (como un índice global).
 * Mapea: Nombre del Archivo -> Lista de Nodos (Puertos/IPs) que lo tienen.
 */
public class DistributedDirectory {

    // K: Nombre o ID del archivo (ej. "documento.pdf")
    // V: Lista de identificadores de nodos (ej. "localhost:50051", "192.168.1.5:50052")
    private final Map<String, List<String>> catalogoGlobal;

    // K: Identificador del nodo (ej. "localhost:50051")
    // V: Espacio disponible reportado en bytes
    private final Map<String, Long> nodosActivos;

    // Cuántas copias (réplicas) queremos de cada archivo por defecto
    private final int FACTOR_REPLICACION = 2;

    public DistributedDirectory() {
        this.catalogoGlobal = new ConcurrentHashMap<>();
        this.nodosActivos = new ConcurrentHashMap<>();
    }

    /**
     * Registra que un nodo específico tiene una copia de un archivo.
     */
    public void registrarUbicacion(String idArchivo, String idNodo) {
        catalogoGlobal.computeIfAbsent(idArchivo, k -> new ArrayList<>()).add(idNodo);
        System.out.println("[Directorio] Registrado: " + idArchivo + " se encuentra en -> " + idNodo);
    }

    /**
     * Consulta qué nodos tienen un archivo específico.
     */
    public List<String> localizarArchivo(String idArchivo) {
        return catalogoGlobal.getOrDefault(idArchivo, new ArrayList<>());
    }

    /**
     * Actualiza la información de un nodo activo (típicamente llamado desde Heartbeat o StateSync).
     */
    public void actualizarEstadoNodo(String idNodo, long espacioDisponibleBytes) {
        nodosActivos.put(idNodo, espacioDisponibleBytes);
    }

    /**
     * Elimina un nodo de la lista de activos si falla (llamado por Heartbeat/Quorum).
     */
    public void removerNodoActivo(String idNodo) {
        nodosActivos.remove(idNodo);
        // Opcional: Podrías buscar en catalogoGlobal y removerlo de las listas de archivos
        System.out.println("[Directorio] Nodo removido de activos: " + idNodo);
    }

    /**
     * Asigna un conjunto de nodos (Workers) para almacenar un nuevo archivo,
     * priorizando aquellos con más espacio disponible.
     * * @param fileName El nombre del nuevo archivo.
     * @param fileSize El tamaño del archivo (para asegurar que quepa).
     * @return Una lista de IDs de nodos asignados.
     */
    public List<String> assignWorkersToNewFile(String fileName, long fileSize) {
        System.out.println("[Directorio] Asignando workers para: " + fileName + " (" + fileSize + " bytes)");

        // Filtrar nodos que tengan espacio suficiente
        List<Map.Entry<String, Long>> nodosCapaces = nodosActivos.entrySet().stream()
                .filter(entry -> entry.getValue() >= fileSize)
                .collect(Collectors.toList());

        if (nodosCapaces.isEmpty()) {
            System.err.println("[Directorio] ¡CRÍTICO! Ningún nodo activo tiene espacio suficiente (" + fileSize + " bytes).");
            return Collections.emptyList(); // Falla graciosamente
        }

        // Ordenar los nodos de mayor a menor espacio disponible (balanceo de carga rudimentario)
        nodosCapaces.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()));

        // Seleccionar los mejores 'N' nodos según el factor de replicación
        int nodosASeleccionar = Math.min(FACTOR_REPLICACION, nodosCapaces.size());
        List<String> nodosSeleccionados = new ArrayList<>();

        for (int i = 0; i < nodosASeleccionar; i++) {
            nodosSeleccionados.add(nodosCapaces.get(i).getKey());
        }

        System.out.println("[Directorio] Nodos asignados para " + fileName + ": " + nodosSeleccionados);
        return nodosSeleccionados;
    }

    public Map<String, List<String>> obtenerEstadoCompleto() { return catalogoGlobal; }
    public void actualizarEstadoCompleto(Map<String, List<String>> nuevoEstado) {
        this.catalogoGlobal.clear();
        this.catalogoGlobal.putAll(nuevoEstado);
    }
}