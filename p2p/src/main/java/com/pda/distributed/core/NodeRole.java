package com.pda.distributed.core;

// Tipos de roles que un nodo puede tener en el anillo
public enum NodeRole {
    LEADER, // Lider, participa en el Quorum y tiene el archivo real
    WORKER // Trabajador, hace lo que le dicen y pide permiso
}
