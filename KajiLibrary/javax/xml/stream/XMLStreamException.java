package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLStreamException -- lo que se lanza cuando la lectura o la
 * escritura de XML no puede seguir.
 *
 * <p>Es una excepcion **chequeada**, y eso esta puesto a proposito: casi todo metodo de
 * {@link XMLStreamReader} y {@link XMLStreamWriter} la declara, asi que el compilador obliga a
 * decidir que se hace con un documento mal formado. La alternativa --una no chequeada-- convertiria
 * el caso mas comun de todos (entrada invalida) en algo que se descubre en produccion.
 *
 * <h2>Dos campos protegidos que hoy no se escribirian</h2>
 *
 * <p>{@link #nested} y {@link #location} son {@code protected} porque la clase es de 2004 y
 * {@code Throwable} recien habia estrenado las causas encadenadas. Hoy {@link #nested} seria
 * redundante con {@link Throwable#getCause()} --y de hecho los constructores que la reciben la
 * ponen en los dos lados-- pero se mantiene tal cual porque hay subclases afuera que la leen.
 *
 * <p>La asimetria que si sorprende y que esta reproducida: el constructor de
 * {@linkplain #XMLStreamException(String, Location) mensaje mas ubicacion} deja
 * {@code getCause()} en null, mientras que el de
 * {@linkplain #XMLStreamException(String, Location, Throwable) mensaje, ubicacion y causa} la pone.
 * No es un descuido de aca sino la conducta del contrato.
 *
 * <h2>El mensaje se arma, no se guarda</h2>
 *
 * <p>Los dos constructores que reciben una {@link Location} no guardan el mensaje que se les dio:
 * lo envuelven en {@code "ParseError at [row,col]:[L,C]\nMessage: ..."} antes de pasarlo a
 * {@code super}. Es feo y sale en dos lineas, pero es el texto que los usuarios de StAX ya conocen y
 * que hay herramientas que parsean, asi que se copia al caracter.
 */
public class XMLStreamException extends Exception {

    /**
     * La excepcion de mas adentro, si la hay.
     *
     * <p>Duplica {@link Throwable#getCause()} cuando el constructor recibio las dos cosas; ver el
     * encabezado.
     */
    protected Throwable nested;

    /** Donde paso, o null si quien lanzo no lo sabia. */
    protected Location location;

    /** Sin mensaje, sin causa y sin ubicacion. */
    public XMLStreamException() {
        super();
    }

    /**
     * Con mensaje solo.
     *
     * @param msg el mensaje
     */
    public XMLStreamException(String msg) {
        super(msg);
    }

    /**
     * Envolviendo otra excepcion.
     *
     * <p>El mensaje resultante es el {@code toString()} de la envuelta, que es lo que hace
     * {@code Throwable(Throwable)}.
     *
     * @param th la excepcion de mas adentro
     */
    public XMLStreamException(Throwable th) {
        super(th);
        nested = th;
    }

    /**
     * Con mensaje propio y causa.
     *
     * @param msg el mensaje
     * @param th la excepcion de mas adentro
     */
    public XMLStreamException(String msg, Throwable th) {
        super(msg, th);
        nested = th;
    }

    /**
     * Con mensaje, ubicacion y causa.
     *
     * @param msg el mensaje, que se envuelve en el formato {@code ParseError at [row,col]}
     * @param location donde paso
     * @param th la excepcion de mas adentro
     */
    public XMLStreamException(String msg, Location location, Throwable th) {
        super("ParseError at [row,col]:[" + location.getLineNumber() + ","
                + location.getColumnNumber() + "]\nMessage: " + msg, th);
        nested = th;
        this.location = location;
    }

    /**
     * Con mensaje y ubicacion.
     *
     * <p>Deja {@code getCause()} y {@link #getNestedException()} en null; ver el encabezado.
     *
     * @param msg el mensaje, que se envuelve en el formato {@code ParseError at [row,col]}
     * @param location donde paso
     */
    public XMLStreamException(String msg, Location location) {
        super("ParseError at [row,col]:[" + location.getLineNumber() + ","
                + location.getColumnNumber() + "]\nMessage: " + msg);
        this.location = location;
    }

    /**
     * La excepcion envuelta, o null.
     *
     * @return la de mas adentro
     */
    public Throwable getNestedException() {
        return nested;
    }

    /**
     * Donde paso, o null si no se sabe.
     *
     * @return la ubicacion
     */
    public Location getLocation() {
        return location;
    }
}
