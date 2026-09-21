package java.util.prefs;

import java.util.EventObject;

// A node's key changed: which node, which key and what it ended up as.
//
// `getNewValue()` returns `null` when the key was removed, and that is the only way of telling a
// removal from a write. There is no `getOldValue()`: the JDK does not expose one because the notice
// is built after applying the change and the previous value is no longer anywhere that can be
// consulted without touching the store again.
//
// **On serialising.** It inherits from `EventObject`, which is `Serializable`, but a preferences
// event cannot be serialised: it would drag the node along, and a node is a position in a live tree,
// not a datum. The JDK settles it with a private `writeObject` that throws
// `NotSerializableException`; here the class simply never serialises successfully because
// `Preferences` is not `Serializable`.
public class PreferenceChangeEvent extends EventObject {

    private static final long serialVersionUID = 793724513368024975L;

    private final String key;
    private final String newValue;

    // The notice that `key` ended up worth `newValue` in `node`. `newValue` at `null` means the key
    // was removed.
    public PreferenceChangeEvent(Preferences node, String key, String newValue) {
        super(node);
        this.key = key;
        this.newValue = newValue;
    }

    // The node where the change happened.
    public Preferences getNode() {
        return (Preferences) getSource();
    }

    // The key that changed.
    public String getKey() {
        return key;
    }

    // The new value, or `null` if the key was removed.
    public String getNewValue() {
        return newValue;
    }
}
