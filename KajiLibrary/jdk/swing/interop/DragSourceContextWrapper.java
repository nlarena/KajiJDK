package jdk.swing.interop;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSourceContext;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.event.InputEvent;
import java.util.Map;

/**
 * The source side of a drag, seen from another toolkit.
 *
 * <h2>What it does</h2>
 *
 * <p>Dragging something out of the window is a conversation with the operating system, not with AWT.
 * This class is the half of that conversation that belongs to the source: it tells the system a drag
 * started, changes the cursor while it lasts, and waits for it to finish.
 *
 * <h2>Why an event loop of its own</h2>
 *
 * <p>{@link #startSecondaryEventLoop} and {@link #quitSecondaryEventLoop} exist because on some
 * systems the drag is a call that does not return until the user drops. The interface has to keep
 * responding for all that time, so events must be served from inside the call. That is a secondary
 * loop.
 *
 * <h2>{@link #convertModifiersToDropAction}</h2>
 *
 * <p>It is the only part that does not depend on the system: the rule that Control copies, Shift
 * moves and the two together link, and that with neither of them the first action the source allows
 * is chosen, in the order move, copy, link. The result is always trimmed to what the source allows,
 * so asking to copy where only moving is possible gives not a copy but nothing.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The key rule really works, and it is the only thing in this package that can be checked against
 * the JDK without a screen. The rest needs the native peer, which does not exist here: see
 * {@link #getDragSourceContext}.
 *
 * @since 9
 */
public abstract class DragSourceContextWrapper {

    private final DragGestureEvent trigger;

    /**
     * One for the drag that gesture started.
     *
     * @param dge the gesture that started it
     */
    public DragSourceContextWrapper(DragGestureEvent dge) {
        this.trigger = dge;
    }

    /**
     * Which action those keys mean, trimmed to what the source allows.
     *
     * <p>Control copies, Shift moves, the two together link. With neither of them the first action
     * the source allows is chosen, in the order move, copy, link -- move first because that is what
     * the user expects when dragging inside the same application. Any other key changes nothing.
     *
     * @param modifiers the keys held down, as {@link InputEvent#getModifiersEx} gives them
     * @param supportedActions the actions the source allows
     * @return the action, or {@link DnDConstants#ACTION_NONE} if none fits
     */
    public static int convertModifiersToDropAction(int modifiers, int supportedActions) {
        final int keys = modifiers & (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK);
        int action;
        if (keys == (InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK)) {
            action = DnDConstants.ACTION_LINK;
        } else if (keys == InputEvent.CTRL_DOWN_MASK) {
            action = DnDConstants.ACTION_COPY;
        } else if (keys == InputEvent.SHIFT_DOWN_MASK) {
            action = DnDConstants.ACTION_MOVE;
        } else if ((supportedActions & DnDConstants.ACTION_MOVE) != 0) {
            action = DnDConstants.ACTION_MOVE;
        } else if ((supportedActions & DnDConstants.ACTION_COPY) != 0) {
            action = DnDConstants.ACTION_COPY;
        } else if ((supportedActions & DnDConstants.ACTION_LINK) != 0) {
            action = DnDConstants.ACTION_LINK;
        } else {
            action = DnDConstants.ACTION_NONE;
        }
        return action & supportedActions;
    }

    /**
     * Sets the cursor that says what will happen if the user drops there.
     *
     * @param c the cursor
     * @param cursorType which cursor it is, in the system's terms
     */
    protected abstract void setNativeCursor(Cursor c, int cursorType);

    /**
     * Asks the system to start the drag.
     *
     * <p>The formats travel separately and not inside the data because the system publishes them
     * before anyone asks for anything: whoever is on the other side has to be able to decide whether
     * to accept the drag without transferring yet.
     *
     * @param t the data being dragged
     * @param formats the formats, in the system's terms
     * @param formatMap from each system format to the AWT format it corresponds to
     */
    protected abstract void startDrag(Transferable t, long[] formats,
            Map<Long, DataFlavor> formatMap);

    /** Serves events until the drag finishes. */
    public abstract void startSecondaryEventLoop();

    /** Cuts the loop {@link #startSecondaryEventLoop} opened. */
    public abstract void quitSecondaryEventLoop();

    /**
     * Says the drag finished and waits for the word to arrive.
     *
     * <p>The word goes through the event queue and not straight across because whoever listens for
     * it is application code, and that runs on the event thread. After queueing it the secondary
     * loop is entered, which is what gives the event thread a chance to serve it before this call
     * returns.
     *
     * @param success whether the target kept the data
     * @param operations what was done with them
     * @param x where it was dropped, on screen
     * @param y where it was dropped, on screen
     * @throws IllegalArgumentException if the drag never started, and there is therefore no context
     *     to tell
     */
    public void dragDropFinished(boolean success, int operations, int x, int y) {
        final DragSourceDropEvent ev = new DragSourceDropEvent(getDragSourceContext(),
                operations & sourceActions(), success, x, y);
        EventQueue.invokeLater(new Notice(ev));
        startSecondaryEventLoop();
    }

    /**
     * The context of the drag in progress.
     *
     * <p>It returns {@code null} while there is no drag running, which in this library is always:
     * the context is filled in by {@code DragSource.startDrag} on reaching the native peer, and
     * there is no native peer. The JDK returns the same for a freshly created wrapper.
     *
     * @return the context, or {@code null}
     */
    public DragSourceContext getDragSourceContext() {
        return null;
    }

    /** Which actions the gesture's source allows, to trim what is reported on the drop. */
    private int sourceActions() {
        if (this.trigger == null) {
            return DnDConstants.ACTION_NONE;
        }
        final DragGestureRecognizer r = this.trigger.getSourceAsDragGestureRecognizer();
        return r == null ? DnDConstants.ACTION_NONE : r.getSourceActions();
    }

    /** The end-of-drag notice, to run it on the event thread. */
    private static final class Notice implements Runnable {

        private final DragSourceDropEvent ev;

        Notice(DragSourceDropEvent ev) {
            this.ev = ev;
        }

        public void run() {
            this.ev.getDragSourceContext().dragDropEnd(this.ev);
        }
    }
}
