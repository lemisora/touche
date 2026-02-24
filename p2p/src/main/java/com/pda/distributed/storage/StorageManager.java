package com.pda.distributed.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Se encarga de guardar, leer y gestionar el espacio de los archivos reales en el disco duro
public class StorageManager {

    private final Path directorioAlmacenamiento;

    // Constructor por defecto para Producción
    public StorageManager() {
        this.directorioAlmacenamiento = Paths.get("./archivos");
        inicializarDirectorio();
    }

    // Constructor inyectable para Pruebas (Testing)
    public StorageManager(String rutaPersonalizada) {
        this.directorioAlmacenamiento = Paths.get(rutaPersonalizada);
        inicializarDirectorio();
    }

    private void inicializarDirectorio() {
        try {
            if (!Files.exists(directorioAlmacenamiento)) {
                Files.createDirectories(directorioAlmacenamiento);
                System.out.println("StorageManager: Creado directorio en " + directorioAlmacenamiento.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("StorageManager: Error crítico al crear el directorio: " + e.getMessage());
        }
    }

    /**
     * Guarda un arreglo de bytes en el disco físico.
     * @param idFragmento Nombre único del fragmento (ej. "video.mp4.part1")
     * @param datos Los bytes puros a guardar
     * @return true si se guardó con éxito, false si hubo un error
     */
    public boolean guardarFragmento(String idFragmento, byte[] datos) {
        Path rutaArchivo = directorioAlmacenamiento.resolve(idFragmento);
        
        // Usamos try-with-resources para asegurar que el archivo se cierre siempre
        try (FileOutputStream fos = new FileOutputStream(rutaArchivo.toFile())) {
            fos.write(datos);
            System.out.println("[Storage] Fragmento guardado en disco: " + idFragmento + " (" + datos.length + " bytes)");
            return true;
        } catch (IOException e) {
            System.err.println("[Storage] Error al guardar el fragmento " + idFragmento + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Lee un fragmento completo desde el disco hacia la memoria (RAM).
     */
    public byte[] leerFragmento(String idFragmento) {
        Path rutaArchivo = directorioAlmacenamiento.resolve(idFragmento);
        
        if (!Files.exists(rutaArchivo)) {
            System.err.println("[Storage] El fragmento solicitado no existe: " + idFragmento);
            return null;
        }
        
        try {
            return Files.readAllBytes(rutaArchivo);
        } catch (IOException e) {
            System.err.println("[Storage] Error al leer el fragmento " + idFragmento + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Consulta el espacio libre del disco duro (Útil para el Algoritmo de Distribución).
     * @return Espacio libre en bytes.
     */
    public long obtenerEspacioDisponible() {
        try {
            // Consulta a nivel de Sistema Operativo el espacio de la partición
            return Files.getFileStore(directorioAlmacenamiento).getUsableSpace();
        } catch (IOException e) {
            System.err.println("[Storage] Error al consultar espacio disponible.");
            return 0;
        }
    }
}