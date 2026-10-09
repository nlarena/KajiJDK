package java.util;

// An EventListener that wraps another one and carries extra context alongside it — the pattern
// the JDK uses when a listener has to be registered together with a name or a property it cares
// about, without inventing a separate registry to hold that association.
//
// It is abstract and adds nothing but the wrapped listener: the whole point is that a subclass
// supplies the context. Being itself an EventListener is what lets the proxy be handed to any
// API that takes one, which is how the wrapped listener reaches the source at all.
public abstract class EventListenerProxy<T extends EventListener> implements EventListener {

    // The wrapped listener. Final: a proxy's identity is the pair (this context, that listener),
    // so letting it be swapped would make the proxy mean something else halfway through.
    private final T listener;

    // Wraps `listener`. Subclasses call this from their own constructor after taking whatever
    // context they add.
    public EventListenerProxy(T listener) {
        this.listener = listener;
    }

    // The wrapped listener.
    public T getListener() {
        return this.listener;
    }
}
