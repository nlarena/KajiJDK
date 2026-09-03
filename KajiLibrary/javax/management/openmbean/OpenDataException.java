package javax.management.openmbean;

import javax.management.JMException;

/**
 * Un dato que no cumple con el tipo abierto que dice tener.
 *
 * <p>Es **verificada**, y es la única de las cuatro de este paquete que lo es. La diferencia no es
 * de estilo: ésta la tira un constructor al que uno le pasa datos que vienen de afuera --de una
 * conexión, de un archivo de configuración-- y que puede no haber podido validar antes. Las otras
 * tres son errores de programación sobre datos que uno ya tiene en la mano, y por eso son de
 * ejecución.
 */
public class OpenDataException extends JMException {

    private static final long serialVersionUID = 8346311255433349870L;

    /** Sin mensaje. */
    public OpenDataException() {
        super();
    }

    /** Con ese mensaje. */
    public OpenDataException(String msg) {
        super(msg);
    }
}
