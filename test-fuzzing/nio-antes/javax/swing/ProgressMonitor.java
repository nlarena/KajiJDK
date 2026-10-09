package javax.swing;

import java.awt.Component;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A progress notice that appears only if the task turns out to be a long one.
 *
 * <h2>The two delays, which are the whole class</h2>
 *
 * <p>Most of the tasks one fears will be slow end in a blink. Showing a notice for each one
 * fills the screen with windows that appear and disappear.
 *
 * <p>This class waits twice. First {@link #setMillisToDecideToPopup} -- half a second by default
 * -- doing nothing: if the task finishes there, there never was a notice. Once that term is up,
 * it <em>estimates</em> how much is left from what has been advanced so far, and only if the
 * estimate goes over {@link #setMillisToPopup} does it show the notice. A task that in half a
 * second is already at 90% does not deserve it.
 *
 * <p>That estimate is the reason {@link #setProgress} has to be called often: with no advances
 * there is nothing to estimate from.
 *
 * <h2>Cancelling is a question, not an order</h2>
 *
 * <p>{@link #isCanceled} says that the user pressed Cancel. It stops nothing: the task has to
 * look at it and decide. It is right -- only the task knows how to give up without leaving
 * things half done -- and it is what has to be remembered, because a task that does not consult
 * it shows a cancel button that does not cancel.
 *
 * <h2>With no screen</h2>
 *
 * <p>The whole arithmetic -- the bounds, the progress, the two delays, the estimate -- happens
 * all the same. What does not appear is the notice, and therefore {@link #isCanceled} always
 * gives false: there is no button to press.
 */
public class ProgressMonitor implements Accessible {

    protected AccessibleContext accessibleContext;

    private final Component parentComponent;
    private Object message;
    private String note;
    private int min;
    private int max;
    private int v;
    private int millisToDecideToPopup = 500;
    private int millisToPopup = 2000;
    private long T0;
    private boolean canceled;
    private boolean closed;
    private JDialog dialog;
    private JProgressBar myBar;
    private JLabel noteLabel;

    /**
     * A monitor for a task that goes from {@code min} to {@code max}.
     *
     * <p>It shows nothing yet; see the class note.
     */
    public ProgressMonitor(Component parentComponent, Object message, String note, int min,
            int max) {
        this.parentComponent = parentComponent;
        this.message = message;
        this.note = note;
        this.min = min;
        this.max = max;
        this.v = min;
        this.T0 = System.currentTimeMillis();
    }

    /**
     * It notes how much was advanced, and decides whether it is time to show the notice.
     *
     * <p>Reaching the maximum closes it: the task finished.
     */
    public void setProgress(int nv) {
        v = nv;
        if (nv >= max) {
            close();
            return;
        }
        if (closed) {
            return;
        }
        if (dialog != null) {
            update();
            return;
        }
        long dur = System.currentTimeMillis() - T0;
        if (dur < millisToDecideToPopup) {
            return;
        }
        // The total is estimated from what was advanced, and it is shown only if what is left
                    // justifies it. See the class note.
        int progress = nv - min;
        if (progress <= 0) {
            return;
        }
        long estimated = dur * (max - min) / progress;
        if (estimated - dur < millisToPopup) {
            return;
        }
        show();
    }

    /** It builds and shows the notice; with no screen it does not get as far as being shown. */
    private void show() {
        myBar = new JProgressBar();
        myBar.setMinimum(min);
        myBar.setMaximum(max);
        myBar.setValue(v);
        if (note != null) {
            noteLabel = new JLabel(note);
        }
        try {
            JOptionPane pane = new JOptionPane(new Object[] {message, noteLabel, myBar},
                    JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
                    new Object[] {UIManager.getString("OptionPane.cancelButtonText") != null
                            ? UIManager.getString("OptionPane.cancelButtonText") : "Cancel"},
                    null);
            dialog = pane.createDialog(parentComponent,
                    UIManager.getString("ProgressMonitor.progressText") != null
                            ? UIManager.getString("ProgressMonitor.progressText") : "Progress...");
            dialog.setVisible(true);
        } catch (java.awt.HeadlessException e) {
            // With no screen there is no notice; the arithmetic goes on the same. See the class
            // note.
            dialog = null;
        }
    }

    private void update() {
        if (myBar != null) {
            myBar.setValue(v);
        }
    }

    /**
     * It closes the notice if it was up.
     *
     * <p>It can be called even though it never appeared; that is the usual thing when the task was
     * fast.
     */
    public void close() {
        closed = true;
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
        myBar = null;
        noteLabel = null;
    }

    public int getMinimum() {
        return min;
    }

    public void setMinimum(int m) {
        min = m;
        if (myBar != null) {
            myBar.setMinimum(m);
        }
    }

    public int getMaximum() {
        return max;
    }

    public void setMaximum(int m) {
        max = m;
        if (myBar != null) {
            myBar.setMaximum(m);
        }
    }

    /** Whether the user pressed Cancel; see the class note. */
    public boolean isCanceled() {
        return canceled;
    }

    /** How long it waits before even thinking of showing the notice. */
    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
    }

    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }

    /** How much has to be left, estimated, for it to be worth showing. */
    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }

    public int getMillisToPopup() {
        return millisToPopup;
    }

    /**
     * The text that changes as the task advances.
     *
     * <p>Null when the monitor is built means there is not going to be one, and setting it
     * afterwards does not add it: the notice is built once and does not change shape.
     */
    public void setNote(String note) {
        this.note = note;
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }

    public String getNote() {
        return note;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
