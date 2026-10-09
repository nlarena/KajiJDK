package java.util;

// The root marker of the event-listener protocol: a tagging interface with no methods, which
// every listener type in the JDK's event model extends (ActionListener, ChangeListener, and
// the rest, all outside java.base). It carries no behaviour — its only job is to give
// EventListenerProxy and the reflective listener plumbing a common supertype to name.
public interface EventListener {
}
