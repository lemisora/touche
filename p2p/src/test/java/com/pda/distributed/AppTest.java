package com.pda.distributed;

import org.junit.Test;
import picocli.CommandLine;
import static org.junit.Assert.*;

public class AppTest {

    @Test
    public void testArgumentParsing_ConSemillaYTodosLosParametros() {
        // Arrange
        App app = new App();
        CommandLine cmd = new CommandLine(app);

        // Simulamos los argumentos que un usuario escribiría en la terminal
        String[] args = {
                "-i", "192.168.1.50",
                "-I", "99",
                "-n", "NodoWorker",
                "-s", "100.10.20.30:50051"
        };

        // Act
        cmd.parseArgs(args);

        // Assert: Validamos que Picocli reconoció la semilla y el ID
        assertTrue("Debería reconocer el argumento semilla (-s)", cmd.getParseResult().hasMatchedOption("-s"));
        assertEquals("100.10.20.30:50051", cmd.getParseResult().matchedOptionValue("-s", ""));
        assertEquals(99, cmd.getParseResult().matchedOptionValue("-I", "0"));
    }

    @Test
    public void testArgumentParsing_SinArgumentos_UsaValoresPorDefecto() {
        // Arrange
        App app = new App();
        CommandLine cmd = new CommandLine(app);

        // Act: Ejecutamos sin ningún argumento
        String[] args = {};
        cmd.parseArgs(args);

        // Assert: Debe usar los defaultValues definidos en las anotaciones @Option
        assertEquals("localhost", cmd.getParseResult().matchedOptionValue("-i", "localhost"));
        assertFalse("No debería tener el argumento semilla", cmd.getParseResult().hasMatchedOption("-s"));
    }
}