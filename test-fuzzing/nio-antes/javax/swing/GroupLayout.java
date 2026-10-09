package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * It lays out by describing the two axes separately.
 *
 * <h2>Each component is declared twice</h2>
 *
 * <p>It is what has to be understood before anything else, and what surprises at first: a group
 * is built for the horizontal axis and another for the vertical one, and <strong>each component
 * has to appear in both</strong>. One says where it falls across, the other where it falls
 * down.
 *
 * <p>It sounds redundant and it is not: it is what allows a label to be aligned to the left
 * with two others horizontally, and at the same time in the same row as its field vertically.
 * With a single tree of positions that cannot be said without a grid, and a grid does not know
 * about rows of different heights.
 *
 * <h2>Two kinds of group</h2>
 *
 * <p>A {@link SequentialGroup} puts its members <em>one after another</em> along the axis; a
 * {@link ParallelGroup} puts them <em>in the same place</em>, aligned with one another. By
 * nesting the two any form is described.
 *
 * <h2>The gaps, which is what almost nobody wants to compute</h2>
 *
 * <p>{@link #setAutoCreateGaps} and {@link #setAutoCreateContainerGaps} set by themselves the
 * space that applies between components and against the edge, by asking {@link LayoutStyle} --
 * which knows what the system's style guide says. It is the reason a form built with this class
 * looks right on Windows and on Linux without touching a number.
 *
 * <h2>The two magic sizes</h2>
 *
 * <p>{@link #DEFAULT_SIZE} means "whatever the component says" and {@link #PREFERRED_SIZE}
 * "the preferred one, and let it not change". They are used in {@code addComponent}'s three
 * gaps, and the typical combination -- {@code addComponent(c, PREFERRED_SIZE, PREFERRED_SIZE,
 * PREFERRED_SIZE)} -- is how one says "this one does not stretch".
 */
public class GroupLayout implements LayoutManager2 {

    /** "The size the component says." */
    public static final int DEFAULT_SIZE = -1;

    /** "The component's preferred one." */
    public static final int PREFERRED_SIZE = -2;

    private static final int MIN = 0;
    private static final int PREF = 1;
    private static final int MAX = 2;

    /** The horizontal axis. */
    static final int HORIZONTAL = 0;

    /** The vertical axis. */
    static final int VERTICAL = 1;

    private final Container host;
    private Group horizontalGroup;
    private Group verticalGroup;
    private boolean honorsVisibility = true;
    private boolean autocreatePadding;
    private boolean autocreateContainerPadding;
    private LayoutStyle layoutStyle;
    private final Map<Component, Boolean> componentHonorsVisibility =
            new HashMap<Component, Boolean>();
    private final List<Component[]> linkedH = new ArrayList<Component[]>();
    private final List<Component[]> linkedV = new ArrayList<Component[]>();

    /**
     * For that container.
     *
     * @throws IllegalArgumentException if it is null
     */
    public GroupLayout(Container host) {
        if (host == null) {
            throw new IllegalArgumentException("Container must be non-null");
        }
        this.host = host;
        // The two axes start described with an empty parallel group. It is what makes measuring a
                // newly created layout give zero instead of blowing up: there is no "undescribed"
                // state.
        setHorizontalGroup(createParallelGroup(Alignment.LEADING));
        setVerticalGroup(createParallelGroup(Alignment.LEADING));
    }

    /**
     * Whether a hidden component stops taking up room.
     *
     * <p>Switched on -- which is the default -- an invisible component measures zero and the others
     * shift. Switched off, it goes on taking up its place. Both ways are used: the first for what
     * appears and disappears, the second so that the screen does not jump.
     */
    public void setHonorsVisibility(boolean honorsVisibility) {
        if (this.honorsVisibility != honorsVisibility) {
            this.honorsVisibility = honorsVisibility;
            invalidateHost();
        }
    }

    public boolean getHonorsVisibility() {
        return honorsVisibility;
    }

    /**
     * The same, for a particular component.
     *
     * <p>Null gives it back to whatever the container says.
     *
     * @throws IllegalArgumentException if the component is null
     */
    public void setHonorsVisibility(Component component, Boolean honorsVisibility) {
        if (component == null) {
            throw new IllegalArgumentException("Component must be non-null");
        }
        if (honorsVisibility == null) {
            componentHonorsVisibility.remove(component);
        } else {
            componentHonorsVisibility.put(component, honorsVisibility);
        }
        invalidateHost();
    }

    /** Whether the gaps between components are set by themselves; see the class note. */
    public void setAutoCreateGaps(boolean autoCreatePadding) {
        if (this.autocreatePadding != autoCreatePadding) {
            this.autocreatePadding = autoCreatePadding;
            invalidateHost();
        }
    }

    public boolean getAutoCreateGaps() {
        return autocreatePadding;
    }

    /** Whether the gap against the container's edge is set by itself. */
    public void setAutoCreateContainerGaps(boolean autoCreateContainerPadding) {
        if (this.autocreateContainerPadding != autoCreateContainerPadding) {
            this.autocreateContainerPadding = autoCreateContainerPadding;
            invalidateHost();
        }
    }

    public boolean getAutoCreateContainerGaps() {
        return autocreateContainerPadding;
    }

    /**
     * The group that describes the horizontal axis.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void setHorizontalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        horizontalGroup = group;
        invalidateHost();
    }

    /**
     * The group that describes the vertical axis.
     *
     * @throws IllegalArgumentException if it is null
     */
    public void setVerticalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        verticalGroup = group;
        invalidateHost();
    }

    /** A group that puts its members one after another. */
    public SequentialGroup createSequentialGroup() {
        return new SequentialGroup(this);
    }

    /** A group that puts them in the same place, aligned to the start. */
    public ParallelGroup createParallelGroup() {
        return createParallelGroup(Alignment.LEADING);
    }

    /**
     * The same, with that alignment.
     *
     * @throws IllegalArgumentException if the alignment is null
     */
    public ParallelGroup createParallelGroup(Alignment alignment) {
        return createParallelGroup(alignment, true);
    }

    /**
     * The same, being able to ask for the group not to stretch.
     *
     * @throws IllegalArgumentException if the alignment is null
     */
    public ParallelGroup createParallelGroup(Alignment alignment, boolean resizable) {
        if (alignment == null) {
            throw new IllegalArgumentException("alignment must be non null");
        }
        if (alignment == Alignment.BASELINE) {
            return new ParallelGroup(this, alignment, resizable);
        }
        return new ParallelGroup(this, alignment, resizable);
    }

    /** A group aligned by the text's baseline. */
    public ParallelGroup createBaselineGroup(boolean resizable, boolean anchorBaselineToTop) {
        return new ParallelGroup(this, Alignment.BASELINE, resizable);
    }

    /**
     * It makes those components all measure the same, on both axes.
     *
     * <p>They take the size of the largest. It is how three buttons with texts of different
     * lengths are made the same width, which is what is expected of a row of buttons.
     *
     * @throws IllegalArgumentException if any of them is null
     */
    public void linkSize(Component... components) {
        linkSize(SwingConstants.HORIZONTAL, components);
        linkSize(SwingConstants.VERTICAL, components);
    }

    /**
     * The same, on a single axis.
     *
     * @throws IllegalArgumentException if any of them is null or the axis is not one of the two
     */
    public void linkSize(int axis, Component... components) {
        if (components == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        for (int i = components.length - 1; i >= 0; i--) {
            if (components[i] == null) {
                throw new IllegalArgumentException("Components must be non-null");
            }
        }
        if (axis == SwingConstants.HORIZONTAL) {
            linkedH.add(components.clone());
        } else if (axis == SwingConstants.VERTICAL) {
            linkedV.add(components.clone());
        } else {
            throw new IllegalArgumentException("Axis must be one of "
                    + "SwingConstants.HORIZONTAL or SwingConstants.VERTICAL");
        }
        invalidateHost();
    }

    /**
     * It swaps one component for another without rebuilding the groups.
     *
     * <p>It is what allows a field in an already described form to be replaced by another.
     *
     * @throws IllegalArgumentException if any of them is null
     */
    public void replace(Component existingComponent, Component newComponent) {
        if (existingComponent == null || newComponent == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        if (horizontalGroup != null) {
            horizontalGroup.replace(existingComponent, newComponent);
        }
        if (verticalGroup != null) {
            verticalGroup.replace(existingComponent, newComponent);
        }
        host.remove(existingComponent);
        host.add(newComponent);
        invalidateHost();
    }

    /** Who knows how much space goes between two things; null uses the look and feel's. */
    public void setLayoutStyle(LayoutStyle layoutStyle) {
        this.layoutStyle = layoutStyle;
        invalidateHost();
    }

    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }

    LayoutStyle style() {
        if (layoutStyle != null) {
            return layoutStyle;
        }
        return LayoutStyle.getInstance();
    }

    /** It does nothing: the components are declared in the groups, not here. */
    public void addLayoutComponent(String name, Component component) {
    }

    /** It does nothing: removing a component from the groups is {@link #replace}'s business. */
    public void removeLayoutComponent(Component component) {
        componentHonorsVisibility.remove(component);
    }

    /**
     * @throws IllegalArgumentException if it is not this layout's container
     */
    public Dimension preferredLayoutSize(Container parent) {
        checkParent(parent);
        prepareGroups();
        return measure(PREF);
    }

    /**
     * @throws IllegalArgumentException if it is not this layout's container
     */
    public Dimension minimumLayoutSize(Container parent) {
        checkParent(parent);
        prepareGroups();
        return measure(MIN);
    }

    /**
     * @throws IllegalArgumentException if it is not this layout's container
     */
    public Dimension maximumLayoutSize(Container parent) {
        checkParent(parent);
        prepareGroups();
        return measure(MAX);
    }

    private Dimension measure(int which) {
        Insets insets = host.getInsets();
        int w = horizontalGroup.size(HORIZONTAL, which);
        int h = verticalGroup.size(VERTICAL, which);
        long tw = (long) w + insets.left + insets.right;
        long th = (long) h + insets.top + insets.bottom;
        return new Dimension((int) Math.min(tw, Integer.MAX_VALUE),
                (int) Math.min(th, Integer.MAX_VALUE));
    }

    /**
     * It places each component.
     *
     * <p>It is the only one of {@link java.awt.LayoutManager2}'s methods that does <em>not</em>
     * require the container to be its own: it lays its own out all the same, whoever is looking.
     * The asymmetry is the JDK's and it is measured.
     */
    public void layoutContainer(Container parent) {
        prepareGroups();
        Insets insets = host.getInsets();
        int width = host.getWidth() - insets.left - insets.right;
        int height = host.getHeight() - insets.top - insets.bottom;
        horizontalGroup.setSize(HORIZONTAL, 0, width);
        verticalGroup.setSize(VERTICAL, 0, height);
        for (int i = 0; i < host.getComponentCount(); i++) {
            Component c = host.getComponent(i);
            Rect r = new Rect();
            horizontalGroup.place(HORIZONTAL, c, r);
            verticalGroup.place(VERTICAL, c, r);
            if (r.setH && r.setV) {
                c.setBounds(insets.left + r.x, insets.top + r.y, r.w, r.h);
            }
        }
    }

    /** Where a component is going to end up; it is filled in one axis at a time. */
    static class Rect {
        int x;
        int y;
        int w;
        int h;
        boolean setH;
        boolean setV;
    }

    public void addLayoutComponent(Component component, Object constraints) {
    }

    public float getLayoutAlignmentX(Container parent) {
        checkParent(parent);
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container parent) {
        checkParent(parent);
        return 0.5f;
    }

    public void invalidateLayout(Container parent) {
        checkParent(parent);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("HORIZONTAL\n").append(horizontalGroup)
                .append("\nVERTICAL\n").append(verticalGroup);
        return b.toString();
    }

    /**
     * @throws IllegalArgumentException if it is not this layout's container
     */
    private void checkParent(Container parent) {
        if (parent != host) {
            throw new IllegalArgumentException(
                    "GroupLayout can only be used with one Container at a time");
        }
    }

    /** It leaves the two groups ready to measure or to place. */
    private void prepareGroups() {
        horizontalGroup.prepareGaps(HORIZONTAL, this);
        verticalGroup.prepareGaps(VERTICAL, this);
        applyLinks();
    }

    /** It gives the linked components the size of the largest; see {@link #linkSize}. */
    private void applyLinks() {
        applyLinks(linkedH, HORIZONTAL);
        applyLinks(linkedV, VERTICAL);
    }

    private void applyLinks(List<Component[]> list, int axis) {
        for (int i = 0; i < list.size(); i++) {
            Component[] group = list.get(i);
            int max = 0;
            for (int j = 0; j < group.length; j++) {
                Dimension d = group[j].getPreferredSize();
                int v = (axis == HORIZONTAL) ? d.width : d.height;
                if (v > max) {
                    max = v;
                }
            }
            for (int j = 0; j < group.length; j++) {
                Group g = (axis == HORIZONTAL) ? horizontalGroup : verticalGroup;
                g.setLink(group[j], max);
            }
        }
    }

    private void invalidateHost() {
        if (host instanceof JComponent) {
            ((JComponent) host).revalidate();
        } else {
            host.invalidate();
        }
        host.repaint();
    }

    /** Whether that component counts as visible for the measurements. */
    boolean count(Component c) {
        Boolean own = componentHonorsVisibility.get(c);
        boolean honours = (own != null) ? own.booleanValue() : honorsVisibility;
        return !honours || c.isVisible();
    }

    /** How a {@link ParallelGroup}'s members are aligned. */
    public enum Alignment {

        /** At the start of the axis: at the top or on the left. */
        LEADING,

        /** At the end: at the bottom or on the right. */
        TRAILING,

        /** In the middle. */
        CENTER,

        /** By the text's baseline; it only makes sense vertically. */
        BASELINE;
    }

    /**
     * A distance with a minimum, a preferred and a maximum.
     *
     * <p>It is not public -- not in the JDK either --: what is seen from outside are the groups.
     * Everything that goes into a group -- a component, a gap, another group -- is one of
     * these.
     */
    abstract static class Spring {

        int origin;
        int size;

        abstract int compute(int axis, int which);

        /** It gives it position and size; the subclasses that hold others share it out. */
        void setSize(int axis, int origin, int size) {
            this.origin = origin;
            this.size = size;
        }

        int size(int axis, int which) {
            return compute(axis, which);
        }

        /** It looks that component up and places it; see {@link GroupLayout#layoutContainer}. */
        void place(int axis, Component c, Rect r) {
        }

        void replace(Component old, Component newValue) {
        }

        void setLink(Component c, int tam) {
        }

        void prepareGaps(int axis, GroupLayout l) {
        }
    }

    /**
     * A group: several springs treated as one.
     *
     * <p>What changes between the two subclasses is a single thing -- whether the sizes are added
     * up or the largest is taken -- and everything else comes from that.
     */
    public abstract static class Group extends Spring {

        final List<Spring> springs = new ArrayList<Spring>();
        final GroupLayout owner;

        Group(GroupLayout owner) {
            this.owner = owner;
        }

        /**
         * It adds another group inside.
         *
         * @throws IllegalArgumentException if it is null
         */
        public Group addGroup(Group group) {
            return addSpring(group);
        }

        /**
         * It adds a component with its natural size.
         *
         * @throws IllegalArgumentException if it is null
         */
        public Group addComponent(Component component) {
            return addComponent(component, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /**
         * It adds a component with those three sizes.
         *
         * <p>See {@link GroupLayout}'s note about {@link GroupLayout#DEFAULT_SIZE} and
         * {@link GroupLayout#PREFERRED_SIZE}.
         *
         * @throws IllegalArgumentException if it is null or the sizes are inconsistent
         */
        public Group addComponent(Component component, int min, int pref, int max) {
            return addSpring(new ComponentSpring(owner, component, min, pref, max));
        }

        /**
         * It adds a fixed gap.
         *
         * @throws IllegalArgumentException if it is negative
         */
        public Group addGap(int size) {
            return addGap(size, size, size);
        }

        /**
         * It adds an elastic gap.
         *
         * @throws IllegalArgumentException if the sizes are inconsistent
         */
        public Group addGap(int min, int pref, int max) {
            return addSpring(new GapSpring(min, pref, max));
        }

        Spring getSpring(int index) {
            return springs.get(index);
        }

        int indexOf(Spring spring) {
            return springs.indexOf(spring);
        }

        /**
         * @throws IllegalArgumentException if it is null
         */
        Group addSpring(Spring spring) {
            if (spring == null) {
                throw new IllegalArgumentException("Spring must be non-null");
            }
            springs.add(spring);
            return this;
        }

        abstract int operator(int a, int b);

        int compute(int axis, int which) {
            int result = 0;
            boolean first = true;
            for (int i = 0; i < springs.size(); i++) {
                Spring s = springs.get(i);
                int v = s.size(axis, which);
                if (first) {
                    result = v;
                    first = false;
                } else {
                    result = operator(result, v);
                }
            }
            return Math.max(0, result);
        }

        void place(int axis, Component c, Rect r) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).place(axis, c, r);
            }
        }

        void replace(Component old, Component newValue) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).replace(old, newValue);
            }
        }

        void setLink(Component c, int tam) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).setLink(c, tam);
            }
        }

        void prepareGaps(int axis, GroupLayout l) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).prepareGaps(axis, l);
            }
        }

        public String toString() {
            return getClass().getSimpleName() + springs;
        }
    }

    /**
     * It puts its members one after another.
     *
     * <p>The group's size is the sum. On sharing out a size other than the preferred one, the
     * leftover is distributed among those that can stretch, in proportion to how much they can: it
     * is what makes an elastic field take all the space and a label not move.
     */
    public static final class SequentialGroup extends Group {

        SequentialGroup(GroupLayout owner) {
            super(owner);
        }

        public SequentialGroup addGroup(Group group) {
            return (SequentialGroup) super.addGroup(group);
        }

        /** The same, being able to exclude the group from the baseline computation. */
        public SequentialGroup addGroup(boolean useAsBaseline, Group group) {
            return addGroup(group);
        }

        public SequentialGroup addComponent(Component component) {
            return (SequentialGroup) super.addComponent(component);
        }

        /** The same, being able to exclude the component from the baseline computation. */
        public SequentialGroup addComponent(boolean useAsBaseline, Component component) {
            return addComponent(component);
        }

        public SequentialGroup addComponent(Component component, int min, int pref, int max) {
            return (SequentialGroup) super.addComponent(component, min, pref, max);
        }

        /** The same; see {@link #addComponent(boolean, Component)}. */
        public SequentialGroup addComponent(boolean useAsBaseline, Component component, int min,
                int pref, int max) {
            return addComponent(component, min, pref, max);
        }

        public SequentialGroup addGap(int size) {
            return (SequentialGroup) super.addGap(size);
        }

        public SequentialGroup addGap(int min, int pref, int max) {
            return (SequentialGroup) super.addGap(min, pref, max);
        }

        /**
         * A gap of the size that applies between those two components.
         *
         * <p>{@link LayoutStyle} decides it; see {@link GroupLayout}'s note.
         *
         * @throws IllegalArgumentException if something is null
         */
        public SequentialGroup addPreferredGap(JComponent comp1, JComponent comp2,
                LayoutStyle.ComponentPlacement type) {
            return addPreferredGap(comp1, comp2, type, DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /**
         * The same, being able to stretch.
         *
         * @throws IllegalArgumentException if something is null
         */
        public SequentialGroup addPreferredGap(JComponent comp1, JComponent comp2,
                LayoutStyle.ComponentPlacement type, int pref, int max) {
            if (comp1 == null || comp2 == null || type == null) {
                throw new IllegalArgumentException("Components and type must be non-null");
            }
            return (SequentialGroup) addSpring(
                    new AutoGapSpring(owner, comp1, comp2, type, pref, max));
        }

        /**
         * A gap between whatever comes before and whatever comes afterwards.
         *
         * @throws IllegalArgumentException if the type is null or is {@code INDENT}
         */
        public SequentialGroup addPreferredGap(LayoutStyle.ComponentPlacement type) {
            return addPreferredGap(type, DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /**
         * The same, being able to stretch.
         *
         * @throws IllegalArgumentException if the type is null or is {@code INDENT}
         */
        public SequentialGroup addPreferredGap(LayoutStyle.ComponentPlacement type, int pref,
                int max) {
            if (type == null) {
                throw new IllegalArgumentException("Type must be non-null");
            }
            if (type == LayoutStyle.ComponentPlacement.INDENT) {
                throw new IllegalArgumentException("Unsupported type");
            }
            return (SequentialGroup) addSpring(
                    new AutoGapSpring(owner, null, null, type, pref, max));
        }

        /** The gap that applies against the container's edge. */
        public SequentialGroup addContainerGap() {
            return addContainerGap(DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /**
         * The same, being able to stretch.
         *
         * @throws IllegalArgumentException if the sizes are inconsistent
         */
        public SequentialGroup addContainerGap(int pref, int max) {
            return (SequentialGroup) addSpring(new ContainerGapSpring(owner, pref, max));
        }

        int operator(int a, int b) {
            long total = (long) a + (long) b;
            return (int) Math.min(total, Integer.MAX_VALUE);
        }

        /**
         * It shares the size out among the members.
         *
         * <p>Each one starts at its preferred one; the leftover -- or the shortfall -- is shared
         * out among those that can move, in proportion to how much room each one has. See the class
         * note.
         */
        void setSize(int axis, int origin, int size) {
            super.setSize(axis, origin, size);
            int n = springs.size();
            if (n == 0) {
                return;
            }
            int[] pref = new int[n];
            int[] margin = new int[n];
            long totalPref = 0;
            long totalMargin = 0;
            boolean grow = true;
            for (int i = 0; i < n; i++) {
                pref[i] = springs.get(i).size(axis, PREF);
                totalPref = totalPref + pref[i];
            }
            long delta = size - totalPref;
            grow = (delta >= 0);
            for (int i = 0; i < n; i++) {
                Spring s = springs.get(i);
                if (grow) {
                    margin[i] = s.size(axis, MAX) - pref[i];
                } else {
                    margin[i] = pref[i] - s.size(axis, MIN);
                }
                if (margin[i] < 0) {
                    margin[i] = 0;
                }
                totalMargin = totalMargin + margin[i];
            }
            int pos = origin;
            long restante = (delta < 0) ? -delta : delta;
            long shared = 0;
            for (int i = 0; i < n; i++) {
                int extra = 0;
                if (totalMargin > 0 && restante > 0) {
                    if (i == n - 1) {
                        extra = (int) Math.min(restante - shared, margin[i]);
                    } else {
                        extra = (int) (restante * margin[i] / totalMargin);
                        if (extra > margin[i]) {
                            extra = margin[i];
                        }
                    }
                    shared = shared + extra;
                }
                int t = grow ? pref[i] + extra : pref[i] - extra;
                springs.get(i).setSize(axis, pos, t);
                pos = pos + t;
            }
        }
    }

    /**
     * It puts its members in the same place, aligned with one another.
     *
     * <p>The group's size is the largest one's. Each member receives the group's size if it can
     * stretch, and if not it stays its preferred size, placed according to the alignment.
     */
    public static class ParallelGroup extends Group {

        private final Alignment childAlignment;
        private final boolean resizable;
        private final Map<Spring, Alignment> alignments = new HashMap<Spring, Alignment>();

        ParallelGroup(GroupLayout owner, Alignment childAlignment, boolean resizable) {
            super(owner);
            this.childAlignment = childAlignment;
            this.resizable = resizable;
        }

        public ParallelGroup addGroup(Group group) {
            return (ParallelGroup) super.addGroup(group);
        }

        public ParallelGroup addComponent(Component component) {
            return (ParallelGroup) super.addComponent(component);
        }

        public ParallelGroup addComponent(Component component, int min, int pref, int max) {
            return (ParallelGroup) super.addComponent(component, min, pref, max);
        }

        public ParallelGroup addGap(int size) {
            return (ParallelGroup) super.addGap(size);
        }

        public ParallelGroup addGap(int min, int pref, int max) {
            return (ParallelGroup) super.addGap(min, pref, max);
        }

        /**
         * It adds a group with an alignment of its own.
         *
         * @throws IllegalArgumentException if the alignment is null
         */
        public ParallelGroup addGroup(Alignment alignment, Group group) {
            if (alignment == null) {
                throw new IllegalArgumentException("Alignment must be non-null");
            }
            addSpring(group);
            alignments.put(group, alignment);
            return this;
        }

        /**
         * It adds a component with an alignment of its own.
         *
         * @throws IllegalArgumentException if the alignment is null
         */
        public ParallelGroup addComponent(Component component, Alignment alignment) {
            return addComponent(component, alignment, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /**
         * The same, with the three sizes.
         *
         * @throws IllegalArgumentException if the alignment is null
         */
        public ParallelGroup addComponent(Component component, Alignment alignment, int min,
                int pref, int max) {
            if (alignment == null) {
                throw new IllegalArgumentException("Alignment must be non-null");
            }
            Spring s = new ComponentSpring(owner, component, min, pref, max);
            addSpring(s);
            alignments.put(s, alignment);
            return this;
        }

        /**
         * Whether the group stretches; see {@link GroupLayout#createParallelGroup(Alignment,
         * boolean)}.
         */
        boolean isResizable() {
            return resizable;
        }

        int operator(int a, int b) {
            return Math.max(a, b);
        }

        int compute(int axis, int which) {
            if (!resizable && which != PREF) {
                return compute(axis, PREF);
            }
            return super.compute(axis, which);
        }

        /** The group's size to each one if it can; if not, its own, aligned. */
        void setSize(int axis, int origin, int size) {
            super.setSize(axis, origin, size);
            for (int i = 0; i < springs.size(); i++) {
                Spring s = springs.get(i);
                int max = s.size(axis, MAX);
                int min = s.size(axis, MIN);
                int t = Math.max(min, Math.min(size, max));
                Alignment a = alignments.get(s);
                if (a == null) {
                    a = childAlignment;
                }
                int off = 0;
                if (t < size) {
                    if (a == Alignment.TRAILING) {
                        off = size - t;
                    } else if (a == Alignment.CENTER) {
                        off = (size - t) / 2;
                    }
                }
                s.setSize(axis, origin + off, t);
            }
        }
    }

    /** A component inside a group. */
    private static class ComponentSpring extends Spring {

        private final GroupLayout owner;
        private Component component;
        private final int min;
        private final int pref;
        private final int max;
        private int linked = -1;

        ComponentSpring(GroupLayout owner, Component component, int min, int pref, int max) {
            if (component == null) {
                throw new IllegalArgumentException("Component must be non-null");
            }
            checkSize(min, pref, max, true);
            this.owner = owner;
            this.component = component;
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        int compute(int axis, int which) {
            if (!owner.count(component)) {
                return 0;
            }
            if (linked >= 0) {
                return linked;
            }
            int requested = (which == MIN) ? min : ((which == PREF) ? pref : max);
            if (requested >= 0) {
                return requested;
            }
            if (requested == PREFERRED_SIZE) {
                return natural(axis, PREF);
            }
            return natural(axis, which);
        }

        private int natural(int axis, int which) {
            Dimension d;
            if (which == MIN) {
                d = component.getMinimumSize();
            } else if (which == PREF) {
                d = component.getPreferredSize();
            } else {
                d = component.getMaximumSize();
            }
            return (axis == HORIZONTAL) ? d.width : d.height;
        }

        void place(int axis, Component c, Rect r) {
            if (c != component) {
                return;
            }
            if (axis == HORIZONTAL) {
                r.x = origin;
                r.w = size;
                r.setH = true;
            } else {
                r.y = origin;
                r.h = size;
                r.setV = true;
            }
        }

        void replace(Component old, Component newValue) {
            if (component == old) {
                component = newValue;
            }
        }

        void setLink(Component c, int tam) {
            if (component == c) {
                linked = tam;
            }
        }

        public String toString() {
            return "Component(" + component.getName() + ")";
        }
    }

    /** A gap of a given size. */
    private static class GapSpring extends Spring {

        private final int min;
        private final int pref;
        private final int max;

        GapSpring(int min, int pref, int max) {
            checkSize(min, pref, max, false);
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        int compute(int axis, int which) {
            int v = (which == MIN) ? min : ((which == PREF) ? pref : max);
            return (v < 0) ? 0 : v;
        }

        public String toString() {
            return "Gap(" + min + "," + pref + "," + max + ")";
        }
    }

    /** A gap whose size is decided by {@link LayoutStyle}. */
    private static class AutoGapSpring extends Spring {

        private final GroupLayout owner;
        private JComponent c1;
        private JComponent c2;
        private final LayoutStyle.ComponentPlacement type;
        private final int pref;
        private final int max;

        AutoGapSpring(GroupLayout owner, JComponent c1, JComponent c2,
                LayoutStyle.ComponentPlacement type, int pref, int max) {
            this.owner = owner;
            this.c1 = c1;
            this.c2 = c2;
            this.type = type;
            this.pref = pref;
            this.max = max;
        }

        private int base(int axis) {
            if (c1 == null || c2 == null) {
                // With no concrete components there is nobody to measure the neighbour on: the
                                // "related" gap against the container itself is used, which is what
                                // is seen in practice.
                return (type == LayoutStyle.ComponentPlacement.UNRELATED) ? 12 : 6;
            }
            int position = (axis == HORIZONTAL) ? SwingConstants.EAST : SwingConstants.SOUTH;
            return owner.style().getPreferredGap(c1, c2, type, position, null);
        }

        int compute(int axis, int which) {
            int b = base(axis);
            if (which == MIN) {
                return b;
            }
            if (which == PREF) {
                return (pref >= 0) ? pref : b;
            }
            if (max == PREFERRED_SIZE) {
                return (pref >= 0) ? pref : b;
            }
            return (max >= 0) ? max : Integer.MAX_VALUE;
        }

        void replace(Component old, Component newValue) {
            if (c1 == old && newValue instanceof JComponent) {
                c1 = (JComponent) newValue;
            }
            if (c2 == old && newValue instanceof JComponent) {
                c2 = (JComponent) newValue;
            }
        }

        public String toString() {
            return "AutoGap(" + type + ")";
        }
    }

    /** The gap against the container's edge. */
    private static class ContainerGapSpring extends Spring {

        private final GroupLayout owner;
        private final int pref;
        private final int max;

        ContainerGapSpring(GroupLayout owner, int pref, int max) {
            this.owner = owner;
            this.pref = pref;
            this.max = max;
        }

        int compute(int axis, int which) {
            int b = 6;
            if (which == MIN) {
                return b;
            }
            if (which == PREF) {
                return (pref >= 0) ? pref : b;
            }
            if (max == PREFERRED_SIZE) {
                return (pref >= 0) ? pref : b;
            }
            return (max >= 0) ? max : Integer.MAX_VALUE;
        }

        public String toString() {
            return "ContainerGap";
        }
    }

    /**
     * That the three sizes be consistent.
     *
     * <p>A component admits {@link #DEFAULT_SIZE} and {@link #PREFERRED_SIZE} in the minimum and
     * in the maximum -- "whatever it has" and "the preferred one" are valid answers when there
     * is somebody to ask --; a gap has nobody to ask its default size, and that is why it only
     * admits {@link #PREFERRED_SIZE}, which there means "do not stretch".
     *
     * @throws IllegalArgumentException if the three sizes are not consistent
     */
    private static void checkSize(int min, int pref, int max, boolean isComponent) {
        checkResizeType(min, isComponent);
        if (!isComponent && pref < 0) {
            throw new IllegalArgumentException("Pref must be positive, DEFAULT_SIZE "
                    + "or PREFERRED_SIZE");
        }
        checkResizeType(max, isComponent);
        if (min >= 0 && pref >= 0 && min > pref) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
        if (pref >= 0 && max >= 0 && pref > max) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
        if (min >= 0 && max >= 0 && min > max) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
    }

    private static void checkResizeType(int type, boolean isComponent) {
        if (type < 0 && ((isComponent && type != DEFAULT_SIZE && type != PREFERRED_SIZE)
                || (!isComponent && type != PREFERRED_SIZE))) {
            throw new IllegalArgumentException("Invalid size");
        }
    }
}
