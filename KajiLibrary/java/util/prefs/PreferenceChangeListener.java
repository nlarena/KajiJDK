package java.util.prefs;

import java.util.EventListener;

// Whoever wants to learn that a node's key changed value.
//
// **One node** is listened to, not a subtree: the children's changes do not arrive here. It is
// deliberate -- propagating upwards would force notifying the root for any change anywhere.
public interface PreferenceChangeListener extends EventListener {

    // A key of `evt.getNode()` was added, changed or removed. On a removal
    // {@link PreferenceChangeEvent#getNewValue} returns `null`.
    void preferenceChange(PreferenceChangeEvent evt);
}
