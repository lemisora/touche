package com.pda.distributed.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Sincroniza el estado del sistema entre los líderes activos
public class StateSyncService {

    private NetworkService networkService;

    // Guarda cuándo fue la última vez que obtuvimos respuesta de los demás nodos.
    // Puerto -> Milisegundos
    private final Map<Integer, Long> ultimaConexion = new ConcurrentHashMap<>();

    // El hilo en segundo plano (Gossip) que latirá constantemente
    private Thread hiloGossip;

    // Bandera para saber si el hilo debe seguir corriendo
    private boolean activo = false;

    public StateSyncService() {
        // Inicialización
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    // Inicia el hilo en segundo plano para que envíe el estado y revise muertos
    public void iniciarGossip(int miPuerto) {
        activo = true;
        hiloGossip = new Thread(() -> {
            while (activo) {
                try {
                    // Cada 5 segundos, manda estado y verifica
                    Thread.sleep(5000);

                    enviarEstadoRed("Estado actual del Nodo " + miPuerto);
                    detectarLideresMuertos();

                } catch (InterruptedException e) {
                    System.out.println("StateSync: Hilo de sincronización interrumpido.");
                    activo = false;
                }
            }
        });
        hiloGossip.start();
        System.out.println("StateSync: Hilo de Gossip (latido) iniciado...");
    }

    public void detenerGossip() {
        activo = false;
        if (hiloGossip != null) {
            hiloGossip.interrupt();
        }
    }

    // Método que manda mi estado hacia los demás (broadcastStateUpdate en UML)
    private void enviarEstadoRed(String miEstado) {
        if (networkService != null) {
            // System.out.println("StateSync: Sincronizando estado (" + miEstado + ") a la
            // red...");
            networkService.sincronizarEstado(miEstado); // Crearemos este método en NetworkService luego
        }
    }

    // Se llama cuando otro nodo nos manda su estado por RPC (receiveStateUpdate en
    // UML)
    public void recibirEstado(String estadoRecibido, int puertoOrigen) {
        // Guardamos el momento exacto (en milisegundos) en que supimos de él
        long tiempoActual = System.currentTimeMillis();
        ultimaConexion.put(puertoOrigen, tiempoActual);

        System.out.println("StateSync: Estado recibido de puerto " + puertoOrigen + ". Actualizando marca de tiempo a "
                + tiempoActual);
    }

    // Revisa cuáles nodos llevan mucho tiempo sin reportarse (detectDeadLeaders en
    // UML)
    private void detectarLideresMuertos() {
        long tiempoActual = System.currentTimeMillis();

        // Si un nodo no manda estado en 15 segundos (15000ms), lo damos por muerto
        long tiempoMaximoInactivo = 15000;

        for (Map.Entry<Integer, Long> nodo : ultimaConexion.entrySet()) {
            int puerto = nodo.getKey();
            long ultimaVez = nodo.getValue();

            if (tiempoActual - ultimaVez > tiempoMaximoInactivo) {
                System.out.println("¡ALERTA! El nodo en el puerto " + puerto + " lleva " + (tiempoActual - ultimaVez)
                        + "ms sin responder. Declarando MUERTO.");
                // Aquí deberíamos decirle a NetworkService que corte la conexión
                // y sacarlo de la libreta de direcciones
                ultimaConexion.remove(puerto);
            }
        }
    }
}
