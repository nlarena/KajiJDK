package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.LineUnavailableException -- la linea existe pero no se puede usar
 * ahora.
 *
 * <p>Es la distincion que hace util a esta clase: no significa que el sistema no soporte lo que se
 * pidio --para eso esta {@link IllegalArgumentException}-- sino que en <b>este momento</b> no hay
 * recurso.
 *
 * <p>La causa habitual es que otro programa se llevo el dispositivo, o que se agotaron las lineas
 * simultaneas del mezclador. Reintentar mas tarde puede funcionar, y por eso es comprobada.
 */
public class LineUnavailableException extends Exception {

    private static final long serialVersionUID = -2046718279487432130L;

    /** Sin detalle. */
    public LineUnavailableException() {
        super();
    }

    /** Con mensaje. */
    public LineUnavailableException(String message) {
        super(message);
    }
}
