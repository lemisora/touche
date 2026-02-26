package com.pda.distributed;

import com.pda.distributed.utils.ConsoleLogger;

import java.io.IOException;

import com.pda.distributed.core.Nodo;
import com.pda.distributed.core.NodeRole;

public class App {
    /*
     * Uso: mvn compile exec:java -Dexec.mainClass="com.pda.distributed.App"
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        // Ahora no necesitamos pasar puertos por argumento, porque el NetworkService
        // elegirá uno a partir de 50000
        // y el DiscoveryService los encontrará.

        // Creamos una ID aleatoria temporal para identificar visualmente a este nodo en
        // la terminal
        int randomId = (int) (Math.random() * 1000);

        // Creamos el Nodo Facade. Pasamos puerto 0 como placeholder, la lógica real lo
        // asignará dinámicamente
        Nodo miNodo = new Nodo(randomId, "localhost", 0, "Nodo-" + randomId, NodeRole.LEADER);

        // Iniciar el Nodo (enciende su red interna, asigna puerto e inicia UDP
        // Discovery)
        miNodo.start();

        // Damos tiempo a iniciar bien (espera antes de habilitar la consola
        // interactiva)
        Thread.sleep(1500);

        // --- CLI Interactivo ---
        ConsoleLogger.info("App",
                "Sistema listo. Comandos disponibles: estado, info, conectar <ip> <puerto>, votar <mensaje>, salir");
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        while (true) {
            String comando = scanner.nextLine();

            if (comando.equalsIgnoreCase("salir")) {
                miNodo.stop();
                break;
            } else if (comando.equalsIgnoreCase("estado")) {
                ConsoleLogger.info("App", "Estado actual: " + miNodo.getRole() + " en puerto " + miNodo.getPort());
            } else if (comando.equalsIgnoreCase("info")) {
                ConsoleLogger.info("App", "\n" + miNodo.getNetworkInfo());
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
                        "Comando desconocido. Use: estado, info, conectar <ip> <puerto>, votar <mensaje>, salir");
            }
        }
    }
}
