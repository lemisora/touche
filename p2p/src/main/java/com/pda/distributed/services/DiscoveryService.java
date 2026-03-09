package com.pda.distributed.services;

import com.pda.distributed.utils.ConsoleLogger;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Servicio encargado de gritar por la red local "¡Existo!" y escuchar a los demás.
 * Actúa como un descubridor automático para evitar poner la IP semilla a mano.
 */
public class DiscoveryService {

    private final int UDP_PORT = 8888;
    private final String BROADCAST_ADDRESS = "255.255.255.255";
    private final String MAGIC_WORD = "PDA_NODE_ANNOUNCEMENT:";

    private final NetworkService networkService;
    private int miPuertoGrpc;

    private Thread hiloListener;
    private Thread hiloBroadcaster;
    private boolean activo = false;

    // Inyectamos el NetworkService que es quien controla a este descubridor
    public DiscoveryService(NetworkService networkService) {
        this.networkService = networkService;
    }

    /**
     * Inicia los hilos de escucha y anuncio.
     * @param miPuertoGrpc El puerto en el que este nodo levantó su servidor gRPC.
     */
    public void iniciar(int miPuertoGrpc) {
        this.miPuertoGrpc = miPuertoGrpc;
        this.activo = true;

        iniciarListener();
        iniciarBroadcaster();

        ConsoleLogger.info("Discovery", "Servicio UDP iniciado en puerto " + UDP_PORT + ". Buscando nodos locales...");
    }

    public void detener() {
        this.activo = false;
        if (hiloListener != null) {
            hiloListener.interrupt();
        }
        if (hiloBroadcaster != null) {
            hiloBroadcaster.interrupt();
        }
        ConsoleLogger.info("Discovery", "Servicio UDP detenido.");
    }

    private void iniciarListener() {
        hiloListener = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(null)) {
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress("0.0.0.0", UDP_PORT));

                byte[] buffer = new byte[1024];

                while (activo) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // Se bloquea esperando mensajes

                    String mensaje = new String(packet.getData(), 0, packet.getLength()).trim();

                    if (mensaje.startsWith(MAGIC_WORD)) {
                        String ipOrigen = packet.getAddress().getHostAddress();
                        procesarAnuncio(mensaje, ipOrigen);
                    }
                }
            } catch (Exception e) {
                if (activo) {
                    ConsoleLogger.error("Discovery", "Error recibiendo UDP: " + e.getMessage());
                }
            }
        });
        hiloListener.setDaemon(true); // Para que no bloquee el apagado del programa
        hiloListener.start();
    }

    private void iniciarBroadcaster() {
        hiloBroadcaster = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);

                while (activo) {
                    // Anunciamos solo nuestro puerto: PDA_NODE_ANNOUNCEMENT:50051
                    String mensaje = MAGIC_WORD + miPuertoGrpc;
                    byte[] buffer = mensaje.getBytes();

                    // 1. Intento general de Broadcast (255.255.255.255)
                    try {
                        socket.send(new DatagramPacket(buffer, buffer.length, InetAddress.getByName(BROADCAST_ADDRESS), UDP_PORT));
                    } catch (Exception ignored) {}

                    // 2. Enviar explícitamente a todas las interfaces locales (LAN/Wi-Fi)
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();
                        if (!networkInterface.isUp()) continue;

                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();
                            if (broadcast == null) continue;

                            try {
                                socket.send(new DatagramPacket(buffer, buffer.length, broadcast, UDP_PORT));
                            } catch (Exception ignored) {}
                        }
                    }

                    // 3. Asegurar Loopback para pruebas en la misma computadora (localhost)
                    try {
                        socket.send(new DatagramPacket(buffer, buffer.length, InetAddress.getByName("127.255.255.255"), UDP_PORT));
                    } catch (Exception ignored) {}

                    // Gritamos cada 3 segundos
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                if (activo) {
                    ConsoleLogger.error("Discovery", "Error enviando UDP: " + e.getMessage());
                }
            }
        });
        hiloBroadcaster.setDaemon(true);
        hiloBroadcaster.start();
    }

    private void procesarAnuncio(String mensaje, String ipOrigen) {
        try {
            // Ejemplo de mensaje: PDA_NODE_ANNOUNCEMENT:50051
            String puertoString = mensaje.substring(MAGIC_WORD.length()).trim();

            // Construimos la clave unificada "IP:Puerto"
            String direccionDescubierta = ipOrigen + ":" + puertoString;
            String miDireccion = networkService.getMiDireccion();

            // 1. Evitar conectarnos a nosotros mismos
            if (direccionDescubierta.equals(miDireccion)) {
                return;
            }

            // 2. Evitar reconectar si ya estamos conectados a este nodo
            if (!networkService.estaConectado(direccionDescubierta)) {
                ConsoleLogger.info("Discovery", "¡Grito UDP escuchado! Nodo local descubierto en: " + direccionDescubierta);

                // 3. ¡Magia! Intentamos unirnos usando este nodo recién descubierto como semilla
                networkService.joinNetwork(direccionDescubierta, miDireccion, networkService.getMiId());
            }

        } catch (Exception e) {
            ConsoleLogger.advertencia("Discovery", "Mensaje UDP malformado ignorado: " + mensaje);
        }
    }
}