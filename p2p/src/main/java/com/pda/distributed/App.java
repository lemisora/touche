package com.pda.distributed;

import java.io.IOException;

import com.pda.distributed.network.NodeClient;
import com.pda.distributed.network.NodeServer;

public class App {
    /*
     * java -cp target/p2p-1.0-SNAPSHOT.jar com.pda.distributed.App 2000
     * [IP_ADDRESS] 2000
     * 
     * Argumentos recibidos:
     * - args[0]: Puerto del nodo
     * - args[1]: IP del nodo a escuchar
     * - args[2]: Puerto del nodo a escuchar
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        // Validamos que se hayan recibido los argumentos correctos
        if (args.length != 3) {
            System.out.println("Uso: mvn compile exec:java -Dexec.mainClass=\"com.pda.distributed.App\" -Dexec.args=\"<puerto> <ip> <puerto>\"");
            return;
        }

        int miPuerto = Integer.parseInt(args[0]);
        String ipDestino = args[1];
        int puertoDestino = Integer.parseInt(args[2]);

        // Creamos el servidor
        NodeServer server = new NodeServer(miPuerto);
        server.start();

        // Damos tiempo a iniciar bien
        Thread.sleep(2000);

        // Creamos el cliente
        if (miPuerto != puertoDestino) {
            System.out.println("Iniciando cliente...");
            System.out.println("Conectando a: " + ipDestino + ":" + puertoDestino);
            NodeClient client = new NodeClient(ipDestino, puertoDestino);
            client.ping();
        } else {
            System.out.println("No se puede conectar con el mismo nodo");
        }

        // Damos tiempo a que el cliente termine
        Thread.sleep(2000);

        // Bloqueamos el hilo principal para que el servidor no se cierre
        server.blockUntilShutdown();
    }
}


    