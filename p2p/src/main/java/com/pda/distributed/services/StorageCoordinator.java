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

    private static final int CHUNK_SIZE = 2 * 1024 * 1024;
    private int numeroFragmento = 0;

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

    // private void ejecutarSubidaReal(File archivo, java.util.List<String> nodosDestino) {
    //     String nombreArchivo = archivo.getName();

    //     // Registrar en el directorio lógico
    //     if (distributedDirectory != null) {
    //         System.out.println("[Coordinator] Registrando metadatos en DistributedDirectory...");
    //         for (String nodo : nodosDestino) {
    //             distributedDirectory.registrarUbicacion(nombreArchivo, nodo);
    //         }
    //     }

    //     // Fragmentar y Enviar por la red a los Workers elegidos
    //     if (networkService != null) {
    //         byte[] buffer = new byte[CHUNK_SIZE];
    //         int bytesLeidos;
    //         numeroFragmento = 0;

    //         // FileInputStream es súper eficiente: solo carga a RAM lo que le pedimos (los
    //         // 2MB del buffer)
    //         try (java.io.FileInputStream fis = new java.io.FileInputStream(archivo)) {

    //             while ((bytesLeidos = fis.read(buffer)) != -1) {
    //                 // Si el pedazo leído es menor a 2MB (ej. al final del archivo), recortamos el
    //                 // arreglo
    //                 byte[] chunkExacto = (bytesLeidos == CHUNK_SIZE) ? buffer
    //                         : java.util.Arrays.copyOf(buffer, bytesLeidos);

    //                 // Nombramos el pedacito (ej. "gnome_os_installer.iso_part0")
    //                 String idFragmento = nombreArchivo + "_part" + numeroFragmento;

    //                 for (String nodoId : nodosDestino) {
    //                     String[] partes = nodoId.split(":");
    //                     int puerto = partes.length > 1 ? Integer.parseInt(partes[1]) : Integer.parseInt(nodoId);

    //                     // Lógica inteligente: ¿Soy yo mismo o es otro nodo?
    //                     if (puerto == networkService.getMiPuerto()) {
    //                         // Guardamos directamente en nuestro disco físico
    //                         procesarFragmentoEntrante(idFragmento, chunkExacto);
    //                     } else {
    //                         // Enviamos el pedazo de 2MB por la red
    //                         ConsoleLogger.info("Coordinator",
    //                                 "Enviando " + idFragmento + " vía gRPC al puerto " + puerto);
    //                         networkService.enviarFragmentoBasico(puerto, idFragmento, chunkExacto);
    //                     }

    //                     // Opcional: Registrar que este fragmento específico vive en ese nodo
    //                     if (distributedDirectory != null) {
    //                         distributedDirectory.registrarUbicacion(idFragmento, nodoId);
    //                     }
    //                 }
    //                 numeroFragmento++;
    //             }
    //         } catch (Exception e) {
    //             ConsoleLogger.error("Coordinator", "Error al leer/fragmentar el archivo: " + e.getMessage());
    //         }
    //     } else {
    //         System.out.println("[Coordinator] (Simulación) NetworkService no disponible.");
    //     }

    //     ConsoleLogger.exito("Coordinator", "Procesamiento de subida concluido para: " + nombreArchivo
    //             + " (Total fragmentos: " + numeroFragmento + ")");
    // }

    private void ejecutarSubidaReal(File archivo, java.util.List<String> nodosDestinoAprobados) {
            String nombreArchivo = archivo.getName();
    
            if (networkService != null) {
                // 1. Obtenemos todos los puertos de los nodos vivos en la red
                java.util.List<Integer> puertosActivos = networkService.getConnectedPorts();
                // 2. Nos agregamos a nosotros mismos a la lista (para también guardar pedazos)
                puertosActivos.add(networkService.getMiPuerto());
                
                // Ordenamos para que la repartición sea predecible
                java.util.Collections.sort(puertosActivos);
    
                byte[] buffer = new byte[CHUNK_SIZE];
                int bytesLeidos;
                int numeroFragmento = 0;
    
                ConsoleLogger.info("Coordinator", "Iniciando distribución P2P entre " + puertosActivos.size() + " nodos...");
    
                try (java.io.FileInputStream fis = new java.io.FileInputStream(archivo)) {
                    
                    while ((bytesLeidos = fis.read(buffer)) != -1) {
                        byte[] chunkExacto = (bytesLeidos == CHUNK_SIZE) ? buffer : java.util.Arrays.copyOf(buffer, bytesLeidos);
                        String idFragmento = nombreArchivo + "_part" + numeroFragmento;
    
                        // MAGIA P2P (Round-Robin): Elegimos el nodo usando el residuo de la división
                        // Ej. Si hay 2 nodos: Fragmento 0 -> Nodo 0, Frag 1 -> Nodo 1, Frag 2 -> Nodo 0...
                        int puertoElegido = puertosActivos.get(numeroFragmento % puertosActivos.size());
    
                        // Enviamos o guardamos según quién fue el elegido
                        if (puertoElegido == networkService.getMiPuerto()) {
                            ConsoleLogger.info("Coordinator", "➜ " + idFragmento + " se queda en disco local.");
                            procesarFragmentoEntrante(idFragmento, chunkExacto);
                            
                            if (distributedDirectory != null) {
                                distributedDirectory.registrarUbicacion(idFragmento, "localhost:" + puertoElegido);
                            }
                        } else {
                            ConsoleLogger.info("Coordinator", "➔ Enviando " + idFragmento + " vía gRPC al puerto " + puertoElegido);
                            networkService.enviarFragmentoBasico(puertoElegido, idFragmento, chunkExacto);
                            
                            if (distributedDirectory != null) {
                                distributedDirectory.registrarUbicacion(idFragmento, "remote:" + puertoElegido);
                            }
                        }
                        numeroFragmento++;
                    }
                } catch (Exception e) {
                    ConsoleLogger.error("Coordinator", "Error al leer/fragmentar el archivo: " + e.getMessage());
                }
            } else {
                System.out.println("[Coordinator] (Simulación) NetworkService no disponible.");
            }
    
            ConsoleLogger.exito("Coordinator", "Distribución concluida para: " + nombreArchivo + " (Total fragmentos: " + numeroFragmento + ")");
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
