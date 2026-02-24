package com.pda.distributed.services;

import java.util.Set;
import java.util.HashSet;
import java.io.File;

// Observa las carpetas locales en busca de nuevos archivos para subir
public class FileWatcherService {

    private StorageCoordinator storageCoordinator;

    // Archivos que ya procesamos para no subirlos dos veces
    private final Set<String> archivosProcesados = new HashSet<>();

    // Hilo en segundo plano
    private Thread hiloObservador;
    private boolean activo = false;

    // La carpeta que vamos a vigilar
    private final String rutaCarpeta = "./mis_archivos";

    public FileWatcherService() {
        // Inicialización
        File directorio = new File(rutaCarpeta);
        if (!directorio.exists()) {
            directorio.mkdirs(); // Crea la carpeta si no existe
        }
    }

    public void setStorageCoordinator(StorageCoordinator storageCoordinator) {
        this.storageCoordinator = storageCoordinator;
    }

    public void iniciar() {
        activo = true;
        hiloObservador = new Thread(() -> {
            System.out.println("FileWatcher: Iniciando vigilancia en la carpeta '" + rutaCarpeta + "'...");
            while (activo) {
                try {
                    buscarArchivosNuevos();
                    Thread.sleep(3000); // Revisa cada 3 segundos
                } catch (InterruptedException e) {
                    System.out.println("FileWatcher: Hilo interrumpido.");
                    activo = false;
                }
            }
        });
        hiloObservador.start();
    }

    public void detener() {
        activo = false;
        if (hiloObservador != null) {
            hiloObservador.interrupt();
        }
    }

    private void buscarArchivosNuevos() {
        File directorio = new File(rutaCarpeta);
        File[] archivos = directorio.listFiles();

        if (archivos != null) {
            for (File archivo : archivos) {
                if (archivo.isFile()) {
                    String nombreArchivo = archivo.getName();

                    // Si encontramos un archivo que no hemos visto antes
                    if (!archivosProcesados.contains(nombreArchivo)) {
                        archivosProcesados.add(nombreArchivo);
                        notificarStorageCoordinator(archivo.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void notificarStorageCoordinator(String rutaAbsoluta) {
        if (storageCoordinator != null) {
            storageCoordinator.manejarNuevoArchivoLocal(rutaAbsoluta);
        } else {
            System.err.println("FileWatcher: StorageCoordinator no inicializado.");
        }
    }
}
