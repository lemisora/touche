package com.pda.distributed.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Herramienta para imprimir mensajes bonitos y ordenados en la consola
public class ConsoleLogger {
    // Definimos colores usando códigos ANSI
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Rol del nodo de la consola actual, para imprimirlo siempre ("LEADER" o
    // "WORKER")
    private static String rolConfigurado = "INICIO";

    public static void setRolConfigurado(String rol) {
        rolConfigurado = rol;
    }

    private static void log(String color, String modulo, String mensaje) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.println(color + "[" + timestamp + "] [" + rolConfigurado + "] [" + modulo + "] " + mensaje + RESET);
    }

    // Información general, ej: conexiones de red
    public static void info(String modulo, String mensaje) {
        log(CYAN, modulo, mensaje);
    }

    // Éxitos, ej: Quorum alcanzado
    public static void exito(String modulo, String mensaje) {
        log(GREEN, modulo, mensaje);
    }

    // Votaciones, alertas tempranas
    public static void advertencia(String modulo, String mensaje) {
        log(YELLOW, modulo, mensaje);
    }

    // Nodos muertos, fallas
    public static void error(String modulo, String mensaje) {
        log(RED, modulo, mensaje);
    }
}
