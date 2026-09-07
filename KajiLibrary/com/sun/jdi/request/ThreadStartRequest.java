package com.sun.jdi.request;

import com.sun.jdi.ThreadReference;

/**
 * Pedir aviso cuando arranque un hilo.
 *
 * @since 1.3
 */
public interface ThreadStartRequest extends EventRequest {

    /**
     * Filtra por thread; solo con el pedido deshabilitado.
     *
     * @param thread el ThreadReference
     */
    void addThreadFilter(ThreadReference thread);

    /**
     * Filtra por platform threads only; solo con el pedido deshabilitado.
     */
    void addPlatformThreadsOnlyFilter();
}
