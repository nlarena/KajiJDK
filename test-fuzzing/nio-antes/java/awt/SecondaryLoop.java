package java.awt;

/**
 * A wait that does not block the event thread.
 *
 * <p>It resolves the contradiction of a modal dialog: the call that opens it has to **not return**
 * until it closes, but if that call came from the event thread and stays waiting, the whole
 * interface freezes and the dialog can never be closed.
 *
 * <p>The way out is a second, nested event loop: {@link #enter} keeps dispatching events while it
 * waits, and {@link #exit} stops it. That is why the loop is an object and not a method — it has to
 * be possible to end it from inside dispatch itself.
 *
 * <p>A loop is used **only once**: {@link #enter} on one that is already running returns `false`
 * instead of nesting.
 */
public interface SecondaryLoop {

    /**
     * Enters the loop and does not return until someone calls {@link #exit}.
     *
     * @return `true` if the loop ended normally, `false` if it was already running
     */
    boolean enter();

    /**
     * Stops the loop.
     *
     * @return `true` if there was a loop to stop
     */
    boolean exit();
}
