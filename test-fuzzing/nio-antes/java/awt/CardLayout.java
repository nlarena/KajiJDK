package java.awt;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Stacks the children like cards and shows **only one** at a time.
 *
 * <p>It is the layout of a step-by-step wizard, or of a tabbed panel without the tabs: the children
 * are all added and occupy the same place, and only one is visible.
 *
 * <p>Each card is added with a **name**, and that is why
 * {@link LayoutManager#addLayoutComponent(String, Component)} exists with that odd signature in the
 * old interface: this layout is practically the only one that uses it for something.
 *
 * <p>The sizes are those of the **largest** card, not of the one being shown. That is right: if the
 * container adjusted to the visible card, changing card would make it jump in size.
 */
public class CardLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = -4328196481005934313L;

    // `java.util.List` goes with its full name: in this package `List` is the AWT widget, and an
    // `import java.util.List` --which the JLS allows, and which shadows the package's namesake--
    // our javac does not yet resolve the right way round (finding #493, still open).
    private final java.util.List<Component> components = new ArrayList<Component>();
    private final java.util.List<String> names = new ArrayList<String>();
    private int hgap;
    private int vgap;
    private int current;

    /** With no margin around the cards. */
    public CardLayout() {
        this(0, 0);
    }

    /** With the given margins. */
    public CardLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /** The horizontal margin. */
    public int getHgap() {
        return this.hgap;
    }

    /** Changes the horizontal margin. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** The vertical margin. */
    public int getVgap() {
        return this.vgap;
    }

    /** Changes the vertical margin. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Adds a card with that name.
     *
     * <p>The first one added is the one shown; the rest are born hidden.
     *
     * @throws IllegalArgumentException if the name is not a string
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getTreeLock()) {
            if (constraints == null) {
                this.addCard(comp, "");
                return;
            }
            if (!(constraints instanceof String)) {
                throw new IllegalArgumentException(
                        "cannot add to layout: constraint must be a string");
            }
            this.addCard(comp, (String) constraints);
        }
    }

    /** Stores the card and hides all but the one to show. */
    private void addCard(Component comp, String name) {
        if (!this.components.isEmpty()) {
            comp.setVisible(false);
        }
        for (int i = 0; i < this.names.size(); i++) {
            if (this.names.get(i).equals(name)) {
                this.components.get(i).setVisible(false);
            }
        }
        this.components.add(comp);
        this.names.add(name);
        if (this.components.size() == 1) {
            comp.setVisible(true);
            this.current = 0;
        }
    }

    /**
     * Adds a card with that name.
     *
     * @deprecated it is from the 1.0 model. Use {@link #addLayoutComponent(Component, Object)}.
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            this.addCard(comp, name == null ? "" : name);
        }
    }

    /**
     * Removes that card.
     *
     * <p>If it was the one being shown, the first one left is shown: leaving the container with no
     * visible card would be a blank panel for no reason. The JDK shows the next card instead.
     */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            int i = this.components.indexOf(comp);
            if (i < 0) {
                return;
            }
            boolean wasVisible = comp.isVisible();
            this.components.remove(i);
            this.names.remove(i);
            if (wasVisible && !this.components.isEmpty()) {
                this.current = 0;
                this.components.get(0).setVisible(true);
            }
        }
    }

    /** The size of the largest card, plus the margins. */
    public Dimension preferredLayoutSize(Container parent) {
        return this.measure(parent, true);
    }

    /** The same, with the minimum sizes. */
    public Dimension minimumLayoutSize(Container parent) {
        return this.measure(parent, false);
    }

    /** The maximum over all the cards, not the visible one's. */
    private Dimension measure(Container parent, boolean preferred) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int w = 0;
            int h = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
            return new Dimension(insets.left + insets.right + w + this.hgap * 2,
                    insets.top + insets.bottom + h + this.vgap * 2);
        }
    }

    /** No limit: the visible card makes use of everything it is given. */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Centred. */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /** Centred. */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /** It keeps no computations between calls. */
    public void invalidateLayout(Container target) {
    }

    /** Gives the visible card all the available space minus the margins. */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible()) {
                    comp.setBounds(this.hgap + insets.left, this.vgap + insets.top,
                            parent.getWidth() - (this.hgap * 2 + insets.left + insets.right),
                            parent.getHeight() - (this.vgap * 2 + insets.top + insets.bottom));
                }
            }
        }
    }

    /** Shows the card at that position and hides the rest. */
    private void showCard(Container parent, int index) {
        synchronized (parent.getTreeLock()) {
            if (this.components.isEmpty()) {
                return;
            }
            int n = this.components.size();
            int i = ((index % n) + n) % n;
            for (int j = 0; j < n; j++) {
                this.components.get(j).setVisible(j == i);
            }
            this.current = i;
            parent.validate();
        }
    }

    /** Shows the first card. */
    public void first(Container parent) {
        this.showCard(parent, 0);
    }

    /**
     * Shows the next one.
     *
     * <p>After the last it goes back to the first: it is a cycle, not a list with an end.
     */
    public void next(Container parent) {
        this.showCard(parent, this.current + 1);
    }

    /** Shows the previous one; before the first it goes to the last. */
    public void previous(Container parent) {
        this.showCard(parent, this.current - 1);
    }

    /** Shows the last one. */
    public void last(Container parent) {
        this.showCard(parent, this.components.size() - 1);
    }

    /**
     * Shows the card that was added with that name.
     *
     * <p>If there is none with that name nothing happens: it is what the JDK does, and it makes
     * sense — asking for a card that is not there should not break navigation.
     */
    public void show(Container parent, String name) {
        synchronized (parent.getTreeLock()) {
            for (int i = 0; i < this.names.size(); i++) {
                if (this.names.get(i).equals(name)) {
                    this.showCard(parent, i);
                    return;
                }
            }
        }
    }

    public String toString() {
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + "]";
    }
}
