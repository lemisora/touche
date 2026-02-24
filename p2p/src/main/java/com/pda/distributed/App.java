package com.pda.distributed;

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
            System.out.println(
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

        // Conectarse al otro nodo
        if (miPuerto != puertoDestino) {
            System.out.println("El Nodo intentará conectarse a: " + ipDestino + ":" + puertoDestino);
            miNodo.connectToPeer(ipDestino, puertoDestino);

            // --- PRUEBA DEL QUORUM ---
            Thread.sleep(1000); // 1 seg para que guarde el canal
            miNodo.proponer("ELECCION_PRINCIPAL", "Elegir a Nodo 3000 como líder principal");
            // --------------------------

        } else {
            System.out.println("No se puede conectar con el mismo nodo");
        }

        // Bloqueamos el hilo principal
        miNodo.blockUntilShutdown();
    }
}