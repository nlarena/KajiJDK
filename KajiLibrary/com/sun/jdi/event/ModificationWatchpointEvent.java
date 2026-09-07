package com.sun.jdi.event;

import com.sun.jdi.Value;

/**
 * Se escribio un campo vigilado.
 *
 * <p>Llega <strong>antes</strong> de que la escritura ocurra: {@code valueCurrent} tiene el viejo y
 * {@link #valueToBe} el nuevo.
 *
 * @since 1.3
 */
public interface ModificationWatchpointEvent extends WatchpointEvent {

    /**
     * El value to be.
     *
     * @return el resultado
     */
    Value valueToBe();
}
