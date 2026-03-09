package com.pda.distributed.core;
/** Enumerado de estados de nodo
 * - BLOCKED: Bloqueado, no debería de tomar tareas
 * - READY: Listo para tomar tareas */
public enum NodeState {
    BLOCKED,
    READY,
}
