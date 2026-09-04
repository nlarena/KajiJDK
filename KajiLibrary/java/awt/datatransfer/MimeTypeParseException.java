package java.awt.datatransfer;

/**
 * Un tipo MIME mal escrito.
 *
 * <p>Un tipo MIME tiene forma —`tipo/subtipo` más parámetros— y esta excepción es lo que se levanta
 * cuando la cadena no la respeta.
 */
public class MimeTypeParseException extends Exception {

    private static final long serialVersionUID = -5604407764691570741L;

    /** Sin explicación. */
    public MimeTypeParseException() {
        super();
    }

    /** Con la explicación dada. */
    public MimeTypeParseException(String s) {
        super(s);
    }
}
