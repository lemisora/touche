package com.pda.distributed.services;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Observa la carpeta de entrada local en busca de nuevos archivos para distribuir en la red
public class FileWatcherService {

    private StorageCoordinator storageCoordinator;

    // Set concurrente para llevar registro de los archivos y no procesar duplicados
    private final Set<String> archivosProcesados;
    private Thread hiloObservador;
    private volatile boolean activo;

    // Carpetas hardcodeadas según la definición del equipo
    private final Path directorioEntrada = Paths.get("./archivos_entrada");
    private final Path directorioSistema = Paths.get("./archivos");

    public FileWatcherService() {
        this.archivosProcesados = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // Al arrancar el servicio, nos aseguramos de que ambas carpetas existan
        try {
            if (!Files.exists(directorioEntrada)) {
                Files.createDirectories(directorioEntrada);
                System.out.println("FileWatcher: Creada carpeta de entrada -> " + directorioEntrada.toAbsolutePath());
            }
            if (!Files.exists(directorioSistema)) {
                Files.createDirectories(directorioSistema);
                System.out.println("FileWatcher: Creada carpeta de sistema -> " + directorioSistema.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("FileWatcher: Error al crear directorios base: " + e.getMessage());
        }
    }

    public void setStorageCoordinator(StorageCoordinator storageCoordinator) {
        this.storageCoordinator = storageCoordinator;
    }

    public void iniciar() {
        if (activo) return; // Evitar iniciar dos veces
        activo = true;

        hiloObservador = new Thread(this::observarDirectorioEntrada, "FileWatcherThread");
        hiloObservador.start();
        System.out.println("FileWatcher: Iniciando vigilancia en '" + directorioEntrada + "'...");
    }

    public void detener() {
        activo = false;
        if (hiloObservador != null) {
            hiloObservador.interrupt(); // Despertamos al hilo si está bloqueado esperando
        }
        System.out.println("FileWatcher: Servicio detenido.");
    }

    private void observarDirectorioEntrada() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            // Le decimos al OS que queremos saber si se CREAN cosas en la carpeta de entrada
            directorioEntrada.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (activo) {
                WatchKey key;
                try {
                    key = watchService.take(); // El hilo descansa (0% CPU) hasta que aparezca un archivo
                } catch (InterruptedException e) {
                    // Si llamaron a detener(), salimos pacíficamente
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path nombreArchivo = ev.context();
                    Path archivoCompleto = directorioEntrada.resolve(nombreArchivo);

                    notificarStorageCoordinator(archivoCompleto);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break; // La carpeta fue borrada
                }
            }
        } catch (IOException e) {
            System.err.println("FileWatcher: Error crítico en la vigilancia: " + e.getMessage());
        }
    }

    private void notificarStorageCoordinator(Path rutaArchivo) {
        String nombreArchivo = rutaArchivo.getFileName().toString();

        // .add devuelve true si el archivo no estaba en la lista
        if (archivosProcesados.add(nombreArchivo)) {
            System.out.println("\n[FileWatcher] ¡Nuevo archivo detectado en la bandeja de entrada!: " + nombreArchivo);
            if (storageCoordinator != null) {
                // Convertimos a String para mantener compatibilidad con el código actual del coordinador
                storageCoordinator.manejarNuevoArchivoLocal(rutaArchivo.toAbsolutePath().toString());
            } else {
                System.err.println("[FileWatcher] Error: StorageCoordinator no inicializado.");
            }
        }
    }
}