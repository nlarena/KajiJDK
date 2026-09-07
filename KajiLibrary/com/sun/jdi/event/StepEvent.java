package com.sun.jdi.event;

/**
 * Termino un paso.
 *
 * <p>Es lo que hace posible "paso a paso": se pide un {@code StepRequest}, el hilo avanza lo que se
 * pidio y llega esto.
 *
 * @since 1.3
 */
public interface StepEvent extends LocatableEvent {
}
