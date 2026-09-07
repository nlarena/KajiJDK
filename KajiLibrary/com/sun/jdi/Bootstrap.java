package com.sun.jdi;

/**
 * De donde se saca el {@link VirtualMachineManager}: todo el arranque de JDI.
 *
 * @since 1.3
 */
public class Bootstrap {

    /** Para quien la instancie; la clase no tiene estado. */
    public Bootstrap() {
    }

    /**
     * El gestor de maquinas virtuales, de donde salen todos los conectores.
     *
     * @return el gestor
     * @throws UnsupportedOperationException en esta biblioteca: JDI necesita una implementacion del
     * protocolo de depuracion (JDWP) y del transporte, que son decenas de clases internas y no
     * forman parte de esta API
     */
    public static synchronized VirtualMachineManager virtualMachineManager() {
        throw new UnsupportedOperationException(
                "JDI necesita una implementacion de JDWP y de su transporte, que esta biblioteca no "
                + "trae; lo que si esta es la API entera, contra la que un depurador compila");
    }
}
