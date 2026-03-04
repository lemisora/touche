package com.pda.distributed;

import com.pda.distributed.core.Nodo;
import com.pda.distributed.core.NodeRole;
import com.pda.distributed.utils.ConsoleLogger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "nodo-p2p", mixinStandardHelpOptions = true, version = "1.0", description = "Inicia un nodo P2P con auto-descubrimiento UDP y almacenamiento distribuido.")
public class App implements Callable<Integer> {

    @Option(names = { "-i", "--ip" }, defaultValue = "localhost", description = "IP local de este nodo.")
    private String ip;

    @Option(names = {"-I", "--id"}, defaultValue = "0", description = "ID del Nodo, si no se asigna se genera uno en aleatorio")
    private int id;

    @Option(names = { "-n", "--name" }, defaultValue = "Nodo", description = "Nombre del nodo.")
    private String name;

    @Option(names = { "-r", "--role" }, defaultValue = "WORKER", description = "Rol inicial (LEADER o WORKER).")
    private NodeRole initialRole;

    @Option(names = { "-s", "--seed" }, description = "IP:PUERTO de un nodo semilla en Tailscale para conectarse directamente.")
    private String seedNode;

    public static void main(String[] args) {
        // Picocli procesa los argumentos de la terminal
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if ("localhost".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = getLocalNetworkIp();
        }

        // Generar un ID numérico aleatorio en caso de no haber sido ingresado
        // TODO: Añadir verificación para cambiar identificador en caso de ya existir en el sistema
        if (id == 0) {
            this.id = (int) (System.currentTimeMillis() % 10000);
        }

        String finalName = name + "-" + id;

        ConsoleLogger.info("App", "Preparando nodo " + finalName + " con IP: " + ip + "...");

        // Instanciar el nodo (el puerto se descubrirá solo)
        Nodo miNodo = new Nodo(id, ip, finalName, initialRole);

        try {
            miNodo.start();
        } catch (Exception e) {
            ConsoleLogger.error("App", "Error crítico al arrancar: " + e.getMessage());
            return 1;
        }

        if (seedNode != null && !seedNode.isEmpty()) {
            try {
                String[] partes = seedNode.split(":");
                String ipSemilla = partes[0];
                int puertoSemilla = Integer.parseInt(partes[1]);

                ConsoleLogger.info("App", "Conectando al nodo semilla en Tailscale -> " + seedNode);
                // Damos un segundo para que el servidor gRPC local termine de arrancar bien
                Thread.sleep(1000);
                miNodo.connectToPeer(ipSemilla, puertoSemilla);

            } catch (Exception e) {
                ConsoleLogger.error("App", "Formato de nodo semilla inválido. Use IP:PUERTO (ej. 100.10.20.30:50000)");
            }
        }

        // Shutdown Hook para apagar todo limpiamente (Control+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                miNodo.stop();
            } catch (InterruptedException ignored) {
            }
        }));

        // Hilo para la consola interactiva (CLI)
        Thread consolaInteractiva = new Thread(() -> {
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            try {
                // Pequeño retardo para no pisar el log de inicio
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            System.out.println("=================================================");
            System.out.println("  CONSOLA INTERACTIVA DEL NODO: " + finalName);
            System.out.println("  Comandos disponibles:");
            System.out.println("  - 'info' -> Ver estado del nodo y conexiones.");
            System.out.println("  - 'archivos' -> Ver lista de archivos distribuidos.");
            System.out.println("  - 'subir <ruta_archivo>' -> Distribuir un archivo local.");
            System.out.println("  - 'salir' -> Apagar el nodo.");
            System.out.println("=================================================");

            while (true) {
                System.out.print("> ");
                String comando = scanner.nextLine().trim();

                if ("salir".equalsIgnoreCase(comando)) {
                    System.exit(0);
                } else if ("info".equalsIgnoreCase(comando)) {
                    System.out.println(miNodo.getNetworkInfo());
                } else if ("archivos".equalsIgnoreCase(comando)) {
                    System.out.println(miNodo.getArchivosDistribuidos());
                } else if (comando.toLowerCase().startsWith("subir ")) {
                    String rutaArchivo = comando.substring(6).trim();
                    ConsoleLogger.info("App", "Disparando subida manual para archivo: " + rutaArchivo);
                    miNodo.forzarSubidaManual(rutaArchivo);
                } else if (!comando.isEmpty()) {
                    System.out.println("Comando no reconocido. Escribe 'info', 'archivos', 'subir <ruta>' o 'salir'.");
                }
            }
        });
        consolaInteractiva.setDaemon(true);
        consolaInteractiva.start();

        miNodo.blockUntilShutdown();
        return 0;
    }

    private String getLocalNetworkIp() {
        String fallbackIp = "127.0.0.1";
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface
                    .getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.")) {
                            return ip;
                        } else if (fallbackIp.equals("127.0.0.1")) {
                            fallbackIp = ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar y usar localhost como fallback
        }
        return fallbackIp;
    }
}
