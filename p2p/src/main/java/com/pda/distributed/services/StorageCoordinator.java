package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;

import java.io.File;

// Coordina la distribución física y lógica de los archivos
public class StorageCoordinator {

    // Dependencias de Red y Consenso (Próximos a implementar)
    private NetworkService networkService;
    private QuorumService quorumService;

    // Dependencias de Almacenamiento Local (Próximos a implementar)
    private StorageManager storageManager;
    private DistributedDirectory distributedDirectory;

    public StorageCoordinator() {
        // El constructor se mantiene limpio.
        // Las dependencias se inyectan desde la clase principal (App o Nodo).
    }

    // Métodos de Inyección de Dependencias (Setters)
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void setQuorumService(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void setDistributedDirectory(DistributedDirectory distributedDirectory) {
        this.distributedDirectory = distributedDirectory;
    }

    /**
     * Invocado por FileWatcherService cuando el usuario coloca un archivo en
     * ./archivos_entrada
     */
    public void manejarNuevoArchivoLocal(String rutaArchivo) {
        System.out.println("[Coordinator] Evaluando nuevo archivo local: " + rutaArchivo);

        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            System.err.println("[Coordinator] Error: El archivo desapareció antes de procesarse.");
            return;
        }
        ConsoleLogger.info("Log", "StorageCoordinator: Se detectó un nuevo archivo local en: " + rutaArchivo);
        ConsoleLogger.info("Log", "StorageCoordinator: Preparando para solicitar ubicación en el anillo...");

        String nombreArchivo = archivo.getName();
        long tamanoBytes = archivo.length();

        System.out.println("[Coordinator] Archivo: " + nombreArchivo + " | Tamaño: " + tamanoBytes + " bytes");

        // 1. Pedir candidatos al Directorio
        java.util.List<String> nodosCandidatos = null;
        if (distributedDirectory != null) {
            nodosCandidatos = distributedDirectory.assignWorkersToNewFile(nombreArchivo, tamanoBytes);
        }

        if (nodosCandidatos == null || nodosCandidatos.isEmpty()) {
            ConsoleLogger.error("Coordinator", "No hay nodos Worker disponibles con suficiente espacio.");
            return;
        }

        String idAccion = "UPLOAD:" + nombreArchivo;
        String detalleAccion = "Guardar " + nombreArchivo + " en " + nodosCandidatos;

        // 2. Proponer al Quorum (Comité)
        if (quorumService != null) {
            System.out.println("[Coordinator] Solicitando consenso al Comité para distribuir...");

            // Aquí configuramos qué pasará SI GANAMOS la elección de subir el archivo
            final java.util.List<String> nodosFinales = nodosCandidatos;
            quorumService.proponerAccion(idAccion, detalleAccion, () -> {
                ConsoleLogger.exito("Coordinator", "El Comité aprobó la subida de " + nombreArchivo);
                ejecutarSubidaReal(archivo, nodosFinales);
            });
        } else {
            ConsoleLogger.advertencia("Coordinator", "QuorumService no disponible. Procediendo sin consenso.");
            ejecutarSubidaReal(archivo, nodosCandidatos);
        }
    }

    private void ejecutarSubidaReal(File archivo, java.util.List<String> nodosDestino) {
        String nombreArchivo = archivo.getName();

        // Registrar en el directorio lógico
        if (distributedDirectory != null) {
            System.out.println("[Coordinator] Registrando metadatos en DistributedDirectory...");
            for (String nodo : nodosDestino) {
                distributedDirectory.registrarUbicacion(nombreArchivo, nodo);
            }
        }

        // Fragmentar y Enviar por la red a los Workers elegidos
        if (networkService != null) {
            try {
                byte[] datos = java.nio.file.Files.readAllBytes(archivo.toPath());
                for (String nodoId : nodosDestino) {
                    // idNodo es típicamente "ip:puerto", para este MVP simplificamos extrayendo el
                    // puerto
                    String[] partes = nodoId.split(":");
                    int puerto = partes.length > 1 ? Integer.parseInt(partes[1]) : Integer.parseInt(nodoId);

                    ConsoleLogger.info("Coordinator", "Enviando fragmentos vía gRPC al Worker en puerto " + puerto);
                    networkService.enviarFragmentoBasico(puerto, nombreArchivo, datos);
                }
            } catch (Exception e) {
                ConsoleLogger.error("Coordinator", "Error leyendo/enviando el archivo: " + e.getMessage());
            }
        } else {
            System.out.println(
                    "[Coordinator] (Simulación) NetworkService no disponible. Simulación de envío completada.");
        }

        ConsoleLogger.exito("Coordinator", "Procesamiento de subida concluido para: " + nombreArchivo);
    }

    /**
     * Invocado por PdaServiceGrpcImpl cuando OTRO nodo nos envía un fragmento para
     * guardar
     */
    public void procesarFragmentoEntrante(String idArchivo, byte[] datosFragmento) {
        ConsoleLogger.info("StorageCoordinator", "Fragmento recibido de red para el archivo: " + idArchivo + " ("
                + datosFragmento.length + " bytes)");

        if (storageManager != null) {
            System.out.println("[Coordinator] Delegando escritura al disco mediante StorageManager...");
            boolean exito = storageManager.guardarFragmento(idArchivo, datosFragmento);
            if (exito) {
                ConsoleLogger.exito("StorageCoordinator", "Archivo guardado físicamente: " + idArchivo);
            } else {
                ConsoleLogger.error("StorageCoordinator", "Hubo un error guardando el archivo en disco.");
            }
        } else {
            System.out.println(
                    "[Coordinator] (Simulación) StorageManager no disponible. Fingiendo que se guardó en el disco local.");
        }
    }
}