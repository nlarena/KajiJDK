package javax.xml.parsers;

/**
 * KajiLibrary's javax.xml.parsers.FactoryConfigurationError -- no hay fabrica.
 *
 * <p>Es un {@link Error} y no una excepcion, lo que parece desmedido y no lo es: significa que la
 * implementacion de XML que se nombro en la configuracion <b>no existe</b> o no se pudo cargar. No
 * es un pedido que fallo, es la plataforma armada mal, y no hay nada que un {@code catch} alrededor
 * de la llamada pueda hacer al respecto.
 *
 * <h2>Los dos accesos a la causa</h2>
 *
 * <p>{@link #getException} y {@link #getCause} devuelven lo mismo. La primera es de 2000, anterior a
 * que {@code Throwable} tuviera causa encadenada; la segunda llego con Java 1.4 y es la que ve un
 * {@code printStackTrace}. Se conservan las dos, y la segunda esta implementada en terminos de la
 * primera para que no puedan discrepar.
 *
 * <p>{@link #getMessage} tambien tiene una vuelta: si no se dio mensaje propio, devuelve el de la
 * causa. Sin eso, el error mas comun de este paquete --una clase mal escrita en una propiedad-- se
 * imprimiria sin decir cual.
 */
public class FactoryConfigurationError extends Error {

    private static final long serialVersionUID = -827108682472263355L;

    /** La causa; ver la nota de la clase sobre por que no se usa la de {@code Throwable}. */
    private Exception exception;

    /** Sin detalle. */
    public FactoryConfigurationError() {
        super();
        this.exception = null;
    }

    /** Con un mensaje. */
    public FactoryConfigurationError(String msg) {
        super(msg);
        this.exception = null;
    }

    /** Envolviendo lo que fallo de verdad. */
    public FactoryConfigurationError(Exception e) {
        super(e.toString());
        this.exception = e;
    }

    /** Con las dos cosas. */
    public FactoryConfigurationError(Exception e, String msg) {
        super(msg);
        this.exception = e;
    }

    /** El mensaje propio, o el de la causa si no hay. Ver la nota de la clase. */
    public String getMessage() {
        String message = super.getMessage();
        if (message == null && this.exception != null) {
            return this.exception.getMessage();
        }
        return message;
    }

    /** La causa, en la forma vieja. */
    public Exception getException() {
        return this.exception;
    }

    /** La causa, en la forma que entiende {@code Throwable}. Es la misma. */
    public Throwable getCause() {
        return this.exception;
    }
}
