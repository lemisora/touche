package com.pda.distributed.services;

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
     * Invocado por FileWatcherService cuando el usuario coloca un archivo en ./archivos_entrada
     */
    public void manejarNuevoArchivoLocal(String rutaArchivo) {
        System.out.println("[Coordinator] Evaluando nuevo archivo local: " + rutaArchivo);
        
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            System.err.println("[Coordinator] Error: El archivo desapareció antes de procesarse.");
            return;
        }

        String nombreArchivo = archivo.getName();
        long tamanoBytes = archivo.length(); // Tu prueba era de 0-bytes, pero aquí ya soportamos reales

        System.out.println("[Coordinator] Archivo: " + nombreArchivo + " | Tamaño: " + tamanoBytes + " bytes");

        // FLUJO SIMULADO -- Falta implementar el envio de fragmentos por la red --
        
        // Pedir permiso/ubicación al líder o al quorum
        if (quorumService != null) {
            System.out.println("[Coordinator] Solicitando nodos al QuorumService para distribuir...");
            // List<String> nodosDestino = quorumService.proposeAction(...);
        } else {
            System.out.println("[Coordinator] (Simulación) QuorumService no disponible. Asumiendo que hay espacio.");
        }

        // Fragmentar y Enviar por la red
        if (networkService != null) {
            System.out.println("[Coordinator] Enviando fragmentos vía NetworkService (gRPC)...");
            // byte[] chunk = leerArchivo(archivo);
            // networkService.sendChunk(nodoDestino, chunk);
        } else {
            System.out.println("[Coordinator] (Simulación) NetworkService no disponible. Simulación de envío completada.");
        }

        // Registrar en el directorio lógico
        if (distributedDirectory != null) {
            System.out.println("[Coordinator] Registrando metadatos en DistributedDirectory...");
            // distributedDirectory.registrarUbicacion(nombreArchivo, nodosDestino);
        }
        
        System.out.println("[Coordinator] ✅ Procesamiento de subida concluido para: " + nombreArchivo + "\n");
    }

    /**
     * Invocado por PdaServiceGrpcImpl cuando OTRO nodo nos envía un fragmento para guardar
     */
    public void procesarFragmentoEntrante(String idArchivo, byte[] datosFragmento) {
        System.out.println("\n[Coordinator] Recibiendo fragmento de red. ID: " + idArchivo + " | " + datosFragmento.length + " bytes");

        if (storageManager != null) {
            System.out.println("[Coordinator] Delegando escritura al disco mediante StorageManager...");
            // boolean exito = storageManager.storeChunk(idArchivo, datosFragmento);
            // return exito;
        } else {
            System.out.println("[Coordinator] (Simulación) StorageManager no disponible. Fingiendo que se guardó en el disco local.");
        }
    }
}