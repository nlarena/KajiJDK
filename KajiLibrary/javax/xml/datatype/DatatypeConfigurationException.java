package javax.xml.datatype;

/**
 * KajiLibrary's javax.xml.datatype.DatatypeConfigurationException -- no se pudo conseguir una
 * {@link DatatypeFactory}.
 *
 * <p>Es la excepcion de la busqueda de implementacion, no la de los datos: un valor lexico mal
 * escrito levanta {@link IllegalArgumentException}, no esto. Esto significa que no hay ninguna
 * implementacion disponible, o que la que se nombro no se pudo cargar.
 *
 * <p>Es <b>verificada</b>, y a proposito: es un problema de despliegue --falta un jar, sobra una
 * propiedad del sistema-- que quien llama tiene que decidir como manejar. Fijarse el contraste con
 * {@link javax.xml.stream.FactoryConfigurationError}, que para el mismo problema es un
 * {@code Error}; las dos APIs se escribieron con criterios distintos y cada una se quedo con el
 * suyo.
 *
 * <p>Los cuatro constructores son los de siempre. Los dos que toman causa la pasan al constructor de
 * {@link Exception}, asi que {@code getCause()} anda: no hay campo propio ni
 * {@code initCause} manual.
 */
public class DatatypeConfigurationException extends Exception {

    /** El mismo del original, para que una instancia serializada cruce entre las dos bibliotecas. */
    private static final long serialVersionUID = -1699373159027047238L;

    /** Sin mensaje ni causa. */
    public DatatypeConfigurationException() {
        super();
    }

    /**
     * Con mensaje.
     *
     * @param message que paso
     */
    public DatatypeConfigurationException(String message) {
        super(message);
    }

    /**
     * Con mensaje y causa.
     *
     * @param message que paso
     * @param cause la excepcion de abajo
     */
    public DatatypeConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Solo con causa; el mensaje sale de ella.
     *
     * @param cause la excepcion de abajo
     */
    public DatatypeConfigurationException(Throwable cause) {
        super(cause);
    }
}
