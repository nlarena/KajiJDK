package java.awt;

import java.io.Serializable;

/**
 * Traverses the focus in the **order the children were added**, entering each container.
 *
 * <p>It is the simplest policy that is good for anything: a depth-first walk of the tree, from left
 * to right, which is exactly the order of the `add` calls. If that order matches the visual order
 * —and it usually does, because components get added as they are read— the Tab does what the user
 * expects without anyone configuring anything.
 *
 * <p>What makes it interesting is {@link #setImplicitDownCycleTraversal}: with `true`, which is the
 * default, on reaching a **cycle root** the traversal **enters** it instead of skipping it. With
 * `false`, the root is visited as one more component and one has to enter by hand. It is the
 * difference between a Tab that goes through the whole tree and one that stays at the current
 * level.
 *
 * <p><strong>The five methods have preconditions worth keeping in mind</strong>, because they fail
 * in two different ways and for different reasons. The container passed in has to be a **cycle
 * root** or a **policy provider** —if not, the question makes no sense: what is traversed is not
 * any container but a cycle— and that is rejected with `IllegalArgumentException`. On the other
 * hand, if the container is not **visible and displayable**, the answer is `null`: it is not an
 * error to ask about a cycle that cannot be seen yet, there is simply nobody to give the focus to.
 * Without a screen nothing is displayable, so without a screen these methods always return `null`.
 */
public class ContainerOrderFocusTraversalPolicy extends FocusTraversalPolicy
        implements Serializable {

    private static final long serialVersionUID = 486933713763926351L;

    /** Whether reaching a cycle root means entering it. */
    private boolean implicitDownCycleTraversal = true;

    /** A container-order policy, one that enters the cycles. */
    public ContainerOrderFocusTraversalPolicy() {
    }

    /**
     * The one after `aComponent` within `aContainer`.
     *
     * @return the next one, or the first one if `aComponent` was the last —the traversal is a
     *     cycle—, or `null` if there is none that accepts the focus
     * @throws IllegalArgumentException if either is `null` or if `aComponent` is not in
     *     `aContainer`
     */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        this.checkArgs(aContainer, aComponent);
        if (!this.traversable(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> order = new java.util.ArrayList<Component>();
        this.collect(aContainer, order, aComponent);
        int i = order.indexOf(aComponent);
        if (i < 0) {
            return this.getFirstComponent(aContainer);
        }
        for (int j = i + 1; j < order.size(); j++) {
            Component c = order.get(j);
            if (this.accept(c)) {
                return this.downCycleIfEnabled(c);
            }
        }
        return this.getFirstComponent(aContainer);
    }

    /**
     * The one before `aComponent` within `aContainer`.
     *
     * @return the previous one, or the last one if `aComponent` was the first, or `null` if there
     *     is none
     * @throws IllegalArgumentException if either is `null` or if `aComponent` is not in
     *     `aContainer`
     */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        this.checkArgs(aContainer, aComponent);
        if (!this.traversable(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> order = new java.util.ArrayList<Component>();
        this.collect(aContainer, order, aComponent);
        int i = order.indexOf(aComponent);
        if (i < 0) {
            return this.getLastComponent(aContainer);
        }
        for (int j = i - 1; j >= 0; j--) {
            Component c = order.get(j);
            if (this.accept(c)) {
                return c;
            }
        }
        return this.getLastComponent(aContainer);
    }

    /**
     * The first one of the traversal.
     *
     * @return the first one, or `null` if none accepts the focus
     * @throws IllegalArgumentException if the container is `null`
     */
    public Component getFirstComponent(Container aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
        if (!this.traversable(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> order = new java.util.ArrayList<Component>();
        this.collect(aContainer, order, null);
        for (int i = 0; i < order.size(); i++) {
            if (this.accept(order.get(i))) {
                return this.downCycleIfEnabled(order.get(i));
            }
        }
        return null;
    }

    /**
     * The last one of the traversal.
     *
     * @return the last one, or `null` if none accepts the focus
     * @throws IllegalArgumentException if the container is `null`
     */
    public Component getLastComponent(Container aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
        if (!this.traversable(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> order = new java.util.ArrayList<Component>();
        this.collect(aContainer, order, null);
        for (int i = order.size() - 1; i >= 0; i--) {
            if (this.accept(order.get(i))) {
                return order.get(i);
            }
        }
        return null;
    }

    /**
     * Whose turn the focus is when the cycle becomes visible.
     *
     * <p>It is the first one: this policy does not tell "the first one" from "the starting one",
     * which is exactly what a more elaborate policy does.
     */
    public Component getDefaultComponent(Container aContainer) {
        return this.getFirstComponent(aContainer);
    }

    /**
     * Gathers the children in order, entering the containers.
     *
     * <p>An invisible or disabled container is not walked: its children could not receive the focus
     * either, and going down to look at them one by one would be walking a whole subtree to discard
     * it.
     *
     * @param stopAt if it shows up, the descent stops at it —it is the cycle root the question is
     *     asked from—
     */
    private void collect(Container cont, java.util.ArrayList<Component> out, Component stopAt) {
        Component[] children = cont.getComponents();
        for (int i = 0; i < children.length; i++) {
            Component c = children[i];
            out.add(c);
            if (!(c instanceof Container)) {
                continue;
            }
            Container k = (Container) c;
            if (!k.isVisible() || !k.isEnabled()) {
                continue;
            }
            // A cycle root is not opened here: its children belong to **its** traversal, not to
            // this one. With the implicit descent on, entering it is `downCycleIfEnabled`'s
            // business.
            if (k.isFocusCycleRoot() && k != stopAt) {
                continue;
            }
            this.collect(k, out, stopAt);
        }
    }

    /**
     * If the component is a cycle root and the implicit descent is on, returns **whose turn it is
     * inside** instead of the root itself.
     */
    private Component downCycleIfEnabled(Component c) {
        if (!this.implicitDownCycleTraversal || !(c instanceof Container)) {
            return c;
        }
        Container k = (Container) c;
        if (!k.isFocusCycleRoot()) {
            return c;
        }
        FocusTraversalPolicy p = k.getFocusTraversalPolicy();
        Component inside = p == null ? null : p.getDefaultComponent(k);
        return inside != null ? inside : c;
    }

    /**
     * Checks the arguments of the methods that take a component.
     *
     * <p>The two conditions it demands are the JDK's: that the container be a cycle root or a
     * policy provider, and —if it is a root— that the component belong to **that** cycle. Asking
     * "who comes after this one" about a cycle the component is not part of has no answer, and
     * answering something would be worse than rejecting the question.
     */
    private void checkArgs(Container aContainer, Component aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
        if (!aContainer.isFocusTraversalPolicyProvider() && !aContainer.isFocusCycleRoot()) {
            throw new IllegalArgumentException(
                    "aContainer should be focus cycle root or focus traversal policy provider");
        }
        if (aContainer.isFocusCycleRoot() && !aComponent.isFocusCycleRoot(aContainer)) {
            throw new IllegalArgumentException("aContainer is not a focus cycle root of aComponent");
        }
    }

    /**
     * Whether the container can be traversed now.
     *
     * <p>A cycle that cannot be seen has nobody to give the focus to. It is an answer, not an
     * error, and that is why the five methods return `null` instead of throwing.
     */
    private boolean traversable(Container aContainer) {
        synchronized (aContainer.getTreeLock()) {
            return aContainer.isVisible() && aContainer.isDisplayable();
        }
    }

    /**
     * Whether reaching a cycle root means the traversal enters it.
     *
     * <p>With `false`, the root is returned as one more component and whoever traverses decides
     * whether to enter.
     */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    /** Whether it enters the cycle roots; by default, `true`. */
    public boolean getImplicitDownCycleTraversal() {
        return this.implicitDownCycleTraversal;
    }

    /**
     * Whether that component takes part in the traversal.
     *
     * <p>It has to be visible, displayable, enabled and accept the focus. A subclass that wants to
     * skip components overrides this and nothing else.
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.isVisible() || !aComponent.isDisplayable() || !aComponent.isEnabled()
                || !aComponent.isFocusable()) {
            return false;
        }
        return true;
    }
}
