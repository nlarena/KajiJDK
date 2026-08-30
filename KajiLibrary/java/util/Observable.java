package java.util;

// The subject half of the observer protocol: something that keeps a list of Observers and tells
// them when it has changed. Deprecated in the JDK since 9 — see Observer for why — and present
// here because the surface is part of the contract.
//
// The "changed" flag is what makes this more than a listener list: a subclass mutates itself,
// calls setChanged(), and only then does notifyObservers() actually notify. Without the flag
// set, notifyObservers() is a no-op. That is deliberate, and the source of most surprise.
//
// Internals are ours: the JDK holds the observers in a Vector, this holds a plain Object[] that
// grows by doubling. Nothing observable depends on which — the array never escapes.
public class Observable {

    // Whether something has changed since the last notification. Guarded by `this`.
    private boolean changed;

    // The registered observers, in registration order; the first `count` slots are live.
    private Object[] observers;
    private int count;

    // A new observable with no observers and nothing changed.
    public Observable() {
        this.observers = new Object[8];
        this.count = 0;
        this.changed = false;
    }

    // Registers an observer, unless it is already registered. Duplicate registration is a
    // no-op rather than an error, and a null observer is rejected.
    public synchronized void addObserver(Observer o) {
        if (o == null) {
            throw new NullPointerException();
        }
        int i = 0;
        while (i < this.count) {
            if (this.observers[i] == o) {
                return;
            }
            i = i + 1;
        }
        if (this.count == this.observers.length) {
            Object[] bigger = new Object[this.observers.length * 2];
            int j = 0;
            while (j < this.count) {
                bigger[j] = this.observers[j];
                j = j + 1;
            }
            this.observers = bigger;
        }
        this.observers[this.count] = o;
        this.count = this.count + 1;
    }

    // Unregisters an observer. Removing one that was never registered is a no-op.
    public synchronized void deleteObserver(Observer o) {
        int i = 0;
        while (i < this.count) {
            if (this.observers[i] == o) {
                int j = i;
                while (j < this.count - 1) {
                    this.observers[j] = this.observers[j + 1];
                    j = j + 1;
                }
                this.observers[this.count - 1] = null;
                this.count = this.count - 1;
                return;
            }
            i = i + 1;
        }
    }

    // Equivalent to notifyObservers(null).
    public void notifyObservers() {
        this.notifyObservers(null);
    }

    // If, and only if, this has been marked changed: clears the mark and hands `arg` to every
    // registered observer, most recently registered first.
    //
    // The snapshot under the lock is the point. Notification runs *outside* the lock, because an
    // observer is arbitrary code that may well call back into this object; holding the lock
    // across that call is how the JDK's own version would deadlock if it did. The cost is that
    // an observer unregistered during the notification still gets this one last call.
    public void notifyObservers(Object arg) {
        Object[] snapshot;
        int n;
        synchronized (this) {
            if (!this.changed) {
                return;
            }
            n = this.count;
            snapshot = new Object[n];
            int i = 0;
            while (i < n) {
                snapshot[i] = this.observers[i];
                i = i + 1;
            }
            this.clearChanged();
        }
        int k = n - 1;
        while (k >= 0) {
            ((Observer) snapshot[k]).update(this, arg);
            k = k - 1;
        }
    }

    // Unregisters every observer.
    public synchronized void deleteObservers() {
        int i = 0;
        while (i < this.count) {
            this.observers[i] = null;
            i = i + 1;
        }
        this.count = 0;
    }

    // Marks this as changed, so the next notifyObservers actually notifies. Protected: only a
    // subclass gets to decide that its own state changed.
    protected synchronized void setChanged() {
        this.changed = true;
    }

    // Clears the changed mark. Called for you by notifyObservers.
    protected synchronized void clearChanged() {
        this.changed = false;
    }

    // Whether this is currently marked changed.
    public synchronized boolean hasChanged() {
        return this.changed;
    }

    // How many observers are registered.
    public synchronized int countObservers() {
        return this.count;
    }
}
