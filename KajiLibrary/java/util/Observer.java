package java.util;

// The observer half of the classic observer protocol: something that wants to be told when an
// Observable it registered with has changed.
//
// Deprecated in the JDK since 9, and for good reasons that are worth writing down rather than
// inheriting silently: the protocol carries no information about *what* changed (the payload is
// a bare Object), the notification order is unspecified, and nothing here is safe against a
// listener that mutates the observer list while being notified. It exists in KajiLibrary for
// the same reason it still exists in java.base — the surface is part of the contract.
public interface Observer {

    // Called by an Observable that has changed, with the argument passed to notifyObservers,
    // or null when notifyObservers() was called with no argument.
    void update(Observable o, Object arg);
}
