package com.sun.jdi.event;

/**
 * The connection was cut off.
 *
 * <p>It is always the last event. After it, the queue throws {@link VMDisconnectedException}.
 *
 * @since 1.3
 */
public interface VMDisconnectEvent extends Event {
}
