package java.beans.beancontext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The children added to or removed from a context.
 *
 * <p>One event can carry **several** children, not just one, so that a bulk change can be announced
 * once with the complete list instead of once per child, with intermediate states nobody made. This
 * note gave `addAll` and `removeAll` as the operations that do that; in {@link BeanContextSupport}
 * both throw {@link UnsupportedOperationException}, and `add` and `remove` fire one event per
 * child, so no event built there names more than one.
 *
 * <p>The collection is copied on construction. Without the copy, whoever passed the list could
 * change it while the listeners walk it, and two listeners would see different things in the same
 * event.
 */
public class BeanContextMembershipEvent extends BeanContextEvent {

    /** The children the event names. */
    protected Collection children;

    /** The event with those children. */
    public BeanContextMembershipEvent(BeanContext bc, Collection changes) {
        super(bc);
        if (changes == null) {
            throw new NullPointerException("changes");
        }
        List<Object> copy = new ArrayList<Object>();
        Iterator it = changes.iterator();
        while (it.hasNext()) {
            copy.add(it.next());
        }
        this.children = copy;
    }

    /** The event with those children. */
    public BeanContextMembershipEvent(BeanContext bc, Object[] changes) {
        super(bc);
        if (changes == null) {
            throw new NullPointerException("changes");
        }
        List<Object> copy = new ArrayList<Object>();
        for (int i = 0; i < changes.length; i++) {
            copy.add(changes[i]);
        }
        this.children = copy;
    }

    /** How many children it names. */
    public int size() {
        return this.children.size();
    }

    /** Whether that object is one of the children it names. */
    public boolean contains(Object child) {
        return this.children.contains(child);
    }

    /** The children, as an array. */
    public Object[] toArray() {
        return this.children.toArray();
    }

    /** The children, one by one. */
    public Iterator iterator() {
        return this.children.iterator();
    }
}
