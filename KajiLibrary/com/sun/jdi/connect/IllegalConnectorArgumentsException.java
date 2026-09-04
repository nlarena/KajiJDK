package com.sun.jdi.connect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uno o varios argumentos de un {@link Connector} estaban mal.
 *
 * <p>Lleva **la lista de nombres** de los argumentos culpables, y no solo un mensaje. Es la
 * diferencia entre un depurador que puede marcar en rojo los dos campos que estan mal y uno que
 * solo puede mostrar un cartel: el formulario de conexion se arma desde
 * {@link Connector#defaultArguments()}, asi que quien lo dibujo tiene los controles indexados por
 * ese mismo nombre.
 */
public class IllegalConnectorArgumentsException extends Exception {

    private static final long serialVersionUID = -3042212603611350941L;

    /** Los nombres de los argumentos que estaban mal. De paquete, como en el JDK. */
    List<String> names;

    /**
     * Un fallo sobre un solo argumento.
     *
     * @param s el detalle
     * @param name el nombre del argumento
     */
    public IllegalConnectorArgumentsException(String s, String name) {
        super(s);
        this.names = new ArrayList<String>();
        this.names.add(name);
    }

    /**
     * Un fallo sobre varios argumentos.
     *
     * @param s el detalle
     * @param names los nombres; se copian
     */
    public IllegalConnectorArgumentsException(String s, List<String> names) {
        super(s);
        this.names = new ArrayList<String>(names);
    }

    /** Los nombres de los argumentos que estaban mal, en una lista que no se puede modificar. */
    public List<String> argumentNames() {
        return Collections.unmodifiableList(this.names);
    }
}
