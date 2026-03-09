package com.pda.distributed.services;

import com.pda.distributed.core.RingType;
import com.pda.distributed.utils.ConsoleLogger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuorumService {

    private final NetworkService networkService;
    
    // Límite estático de líderes
    private final int MAX_LEADERS_RING_A = 2;
    
    // Registro global de nodos (K: "IP:Puerto", V: Tipo de Anillo)
    private final Map<String, RingType> nodeRegistry;

    // ID de nuestro propio nodo para el algoritmo Bully
    private int miNodeId = -1;

    public QuorumService(NetworkService networkService) {
        this.networkService = networkService;
        this.nodeRegistry = new ConcurrentHashMap<>();
    }

    public void setMiNodeId(int id) {
        this.miNodeId = id;
    }

    /**
     * Registra un nodo directamente (usado por el propio nodo cuando nace como Génesis).
     */
    public void registrarNodo(String nodeAddress, RingType ringType) {
        // Añadir el nodeAddress y ringType al nodeRegistry.
        nodeRegistry.put(nodeAddress, ringType);
        // Imprimir en consola que se registró un nuevo nodo.
        ConsoleLogger.info("Quorum", "Se registró un nuevo nodo: " + nodeAddress);
    }

    /**
     * Evalúa la petición de un nodo nuevo que quiere unirse a la red.
     * @return El nombre del anillo asignado ("RING_A" o "RING_B").
     */
    public String evaluarIngresoNuevoNodo(String nodeAddress, int nodeId) {
        long numIntegrantesAnilloA = nodeRegistry
                .values()
                .stream()
                .filter(anillo -> anillo == RingType.RING_A)
                .count();

        if (numIntegrantesAnilloA < MAX_LEADERS_RING_A) {
            nodeRegistry.put(nodeAddress, RingType.RING_A);
            return "RING_A";
        } else {
            nodeRegistry.put(nodeAddress, RingType.RING_B);
            return "RING_B";
        }
    }

    /**
     * Algoritmo Bully: Evalúa si le damos nuestro voto a un candidato que quiere ser líder.
     * Regla básica del Bully: Solo votamos "Sí" si el ID del candidato es MAYOR que el nuestro.
     */
    public boolean evaluarVotoBully(int candidatoId) {
	    return candidatoId > this.miNodeId;
    }

    /**
     * Inicia el proceso de elección para ver si este nodo se convierte en el líder principal.
     */
    public void executeBullyElection() {
        ConsoleLogger.info("Quorum", "Iniciando elección Bully. Mi ID: " + miNodeId);
        long votosPositivosRecibidos = 0;
        // Obtener del nodeRegistry todos los nodos que pertenezcan al RING_A.
        List<String> direccionesLideres = nodeRegistry.entrySet().stream()
                .filter(entry -> entry.getValue() == RingType.RING_A)
                .map(Map.Entry::getKey)
                .toList();
        // Usar networkService para enviar un "PeticionVoto" a todos esos nodos.
        for (String direccion : direccionesLideres) {
            boolean votoRecibido = networkService.enviarPeticionVoto(direccion, this.miNodeId);
            if (votoRecibido)
                votosPositivosRecibidos++;
        }

        // Calculamos la mayoría (la mitad más uno)
        int mayoriaNecesaria = (direccionesLideres.size() / 2) + 1;
        ConsoleLogger.info("Quorum", "Votos obtenidos: " + votosPositivosRecibidos + " de " + direccionesLideres.size());

        // Si la mayoría acepta, nos proclamamos líder.
        if (votosPositivosRecibidos >= mayoriaNecesaria) {
            ConsoleLogger.exito("Quorum", "¡Gané la elección! Soy el líder principal del anillo.");
            // TODO (Futuro): Avisarle al StateSyncService que somos el nuevo líder absoluto
            // para que coordine los archivos.
        } else {
            ConsoleLogger.advertencia("Quorum", "No obtuve los votos suficientes. Cediendo el liderazgo.");
        }
    }

    /**
     * Propone una acción (ej. borrar un archivo, cambiar de líder) y espera mayoría.
     */
    public boolean proposeAction(String action) {
        ConsoleLogger.info("Quorum", "Proponiendo acción a la red: " + action);

        long votosPositivosRecibidos = 0;

        List<String> direccionesLideres = nodeRegistry.entrySet().stream()
                .filter(entry -> entry.getValue() == RingType.RING_A)
                .map(Map.Entry::getKey)
                .toList();

        for (String direccion : direccionesLideres) {
            boolean votoRecibido = networkService.enviarPeticionVoto(direccion, this.miNodeId);
            if (votoRecibido)
                votosPositivosRecibidos++;
        }

        // Calculamos la mayoría (la mitad más uno)
        int mayoriaNecesaria = (direccionesLideres.size() / 2) + 1;
        ConsoleLogger.info("Quorum", "Votos obtenidos: " + votosPositivosRecibidos + " de " + direccionesLideres.size());

        return votosPositivosRecibidos >= mayoriaNecesaria;
    }
}