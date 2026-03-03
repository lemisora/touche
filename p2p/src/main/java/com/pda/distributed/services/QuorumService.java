package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Administra las votaciones y toma de decisiones
public class QuorumService {

    // Nuestro puente para enviar mensajes a otros nodos
    private NetworkService networkService;

    // Mapa para guardar los votos recibidos: ID de la acción -> Cantidad de votos
    // Usamos ConcurrentHashMap porque varios hilos (peticiones gRPC) podrían votar
    // al mismo tiempo
    private final Map<String, Integer> votosActivos = new ConcurrentHashMap<>();

    // Callbacks dinámicos por ID de acción
    private final Map<String, Runnable> callbacksActivos = new ConcurrentHashMap<>();

    // Callback que se ejecutará al ganar una elección
    private Runnable onElectionWon;

    public QuorumService() {
        // Inicialización
    }

    public void setOnElectionWon(Runnable onElectionWon) {
        this.onElectionWon = onElectionWon;
    }

    // Inyección de dependencias: Le pasamos el NetworkService creado en el Nodo
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    // Método para proponer una votación a la red con un callback específico
    public void proponerAccion(String idAccion, String accion, Runnable callback) {
        ConsoleLogger.info("Log", "Quorum: Proponiendo acción '" + idAccion + "': " + accion);

        if (callback != null) {
            callbacksActivos.put(idAccion, callback);
        }

        // Empezamos votando por nosotros mismos (el nodo que propone aprueba su propia
        // idea)
        votosActivos.put(idAccion, votosActivos.getOrDefault(idAccion, 0) + 1);

        // Verificamos inmediatamente por si ya tenemos el quórum (ej. un solo nodo)
        if (verificarQuorum(idAccion)) {
            return;
        }

        // Usamos NetworkService para mandar esta propuesta a todos los otros Líderes
        if (networkService != null) {
            ConsoleLogger.info("Log", "Quorum: Enviando propuesta a la red...");
            networkService.solicitarVotos(idAccion);
        } else {
            ConsoleLogger.error("Error", "Quorum: NetworkService no inicializado!");
        }
    }

    // Método para proponer una votación a la red sin callback dinámico
    public void proponerAccion(String idAccion, String accion) {
        proponerAccion(idAccion, accion, null);
    }

    // Método que se llama cuando recibimos el voto de un compañero
    public void recibirVoto(String idAccion, boolean acepta) {
        if (acepta) {
            // Sumamos 1 al conteo actual de esta acción (si no existe, empezamos en 0 + 1)
            int votosActuales = votosActivos.getOrDefault(idAccion, 0) + 1;
            votosActivos.put(idAccion, votosActuales);

            System.out
                    .println("Quorum: Voto a favor recibido para '" + idAccion + "'. Votos totales: " + votosActuales);

            // Verificamos si ya ganamos
            verificarQuorum(idAccion);
        } else {
            ConsoleLogger.info("Log", "Quorum: Voto en contra recibido para '" + idAccion + "'");
        }
    }

    // Método privado para revisar si ya juntamos suficientes votos
    private boolean verificarQuorum(String idAccion) {
        int votos = votosActivos.getOrDefault(idAccion, 0);

        // Calculamos el quorum dinámicamente basado en los nodos conectados
        int totalNodos = 1; // Me incluyo a mí mismo
        if (networkService != null) {
            totalNodos += networkService.getConnectedNodesCount();
        }
        int votosRequeridos = (totalNodos / 2) + 1;

        if (votos >= votosRequeridos) {
            ConsoleLogger.info("Log", ">>> QUORUM ALCANZADO para la acción: " + idAccion + " con " + votos + "/"
                    + votosRequeridos + " votos <<<");

            // Evitamos ejecutarlo múltiples veces limpiando el progreso
            votosActivos.remove(idAccion);

            // Verificamos si hay un callback específico para esta acción
            if (callbacksActivos.containsKey(idAccion)) {
                Runnable callback = callbacksActivos.remove(idAccion);
                callback.run();
            }
            // Si no, volvemos a la lógica por defecto de ELECTION
            else if ("ELECTION".equals(idAccion) && onElectionWon != null) {
                onElectionWon.run();
            }
            return true;
        }
        return false;
    }
}
