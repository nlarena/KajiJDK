package javax.swing.text;

/**
 * Se pidio una posicion que el documento no tiene.
 *
 * <p>Chequeada, a diferencia de {@link IndexOutOfBoundsException}, y la diferencia es de intencion:
 * en un documento la longitud <strong>cambia sola</strong> mientras alguien escribe, asi que un
 * desplazamiento valido cuando se calculo puede no serlo cuando se usa. No es necesariamente un bug
 * del programa, y por eso el compilador obliga a preverla.
 */
public class BadLocationException extends Exception {

    private static final long serialVersionUID = 8934174085564342750L;

    private int offset;

    /**
     * @param s el mensaje
     * @param offset la posicion que se habia pedido
     */
    public BadLocationException(String s, int offset) {
        super(s);
        this.offset = offset;
    }

    /** La posicion que se habia pedido. */
    public int offsetRequested() {
        return this.offset;
    }
}
