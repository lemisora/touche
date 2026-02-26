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

    // Cuántos votos necesitamos para que una decisión se apruebe
    private int votosRequeridos;

    public QuorumService() {
        // Inicialización temporal (esto debería configurarse después dependiendo de
        // cuántos nodos existan)
        this.votosRequeridos = 2; // Ejemplo: 2 votos para tener mayoría
    }

    // Inyección de dependencias: Le pasamos el NetworkService creado en el Nodo
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    // Método para proponer una votación a la red
    public void proponerAccion(String idAccion, String accion) {
        ConsoleLogger.info("Log", "Quorum: Proponiendo acción '" + idAccion + "': " + accion);

        // Empezamos votando por nosotros mismos (el nodo que propone aprueba su propia
        // idea)
        votosActivos.put(idAccion, 1);

        // Usamos NetworkService para mandar esta propuesta a todos los otros Líderes
        if (networkService != null) {
            ConsoleLogger.info("Log", "Quorum: Enviando propuesta a la red...");
            networkService.solicitarVotos(idAccion);
        } else {
            ConsoleLogger.error("Error", "Quorum: NetworkService no inicializado!");
        }
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

        if (votos >= votosRequeridos) {
            ConsoleLogger.info("Log", ">>> QUORUM ALCANZADO para la acción: " + idAccion + " <<<");
            // Aquí se ejecutaría la acción aprobada...
            return true;
        }
        return false;
    }
}
