package javax.print.event;

import java.util.EventObject;

/**
 * KajiLibrary's javax.print.event.PrintEvent -- la raiz de los eventos de impresion.
 *
 * <p>No agrega nada sobre {@link EventObject} salvo un {@code toString} con formato propio. Existe
 * para que las cuatro subclases tengan un tipo comun.
 *
 * <p>Hereda una cosa que sorprende: pasar null como fuente lanza {@link IllegalArgumentException}, no
 * {@link NullPointerException}. Viene de {@code EventObject} y las subclases la propagan.
 */
public class PrintEvent extends EventObject {

    private static final long serialVersionUID = 2286914924430763847L;

    /**
     * @param source de donde salio el evento
     * @throws IllegalArgumentException si es null
     */
    public PrintEvent(Object source) {
        super(source);
    }

    /** {@code "PrintEvent on "} y la fuente. */
    @Override
    public String toString() {
        return "PrintEvent on " + getSource().toString();
    }
}
