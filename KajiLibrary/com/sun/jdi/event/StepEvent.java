package com.sun.jdi.event;

/**
 * A step finished.
 *
 * <p>It is what makes "step by step" possible: a {@code StepRequest} is asked for, the thread
 * advances what was asked and this arrives.
 *
 * @since 1.3
 */
public interface StepEvent extends LocatableEvent {
}
