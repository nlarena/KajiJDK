package java.awt;

/**
 * Where the mouse pointer is, without needing an event.
 *
 * <p>It is the way to ask for the mouse position **outside** event dispatch: a {@link
 * java.awt.event.MouseEvent} says where the pointer was when something happened, and this says
 * where it is now.
 *
 * <p>Without a screen there is no pointer, so both methods throw {@link HeadlessException}. There
 * is no reasonable answer: neither a (0,0) —which would be an invented position— nor zero buttons,
 * which would make it look as if there were a mouse without buttons instead of no mouse.
 */
public class MouseInfo {

    /** Not instantiated: it is all static. */
    private MouseInfo() {
    }

    /**
     * Where the pointer is.
     *
     * @throws HeadlessException always: without a screen there is no pointer
     */
    public static PointerInfo getPointerInfo() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * How many buttons the mouse has.
     *
     * @throws HeadlessException always, for the same reason
     */
    public static int getNumberOfButtons() throws HeadlessException {
        throw new HeadlessException();
    }
}
