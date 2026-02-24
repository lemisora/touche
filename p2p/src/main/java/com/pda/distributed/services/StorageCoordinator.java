package com.pda.distributed.services;

import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;

// Coordina la distribución física y lógica de los archivos
public class StorageCoordinator {

    // Dependencias inyectadas para comunicarse con la red y tomar decisiones
    private NetworkService networkService;
    private QuorumService quorumService;

    // Componentes que implementará tu compañero después (comentados por ahora)
    // private final StorageManager gestorAlmacenamiento;
    // private final DistributedDirectory directorioDistribuido;

    public StorageCoordinator() {
        // Inicialización de componentes de storage
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void setQuorumService(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    // Este método es llamado por el FileWatcherService cuando detecta un archivo
    // nuevo en la carpeta
    public void manejarNuevoArchivoLocal(String rutaArchivo) {
        System.out.println("StorageCoordinator: Se detectó un nuevo archivo local en: " + rutaArchivo);
        System.out.println("StorageCoordinator: Preparando para solicitar ubicación en el anillo...");

        // Aquí pediremos al Quorum o al Lider principal que nos asigne nodos
        // trabajadores
        // // TODO : Usar DistributedDirectory para asignar fragmentos
        // lógicos
        // // TODO : Usar NetworkService para mandar los fragmentos físicos
        // por gRPC
    }

    // Este método es llamado por PdaServiceGrpcImpl cuando recibimos un fragmento
    // por red
    public void procesarFragmentoEntrante(String idArchivo, byte[] datosFragmento) {
        System.out.println("StorageCoordinator: Fragmento recibido de red para el archivo: " + idArchivo + " ("
                + datosFragmento.length + " bytes)");

        // // TODO : Usar StorageManager para escribir estos bytes al disco
        // duro real
    }
}
