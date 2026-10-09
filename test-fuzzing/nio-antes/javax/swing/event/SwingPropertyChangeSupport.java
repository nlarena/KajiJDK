package javax.swing.event;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;

/**
 * JavaBeans' {@link PropertyChangeSupport}, with Swing's thread rule.
 *
 * <h2>What it adds</h2>
 *
 * <p>Swing has a hard rule: everything that touches the interface runs on the event dispatch
 * thread. A bean that changes a property from a worker thread and notifies directly would make a
 * listener update a component from the wrong thread -- which does not fail right away, it fails
 * later and somewhere else.
 *
 * <p>With {@link #isNotifyOnEDT} at {@code true} the notice is re-queued on the right thread. The
 * flag starts off for compatibility: this class predates the rule.
 *
 * <h2>What this VM does not do</h2>
 *
 * <p>Re-queueing needs the dispatch thread, which the windowing system provides. This VM does not
 * have it, so the notice goes out on the calling thread, as if the flag were off. The flag is
 * kept and reported faithfully; what does not happen is the thread hop, and it is said here
 * instead of pretending a guarantee that is not met.
 */
public final class SwingPropertyChangeSupport extends PropertyChangeSupport {

    private static final long serialVersionUID = 7162625831330845068L;

    private final boolean notifyOnEDT;

    /** Without re-queueing, which is the historical behaviour. */
    public SwingPropertyChangeSupport(Object sourceBean) {
        this(sourceBean, false);
    }

    /** Choosing whether the notices are re-queued to the interface's thread. */
    public SwingPropertyChangeSupport(Object sourceBean, boolean notifyOnEDT) {
        super(sourceBean);
        this.notifyOnEDT = notifyOnEDT;
    }

    /** Hands the notice out; see the class note about the thread. */
    public void firePropertyChange(PropertyChangeEvent evt) {
        super.firePropertyChange(evt);
    }

    /** Whether the notices should be re-queued to the interface's thread. */
    public boolean isNotifyOnEDT() {
        return this.notifyOnEDT;
    }
}
