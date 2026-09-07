package com.sun.jdi;

/**
 * Un candado que un hilo tiene tomado.
 *
 * @since 1.3
 */
public interface MonitorInfo extends Mirror {

    /**
     * El monitor.
     *
     * @return el resultado
     */
    ObjectReference monitor();

    /**
     * El stack depth.
     *
     * @return el resultado
     */
    int stackDepth();

    /**
     * El thread.
     *
     * @return el resultado
     */
    ThreadReference thread();
}
