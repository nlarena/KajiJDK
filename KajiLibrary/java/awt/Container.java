package java.awt;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Set;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * A component that contains other components.
 *
 * <p>That a container **is** a component is what lets the interface be built as a tree of arbitrary
 * depth with no special case at all: a panel inside another panel inside a window are all the same
 * thing.
 *
 * <p>Where the children go is not decided by the container but by its {@link LayoutManager}. With
 * no layout, the children stay where they are put by hand — which is sometimes exactly what is
 * wanted and almost always not, because it stops working as soon as the font size changes.
 *
 * <p>The **Z order** is the order of the children in the list, and it decides two things at once:
 * which one is drawn on top and which one receives a click first. Index 0 is the topmost one, which
 * is the other way round from what intuition says.
 *
 * <p>The focus cycle is the other hierarchy that lives here. A container can be a **cycle root**,
 * and then the tab key goes round inside it instead of leaving. It is what keeps the focus in a
 * dialog from escaping to the window behind.
 */
public class Container extends Component {

    private static final long serialVersionUID = 4613797578919906343L;

    private final List<Component> component = new ArrayList<Component>();
    private LayoutManager layoutMgr;
    private transient ContainerListener containerListener;
    private FocusTraversalPolicy focusTraversalPolicy;
    private boolean focusCycleRoot;
    private boolean focusTraversalPolicyProvider;

    /** An empty container, with no layout. */
    public Container() {
    }

    /** How many children it has. */
    public int getComponentCount() {
        return this.countComponents();
    }

    /**
     * How many children it has.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getComponentCount}.
     */
    @Deprecated
    public int countComponents() {
        synchronized (this.getTreeLock()) {
            return this.component.size();
        }
    }

    /**
     * The child at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is none
     */
    public Component getComponent(int n) {
        synchronized (this.getTreeLock()) {
            if (n < 0 || n >= this.component.size()) {
                throw new ArrayIndexOutOfBoundsException("No such child: " + n);
            }
            return this.component.get(n);
        }
    }

    /** The children, in Z order: 0 is the topmost one. */
    public Component[] getComponents() {
        synchronized (this.getTreeLock()) {
            return this.component.toArray(new Component[this.component.size()]);
        }
    }

    /**
     * The insets the container keeps for itself: borders, title bar.
     *
     * <p>With no window there is no decoration to keep room for, so they are zero. A subclass with
     * a border of its own overrides it.
     */
    public Insets getInsets() {
        return this.insets();
    }

    /**
     * The insets kept.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getInsets}.
     */
    @Deprecated
    public Insets insets() {
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Adds a child at the end.
     *
     * @return the same component, so calls can be chained
     * @throws NullPointerException if the component is `null`
     * @throws IllegalArgumentException if it is added to itself or to one of its descendants
     */
    public Component add(Component comp) {
        this.addImpl(comp, null, -1);
        return comp;
    }

    /**
     * Adds a child with that name, for the layouts that use them.
     *
     * @return the same component
     * @throws NullPointerException if the component is `null`
     */
    public Component add(String name, Component comp) {
        this.addImpl(comp, name, -1);
        return comp;
    }

    /**
     * Adds a child at that position of the Z order.
     *
     * @param index where to put it, or -1 for the end
     * @return the same component
     * @throws NullPointerException if the component is `null`
     */
    public Component add(Component comp, int index) {
        this.addImpl(comp, null, index);
        return comp;
    }

    /**
     * Adds a child with constraints for the layout.
     *
     * @throws NullPointerException if the component is `null`
     */
    public void add(Component comp, Object constraints) {
        this.addImpl(comp, constraints, -1);
    }

    /**
     * Like the previous one, at that position.
     *
     * @throws NullPointerException if the component is `null`
     */
    public void add(Component comp, Object constraints, int index) {
        this.addImpl(comp, constraints, index);
    }

    /**
     * The only place a child gets added through.
     *
     * <p>The five public {@code add} methods go through here, so overriding it is the way to
     * intercept **every** addition without having to override five methods.
     *
     * @throws NullPointerException if the component is `null`
     * @throws IllegalArgumentException if the component is this container itself or an ancestor of
     *     it, or if the index is not valid
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        synchronized (this.getTreeLock()) {
            if (comp == null) {
                throw new NullPointerException("component is null");
            }
            if (index > this.component.size() || index < -1) {
                throw new IllegalArgumentException(
                        "illegal component position");
            }
            // Adding a container inside itself or inside one of its children would build a cycle,
            // and the tree would stop being a tree: walking it would never end.
            if (comp instanceof Container) {
                if (comp == this || ((Container) comp).isAncestorOf(this)) {
                    throw new IllegalArgumentException("adding container's parent to itself");
                }
            }
            Container previous = comp.getParent();
            if (previous != null) {
                previous.remove(comp);
            }
            if (index == -1) {
                this.component.add(comp);
            } else {
                this.component.add(index, comp);
            }
            comp.setParent(this);
            if (this.layoutMgr != null) {
                if (this.layoutMgr instanceof LayoutManager2) {
                    ((LayoutManager2) this.layoutMgr).addLayoutComponent(comp, constraints);
                } else if (constraints instanceof String) {
                    this.layoutMgr.addLayoutComponent((String) constraints, comp);
                } else {
                    this.layoutMgr.addLayoutComponent(null, comp);
                }
            }
            this.invalidate();
            if (this.containerListener != null
                    || (this.eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0) {
                this.processContainerEvent(
                        new ContainerEvent(this, ContainerEvent.COMPONENT_ADDED, comp));
            }
        }
    }

    /**
     * Removes the child at that position.
     *
     * @throws ArrayIndexOutOfBoundsException if there is none
     */
    public void remove(int index) {
        synchronized (this.getTreeLock()) {
            if (index < 0 || index >= this.component.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            Component comp = this.component.remove(index);
            comp.setParent(null);
            if (this.layoutMgr != null) {
                this.layoutMgr.removeLayoutComponent(comp);
            }
            this.invalidate();
            if (this.containerListener != null
                    || (this.eventMask & AWTEvent.CONTAINER_EVENT_MASK) != 0) {
                this.processContainerEvent(
                        new ContainerEvent(this, ContainerEvent.COMPONENT_REMOVED, comp));
            }
        }
    }

    /** Removes that child; if it was not there, nothing happens. */
    public void remove(Component comp) {
        synchronized (this.getTreeLock()) {
            int i = this.component.indexOf(comp);
            if (i >= 0) {
                this.remove(i);
            }
        }
    }

    /** Removes every child. */
    public void removeAll() {
        synchronized (this.getTreeLock()) {
            while (!this.component.isEmpty()) {
                this.remove(this.component.size() - 1);
            }
        }
    }

    /**
     * Which position that child occupies in the Z order.
     *
     * @return the position, or -1 if it is not a child of this container
     */
    public int getComponentZOrder(Component comp) {
        if (comp == null) {
            return -1;
        }
        synchronized (this.getTreeLock()) {
            if (comp.getParent() != this) {
                return -1;
            }
            return this.component.indexOf(comp);
        }
    }

    /**
     * Moves a child to that position of the Z order.
     *
     * <p>It fires no container events: the child was neither added nor removed, it only changed
     * place in the stack. That is the difference from removing it and adding it again, which would
     * fire them.
     *
     * @throws NullPointerException if the component is `null`
     * @throws IllegalArgumentException if the component is this container or an ancestor, or if the
     *     position is not valid
     */
    public void setComponentZOrder(Component comp, int index) {
        synchronized (this.getTreeLock()) {
            if (comp == null) {
                throw new NullPointerException("comp is null");
            }
            if (comp == this) {
                throw new IllegalArgumentException("component cannot be added to itself");
            }
            if (index < 0 || index > this.component.size()
                    || (comp.getParent() == this && index == this.component.size())) {
                throw new IllegalArgumentException("illegal component position");
            }
            if (comp.getParent() == this) {
                this.component.remove(comp);
                this.component.add(index, comp);
            } else {
                this.addImpl(comp, null, index);
            }
        }
    }

    /** Whether that component hangs off this container, at any depth. */
    public boolean isAncestorOf(Component c) {
        synchronized (this.getTreeLock()) {
            Container p = c == null ? null : c.getParent();
            while (p != null) {
                if (p == this) {
                    return true;
                }
                p = p.getParent();
            }
            return false;
        }
    }

    /** The layout, or `null` if it has none. */
    public LayoutManager getLayout() {
        return this.layoutMgr;
    }

    /** Changes its layout and invalidates, because the positions stop being valid. */
    public void setLayout(LayoutManager mgr) {
        this.layoutMgr = mgr;
        this.invalidate();
    }

    /** Asks the layout to place the children. */
    public void doLayout() {
        this.layout();
    }

    /**
     * Places the children.
     *
     * @deprecated it is from the 1.0 model. Use {@link #doLayout}.
     */
    @Deprecated
    public void layout() {
        LayoutManager m = this.layoutMgr;
        if (m != null) {
            m.layoutContainer(this);
        }
    }

    /**
     * Whether validating has to stop here instead of going on upwards.
     *
     * <p>It answers `false`: an ordinary container propagates the validation upwards. The ones with
     * a size of their own —a window, a panel with bars— override it, and that is what keeps
     * changing a button from revalidating the whole application.
     */
    public boolean isValidateRoot() {
        return false;
    }

    /**
     * Lays the subtree out again if it was needed.
     *
     * <p>It is the expensive operation of laying out, and that is why it does nothing if the
     * container was valid already.
     */
    public void validate() {
        synchronized (this.getTreeLock()) {
            // Without a screen there is nothing to lay out, and marking it valid would be claiming
            // that it was laid out. It is the same thing the JDK does: `Container.validate` does
            // not call the component's version and does nothing while there is no window behind it.
            if (!this.isDisplayable()) {
                return;
            }
            if (!this.isValid()) {
                this.validateTree();
            }
            super.validate();
        }
    }

    /**
     * Walks the subtree laying out from the top down.
     *
     * <p>The order matters: a child cannot be placed before its parent knows how much room it gets.
     */
    protected void validateTree() {
        synchronized (this.getTreeLock()) {
            this.doLayout();
            for (int i = 0; i < this.component.size(); i++) {
                Component c = this.component.get(i);
                if (c instanceof Container) {
                    ((Container) c).validateTree();
                } else {
                    c.validate();
                }
            }
        }
    }

    /** Marks that laying out has to happen again, and tells the layout. */
    public void invalidate() {
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m instanceof LayoutManager2) {
                ((LayoutManager2) m).invalidateLayout(this);
            }
            super.invalidate();
        }
    }

    /** The preferred size, according to the layout. */
    public Dimension getPreferredSize() {
        return this.preferredSize();
    }

    /**
     * The preferred size.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getPreferredSize}.
     */
    @Deprecated
    public Dimension preferredSize() {
        if (this.isPreferredSizeSet()) {
            return super.preferredSize();
        }
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m != null) {
                return m.preferredLayoutSize(this);
            }
            // `super.preferredSize()` and not `super.getPreferredSize()`: the latter dispatches
            // back to this very method and the call never ends.
            return super.preferredSize();
        }
    }

    /** The minimum size, according to the layout. */
    public Dimension getMinimumSize() {
        return this.minimumSize();
    }

    /**
     * The minimum size.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getMinimumSize}.
     */
    @Deprecated
    public Dimension minimumSize() {
        if (this.isMinimumSizeSet()) {
            return super.minimumSize();
        }
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m != null) {
                return m.minimumLayoutSize(this);
            }
            return super.minimumSize();
        }
    }

    /**
     * The maximum size, according to the layout.
     *
     * <p>Only a {@link LayoutManager2} knows how to answer it; with a simple layout it falls back
     * to the answer of {@link Component}, which is "no limit".
     */
    public Dimension getMaximumSize() {
        if (this.isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        synchronized (this.getTreeLock()) {
            LayoutManager m = this.layoutMgr;
            if (m instanceof LayoutManager2) {
                return ((LayoutManager2) m).maximumLayoutSize(this);
            }
            return super.getMaximumSize();
        }
    }

    /** How it aligns horizontally, according to the layout. */
    public float getAlignmentX() {
        LayoutManager m = this.layoutMgr;
        if (m instanceof LayoutManager2) {
            return ((LayoutManager2) m).getLayoutAlignmentX(this);
        }
        return super.getAlignmentX();
    }

    /** How it aligns vertically, according to the layout. */
    public float getAlignmentY() {
        LayoutManager m = this.layoutMgr;
        if (m instanceof LayoutManager2) {
            return ((LayoutManager2) m).getLayoutAlignmentY(this);
        }
        return super.getAlignmentY();
    }

    /** It draws itself and draws its children. */
    public void paint(Graphics g) {
        this.paintComponents(g);
    }

    /**
     * Clears the background, draws itself and draws its children.
     *
     * <p>The clearing only if the container is opaque: if it is not, clearing would cover what is
     * below.
     */
    public void update(Graphics g) {
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.setColor(this.getForeground());
        }
        this.paint(g);
    }

    /** It prints itself and prints its children. */
    public void print(Graphics g) {
        this.printComponents(g);
    }

    /**
     * Draws the children, each one with its own clipped context.
     *
     * <p>The clip per child is what guarantees that a child cannot paint outside its rectangle
     * however hard it tries.
     */
    public void paintComponents(Graphics g) {
        synchronized (this.getTreeLock()) {
            for (int i = this.component.size() - 1; i >= 0; i--) {
                Component c = this.component.get(i);
                if (c.isVisible() && g != null) {
                    Graphics cg = g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
                    if (cg != null) {
                        try {
                            c.paintAll(cg);
                        } finally {
                            cg.dispose();
                        }
                    }
                }
            }
        }
    }

    /** The same, for printing. */
    public void printComponents(Graphics g) {
        synchronized (this.getTreeLock()) {
            for (int i = this.component.size() - 1; i >= 0; i--) {
                Component c = this.component.get(i);
                if (c.isVisible() && g != null) {
                    Graphics cg = g.create(c.getX(), c.getY(), c.getWidth(), c.getHeight());
                    if (cg != null) {
                        try {
                            c.printAll(cg);
                        } finally {
                            cg.dispose();
                        }
                    }
                }
            }
        }
    }

    /** Adds a container listener; a `null` is ignored. */
    public synchronized void addContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        this.containerListener = AWTEventMulticaster.add(this.containerListener, l);
        this.enableEvents(AWTEvent.CONTAINER_EVENT_MASK);
    }

    /** Removes that listener. */
    public synchronized void removeContainerListener(ContainerListener l) {
        if (l == null) {
            return;
        }
        this.containerListener = AWTEventMulticaster.remove(this.containerListener, l);
    }

    /** The container listeners. */
    public synchronized ContainerListener[] getContainerListeners() {
        return AWTEventMulticaster.getListeners(this.containerListener, ContainerListener.class);
    }

    /**
     * The listeners of that class.
     *
     * <p>The {@code T extends EventListener} bound is what keeps the question well posed: a class
     * that is not a listener one cannot be passed without raw types.
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ContainerListener.class) {
            return AWTEventMulticaster.getListeners(this.containerListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    /** Sorts the event out; the container ones it handles, the rest go up. */
    protected void processEvent(AWTEvent e) {
        if (e instanceof ContainerEvent) {
            this.processContainerEvent((ContainerEvent) e);
        } else {
            super.processEvent(e);
        }
    }

    /** Tells the container listeners. */
    protected void processContainerEvent(ContainerEvent e) {
        ContainerListener l = this.containerListener;
        if (l == null) {
            return;
        }
        if (e.getID() == ContainerEvent.COMPONENT_ADDED) {
            l.componentAdded(e);
        } else if (e.getID() == ContainerEvent.COMPONENT_REMOVED) {
            l.componentRemoved(e);
        }
    }

    /**
     * Sends it an event of the old model.
     *
     * @deprecated it is from the 1.0 model. Use {@link #dispatchEvent}.
     */
    @Deprecated
    public void deliverEvent(Event e) {
        Component comp = this.getComponentAt(e.x, e.y);
        if (comp != null && comp != this) {
            comp.deliverEvent(e);
        } else {
            this.postEvent(e);
        }
    }

    /**
     * Which child is at that point.
     *
     * <p>It walks in Z order, so it returns the **topmost** one: it is the one that would receive
     * the click.
     */
    public Component getComponentAt(int x, int y) {
        return this.locate(x, y);
    }

    /**
     * Which child is at that point.
     *
     * @deprecated it is from the 1.0 model. Use {@link #getComponentAt(int, int)}.
     */
    @Deprecated
    public Component locate(int x, int y) {
        if (!this.contains(x, y)) {
            return null;
        }
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                Component c = this.component.get(i);
                if (c.isVisible() && c.contains(x - c.getX(), y - c.getY())) {
                    return c;
                }
            }
        }
        return this;
    }

    /**
     * Which child is at that point.
     *
     * @throws NullPointerException if the point is `null`
     */
    public Component getComponentAt(Point p) {
        return this.getComponentAt(p.x, p.y);
    }

    /**
     * Which component is at that point, **going down** the tree.
     *
     * <p>That is the difference from {@link #getComponentAt}: that one looks only at the direct
     * children, this one reaches the leaf. It is what is needed to know who to deliver a click to.
     *
     * @return the deepest component, or `null` if the point falls outside
     */
    public Component findComponentAt(int x, int y) {
        synchronized (this.getTreeLock()) {
            if (!this.contains(x, y) || !this.isVisible() || !this.isEnabled()) {
                return null;
            }
            for (int i = 0; i < this.component.size(); i++) {
                Component c = this.component.get(i);
                int cx = x - c.getX();
                int cy = y - c.getY();
                if (!c.isVisible() || !c.contains(cx, cy)) {
                    continue;
                }
                if (c instanceof Container) {
                    Component deep = ((Container) c).findComponentAt(cx, cy);
                    if (deep != null) {
                        return deep;
                    }
                } else {
                    return c;
                }
            }
            return this;
        }
    }

    /**
     * The same, with a point.
     *
     * @throws NullPointerException if the point is `null`
     */
    public Component findComponentAt(Point p) {
        return this.findComponentAt(p.x, p.y);
    }

    /**
     * Where the mouse is over this container.
     *
     * @param allowChildren whether it counts when the mouse is over a child
     * @return `null` always: the container is not on a screen
     * @throws HeadlessException if there is no screen
     */
    public Point getMousePosition(boolean allowChildren) throws HeadlessException {
        return null;
    }

    /** Notifies that it can be shown, and tells its children. */
    public void addNotify() {
        synchronized (this.getTreeLock()) {
            super.addNotify();
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).addNotify();
            }
        }
    }

    /** Notifies that it can no longer be shown, and tells its children. */
    public void removeNotify() {
        synchronized (this.getTreeLock()) {
            for (int i = this.component.size() - 1; i >= 0; i--) {
                this.component.get(i).removeNotify();
            }
            super.removeNotify();
        }
    }

    /**
     * Changes the font of this one and with it that of the children that have none of their own.
     *
     * <p>It is overridden so as to invalidate the subtree: changing the font changes how much the
     * text of every descendant that inherits it measures.
     */
    public void setFont(Font f) {
        super.setFont(f);
        this.invalidate();
    }

    /** Gives that orientation to itself and to every descendant. */
    public void applyComponentOrientation(ComponentOrientation o) {
        super.applyComponentOrientation(o);
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).applyComponentOrientation(o);
            }
        }
    }

    /** Adds someone to tell about the property changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
    }

    /** Adds a listener for one particular property. */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        super.addPropertyChangeListener(propertyName, listener);
    }

    /** In what order the tab key walks the children. */
    public FocusTraversalPolicy getFocusTraversalPolicy() {
        if (!this.isFocusTraversalPolicyProvider() && !this.isFocusCycleRoot()) {
            return null;
        }
        FocusTraversalPolicy p = this.focusTraversalPolicy;
        if (p != null) {
            return p;
        }
        Container parent = this.getParent();
        if (parent != null) {
            return parent.getFocusTraversalPolicy();
        }
        return null;
    }

    /** Changes its traversal order; with `null` it goes back to inheriting it. */
    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        FocusTraversalPolicy old;
        synchronized (this) {
            old = this.focusTraversalPolicy;
            this.focusTraversalPolicy = policy;
        }
        this.firePropertyChange("focusTraversalPolicy", old, policy);
    }

    /** Whether it has a traversal order of its own. */
    public boolean isFocusTraversalPolicySet() {
        return this.focusTraversalPolicy != null;
    }

    /**
     * Whether the tab key goes round inside it instead of leaving.
     *
     * <p>It is what shuts the focus into a dialog.
     */
    public boolean isFocusCycleRoot() {
        return this.focusCycleRoot;
    }

    /** Declares whether the focus goes round inside it. */
    public void setFocusCycleRoot(boolean focusCycleRoot) {
        boolean old;
        synchronized (this) {
            old = this.focusCycleRoot;
            this.focusCycleRoot = focusCycleRoot;
        }
        this.firePropertyChange("focusCycleRoot", old, focusCycleRoot);
    }

    /**
     * Whether that container is the focus cycle root of this one.
     *
     * <p>A container that is a cycle root is the root **of itself**, which is the difference from
     * the version of {@link Component}.
     */
    public boolean isFocusCycleRoot(Container container) {
        if (this.isFocusCycleRoot() && container == this) {
            return true;
        }
        return super.isFocusCycleRoot(container);
    }

    /**
     * Whether it gives a traversal order to its children without being a cycle root.
     *
     * <p>It is the middle ground between the two: it orders its own, but the tab key can leave.
     */
    public final boolean isFocusTraversalPolicyProvider() {
        return this.focusTraversalPolicyProvider;
    }

    /** Declares whether it gives a traversal order. */
    public final void setFocusTraversalPolicyProvider(boolean provider) {
        boolean old;
        synchronized (this) {
            old = this.focusTraversalPolicyProvider;
            this.focusTraversalPolicyProvider = provider;
        }
        this.firePropertyChange("focusTraversalPolicyProvider", old, provider);
    }

    /** Takes the focus down into this container; it does nothing with no focus manager. */
    public void transferFocusDownCycle() {
    }

    /**
     * The traversal keys in that direction.
     *
     * @throws IllegalArgumentException if the direction is not one of the four
     */
    public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
        return super.getFocusTraversalKeys(id);
    }

    /**
     * Changes the traversal keys.
     *
     * @throws IllegalArgumentException if the direction or some keystroke is not valid
     */
    public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
        super.setFocusTraversalKeys(id, keystrokes);
    }

    /**
     * Whether keys of its own were set in that direction.
     *
     * @throws IllegalArgumentException if the direction is not one of the four
     */
    public boolean areFocusTraversalKeysSet(int id) {
        return super.areFocusTraversalKeysSet(id);
    }

    /** Writes this container and its subtree out, with growing indentation. */
    public void list(PrintStream out, int indent) {
        super.list(out, indent);
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).list(out, indent + 1);
            }
        }
    }

    /** The same, to a writer. */
    public void list(PrintWriter out, int indent) {
        super.list(out, indent);
        synchronized (this.getTreeLock()) {
            for (int i = 0; i < this.component.size(); i++) {
                this.component.get(i).list(out, indent + 1);
            }
        }
    }

    protected String paramString() {
        String s = super.paramString();
        LayoutManager m = this.layoutMgr;
        if (m != null) {
            s = s + ",layout=" + m.getClass().getName();
        }
        return s;
    }

    /**
     * The accessibility of a container.
     *
     * <p>The only thing it adds over a component is the children, and that is all that is needed:
     * the accessibility tree follows the component one.
     */
    protected class AccessibleAWTContainer extends AccessibleAWTComponent {

        /** For the subclasses. */
        protected AccessibleAWTContainer() {
        }

        /** How many children it has. */
        public int getAccessibleChildrenCount() {
            return Container.this.getComponentCount();
        }

        /**
         * The child at that position, if it is accessible.
         *
         * @return the child, or `null` if there is none or it is not accessible
         */
        public Accessible getAccessibleChild(int i) {
            if (i < 0 || i >= Container.this.getComponentCount()) {
                return null;
            }
            Component c = Container.this.getComponent(i);
            if (c instanceof Accessible) {
                return (Accessible) c;
            }
            return null;
        }
    }
}
