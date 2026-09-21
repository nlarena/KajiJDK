package java.util.prefs;

import java.util.EventObject;

// A node has gained or lost a child: which one is the parent and which one the child.
//
// The same event serves for both --what tells them apart is which of {@link NodeChangeListener}'s
// two methods it arrived at-- and that is why there is no flag inside saying whether it was an
// addition or a removal.
//
// Careful with a `childRemoved`'s child: it arrives **already removed**, so almost anything asked of
// it will throw `IllegalStateException`. The only things safe to read off it are `name()` and
// `absolutePath()`, which do not consult the store.
public class NodeChangeEvent extends EventObject {

    private static final long serialVersionUID = 8068949086596572957L;

    private final Preferences child;

    // The notice that `child` was born of --or died under-- `parent`.
    public NodeChangeEvent(Preferences parent, Preferences child) {
        super(parent);
        this.child = child;
    }

    // The parent node.
    public Preferences getParent() {
        return (Preferences) getSource();
    }

    // The child that was added or removed.
    public Preferences getChild() {
        return child;
    }
}
