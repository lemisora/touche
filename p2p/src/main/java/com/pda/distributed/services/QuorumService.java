package com.pda.distributed.services;

import com.pda.distributed.core.RingType;
import com.pda.distributed.utils.ConsoleLogger;

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
        // TODO: Añadir el nodeAddress y ringType al nodeRegistry.
        // TODO: Imprimir en consola que se registró un nuevo nodo.
    }

    /**
     * Evalúa la petición de un nodo nuevo que quiere unirse a la red.
     * @return El nombre del anillo asignado ("RING_A" o "RING_B").
     */
    public String evaluarIngresoNuevoNodo(String nodeAddress, int nodeId) {
        // TODO: 1. Contar cuántos nodos en nodeRegistry tienen el valor RingType.RING_A.
        // TODO: 2. Si hay menos de MAX_LEADERS_RING_A, asignarle RING_A. Si no, asignarle RING_B.
        // TODO: 3. Guardar el nuevo nodo en nodeRegistry con su anillo correspondiente.
        // TODO: 4. Retornar el String del anillo ("RING_A" o "RING_B") para responderle.
        return "RING_B"; // Placeholder
    }

    /**
     * Algoritmo Bully: Evalúa si le damos nuestro voto a un candidato que quiere ser líder.
     * Regla básica del Bully: Solo votamos "Sí" si el ID del candidato es MAYOR que el nuestro.
     */
    public boolean evaluarVotoBully(int candidatoId) {
        // TODO: 1. Comparar candidatoId con this.miNodeId.
        // TODO: 2. Si candidatoId > miNodeId, retornamos true (cedemos el liderazgo).
        // TODO: 3. Si candidatoId < miNodeId, retornamos false (nosotros somos más "grandes", así que iniciamos nuestra propia elección).
        return false; // Placeholder
    }

    /**
     * Inicia el proceso de elección para ver si este nodo se convierte en el líder principal.
     */
    public void executeBullyElection() {
        ConsoleLogger.info("Quorum", "Iniciando elección Bully. Mi ID: " + miNodeId);
        // TODO: 1. Obtener del nodeRegistry todos los nodos que pertenezcan al RING_A.
        // TODO: 2. Usar networkService para enviar un "PeticionVoto" a todos esos nodos.
        // TODO: 3. Contar cuántos votos "true" recibimos.
        // TODO: 4. Si la mayoría acepta, nos proclamamos líder.
    }

    /**
     * Propone una acción (ej. borrar un archivo, cambiar de líder) y espera mayoría.
     */
    public boolean proposeAction(String action) {
        ConsoleLogger.info("Quorum", "Proponiendo acción a la red: " + action);
        // TODO: 1. Enviar la propuesta a los líderes del RING_A.
        // TODO: 2. Si > 50% dice que sí, retornar true.
        return true; // Placeholder
    }
}