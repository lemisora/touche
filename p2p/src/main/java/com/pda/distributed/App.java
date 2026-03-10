package com.pda.distributed;

import com.pda.distributed.core.Nodo;
import com.pda.distributed.utils.ConsoleLogger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.pda.distributed.storage.DistributedDirectory;
import java.util.concurrent.Callable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Map;
import java.util.Enumeration;
import java.util.Scanner;

@Command(name = "nodo-p2p", mixinStandardHelpOptions = true, version = "1.0", description = "Inicia un nodo P2P con auto-descubrimiento UDP y gRPC.")
public class App implements Callable<Integer> {

    @Option(names = { "-i", "--ip" }, defaultValue = "localhost", description = "IP local de este nodo.")
    private String ip;

    @Option(names = { "-I", "--id" }, defaultValue = "0", description = "ID numérico del Nodo (para desempates Bully).")
    private int id;

    @Option(names = { "-n", "--name" }, defaultValue = "Nodo", description = "Nombre amigable del nodo.")
    private String name;

    @Option(names = { "-s",
            "--seed" }, description = "IP:PUERTO de un nodo semilla para conectarse directamente (ej. 192.168.1.10:50051).")
    private String seedNode;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Resolver la IP local real si no se pasó una por argumento
        if ("localhost".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = getLocalNetworkIp();
        }

        // Generar ID aleatorio si no se proporcionó
        if (id == 0) {
            this.id = (int) (System.currentTimeMillis() % 10000);
        }

        String finalName = name + "-" + id;
        ConsoleLogger.info("App", "Preparando nodo " + finalName + " con IP: " + ip + "...");

        // Instanciar el nodo (solo con ID, IP y Nombre)
        Nodo miNodo = new Nodo(id, ip, finalName);

        try {
            // El nodo decide su destino: Si hay semilla se une, si no, es Génesis.
            miNodo.start(seedNode);
        } catch (Exception e) {
            ConsoleLogger.error("App", "Error crítico al arrancar: " + e.getMessage());
            return 1;
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
            Scanner scanner = new Scanner(System.in);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }

            System.out.println("=================================================");
            System.out.println("  CONSOLA INTERACTIVA DEL NODO: " + finalName);
            System.out.println("  Comandos disponibles:");
            System.out.println("  - 'info' -> Ver estado del nodo y conexiones.");
            System.out.println("  - 'archivos' -> Ver lista de archivos distribuidos.");
            System.out.println("  - 'subir <ruta>' -> Distribuir un archivo local.");
            System.out.println("  - 'salir' -> Apagar el nodo.");
            System.out.println("=================================================");

            while (true) {
                System.out.print("> ");
                String comando = scanner.nextLine().trim();

                if ("salir".equalsIgnoreCase(comando)) {
                    System.exit(0);
                } else if ("info".equalsIgnoreCase(comando)) {
                    System.out.println("--- INFO DEL NODO ---");
                    System.out.println("ID: " + miNodo.getId());
                    System.out.println("Dirección: " + miNodo.getNodeAddress());
                    System.out.println("Rol: " + miNodo.getCurrentRole());
                    System.out.println("Anillo: " + miNodo.getCurrentRingID());
                    System.out.println("Estado: " + miNodo.getCurrentState());

                    System.out.println("\n--- CONEXIONES ACTIVAS ---");
                    java.util.List<String> conectados = miNodo.getNodosConectados();
                    if (conectados.isEmpty()) {
                        System.out.println("No hay conexiones activas gRPC.");
                    } else {
                        for (String conn : conectados) {
                            System.out.println("- " + conn);
                        }
                    }

                    System.out.println("\n--- REGISTRO GLOBAL (QUORUM) ---");
                    Map<String, com.pda.distributed.core.RingType> registro = miNodo.getNodeRegistry();
                    if (registro.isEmpty()) {
                        System.out.println("Registro vacío.");
                    } else {
                        for (Map.Entry<String, com.pda.distributed.core.RingType> entry : registro.entrySet()) {
                            System.out.println("- " + entry.getKey() + " [" + entry.getValue() + "]");
                        }
                    }
                    System.out.println("---------------------");
                } else if ("archivos".equalsIgnoreCase(comando)) {
                    Map<String, DistributedDirectory.FileMetadata> archivos = miNodo.getArchivosDistribuidos();
                    if (archivos.isEmpty()) {
                        System.out.println("No hay archivos distribuidos registrados en el sistema.");
                    } else {
                        System.out.println("--- ARCHIVOS DISTRIBUIDOS ---");
                        for (Map.Entry<String, DistributedDirectory.FileMetadata> entry : archivos.entrySet()) {
                            DistributedDirectory.FileMetadata meta = entry.getValue();
                            System.out.println("- " + meta.fileName + " (" + meta.sizeBytes + " bytes)");
                            System.out.println("  Ubicaciones: " + String.join(", ", meta.nodeAddresses));
                        }
                        System.out.println("-----------------------------");
                    }
                } else if (comando.toLowerCase().startsWith("subir ")) {
                    String rutaArchivo = comando.substring(6).trim();
                    System.out.println("Disparando subida manual para: " + rutaArchivo);
                    miNodo.forzarSubidaManual(rutaArchivo);
                } else if (!comando.isEmpty()) {
                    System.out.println("Comando no reconocido.");
                }
            }
        });

        consolaInteractiva.setDaemon(true);
        consolaInteractiva.start();

        // Mantiene el hilo principal vivo
        Thread.currentThread().join();
        return 0;
    }

    private String getLocalNetworkIp() {
        String fallbackIp = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("100.")) { // Agregué "100." para Tailscale
                            return ip;
                        } else if (fallbackIp.equals("127.0.0.1")) {
                            fallbackIp = ip;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallbackIp;
    }
}
