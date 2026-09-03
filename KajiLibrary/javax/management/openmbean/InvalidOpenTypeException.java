package javax.management.openmbean;

/**
 * Un valor cuyo tipo abierto no es el que se esperaba en ese lugar.
 *
 * <p>De ejecución: quien la provoca ya tenía el valor y su tipo a mano, así que comprobarlo antes
 * estaba a su alcance. Ver la nota de {@link OpenDataException}, que es la verificada de la familia.
 */
public class InvalidOpenTypeException extends IllegalArgumentException {

    private static final long serialVersionUID = -2837312755412327534L;

    /** Sin mensaje. */
    public InvalidOpenTypeException() {
        super();
    }

    /** Con ese mensaje. */
    public InvalidOpenTypeException(String msg) {
        super(msg);
    }
}
