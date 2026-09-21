package jdk.internal.event;

/**
 * The internal base of every event of JFR.
 *
 * <p>It is not public API --{@code jdk.internal.*} is not exported-- and it exists so that the
 * classes of the JDK that emit events do not have to depend on the {@code jdk.jfr} module.
 * {@link jdk.jfr.Event} inherits from this one.
 *
 * <p>The bodies are empty on purpose, just as in the JDK: when JFR is active, the VM
 * <strong>rewrites</strong> these methods when it loads each subclass, injecting the code that
 * writes the event into the buffer. With no JFR active they do nothing, which is exactly what they
 * have to do.
 */
public abstract class Event {

    /** For the subclasses. */
    protected Event() {
    }

    /** It marks the beginning of the event. */
    public void begin() {
    }

    /** It marks the end of the event. */
    public void end() {
    }

    /** It emits the event. */
    public void commit() {
    }

    /**
     * Whether the event is enabled.
     *
     * @return {@code false} as long as the VM does not rewrite this method
     */
    public boolean isEnabled() {
        return false;
    }

    /**
     * Whether the event would pass the configured filters.
     *
     * @return {@code false} as long as the VM does not rewrite this method
     */
    public boolean shouldCommit() {
        return false;
    }

    /**
     * It sets a field by index.
     *
     * @param index the index of the field
     * @param value the value
     */
    public void set(int index, Object value) {
    }
}
