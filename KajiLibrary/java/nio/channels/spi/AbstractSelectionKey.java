package java.nio.channels.spi;

import java.nio.channels.SelectionKey;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractSelectionKey — the validity of a key, solved.
 *
 * <p>It is the smallest class of the package and it does a single thing, which is the one everybody
 * would do wrongly: keeping **cancelling** apart from **invalidating**.
 *
 * <p>{@link #cancel()} is called by the user and has to be cheap and idempotent; the only thing it
 * does is mark the key and note it in the selector's list of cancelled ones. The real dropping
 * --taking the channel out, releasing whatever there is-- happens afterwards, inside the next
 * selection, where the selector owns its structures. Doing it the other way round --dropping on the
 * spot-- means modifying the set of keys while another thread may be walking it.
 *
 * <p>{@link #invalidate()} is the opposite: it is not called by the user --it is not public-- but
 * by the selector, once it has done the dropping.
 */
public abstract class AbstractSelectionKey extends SelectionKey {

    private boolean validFlag = true;

    protected AbstractSelectionKey() {
    }

    public final boolean isValid() {
        return this.validFlag;
    }

    // It is used by `AbstractSelector.deregister`. Package-private as in the JDK: invalidating
    // without dropping would leave the channel registered in a selector that no longer looks at it.
    void invalidate() {
        this.validFlag = false;
    }

    /**
     * Cancels the key.
     *
     * <p>Idempotent: calling it twice does not note it twice in the list of cancelled ones, which
     * would otherwise grow without limit in any loop that cancels just in case.
     */
    public final void cancel() {
        boolean firstCancel = false;
        synchronized (this) {
            if (this.validFlag) {
                this.validFlag = false;
                firstCancel = true;
            }
        }
        if (firstCancel) {
            ((AbstractSelector) this.selector()).cancel(this);
        }
    }
}
