package com.pda.distributed;

import com.pda.distributed.utils.ConsoleLogger;

import java.io.IOException;

import com.pda.distributed.core.Nodo;
import com.pda.distributed.core.NodeRole;

public class App {
    /*
     * Uso: mvn compile exec:java -Dexec.mainClass="com.pda.distributed.App"
     * -Dexec.args="<mi_puerto> <ip_destino> <puerto_destino>"
     * 
     * Argumentos:
     * - args[0]: Puerto de este nodo
     * - args[1]: IP del nodo destino
     * - args[2]: Puerto del nodo destino
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 3) {
            ConsoleLogger.error("App",
                    "Uso: mvn compile exec:java -Dexec.mainClass=\"com.pda.distributed.App\" -Dexec.args=\"<puerto> <ip> <puerto>\"");
            return;
        }

        int miPuerto = Integer.parseInt(args[0]);
        String ipDestino = args[1];
        int puertoDestino = Integer.parseInt(args[2]);

        // Creamos el Nodo Facade (asignamos temporalmente ID 1 y rol LEADER para
        // probar)
        Nodo miNodo = new Nodo(1, "localhost", miPuerto, "Nodo-" + miPuerto, NodeRole.LEADER);

        // Iniciar el Nodo (enciende su NetworkService internamente)
        miNodo.start();

        // Damos tiempo a iniciar bien
        Thread.sleep(2000);

        // Conectarse al otro nodo (inicial)
        if (miPuerto != puertoDestino) {
            ConsoleLogger.info("App", "Intentando conexión inicial a: " + ipDestino + ":" + puertoDestino);
            miNodo.connectToPeer(ipDestino, puertoDestino);
        } else {
            ConsoleLogger.info("App", "Iniciando sin conexión a otro nodo inicial.");
        }

        // --- CLI Interactivo ---
        ConsoleLogger.info("App",
                "Sistema listo. Comandos disponibles: estado, conectar <ip> <puerto>, votar <mensaje>, salir");
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        while (true) {
            String comando = scanner.nextLine();

            if (comando.equalsIgnoreCase("salir")) {
                miNodo.stop();
                break;
            } else if (comando.equalsIgnoreCase("estado")) {
                ConsoleLogger.info("App", "Estado actual: " + miNodo.getRole() + " en puerto " + miNodo.getPort());
            } else if (comando.startsWith("conectar ")) {
                String[] partes = comando.split(" ");
                if (partes.length == 3) {
                    miNodo.connectToPeer(partes[1], Integer.parseInt(partes[2]));
                } else {
                    ConsoleLogger.error("App", "Uso incorrecto. Formato: conectar <ip> <puerto>");
                }
            } else if (comando.startsWith("votar ")) {
                miNodo.proponer("ACCION_MANUAL", comando.substring(6));
            } else {
                ConsoleLogger.advertencia("App",
                        "Comando desconocido. Use: estado, conectar <ip> <puerto>, votar <mensaje>, salir");
            }
        }
    }
}
