package javax.print.event;

import java.util.EventObject;

/**
 * KajiLibrary's javax.print.event.PrintEvent -- the root of the print events.
 *
 * <p>It adds nothing over {@link EventObject} except a {@code toString} with its own format. It
 * exists so that the three subclasses have a common type. (The note said four; {@link
 * PrintJobEvent}, {@link PrintJobAttributeEvent} and {@link PrintServiceAttributeEvent} are the
 * three.)
 *
 * <p>It inherits something surprising: passing null as the source throws
 * {@link IllegalArgumentException}, not {@link NullPointerException}. It comes from
 * {@code EventObject} and the subclasses propagate it.
 */
public class PrintEvent extends EventObject {

    private static final long serialVersionUID = 2286914924430763847L;

    /**
     * @param source where the event came from
     * @throws IllegalArgumentException if it is null
     */
    public PrintEvent(Object source) {
        super(source);
    }

    /** {@code "PrintEvent on "} and the source. */
    @Override
    public String toString() {
        return "PrintEvent on " + getSource().toString();
    }
}
