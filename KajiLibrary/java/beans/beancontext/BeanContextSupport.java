package java.beans.beancontext;

import java.beans.Beans;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.Visibility;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * The reusable implementation of {@link BeanContext}.
 *
 * <p>It is at once the collection of the children and a child of another context — it extends
 * {@link BeanContextChildSupport} so as not to repeat the upper half of that relation.
 *
 * <h2>Why the children go in a map and not a list</h2>
 *
 * <p>{@link #children} is a `HashMap` from bean to {@link BCSChild}, not a list, for two things
 * that happen often: `contains` and `remove` are called once per membership operation, and with a
 * list they would be full scans; and something more than the child has to be kept for each child,
 * its `BCSChild`. This note said the `BCSChild` records who brought the child in, telling a bean
 * added by itself from one that arrived as another's delegate. {@link #add} always passes this
 * context's peer as that value, so nothing here tells the two apart.
 *
 * <h2>The lock</h2>
 *
 * <p>Everything that touches membership is synchronized on {@link BeanContext#globalHierarchyLock},
 * not on `this`. See the note in {@link BeanContext}: an operation can touch two contexts, and with
 * one lock per context two crossed moves deadlock.
 *
 * <h2>The GUI</h2>
 *
 * <p>{@link #needsGui} answers by asking the children, one by one, instead of keeping a flag. That
 * is deliberate: the answer changes when a child comes or goes, and a flag would have to be
 * recomputed on every add and every remove to answer the same thing.
 */
public class BeanContextSupport extends BeanContextChildSupport
        implements BeanContext, Serializable, PropertyChangeListener, VetoableChangeListener {

    /** The children, from bean to its {@link BCSChild}. See the class note on why it is a map. */
    protected transient HashMap children;

    /** The listeners for children being added and removed. */
    protected transient ArrayList bcmListeners;

    /** This context's locale. */
    protected Locale locale;

    /** Whether it is in design mode. */
    protected boolean designTime;

    /** Whether using a GUI is allowed. */
    protected boolean okToUseGui;

    // How many `writeChildren` calls are in progress; `isSerializing()` is `serializing > 0`. This
    // comment said it counts the serializable children still to be written; it goes up on entry and
    // down on exit, whatever the children are.
    private transient int serializing;

    /** A context with no parent, the default locale, in design mode and with GUI allowed. */
    public BeanContextSupport() {
        this(null, null, true, true);
    }

    /**
     * A context acting in the name of `peer`.
     *
     * <p>This javadoc said a child of `peer`. `peer` is the context this one acts for —see the
     * four-argument constructor— not its parent.
     */
    public BeanContextSupport(BeanContext peer) {
        this(peer, null, true, true);
    }

    /** With that locale. */
    public BeanContextSupport(BeanContext peer, Locale lcle) {
        this(peer, lcle, true, true);
    }

    /** With that locale and that design mode. */
    public BeanContextSupport(BeanContext peer, Locale lcle, boolean dTime) {
        this(peer, lcle, dTime, true);
    }

    /**
     * The constructor all the others end up in.
     *
     * @param peer the context it acts in the name of, or `null` to act in its own name
     * @param lcle the locale, or `null` for the system's
     * @param dTime whether it starts in design mode
     * @param visible whether using a GUI is allowed
     */
    public BeanContextSupport(BeanContext peer, Locale lcle, boolean dTime, boolean visible) {
        super(peer);
        this.locale = lcle == null ? Locale.getDefault() : lcle;
        this.designTime = dTime;
        this.okToUseGui = visible;
        this.initialize();
    }

    /** The context it acts in the name of: this one, or the one that delegated to it. */
    public BeanContext getBeanContextPeer() {
        return (BeanContext) this.getBeanContextChildPeer();
    }

    /**
     * Builds the internal structures. Called from the constructor.
     *
     * <p>This javadoc said it is also called on deserialization, and that this is why it is a
     * method: without it a context read back from a stream would keep null maps. No class in this
     * package declares `readObject`, so nothing calls it then, and `children` and `bcmListeners`,
     * being transient, do come back null.
     */
    protected synchronized void initialize() {
        this.children = new HashMap();
        this.bcmListeners = new ArrayList();
    }

    // ---- membership -----------------------------------------------------------------------------

    /** How many children it has. */
    public int size() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.size();
        }
    }

    /** Whether it has none. */
    public boolean isEmpty() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.isEmpty();
        }
    }

    /** Whether that object is a child of this context. */
    public boolean contains(Object o) {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.containsKey(o);
        }
    }

    /**
     * The same as {@link #contains}.
     *
     * <p>The JDK declares both; `containsKey` is the name the children map uses.
     */
    public boolean containsKey(Object o) {
        return this.contains(o);
    }

    /** Whether all of those are children of this context. */
    public boolean containsAll(Collection c) {
        synchronized (BeanContext.globalHierarchyLock) {
            Iterator it = c.iterator();
            while (it.hasNext()) {
                if (!this.contains(it.next())) {
                    return false;
                }
            }
            return true;
        }
    }

    /** The children, one by one. */
    public Iterator iterator() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().iterator();
        }
    }

    /** The children, as an array. */
    public Object[] toArray() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().toArray();
        }
    }

    /**
     * The children, in that array if they fit.
     *
     * <p>The JDK declares it as a plain `Object[] toArray(Object[])`, because `BeanContext` extends
     * a **raw** `Collection` and there the inherited members come erased. Here it has a type
     * parameter because of a bug in our javac: it does not erase the members of a raw supertype, so
     * it rejects the JDK's form (compiler finding #475, still open: its repro still fails to
     * compile). Both have the same erasure --`Object[] toArray(Object[])`-- so the signature left
     * in the `.class` is the JDK's; what changes is the generic signature, which accepts a little
     * more than the JDK's.
     */
    public <T> T[] toArray(T[] arry) {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().toArray(arry);
        }
    }

    /** The {@link BCSChild} of each child, one by one. */
    protected Iterator bcsChildren() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.values().iterator();
        }
    }

    /** A copy of the array of children, to walk without holding the lock. */
    protected final Object[] copyChildren() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().toArray();
        }
    }

    /**
     * Adds a child.
     *
     * <p>What makes this method long is what has to happen **before** the bean is inside: it is
     * validated, its context is set —which the child can veto— and only then does it enter the map.
     * In the other order, a veto would leave a bean in the collection that does not believe it is a
     * member.
     *
     * @return `false` if it was already there
     * @throws IllegalStateException if this context refuses the child, or the child vetoes its move
     *     into this context
     */
    public boolean add(Object targetChild) {
        if (targetChild == null) {
            throw new IllegalArgumentException("cannot add null");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.children.containsKey(targetChild)) {
                return false;
            }
            if (!this.validatePendingAdd(targetChild)) {
                throw new IllegalStateException("the context refuses " + targetChild);
            }
            BeanContextChild cbcc = BeanContextSupport.getChildBeanContextChild(targetChild);
            if (cbcc != null) {
                try {
                    cbcc.setBeanContext(this.getBeanContextPeer());
                } catch (PropertyVetoException e) {
                    throw new IllegalStateException("the child vetoed joining the context", e);
                }
            }
            BCSChild bcsc = this.createBCSChild(targetChild, this.getBeanContextPeer());
            this.children.put(targetChild, bcsc);
            // The listeners are attached AFTER the child is inside: the context listens to its
            // `beanContext` changes to find out if it moves on its own, and attaching them earlier
            // would make the move above arrive as if it were someone else's.
            if (cbcc != null) {
                cbcc.addPropertyChangeListener("beanContext", this);
                cbcc.addVetoableChangeListener("beanContext", this);
            }
            Visibility v = BeanContextSupport.getChildVisibility(targetChild);
            if (v != null) {
                if (this.okToUseGui) {
                    v.okToUseGui();
                } else {
                    v.dontUseGui();
                }
            }
            if (targetChild instanceof java.beans.DesignMode) {
                ((java.beans.DesignMode) targetChild).setDesignTime(this.designTime);
            }
            this.childJustAddedHook(targetChild, bcsc);
            this.fireChildrenAdded(new BeanContextMembershipEvent(this.getBeanContextPeer(),
                    new Object[] { targetChild }));
            return true;
        }
    }

    /**
     * <strong>Not supported.</strong> Always throws {@link UnsupportedOperationException}.
     *
     * <p>It is what the JDK does, and not an omission on its part: an add can fail on its own --the
     * child vetoes its move, or the context refuses it-- and a bulk operation has no way to say
     * what happened halfway. Undoing the ones already in would fire removal events for things
     * nobody got to see; leaving them leaves the caller not knowing which ones stayed. Throwing is
     * the only answer that does not lie.
     *
     * <p>Whoever wants to add several calls {@link #add} in a loop and decides what to do with the
     * one that fails.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException(
                "a context does not add in bulk: see the addAll javadoc");
    }

    /** Removes a child. `false` if it was not there. */
    public boolean remove(Object targetChild) {
        return this.remove(targetChild, true);
    }

    /**
     * Removes a child, telling it or not.
     *
     * <p>`callChildSetBC` is `false` for when **the child already left on its own**: this context
     * found out through the `beanContext` change event, and calling `setBeanContext` on it again
     * would overwrite its new context with `null`.
     */
    protected boolean remove(Object targetChild, boolean callChildSetBC) {
        if (targetChild == null) {
            throw new IllegalArgumentException("cannot remove null");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (!this.children.containsKey(targetChild)) {
                return false;
            }
            if (!this.validatePendingRemove(targetChild)) {
                throw new IllegalStateException("the context refuses to remove " + targetChild);
            }
            BCSChild bcsc = (BCSChild) this.children.remove(targetChild);
            BeanContextChild cbcc = BeanContextSupport.getChildBeanContextChild(targetChild);
            if (cbcc != null) {
                cbcc.removePropertyChangeListener("beanContext", this);
                cbcc.removeVetoableChangeListener("beanContext", this);
                if (callChildSetBC) {
                    try {
                        cbcc.setBeanContext(null);
                    } catch (PropertyVetoException e) {
                        // If the child vetoes, it goes back into the map and the veto
                        // propagates as an IllegalStateException. This comment said the child
                        // cannot prevent its removal and that the veto is recorded here and not
                        // propagated; the code does the opposite. The two listeners removed
                        // just above are not attached again.
                        this.children.put(targetChild, bcsc);
                        throw new IllegalStateException("the child vetoed leaving the context", e);
                    }
                }
            }
            this.childJustRemovedHook(targetChild, bcsc);
            this.fireChildrenRemoved(new BeanContextMembershipEvent(this.getBeanContextPeer(),
                    new Object[] { targetChild }));
            return true;
        }
    }

    /**
     * <strong>Not supported.</strong> See {@link #addAll}, which explains it.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException(
                "a context does not remove in bulk: see the addAll javadoc");
    }

    /**
     * <strong>Not supported.</strong> See {@link #addAll}, which explains it.
     *
     * @throws UnsupportedOperationException always
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException(
                "a context does not retain in bulk: see the addAll javadoc");
    }

    /**
     * <strong>Not supported.</strong> See {@link #addAll}, which explains it.
     *
     * @throws UnsupportedOperationException always
     */
    public void clear() {
        throw new UnsupportedOperationException(
                "a context does not clear in bulk: see the addAll javadoc");
    }

    /** The subclass's chance to refuse an add. Accepts by default. */
    protected boolean validatePendingAdd(Object targetChild) {
        return true;
    }

    /** The subclass's chance to refuse a removal. Accepts by default. */
    protected boolean validatePendingRemove(Object targetChild) {
        return true;
    }

    /**
     * The {@link BCSChild} for that child.
     *
     * <p>This javadoc said a subclass overrides it to record more. {@code BCSChild}'s constructor
     * is package-private, so only a class in this package can build one.
     */
    protected BCSChild createBCSChild(Object targetChild, Object peer) {
        return new BCSChild(targetChild, peer);
    }

    /** Hook after an add. Empty by default. */
    protected void childJustAddedHook(Object child, BCSChild bcsc) {
    }

    /** Hook after a removal. Empty by default. */
    protected void childJustRemovedHook(Object child, BCSChild bcsc) {
    }

    /** Hook after deserializing a child. Empty by default. */
    protected void childDeserializedHook(Object child, BCSChild bcsc) {
    }

    // ---- the type helpers -----------------------------------------------------------------------
    //
    // They go together because they follow one rule: if the object IS of the type, it is returned;
    // if not, its `BeanContextProxy` is asked for the delegate and that one is tried. That is what
    // lets a bean that delegates take part like one that implements directly. This comment said all
    // six follow it; `getChildSerializable` does not ask the proxy.

    /** That object as a {@link BeanContextChild}, directly or through delegation, or `null`. */
    protected static final BeanContextChild getChildBeanContextChild(Object child) {
        if (child instanceof BeanContextChild) {
            return (BeanContextChild) child;
        }
        if (child instanceof BeanContextProxy) {
            return ((BeanContextProxy) child).getBeanContextProxy();
        }
        return null;
    }

    /** That object as a membership listener, or `null`. */
    protected static final BeanContextMembershipListener getChildBeanContextMembershipListener(
            Object child) {
        if (child instanceof BeanContextMembershipListener) {
            return (BeanContextMembershipListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof BeanContextMembershipListener) {
                return (BeanContextMembershipListener) d;
            }
        }
        return null;
    }

    /** That object as a property change listener, or `null`. */
    protected static final PropertyChangeListener getChildPropertyChangeListener(Object child) {
        if (child instanceof PropertyChangeListener) {
            return (PropertyChangeListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof PropertyChangeListener) {
                return (PropertyChangeListener) d;
            }
        }
        return null;
    }

    /** That object as a veto listener, or `null`. */
    protected static final VetoableChangeListener getChildVetoableChangeListener(Object child) {
        if (child instanceof VetoableChangeListener) {
            return (VetoableChangeListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof VetoableChangeListener) {
                return (VetoableChangeListener) d;
            }
        }
        return null;
    }

    /** That object as a {@link Serializable}, or `null`. */
    protected static final Serializable getChildSerializable(Object child) {
        if (child instanceof Serializable) {
            return (Serializable) child;
        }
        return null;
    }

    /** That object as a {@link Visibility}, or `null`. */
    protected static final Visibility getChildVisibility(Object child) {
        if (child instanceof Visibility) {
            return (Visibility) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof Visibility) {
                return (Visibility) d;
            }
        }
        return null;
    }

    /**
     * Whether the two classes are the same.
     *
     * <p>It also compares by name, not only by `equals`, because **two different loaders give two
     * different `Class` objects for the same type**, and in a bean hierarchy children commonly come
     * from different loaders. This javadoc added that with `==` a service registered by one child
     * would not be found by another; {@link BeanContextServicesSupport} keys its services by the
     * `Class` object in a `HashMap`, and nothing in this tree calls this method.
     */
    protected static final boolean classEquals(Class first, Class second) {
        return first.equals(second) || first.getName().equals(second.getName());
    }

    // ---- resources and creation -----------------------------------------------------------------

    /**
     * The resource, looked up with the class loader of the child asking.
     *
     * <p>Using **its** loader and not the context's is what makes the method useful: a child that
     * came from another `.jar` has its resources there, not here.
     */
    public InputStream getResourceAsStream(String name, BeanContextChild bcc) {
        if (name == null || bcc == null) {
            throw new NullPointerException();
        }
        ClassLoader cl = bcc.getClass().getClassLoader();
        return cl == null ? ClassLoader.getSystemResourceAsStream(name)
                : cl.getResourceAsStream(name);
    }

    /** The resource's URL, by the same rule. */
    public URL getResource(String name, BeanContextChild bcc) {
        if (name == null || bcc == null) {
            throw new NullPointerException();
        }
        ClassLoader cl = bcc.getClass().getClassLoader();
        return cl == null ? ClassLoader.getSystemResource(name) : cl.getResource(name);
    }

    /**
     * Instantiates that bean and adds it to this context.
     *
     * @throws IOException if the bean could not be read
     * @throws ClassNotFoundException if the class was not found
     */
    public Object instantiateChild(String beanName) throws IOException, ClassNotFoundException {
        BeanContext bc = this.getBeanContextPeer();
        return Beans.instantiate(bc.getClass().getClassLoader(), beanName, bc);
    }

    // ---- membership listeners -------------------------------------------------------------------

    /** Registers a listener for children being added and removed. */
    public void addBeanContextMembershipListener(BeanContextMembershipListener bcml) {
        if (bcml == null) {
            throw new NullPointerException("bcml");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (!this.bcmListeners.contains(bcml)) {
                this.bcmListeners.add(bcml);
            }
        }
    }

    /** Removes it. */
    public void removeBeanContextMembershipListener(BeanContextMembershipListener bcml) {
        if (bcml == null) {
            throw new NullPointerException("bcml");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            this.bcmListeners.remove(bcml);
        }
    }

    // Events are delivered over a COPY of the listener list. A listener that unregisters itself
    // while being notified is the normal case --it hears of a child's removal and withdraws-- and
    // without the copy that is a concurrent modification in the middle of the walk.
    private Object[] listenersCopy() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.bcmListeners.toArray();
        }
    }

    /** Announces the additions. */
    protected final void fireChildrenAdded(BeanContextMembershipEvent bcme) {
        Object[] ls = this.listenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextMembershipListener) ls[i]).childrenAdded(bcme);
        }
    }

    /** Announces the removals. */
    protected final void fireChildrenRemoved(BeanContextMembershipEvent bcme) {
        Object[] ls = this.listenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextMembershipListener) ls[i]).childrenRemoved(bcme);
        }
    }

    // ---- design mode, locale and GUI ------------------------------------------------------------

    /** Whether it is in design mode. */
    public synchronized boolean isDesignTime() {
        return this.designTime;
    }

    /** Sets design mode and propagates it to the children that understand it. */
    public synchronized void setDesignTime(boolean dTime) {
        if (this.designTime == dTime) {
            return;
        }
        boolean old = this.designTime;
        this.designTime = dTime;
        Object[] all = this.copyChildren();
        for (int i = 0; i < all.length; i++) {
            if (all[i] instanceof java.beans.DesignMode) {
                ((java.beans.DesignMode) all[i]).setDesignTime(dTime);
            }
        }
        this.firePropertyChange(java.beans.DesignMode.PROPERTYNAME, Boolean.valueOf(old),
                Boolean.valueOf(dTime));
    }

    /** The locale. */
    public synchronized Locale getLocale() {
        return this.locale;
    }

    /**
     * Sets the locale.
     *
     * @throws PropertyVetoException if a listener objects
     */
    public synchronized void setLocale(Locale newLocale) throws PropertyVetoException {
        if (newLocale == null || newLocale.equals(this.locale)) {
            return;
        }
        Locale old = this.locale;
        this.fireVetoableChange("locale", old, newLocale);
        this.locale = newLocale;
        this.firePropertyChange("locale", old, newLocale);
    }

    /**
     * Whether some child needs a GUI. See the class note.
     *
     * <p>The JDK also answers `true` for any child that is a `java.awt.Component` or a
     * `java.awt.Container`, because a component needs a screen by definition. This javadoc said
     * that question cannot be asked here because `java.awt` in this library stops at geometry and
     * colour, with no component hierarchy. Both classes exist in KajiLibrary now; the check is
     * simply not made, so a child that is a component and does **not** implement {@link Visibility}
     * counts as not needing a GUI.
     */
    public synchronized boolean needsGui() {
        Object[] all = this.copyChildren();
        for (int i = 0; i < all.length; i++) {
            Visibility v = BeanContextSupport.getChildVisibility(all[i]);
            if (v != null && v.needsGui()) {
                return true;
            }
        }
        return false;
    }

    /** Forbids the GUI, here and in the children that understand it. */
    public synchronized void dontUseGui() {
        this.okToUseGui = false;
        Object[] all = this.copyChildren();
        for (int i = 0; i < all.length; i++) {
            Visibility v = BeanContextSupport.getChildVisibility(all[i]);
            if (v != null) {
                v.dontUseGui();
            }
        }
    }

    /** Allows it, here and in the children that understand it. */
    public synchronized void okToUseGui() {
        this.okToUseGui = true;
        Object[] all = this.copyChildren();
        for (int i = 0; i < all.length; i++) {
            Visibility v = BeanContextSupport.getChildVisibility(all[i]);
            if (v != null) {
                v.okToUseGui();
            }
        }
    }

    /** Whether the GUI is being avoided: it was forbidden and someone would need it. */
    public synchronized boolean avoidingGui() {
        return !this.okToUseGui && this.needsGui();
    }

    // ---- serialization --------------------------------------------------------------------------

    /** Whether the children are being written right now. */
    public boolean isSerializing() {
        return this.serializing > 0;
    }

    /**
     * Writes the serializable children.
     *
     * <p>Those that are **not** serializable are skipped silently, and that is the underlying
     * decision of this method: a context is a collection of other people's beans and cannot demand
     * that all be serializable. Breaking the whole write over one child that is not would make the
     * others' useless.
     *
     * @throws IOException if the stream fails
     */
    public final void writeChildren(ObjectOutputStream oos) throws IOException {
        synchronized (BeanContext.globalHierarchyLock) {
            this.serializing = this.serializing + 1;
            try {
                Object[] all = this.copyChildren();
                int n = 0;
                for (int i = 0; i < all.length; i++) {
                    if (BeanContextSupport.getChildSerializable(all[i]) != null) {
                        n = n + 1;
                    }
                }
                oos.writeInt(n);
                for (int i = 0; i < all.length; i++) {
                    Serializable s = BeanContextSupport.getChildSerializable(all[i]);
                    if (s != null) {
                        oos.writeObject(s);
                    }
                }
            } finally {
                this.serializing = this.serializing - 1;
            }
        }
    }

    /**
     * Reads the children {@link #writeChildren} wrote and adds them.
     *
     * @throws IOException if the stream fails
     * @throws ClassNotFoundException if some child's class is missing
     */
    public final void readChildren(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        synchronized (BeanContext.globalHierarchyLock) {
            int n = ois.readInt();
            for (int i = 0; i < n; i++) {
                Object child = ois.readObject();
                this.add(child);
                BCSChild bcsc = (BCSChild) this.children.get(child);
                this.childDeserializedHook(child, bcsc);
            }
        }
    }

    /** Writes that collection, skipping what is not serializable. */
    protected final void serialize(ObjectOutputStream oos, Collection coll) throws IOException {
        Object[] all = coll.toArray();
        int n = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i] instanceof Serializable) {
                n = n + 1;
            }
        }
        oos.writeInt(n);
        for (int i = 0; i < all.length; i++) {
            if (all[i] instanceof Serializable) {
                oos.writeObject(all[i]);
            }
        }
    }

    /** Reads into that collection what {@link #serialize} wrote. */
    protected final void deserialize(ObjectInputStream ois, Collection coll)
            throws IOException, ClassNotFoundException {
        int n = ois.readInt();
        for (int i = 0; i < n; i++) {
            coll.add(ois.readObject());
        }
    }

    /** Hook before writing. Empty by default. */
    protected void bcsPreSerializationHook(ObjectOutputStream oos) throws IOException {
    }

    /** Hook before reading. Empty by default. */
    protected void bcsPreDeserializationHook(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
    }

    // ---- what this context hears from its children ----------------------------------------------

    /**
     * A child changed context on its own.
     *
     * <p>If it went somewhere else, this context removes it from its collection **without telling
     * it again** —it already is where it wants to be, and a `setBeanContext(null)` here would
     * overwrite its new context. That is exactly the case `remove(Object, boolean)` exists for.
     */
    public void propertyChange(PropertyChangeEvent pce) {
        if (!"beanContext".equals(pce.getPropertyName())) {
            return;
        }
        Object fresh = pce.getNewValue();
        if (fresh != this.getBeanContextPeer()) {
            this.remove(pce.getSource(), false);
        }
    }

    /**
     * A child asks whether it may change context.
     *
     * <p>This context does not object: a child that wants to leave, leaves. It is written out
     * rather than inherited because the interface requires it, and because this is the natural
     * place someone would look for a retention policy.
     */
    public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException {
    }

    /**
     * What this context keeps of each child.
     *
     * <p>The child, and a second object passed at construction. This javadoc said that object is
     * **who brought the child in** —the context for a bean that came in by itself, the other object
     * for one that arrived as its delegate— and that without it a delegate's removal could not be
     * undone properly. {@link #add} always passes this context's peer, and nothing reads the value
     * back.
     */
    protected class BCSChild implements Serializable {

        private final Object child;
        private final Object proxyPeer;

        BCSChild(Object bcc, Object peer) {
            this.child = bcc;
            this.proxyPeer = peer;
        }

        /** The child. */
        Object getChild() {
            return this.child;
        }

        /** The peer passed when it was added; see the class note. */
        Object getProxyPeer() {
            return this.proxyPeer;
        }
    }
}
