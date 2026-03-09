package com.pda.distributed.core;

import java.io.Serializable;

/**
 * Representa los metadatos y el estado actual de un nodo en la red.
 * Implementa Serializable por si en el futuro se necesita enviar este objeto
 * directamente a través de sockets o guardarlo en disco.
 */
public class NodoInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int idNumerico;
    private int cantidadArchivos;

    public NodoInfo(int idNumerico, int cantidadArchivos) {
        this.idNumerico = idNumerico;
        this.cantidadArchivos = cantidadArchivos;
    }

    // Getters y Setters
    public int getIdNumerico() {
        return idNumerico;
    }

    public void setIdNumerico(int idNumerico) {
        this.idNumerico = idNumerico;
    }

    public int getCantidadArchivos() {
        return cantidadArchivos;
    }

    public void setCantidadArchivos(int cantidadArchivos) {
        this.cantidadArchivos = cantidadArchivos;
    }

    // Método de conveniencia para sumar archivos fácilmente
    public void incrementarArchivos() {
        this.cantidadArchivos++;
    }

    @Override
    public String toString() {
        return "NodoInfo{id=" + idNumerico + ", archivos=" + cantidadArchivos + "}";
    }
}