package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * It walks the focus in the order a comparator says, not in the order things were added.
 *
 * <h2>Why sorting is needed</h2>
 *
 * <p>AWT's policy walks the components in the order they are in the container, which is the
 * order somebody added them in. That almost never agrees with how they are seen: a form built
 * with a grid may have the fields added by columns and be seen by rows, and then the tab key
 * jumps top to bottom when the user expects left to right.
 *
 * <p>This policy sorts before walking. With what criterion is decided by the {@link Comparator},
 * and the only one Swing really uses sorts by position on the screen: it is
 * {@link LayoutFocusTraversalPolicy}.
 *
 * <h2>Going down into the inner cycle</h2>
 *
 * <p>A container that is the root of its own cycle -- an internal frame, a panel marked as such
 * -- is normally skipped whole: the tab key passes by. With
 * {@link #setImplicitDownCycleTraversal} switched on -- which is the default -- the focus
 * <em>goes in</em> and walks what is inside before going on.
 *
 * <h2>Which components go in</h2>
 *
 * <p>{@link #accept} decides. One that is visible, enabled and can receive the focus; the others
 * are skipped. A subclass adjusts it in order to skip those that are covered or read-only too.
 */
public class SortingFocusTraversalPolicy extends InternalFrameFocusTraversalPolicy {

    private Comparator<? super Component> comparator;
    private boolean implicitDownCycleTraversal = true;

    /** With no comparator; a subclass has to set it before using it. */
    protected SortingFocusTraversalPolicy() {
    }

    /** With that sorting criterion. */
    public SortingFocusTraversalPolicy(Comparator<? super Component> comparator) {
        this.comparator = comparator;
    }

    /**
     * The cycle's components, already sorted and filtered.
     *
     * <p>The whole list is built and afterwards it is searched: walking and sorting at once would
     * be faster and much harder to understand, and this list is the size of a screen.
     */
    private List<Component> ciclo(Container aContainer) {
        List<Component> list = new ArrayList<Component>();
        merge(aContainer, list);
        Comparator<? super Component> c = getComparator();
        if (c != null) {
            java.util.Collections.sort(list, c);
        }
        return list;
    }

    private void merge(Container parent, List<Component> list) {
        int n = parent.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component comp = parent.getComponent(i);
            if (comp instanceof Container) {
                Container cont = (Container) comp;
                if (!cont.isFocusCycleRoot() && !cont.isFocusTraversalPolicyProvider()) {
                    // It is not the root of its own cycle: its children are part of this one.
                    merge(cont, list);
                    continue;
                }
                if (getImplicitDownCycleTraversal() && accept(cont)) {
                    list.add(cont);
                    continue;
                }
                if (getImplicitDownCycleTraversal()) {
                    merge(cont, list);
                    continue;
                }
            }
            if (accept(comp)) {
                list.add(comp);
            }
        }
    }

    /** The highest container that is a policy provider, between that one and the component. */
    Container getTopmostProvider(Container focusCycleRoot, Component aComponent) {
        Container aCont = aComponent.getParent();
        Container ftp = null;
        while (aCont != focusCycleRoot && aCont != null) {
            if (aCont.isFocusTraversalPolicyProvider()) {
                ftp = aCont;
            }
            aCont = aCont.getParent();
        }
        if (aCont == null) {
            return null;
        }
        return ftp;
    }

    /**
     * The one that comes next.
     *
     * <p>On reaching the end it goes back to the beginning: a focus cycle is a cycle.
     *
     * @throws IllegalArgumentException if either is null
     */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        require(aContainer, aComponent);
        requireRoot(aContainer);
        List<Component> list = ciclo(aContainer);
        int i = list.indexOf(aComponent);
        if (i < 0) {
            return getFirstComponent(aContainer);
        }
        if (i == list.size() - 1) {
            return list.isEmpty() ? null : list.get(0);
        }
        return list.get(i + 1);
    }

    /**
     * The previous one; on reaching the beginning it jumps to the end.
     *
     * @throws IllegalArgumentException if either is null
     */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        require(aContainer, aComponent);
        requireRoot(aContainer);
        List<Component> list = ciclo(aContainer);
        int i = list.indexOf(aComponent);
        if (i < 0) {
            return getLastComponent(aContainer);
        }
        if (i == 0) {
            return list.isEmpty() ? null : list.get(list.size() - 1);
        }
        return list.get(i - 1);
    }

    /**
     * @throws IllegalArgumentException if the container is null
     */
    public Component getFirstComponent(Container aContainer) {
        requireContainer(aContainer);
        List<Component> list = ciclo(aContainer);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * @throws IllegalArgumentException if the container is null
     */
    public Component getLastComponent(Container aContainer) {
        requireContainer(aContainer);
        List<Component> list = ciclo(aContainer);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /**
     * Where the focus goes on entering the cycle: the first.
     *
     * @throws IllegalArgumentException if the container is null
     */
    public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
    }

    private static void require(Container aContainer, Component aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
    }

    /**
     * It requires the container to be the root of a focus cycle.
     *
     * <p>Only {@link #getComponentAfter} and {@link #getComponentBefore} ask for it: asking "what
     * comes next" only makes sense inside a cycle, whereas "which is the first" can be answered
     * of any container. The asymmetry is the JDK's and it is measured.
     *
     * @throws IllegalArgumentException if it is not
     */
    private static void requireRoot(Container aContainer) {
        if (!aContainer.isFocusCycleRoot() && !aContainer.isFocusTraversalPolicyProvider()) {
            throw new IllegalArgumentException("aContainer should be focus cycle root or "
                    + "focus traversal policy provider");
        }
    }

    private static void requireContainer(Container aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
    }

    /** Whether the focus goes into the inner cycles; see the class note. */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }

    /** The sorting criterion; see the class note. */
    protected void setComparator(Comparator<? super Component> comparator) {
        this.comparator = comparator;
    }

    protected Comparator<? super Component> getComparator() {
        return comparator;
    }

    /**
     * Whether that component goes into the walk.
     *
     * <p>Visible, enabled, and accepting the focus. All three are needed: a hidden component
     * cannot be focused even though it accepts it, and a switched-off one cannot either.
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.isVisible() || !aComponent.isDisplayable()
                || !aComponent.isEnabled() || !aComponent.isFocusable()) {
            return false;
        }
        return true;
    }
}
