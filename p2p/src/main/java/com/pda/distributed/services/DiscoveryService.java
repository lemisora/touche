package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

// Se encarga de gritar por la red local "¡Existo!" y escuchar a los demás
public class DiscoveryService {

    private final int UDP_PORT = 8888;
    private final String BROADCAST_ADDRESS = "255.255.255.255";
    private final String MAGIC_WORD = "PDA_NODE_ANNOUNCEMENT:";

    private NetworkService networkService;
    private int miPuertoGrpc;

    private Thread hiloListener;
    private Thread hiloBroadcaster;
    private boolean activo = false;

    // Para evitar reconectarnos a nosotros mismos
    private final Set<Integer> puertosIgnorados = new HashSet<>();

    private String currRingId = "A"; // Default a "A"

    public DiscoveryService() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void setRingId(String ringId) {
        this.currRingId = ringId;
    }

    public void iniciar(int miPuertoGrpc) {
        this.miPuertoGrpc = miPuertoGrpc;
        this.activo = true;
        this.puertosIgnorados.add(miPuertoGrpc); // Yo no me auto-descubro

        iniciarListener();
        iniciarBroadcaster();

        ConsoleLogger.info("Discovery",
                "Servicio de auto-descubrimiento iniciado (UDP " + UDP_PORT + ") [Anillo: " + currRingId + "]");
    }

    public void detener() {
        this.activo = false;
        if (hiloListener != null)
            hiloListener.interrupt();
        if (hiloBroadcaster != null)
            hiloBroadcaster.interrupt();
    }

    private void iniciarListener() {
        hiloListener = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                // Configuramos para poder reusar el puerto si hay varios nodos en el mismo
                // local
                socket.setReuseAddress(true);
                socket.bind(new java.net.InetSocketAddress(UDP_PORT));

                byte[] buffer = new byte[1024];

                while (activo) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // Se queda esperando mensajes

                    String mensaje = new String(packet.getData(), 0, packet.getLength());

                    if (mensaje.startsWith(MAGIC_WORD)) {
                        procesarAnuncio(mensaje, packet.getAddress().getHostAddress());
                    }
                }
            } catch (Exception e) {
                if (activo) {
                    ConsoleLogger.error("Discovery", "Error recibiendo UDP: " + e.getMessage());
                }
            }
        });
        hiloListener.start();
    }

    private void iniciarBroadcaster() {
        hiloBroadcaster = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);

                while (activo) {
                    // Ahora anunciamos: PDA_NODE_ANNOUNCEMENT:<PUERTO>:<ANILLO>
                    String mensaje = MAGIC_WORD + miPuertoGrpc + ":" + currRingId;
                    byte[] buffer = mensaje.getBytes();

                    DatagramPacket packet = new DatagramPacket(
                            buffer,
                            buffer.length,
                            InetAddress.getByName(BROADCAST_ADDRESS),
                            UDP_PORT);

                    socket.send(packet);

                    // Gritamos cada 3 segundos
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                if (activo) {
                    ConsoleLogger.error("Discovery", "Error enviando UDP: " + e.getMessage());
                }
            }
        });
        hiloBroadcaster.start();
    }

    private void procesarAnuncio(String mensaje, String ipOrigen) {
        try {
            // mensaje = PDA_NODE_ANNOUNCEMENT:50000:A
            String cuerpo = mensaje.substring(MAGIC_WORD.length());
            String[] partes = cuerpo.split(":");
            int puertoGrpcAjeno = Integer.parseInt(partes[0]);
            String anilloAjeno = "A";
            if (partes.length > 1) {
                anilloAjeno = partes[1].trim();
            }

            // Validar que pertenezcan al mismo anillo
            if (!this.currRingId.equals(anilloAjeno)) {
                return; // Ignorar nodos de otro anillo
            }

            // Verificamos que no seamos nosotros mismos y que no estemos conectados ya
            if (!puertosIgnorados.contains(puertoGrpcAjeno) && networkService != null) {
                if (!networkService.estaConectado(puertoGrpcAjeno)) {
                    ConsoleLogger.info("Discovery",
                            "¡Nuevo nodo descubierto automáticamente en " + ipOrigen + ":" + puertoGrpcAjeno
                                    + " [Anillo " + anilloAjeno + "]!");
                    networkService.sendPing(ipOrigen, puertoGrpcAjeno);
                }
            }
        } catch (Exception e) {
            ConsoleLogger.advertencia("Discovery", "Mensaje UDP malformado ignorado: " + mensaje);
        }
    }
}
