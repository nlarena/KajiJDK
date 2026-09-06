import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.event.InputEvent;
import java.util.Map;

import jdk.swing.interop.DispatcherWrapper;
import jdk.swing.interop.DragSourceContextWrapper;
import jdk.swing.interop.DropTargetContextWrapper;
import jdk.swing.interop.LightweightFrameWrapper;
import jdk.swing.interop.SwingInterOpUtils;

/**
 * Checks {@code jdk.swing.interop} against JDK 25.
 *
 * <h2>What can be compared</h2>
 *
 * <p>The key rule of {@code convertModifiersToDropAction}, which is pure arithmetic and depends on
 * nothing; the mouse-grab constant; that giving up an event queue's dispatching has the effects it
 * has to have; and which error each thing that needs a windowing system fails with.
 *
 * <p>What is not compared is really drawing or really dragging: that needs the native half, which
 * neither this library nor a JDK without a screen has.
 *
 * <p>On the JDK it has to be run with {@code -Djava.awt.headless=true}: three of the answers
 * are the {@code HeadlessException} that a machine with a screen does not raise, and this VM
 * has no screen either way.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class IOP1 {

    static final String[] EXPECTED = {
        "mask|-2147483648",
        "actions|0;1;2;2;1073741824;2;2;2;0;0;2;2;0;2;2;2;0;1;0;1;0;1;1;1;0;0;0;0;1073741824;1073741824;1073741824;1073741824;0;1;2;2;1073741824;2;2;2;0;1;0;1;0;1;1;1;0;0;2;2;0;2;2;2;",
        "ungrab-null|false",
        "ungrab-other|false",
        "grab|ok",
        "post-null|ok",
        "dispatch|thread;schedule;",
        "loop|thread;schedule;loop;|true",
        "push|RuntimeException",
        "install-null|NullPointerException",
        "null-queue|NullPointerException",
        "source|ok",
        "context|null",
        "drag-end|IllegalArgumentException",
        "bind-null|NullPointerException",
        "reset-null|NullPointerException",
        "bind|HeadlessException",
        "frame|HeadlessException",
    };

    /** Any event at all, which is the only thing that can be built without components. */
    static class Ev extends AWTEvent {
        private static final long serialVersionUID = 1L;

        Ev(Object source, int id) {
            super(source, id);
        }
    }

    /** A queue that lets {@code dispatchEvent}, which is protected, be called. */
    static class Queue extends EventQueue {
        void dispatch(AWTEvent e) {
            dispatchEvent(e);
        }
    }

    /** A dispatcher that writes down what it is asked for and does nothing else. */
    static class Disp extends DispatcherWrapper {
        final StringBuilder log = new StringBuilder();

        @Override
        public boolean isDispatchThread() {
            log.append("thread;");
            return false;
        }

        @Override
        public void scheduleDispatch(Runnable r) {
            log.append("schedule;");
        }

        @Override
        public SecondaryLoop createSecondaryLoop() {
            log.append("loop;");
            return new Loop();
        }
    }

    /** A secondary loop that waits for nothing. */
    static class Loop implements SecondaryLoop {
        public boolean enter() {
            return false;
        }

        public boolean exit() {
            return false;
        }
    }

    /** A concrete drag source, so that one can be built. */
    static class Source extends DragSourceContextWrapper {
        Source(java.awt.dnd.DragGestureEvent e) {
            super(e);
        }

        @Override
        protected void setNativeCursor(Cursor c, int type) {
        }

        @Override
        protected void startDrag(Transferable t, long[] formats, Map<Long, DataFlavor> map) {
        }

        @Override
        public void startSecondaryEventLoop() {
        }

        @Override
        public void quitSecondaryEventLoop() {
        }
    }

    /** A concrete drop target, so that one can be built. */
    static class Target extends DropTargetContextWrapper {
        public void setTargetActions(int a) {
        }

        public int getTargetActions() {
            return 0;
        }

        public DropTarget getDropTarget() {
            return null;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return null;
        }

        public Transferable getTransferable() {
            return null;
        }

        public boolean isTransferableJVMLocal() {
            return false;
        }

        public void acceptDrag(int a) {
        }

        public void rejectDrag() {
        }

        public void acceptDrop(int a) {
        }

        public void rejectDrop() {
        }

        public void dropComplete(boolean s) {
        }
    }

    /** What the package does, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        a.add("mask|" + SwingInterOpUtils.GRAB_EVENT_MASK);

        // The key rule, over the whole grid that matters.
        final int[] keys = {
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
        };
        final int[] allowed = {0, 1, 2, 3, 1073741824, 1073741827, 1073741831, -1};
        final StringBuilder g = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            for (int j = 0; j < allowed.length; j++) {
                g.append(DragSourceContextWrapper.convertModifiersToDropAction(
                        keys[i], allowed[j])).append(';');
            }
        }
        a.add("actions|" + g);

        // Grabbing the mouse does not exist without a windowing system, neither here nor in a JDK
        // without a screen.
        a.add("ungrab-null|" + SwingInterOpUtils.isUngrabEvent(null));
        a.add("ungrab-other|" + SwingInterOpUtils.isUngrabEvent(new Ev(new Object(), 9999)));
        a.add("grab|" + attempt(new Grab()));
        a.add("post-null|" + attempt(new PostNull()));

        // Giving up dispatching: the queue has to start asking the other one.
        final Queue queue = new Queue();
        final Disp d = new Disp();
        DispatcherWrapper.setFwDispatcher(queue, d);
        queue.dispatch(new Ev(new Object(), 9999));
        a.add("dispatch|" + d.log);
        final SecondaryLoop loop = queue.createSecondaryLoop();
        a.add("loop|" + d.log + "|" + (loop != null));
        a.add("push|" + attempt(new Push(queue)));
        a.add("install-null|" + attempt(new InstallNull(queue)));
        a.add("null-queue|" + attempt(new NullQueue(d)));

        // The source of a drag that never started.
        a.add("source|" + attempt(new Build()));
        a.add("context|" + new Source(null).getDragSourceContext());
        a.add("drag-end|" + attempt(new DragEnd()));

        // The target.
        a.add("bind-null|" + attempt(new BindNull()));
        a.add("reset-null|" + attempt(new ResetNull()));
        a.add("bind|" + attempt(new Bind()));

        // The window nobody sees needs a screen there is not.
        a.add("frame|" + attempt(new Frame()));

        return a.toArray(new String[a.size()]);
    }

    /** Runs it and returns "ok" or the simple name of whatever it threw. */
    static String attempt(Runnable r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class Grab implements Runnable {
        public void run() {
            SwingInterOpUtils.grab(null, null);
            SwingInterOpUtils.ungrab(null, null);
        }
    }

    static class PostNull implements Runnable {
        public void run() {
            SwingInterOpUtils.postEvent(null, null);
        }
    }

    static class Push implements Runnable {
        private final EventQueue queue;

        Push(EventQueue queue) {
            this.queue = queue;
        }

        public void run() {
            queue.push(new EventQueue());
        }
    }

    static class InstallNull implements Runnable {
        private final EventQueue queue;

        InstallNull(EventQueue queue) {
            this.queue = queue;
        }

        public void run() {
            DispatcherWrapper.setFwDispatcher(queue, null);
        }
    }

    static class NullQueue implements Runnable {
        private final DispatcherWrapper d;

        NullQueue(DispatcherWrapper d) {
            this.d = d;
        }

        public void run() {
            DispatcherWrapper.setFwDispatcher(null, d);
        }
    }

    static class Build implements Runnable {
        public void run() {
            new Source(null);
        }
    }

    static class DragEnd implements Runnable {
        public void run() {
            new Source(null).dragDropFinished(true, 3, 10, 20);
        }
    }

    static class BindNull implements Runnable {
        public void run() {
            new Target().setDropTargetContext(null, null);
        }
    }

    static class ResetNull implements Runnable {
        public void run() {
            new Target().reset(null);
        }
    }

    static class Bind implements Runnable {
        public void run() {
            final DropTargetContext c = new DropTarget().getDropTargetContext();
            final Target t = new Target();
            t.setDropTargetContext(c, t);
            t.reset(c);
        }
    }

    static class Frame implements Runnable {
        public void run() {
            new LightweightFrameWrapper();
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
