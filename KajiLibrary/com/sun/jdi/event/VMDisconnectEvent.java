package com.sun.jdi.event;

/**
 * Se corto la conexion.
 *
 * <p>Es siempre el ultimo evento. Despues de el, la cola tira {@link VMDisconnectedException}.
 *
 * @since 1.3
 */
public interface VMDisconnectEvent extends Event {
}
