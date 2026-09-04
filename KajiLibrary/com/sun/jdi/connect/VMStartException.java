package com.sun.jdi.connect;

/**
 * La VM se lanzo pero la conexion de depuracion no llego a establecerse.
 *
 * <p>Lo que la hace util es que **trae el proceso**. Cuando un
 * {@link LaunchingConnector} falla, el motivo casi siempre esta en la salida de error de la VM que
 * se acaba de lanzar --una opcion de `-agentlib:jdwp` mal escrita, un puerto ocupado-- y sin el
 * `Process` esa salida se pierde junto con el proceso huerfano.
 *
 * <p>Quien atrapa esta excepcion tiene entonces dos obligaciones: leer los flujos del proceso para
 * saber que paso, y terminarlo.
 */
public class VMStartException extends Exception {

    private static final long serialVersionUID = 6408644824640801020L;

    /** El proceso de la VM lanzada. De paquete, como en el JDK. */
    Process process;

    /**
     * Un fallo sobre ese proceso, sin detalle.
     *
     * @param process la VM que se lanzo
     */
    public VMStartException(Process process) {
        super();
        this.process = process;
    }

    /**
     * Un fallo sobre ese proceso, con detalle.
     *
     * @param s el detalle
     * @param process la VM que se lanzo
     */
    public VMStartException(String s, Process process) {
        super(s);
        this.process = process;
    }

    /** El proceso de la VM lanzada; ver la nota de la clase sobre que hacer con el. */
    public Process process() {
        return this.process;
    }
}
