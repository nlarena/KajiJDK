package java.util.prefs;

import java.util.EventListener;

// Whoever wants to learn that a node has gained or lost a child.
//
// Just as with the keys, **one level** is listened to: a grandchild being given a child does not
// arrive here.
public interface NodeChangeListener extends EventListener {

    // A child of `evt.getParent()` was born. Mind that `Preferences.node()` creates the node if it
    // did not exist, so this notice can come out of a call that looked like only a query.
    void childAdded(NodeChangeEvent evt);

    // A child of `evt.getParent()` was removed.
    void childRemoved(NodeChangeEvent evt);
}
