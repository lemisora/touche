package com.pda.distributed.storage;

import com.pda.distributed.utils.ConsoleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {

    // Atributos definidos en el diagrama PlantUML
    private final Path archivosDir;
    private final Map<String, Integer> chunkCounters;

    public StorageManager(String rutaDirectorioArchivos) {
        // Inicializamos la ruta usando la API moderna NIO de Java
        this.archivosDir = Paths.get(rutaDirectorioArchivos);
        Path rutaArchivosEntrada = Paths.get(rutaDirectorioArchivos + "_entrada");
        this.chunkCounters = new ConcurrentHashMap<>();
        
        // Crear el directorio físicamente si no existe.
        try {
            Files.createDirectories(this.archivosDir);
            Files.createDirectories(rutaArchivosEntrada);
        } catch (IOException e) {
            
        }
    }

    // ===================================================================================
    // Métodos de Orquestación de Fragmentos (Directo del Diagrama PlantUML)
    // ===================================================================================

    /**
     * Guarda un fragmento y verifica si ya tenemos todos para ensamblarlo.
     */
    public void saveChunk(String fileId, int chunkIndex, byte[] data, int totalExpected) {
        // Crear el nombre del fragmento (ej. fileId + ".part" + chunkIndex).
        String chunkFileName = fileId + ".part" + chunkIndex;
        // Usar el método local guardarFragmento(...) para escribir los bytes en disco.
        boolean fragmentoGuardado = guardarFragmento(chunkFileName, data);
        // Si se guardó con éxito, actualizar el contador en chunkCounters:
        if (fragmentoGuardado) {
            int fragmentosActuales = chunkCounters.merge(fileId, 1, Integer::sum);
            ConsoleLogger.info("StorageManager", "Fragmento guardado correctamente " 
                + fragmentosActuales 
                + "/" + totalExpected);
            
            if (fragmentosActuales == totalExpected) {
                assembleFile(fileId, totalExpected);
            }
        }
    }

    /**
     * Ensambla todos los fragmentos de un archivo en uno solo cuando la descarga termina.
     */
    private void assembleFile(String fileId, int totalExpected) {
        ConsoleLogger.info("Storage", "Ensamblando archivo final: " + fileId);
        Path rutaArchivoFinal = this.archivosDir.resolve(fileId);
        
        // Crear un archivo nuevo vacío con el nombre original (fileId).
        try {
            Files.deleteIfExists(rutaArchivoFinal);
            Files.createFile(rutaArchivoFinal);
            
            // Hacer un ciclo para leer secuencialmente todos los fragmentos (ej. .part0, .part1...).
            for (int i = 0; i < totalExpected; i++) {
                String nombreFragmentoTemp = fileId + ".part" + i;
                Path rutaFragmentoTemp = this.archivosDir.resolve(nombreFragmentoTemp);
                
                byte[] contenidoFragmento = Files.readAllBytes(rutaFragmentoTemp);
               
               Files.write(rutaArchivoFinal, contenidoFragmento, StandardOpenOption.APPEND);
               
               // Eliminar fragmento tras concatenarlo en el archivo final
               Files.delete(rutaFragmentoTemp);
            }
            ConsoleLogger.info("StorageManager", "Se ha escrito exitosamente en disco el archivo " + fileId);
        } catch (IOException e) {
            ConsoleLogger.error("StorageManager", "Error al obtener el fragmento para ensamblado. " + e.getMessage());
        } finally {
            chunkCounters.remove(fileId);
        }
    }

    // ===================================================================================
    // Métodos Utilitarios y de Entrada/Salida (Requeridos por StorageManagerTest.java)
    // ===================================================================================

    /**
     * Guarda físicamente un arreglo de bytes en un archivo.
     */
    public boolean guardarFragmento(String nombreFragmento, byte[] datos) {
        // Construir la ruta completa usando this.archivosDir.resolve(nombreFragmento).
        Path rutaChunk = this.archivosDir.resolve(nombreFragmento);
        // Escribir los bytes en esa ruta usando Files.write(...).
        try {
            Files.write(rutaChunk, datos, StandardOpenOption.CREATE);
            return true;
        } catch (IOException e) {
            ConsoleLogger.error("StorageManager", "Error al guardar el fragmento" + e.getMessage());
            return false;
        }
    }

    /**
     * Lee físicamente un arreglo de bytes desde el disco.
     */
    public byte[] leerFragmento(String nombreFragmento) {
        // Construir la ruta completa.
        Path rutaChunk = this.archivosDir.resolve(nombreFragmento);
        // Validar si el archivo existe usando Files.exists(...).
        if (!Files.exists(rutaChunk)) {
            return null;
        }
        // Leer los bytes usando Files.readAllBytes(...) y retornarlos (o null si falla/no existe).
        try {
            return Files.readAllBytes(rutaChunk);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Retorna el espacio disponible en el disco (en bytes) para saber si podemos recibir más archivos.
     */
    public long obtenerEspacioDisponible() {
        // Usar Files.getFileStore(this.archivosDir).getUsableSpace() para saber el espacio libre.
        try {
            return Files.getFileStore(this.archivosDir).getUsableSpace();
        } catch (IOException e) {
            return 0L;
        }
    }
    
    /** Devuelve la ruta física en la que se encuentran los archivos */
    public Path getArchivosDir(){
        return this.archivosDir;
    }
}