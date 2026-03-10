package com.pda.distributed.services;

import com.pda.distributed.storage.DistributedDirectory;
import com.pda.distributed.storage.StorageManager;
import com.pda.distributed.utils.ConsoleLogger;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class StorageCoordinator {

    private final BlockingQueue<String> uploadQueue;
    private StorageManager storageManager;
    private DistributedDirectory directory;
    private NetworkService networkService;
    private QuorumService quorumService;
    private StateSyncService stateSyncService;

    private final int DEFAULT_REPLICAS = 2;
    private final int CHUNK_SIZE_BYTES = 2 * 1024 * 1024;

    // Hilo para procesar la cola de subidas
    private Thread uploadThread;
    private boolean activo = false;

    private static class NodoCandidato {
        String direccion;
        int id;
        long espacioLibre;

        public NodoCandidato(String direccion, int id, long espacioLibre) {
            this.direccion = direccion;
            this.id = id;
            this.espacioLibre = espacioLibre;
        }
    }

    public StorageCoordinator() {
        this.uploadQueue = new LinkedBlockingQueue<>();
    }

    // Setters para inyección de dependencias
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public void setDistributedDirectory(DistributedDirectory directory) {
        this.directory = directory;
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void setQuorumService(QuorumService quorumService) {
        this.quorumService = quorumService;
    }

    public void setStateSyncService(StateSyncService stateSyncService) {
        this.stateSyncService = stateSyncService;
    }

    /** Función para iniciar hilo que obtiene tareas de uploadQueue */
    public void start() {
        activo = true;
        uploadThread = new Thread(this::processUploadQueue);
        uploadThread.setDaemon(true);
        uploadThread.start();
        ConsoleLogger.info("StorageCoordinator", "Coordinador de almacenamiento iniciado");
    }

    /** Función para detener los hilos del coordinador de almacenamiento */
    public void stop() {
        activo = false;
        if (uploadThread != null && uploadThread.isAlive()) {
            uploadThread.interrupt();
        }
        ConsoleLogger.info("StorageCoordinator", "Coordinador de almacenamiento detenido");
    }

    public void processUploadQueue() {
        while (activo) {
            try {
                // 1. Tomar el siguiente archivo (se queda bloqueado aquí si la cola está vacía)
                String rutaAbsoluta = uploadQueue.take();
                File archivoFisico = new File(rutaAbsoluta);

                if (!archivoFisico.exists()) {
                    ConsoleLogger.error("Coordinator", "El archivo no existe: " + rutaAbsoluta);
                    continue; // Pasa al siguiente de la cola
                }

                String nombreArchivo = archivoFisico.getName();

                // 2. Leemos todos los bytes del archivo (Ideal para archivos pequeños/medianos)
                byte[] fileBytes = Files.readAllBytes(Paths.get(rutaAbsoluta));

                // 3. Calculamos cuántos pedazos van a salir
                // Usamos Math.ceil para redondear hacia arriba (ej. 5MB / 2MB = 2.5 -> 3
                // pedazos)
                int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE_BYTES);

                // 4. Creamos los metadatos y decidimos a quién enviarlo
                DistributedDirectory.FileMetadata metadata = new DistributedDirectory.FileMetadata(nombreArchivo,
                        fileBytes.length);
                List<String> nodosDestino = allocateToNodes(metadata);

                if (nodosDestino.isEmpty()) {
                    ConsoleLogger.advertencia("Coordinator", "No hay nodos disponibles para replicar " + nombreArchivo);
                    continue;
                }

                ConsoleLogger.info("Coordinator",
                        "Partiendo " + nombreArchivo + " en " + totalChunks + " fragmentos...");

                // 5. Partimos el archivo y lo enviamos por la red
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * CHUNK_SIZE_BYTES;
                    int length = Math.min(CHUNK_SIZE_BYTES, fileBytes.length - start);

                    // Copiamos solo la porción que corresponde a este fragmento
                    byte[] chunkData = Arrays.copyOfRange(fileBytes, start, start + length);

                    // Enviamos este fragmento a cada nodo destino asignado
                    for (String nodo : nodosDestino) {
                        // Aquí usaremos un método en NetworkService que envíe el gRPC PeticionSubida
                        networkService.enviarFragmento(nodo, nombreArchivo, i, chunkData, totalChunks);
                    }
                }

                // 6. Actualizamos el directorio global
                metadata.nodeAddresses.addAll(nodosDestino);
                directory.updateMap(metadata);

                if (stateSyncService != null) {
                    stateSyncService.broadcastMapUpdate();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaurar el estado de interrupción
                break;
            } catch (Exception e) {
                ConsoleLogger.error("Coordinator", "Error procesando archivo: " + e.getMessage());
            }
        }
    }

    public List<String> allocateToNodes(DistributedDirectory.FileMetadata fileMetadata) {
        List<String> nodosConectados = networkService.getNodosConectados();
        List<NodoCandidato> candidatos = new ArrayList<>();

        ConsoleLogger.info("Balancer", "Calculando nodos óptimos por espacio en disco...");

        for (String direccion : nodosConectados) {
            // Pedimos las métricas actuales por red
            long espacioLibre = networkService.pedirEspacioLibre(direccion);
            int idNodo = networkService.pedirIdNodo(direccion);

            if (idNodo != -1) {
                candidatos.add(new NodoCandidato(direccion, idNodo, espacioLibre));
            }
        }

        // Ordenamos: Mayor espacio primero. Si hay empate, mayor ID primero.
        candidatos.sort((n1, n2) -> {
            int comparacionEspacio = Long.compare(n2.espacioLibre, n1.espacioLibre);
            if (comparacionEspacio != 0) {
                return comparacionEspacio;
            }
            return Integer.compare(n2.id, n1.id);
        });

        // Seleccionamos los mejores (hasta DEFAULT_REPLICAS)
        List<String> elegidos = new ArrayList<>();
        for (int i = 0; i < Math.min(DEFAULT_REPLICAS, candidatos.size()); i++) {
            elegidos.add(candidatos.get(i).direccion);
        }

        return elegidos;
    }

    /**
     * Rebalanceo Continuo: Revisa si este nodo es el más lleno.
     * Si lo es, transfiere un archivo al nodo más vacío.
     */
    public void balancear() {
        ConsoleLogger.info("Balancer", "Iniciando análisis de balanceo de red...");

        long miEspacioLibre = 0L;
        if (storageManager != null) {
            miEspacioLibre = storageManager.obtenerEspacioDisponible();
        }

        String direccionMasVacia = null;
        long maxEspacioLibre = miEspacioLibre;

        List<String> nodosConectados = networkService.getNodosConectados();
        for (String direccion : nodosConectados) {
            long espacioVecino = networkService.pedirEspacioLibre(direccion);

            // Si el vecino tiene MÁS espacio libre que nuestro récord, él es el más vacío
            if (espacioVecino > maxEspacioLibre) {
                maxEspacioLibre = espacioVecino;
                direccionMasVacia = direccion;
            }
        }

        // Ponemos un umbral de -1 para FORZAR el balanceo durante nuestras pruebas en
        // la misma PC
        // En producción real, esto sería ej. 50 * 1024 * 1024 (50 MB)
        long UMBRAL_BALANCEO = -1;

        if (direccionMasVacia != null && (maxEspacioLibre - miEspacioLibre) > UMBRAL_BALANCEO) {
            ConsoleLogger.advertencia("Balancer",
                    "Soy el nodo más lleno. Evaluando transferir un archivo hacia el más vacío (" + direccionMasVacia
                            + ")");
            transferirArchivoParaBalancear(direccionMasVacia);
        } else {
            ConsoleLogger.exito("Balancer", "La red está balanceada o la diferencia es mínima. No se requiere acción.");
        }
    }

    private void transferirArchivoParaBalancear(String destinoMasVacio) {
        String miDireccion = networkService.getMiDireccion();
        DistributedDirectory.FileMetadata archivoElegido = null;

        // Buscar un archivo que me pertenezca
        for (DistributedDirectory.FileMetadata meta : directory.getGlobalFileMap().values()) {
            if (meta.nodeAddresses.contains(miDireccion)) {
                archivoElegido = meta;
                break;
            }
        }

        if (archivoElegido == null) {
            ConsoleLogger.info("Balancer", "No tengo archivos propios para transferir.");
            return;
        }

        ConsoleLogger.info("Balancer", "Moviendo archivo " + archivoElegido.fileName + " hacia " + destinoMasVacio);

        try {
            // Leer archivo local (Asegúrate de que StorageManager tenga el getter
            // getArchivosDir())
            Path rutaLocal = storageManager.getArchivosDir().resolve(archivoElegido.fileName);
            byte[] fileBytes = Files.readAllBytes(rutaLocal);

            int totalChunks = (int) Math.ceil((double) fileBytes.length / CHUNK_SIZE_BYTES);
            boolean transferenciaExitosa = true;

            // Enviar por fragmentos
            for (int i = 0; i < totalChunks; i++) {
                int start = i * CHUNK_SIZE_BYTES;
                int length = Math.min(fileBytes.length - start, CHUNK_SIZE_BYTES);
                byte[] chunk = Arrays.copyOfRange(fileBytes, start, start + length);

                boolean ok = networkService.enviarFragmento(destinoMasVacio, archivoElegido.fileName, i, chunk,
                        totalChunks);
                if (!ok) {
                    transferenciaExitosa = false;
                    break;
                }
            }

            // Actualizar catálogo y borrar archivo local
            if (transferenciaExitosa) {
                archivoElegido.nodeAddresses.remove(miDireccion);
                if (!archivoElegido.nodeAddresses.contains(destinoMasVacio)) {
                    archivoElegido.nodeAddresses.add(destinoMasVacio);
                }

                directory.updateMap(archivoElegido);
                if (stateSyncService != null) {
                    stateSyncService.broadcastMapUpdate();
                }

                Files.delete(rutaLocal);
                ConsoleLogger.exito("Balancer", "Mudanza completada. Espacio liberado localmente.");
            } else {
                ConsoleLogger.error("Balancer", "Fallo la transferencia hacia " + destinoMasVacio);
            }
        } catch (Exception e) {
            ConsoleLogger.error("Balancer", "Error durante la mudanza: " + e.getMessage());
        }
    }

    /**
     * Invocado por NodeServiceGrpcImpl cuando OTRA computadora nos envía un pedazo.
     */
    public void handleIncomingChunk(String fileId, int chunkIndex, byte[] data, int totalExpected) {
        ConsoleLogger.info("Coordinator", "Recibiendo fragmento externo " + chunkIndex + " de " + fileId);
        // Le pasamos la papa caliente al StorageManager para que lo guarde en el disco
        // local
        storageManager.saveChunk(fileId, chunkIndex, data, totalExpected);
    }

    // Método extra para que el FileWatcher meta cosas a la cola
    public void encolarArchivo(String rutaAbsoluta) {
        uploadQueue.offer(rutaAbsoluta);
        ConsoleLogger.info("StorageCoordinator", "Archivo encolado para subir: " + rutaAbsoluta);
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }
}
