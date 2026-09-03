package java.awt.color;

/**
 * El motor de gestión de color falló.
 *
 * <p>Está declarada porque el contrato la nombra, pero **esta biblioteca no la tira nunca**: no hay
 * motor ICC que pueda fallar. Ver la nota de alcance de {@link ColorSpace}.
 */
public class CMMException extends RuntimeException {

    private static final long serialVersionUID = 5775558044142292260L;

    /** Con ese mensaje. */
    public CMMException(String s) {
        super(s);
    }
}
