package com.pda.distributed;

import com.pda.distributed.core.Nodo;
import com.pda.distributed.core.NodeRole;
import com.pda.distributed.utils.ConsoleLogger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "nodo-p2p", mixinStandardHelpOptions = true, version = "1.0",
        description = "Inicia un nodo P2P con auto-descubrimiento UDP y almacenamiento distribuido.")
public class App implements Callable<Integer> {

    @Option(names = {"-i", "--ip"}, defaultValue = "localhost", description = "IP local de este nodo.")
    private String ip;

    @Option(names = {"-n", "--name"}, defaultValue = "Nodo", description = "Nombre del nodo.")
    private String name;

    @Option(names = {"-r", "--role"}, defaultValue = "WORKER", description = "Rol inicial (LEADER o WORKER).")
    private NodeRole initialRole;

    public static void main(String[] args) {
        // Picocli procesa los argumentos de la terminal
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // Generar un ID numérico aleatorio
        int idAleatorio = (int) (System.currentTimeMillis() % 10000);
        String finalName = name.equals("Nodo") ? "Nodo-" + idAleatorio : name;

        ConsoleLogger.info("App", "Preparando nodo " + finalName + "...");

        // Instanciar el nodo (el puerto se descubrirá solo)
        Nodo miNodo = new Nodo(idAleatorio, ip, finalName, initialRole);

        try {
            miNodo.start();
        } catch (Exception e) {
            ConsoleLogger.error("App", "Error crítico al arrancar: " + e.getMessage());
            return 1;
        }

        // Shutdown Hook para apagar todo limpiamente (Control+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { 
                miNodo.stop(); 
            } catch (InterruptedException ignored) {}
        }));

        miNodo.blockUntilShutdown();
        return 0;
    }
}