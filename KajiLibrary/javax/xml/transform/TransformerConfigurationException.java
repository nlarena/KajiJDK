package javax.xml.transform;

/**
 * KajiLibrary's javax.xml.transform.TransformerConfigurationException -- no se pudo *armar* nada.
 *
 * <p>Separa dos fracasos que se parecen y se arreglan distinto. Una {@link TransformerException} a
 * secas dice que la transformacion fallo; esta dice que **ni siquiera llego a empezar**: la hoja de
 * estilo no compila, la fabrica no soporta una caracteristica que se le pidio, el `Templates` no se
 * pudo construir. Uno se investiga mirando el documento de entrada; el otro, mirando la
 * configuracion. De ahi que sea un tipo propio y no un mensaje distinto.
 *
 * <p>El constructor sin argumentos pone {@code "Configuration Error"} de mensaje en vez de dejarlo
 * nulo. No es decorativo: una excepcion sin mensaje que aparece en un log a las tres de la mañana
 * no dice absolutamente nada, y aca el nombre de la clase ya es toda la informacion que hay.
 */
public class TransformerConfigurationException extends TransformerException {

    private static final long serialVersionUID = -4251405565727967249L;

    /** Sin datos; el mensaje queda en {@code "Configuration Error"}. */
    public TransformerConfigurationException() {
        super("Configuration Error");
    }

    /**
     * Con un mensaje.
     *
     * @param msg la descripcion del error
     */
    public TransformerConfigurationException(String msg) {
        super(msg);
    }

    /**
     * Envolviendo otra excepcion.
     *
     * @param e la causa
     */
    public TransformerConfigurationException(Throwable e) {
        super(e);
    }

    /**
     * Con mensaje y causa.
     *
     * @param msg la descripcion del error
     * @param e la causa
     */
    public TransformerConfigurationException(String msg, Throwable e) {
        super(msg, e);
    }

    /**
     * Con mensaje y ubicacion.
     *
     * @param msg la descripcion del error
     * @param locator donde paso
     */
    public TransformerConfigurationException(String msg, SourceLocator locator) {
        super(msg, locator);
    }

    /**
     * Con mensaje, ubicacion y causa.
     *
     * @param msg la descripcion del error
     * @param locator donde paso
     * @param e la causa
     */
    public TransformerConfigurationException(String msg, SourceLocator locator, Throwable e) {
        super(msg, locator, e);
    }
}
