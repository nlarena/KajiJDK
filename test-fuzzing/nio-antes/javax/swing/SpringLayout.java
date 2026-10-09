package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * It lays out by saying where each edge goes in relation to another edge.
 *
 * <h2>Edges, not positions</h2>
 *
 * <p>Instead of saying "this component goes at (10, 20)", one says "its west edge is ten from
 * the container's west edge" and "its north edge is five from the south edge of the one
 * above". Each relation is a {@link Spring}, so the distances may be elastic and the set lays
 * itself out again when the window changes size.
 *
 * <h2>Two constraints per axis, and the third overrides the first</h2>
 *
 * <p>Horizontally there are four edges -- west, width, east and centre -- but only two are
 * independent: with any two the other two are determined. Setting a third is not an error:
 * <strong>the oldest is discarded</strong> and the derived ones are recomputed. The same
 * vertically, where there is also the baseline.
 *
 * <p>It is the part that surprises and the one to keep in mind: the order the constraints are
 * set in changes the result. Hence {@link Constraints} carries a history.
 *
 * <h2>The container also has edges</h2>
 *
 * <p>Constraints are set on it just as on a child, and it is how it is told how large it wants
 * to be. A container with no constraints of its own measures whatever is needed for them all to
 * fit.
 *
 * <h2>Cycles are detected, they do not hang</h2>
 *
 * <p>Saying that A is to the right of B and B to the right of A has no solution. Instead of
 * going round for ever, a spring that finds itself returns {@link Spring#UNSET}.
 */
public class SpringLayout implements LayoutManager2 {

    private final Map<Component, Constraints> componentConstraints =
            new HashMap<Component, Constraints>();

    private final Spring cyclicReference = Spring.constant(Spring.UNSET);
    private Set<Spring> cyclicSprings;
    private Set<Spring> acyclicSprings;

    /** The top edge. */
    public static final String NORTH = "North";

    /** The bottom edge. */
    public static final String SOUTH = "South";

    /** The right edge. */
    public static final String EAST = "East";

    /** The left edge. */
    public static final String WEST = "West";

    /** The horizontal centre. */
    public static final String HORIZONTAL_CENTER = "HorizontalCenter";

    /** The vertical centre. */
    public static final String VERTICAL_CENTER = "VerticalCenter";

    /** The text's baseline. */
    public static final String BASELINE = "Baseline";

    /** The width. */
    public static final String WIDTH = "Width";

    /** The height. */
    public static final String HEIGHT = "Height";

    private static final String[] ALL_HORIZONTAL = {WEST, WIDTH, EAST, HORIZONTAL_CENTER};

    private static final String[] ALL_VERTICAL = {NORTH, HEIGHT, SOUTH, VERTICAL_CENTER,
        BASELINE};

    /** A layout with no constraint set. */
    public SpringLayout() {
    }

    /**
     * A component's constraints.
     *
     * <p>Only two per axis are independent; see {@link SpringLayout}'s note. The edges that were
     * not set are derived from those that were: the east is the west plus the width, the centre is
     * the west plus half the width, and so on. They are derived on being asked for and kept.
     */
    public static class Constraints {

        private Spring x;
        private Spring y;
        private Spring width;
        private Spring height;
        private Spring east;
        private Spring south;
        private Spring horizontalCenter;
        private Spring verticalCenter;
        private Spring baseline;

        private final List<String> horizontalHistory = new ArrayList<String>(2);
        private final List<String> verticalHistory = new ArrayList<String>(2);

        /** For the baseline, which depends on the component. */
        private Component c;

        /** With no constraint. */
        public Constraints() {
        }

        /** With the north-west corner set. */
        public Constraints(Spring x, Spring y) {
            setX(x);
            setY(y);
        }

        /** With the corner and the measurements. */
        public Constraints(Spring x, Spring y, Spring width, Spring height) {
            setX(x);
            setY(y);
            setWidth(width);
            setHeight(height);
        }

        /**
         * Taken from that component.
         *
         * <p>The position is left fixed -- the one the component has now -- and the measurements
         * are left tied to it, so if the component changes its preferred size the constraints
         * follow it.
         */
        public Constraints(Component c) {
            this.c = c;
            setX(Spring.constant(c.getX()));
            setY(Spring.constant(c.getY()));
            setWidth(Spring.width(c));
            setHeight(Spring.height(c));
        }

        private Spring sum(Spring s1, Spring s2) {
            return (s1 == null || s2 == null) ? null : Spring.sum(s1, s2);
        }

        private Spring difference(Spring s1, Spring s2) {
            return (s1 == null || s2 == null) ? null : Spring.difference(s1, s2);
        }

        private Spring scale(Spring s, float factor) {
            return (s == null) ? null : Spring.scale(s, factor);
        }

        /**
         * It notes that this constraint was set, and discards the oldest if there were already two.
         *
         * <p>On discarding, the derived ones that were left without support are erased: otherwise,
         * they would go on answering with an arithmetic done from something that no longer holds.
         */
        private void pushConstraint(String name, Spring value, boolean horizontal) {
            boolean valid = true;
            List<String> history = horizontal ? horizontalHistory : verticalHistory;
            if (history.contains(name)) {
                history.remove(name);
                valid = false;
            } else if (history.size() == 2 && value != null) {
                history.remove(0);
                valid = false;
            }
            if (value != null) {
                history.add(name);
            }
            if (!valid) {
                String[] all = horizontal ? ALL_HORIZONTAL : ALL_VERTICAL;
                for (int i = 0; i < all.length; i++) {
                    if (!history.contains(all[i])) {
                        setConstraint(all[i], null);
                    }
                }
            }
        }

        /** The west edge. */
        public void setX(Spring x) {
            this.x = x;
            pushConstraint(WEST, x, true);
        }

        /** The west edge, deriving it from the other two if it was not set; see the class note. */
        public Spring getX() {
            return deriveX();
        }

        /** The north edge. */
        public void setY(Spring y) {
            this.y = y;
            pushConstraint(NORTH, y, false);
        }

        /** The north edge, deriving it if needed. */
        public Spring getY() {
            return deriveY();
        }

        public void setWidth(Spring width) {
            this.width = width;
            pushConstraint(WIDTH, width, true);
        }

        /** The width, deriving it if needed. */
        public Spring getWidth() {
            return deriveWidth();
        }

        public void setHeight(Spring height) {
            this.height = height;
            pushConstraint(HEIGHT, height, false);
        }

        /** The height, deriving it if needed. */
        public Spring getHeight() {
            return deriveHeight();
        }

        private void setEast(Spring east) {
            this.east = east;
            pushConstraint(EAST, east, true);
        }

        private Spring getEast() {
            return deriveEast();
        }

        private void setSouth(Spring south) {
            this.south = south;
            pushConstraint(SOUTH, south, false);
        }

        private Spring getSouth() {
            return deriveSouth();
        }

        private void setHorizontalCenter(Spring horizontalCenter) {
            this.horizontalCenter = horizontalCenter;
            pushConstraint(HORIZONTAL_CENTER, horizontalCenter, true);
        }

        private Spring getHorizontalCenter() {
            return deriveCenterH();
        }

        private void setVerticalCenter(Spring verticalCenter) {
            this.verticalCenter = verticalCenter;
            pushConstraint(VERTICAL_CENTER, verticalCenter, false);
        }

        private Spring getVerticalCenter() {
            return deriveCenterV();
        }

        private void setBaseline(Spring baseline) {
            this.baseline = baseline;
            pushConstraint(BASELINE, baseline, false);
        }

        private Spring getBaseline() {
            return baseline;
        }

        /**
         * It sets that edge's constraint.
         *
         * <p>A name that is none of the nine is silently ignored, which is what the JDK does: the
         * list of names is of strings and not of enumeration constants, so there is no way of
         * telling a new name from a misspelt one.
         */
        public void setConstraint(String edgeName, Spring s) {
            if (WEST.equals(edgeName)) {
                setX(s);
            } else if (NORTH.equals(edgeName)) {
                setY(s);
            } else if (EAST.equals(edgeName)) {
                setEast(s);
            } else if (SOUTH.equals(edgeName)) {
                setSouth(s);
            } else if (HORIZONTAL_CENTER.equals(edgeName)) {
                setHorizontalCenter(s);
            } else if (WIDTH.equals(edgeName)) {
                setWidth(s);
            } else if (HEIGHT.equals(edgeName)) {
                setHeight(s);
            } else if (VERTICAL_CENTER.equals(edgeName)) {
                setVerticalCenter(s);
            } else if (BASELINE.equals(edgeName)) {
                setBaseline(s);
            }
        }

        /**
         * That edge's constraint, deriving it if needed.
         *
         * <p>Null if there is nothing to derive it from; see the class note.
         */
        public Spring getConstraint(String edgeName) {
            if (WEST.equals(edgeName)) {
                return getX();
            }
            if (NORTH.equals(edgeName)) {
                return getY();
            }
            if (EAST.equals(edgeName)) {
                return getEast();
            }
            if (SOUTH.equals(edgeName)) {
                return getSouth();
            }
            if (WIDTH.equals(edgeName)) {
                return getWidth();
            }
            if (HEIGHT.equals(edgeName)) {
                return getHeight();
            }
            if (HORIZONTAL_CENTER.equals(edgeName)) {
                return getHorizontalCenter();
            }
            if (VERTICAL_CENTER.equals(edgeName)) {
                return getVerticalCenter();
            }
            if (BASELINE.equals(edgeName)) {
                return getBaseline();
            }
            return null;
        }

        private Spring deriveX() {
            if (x == null) {
                if (east != null && width != null) {
                    x = difference(east, width);
                } else if (horizontalCenter != null && width != null) {
                    x = difference(horizontalCenter, scale(width, 0.5f));
                }
            }
            return x;
        }

        private Spring deriveWidth() {
            if (width == null) {
                if (east != null && x != null) {
                    width = difference(east, x);
                } else if (horizontalCenter != null && x != null) {
                    width = scale(difference(horizontalCenter, x), 2.0f);
                }
            }
            return width;
        }

        private Spring deriveEast() {
            if (east == null) {
                if (x != null && width != null) {
                    east = sum(x, width);
                } else if (horizontalCenter != null && x != null) {
                    east = difference(scale(horizontalCenter, 2.0f), x);
                }
            }
            return east;
        }

        private Spring deriveCenterH() {
            if (horizontalCenter == null) {
                if (x != null && width != null) {
                    horizontalCenter = sum(x, scale(width, 0.5f));
                } else if (east != null && x != null) {
                    horizontalCenter = scale(sum(x, east), 0.5f);
                }
            }
            return horizontalCenter;
        }

        private Spring deriveY() {
            if (y == null) {
                if (south != null && height != null) {
                    y = difference(south, height);
                } else if (verticalCenter != null && height != null) {
                    y = difference(verticalCenter, scale(height, 0.5f));
                }
            }
            return y;
        }

        private Spring deriveHeight() {
            if (height == null) {
                if (south != null && y != null) {
                    height = difference(south, y);
                } else if (verticalCenter != null && y != null) {
                    height = scale(difference(verticalCenter, y), 2.0f);
                }
            }
            return height;
        }

        private Spring deriveSouth() {
            if (south == null) {
                if (y != null && height != null) {
                    south = sum(y, height);
                } else if (verticalCenter != null && y != null) {
                    south = difference(scale(verticalCenter, 2.0f), y);
                }
            }
            return south;
        }

        private Spring deriveCenterV() {
            if (verticalCenter == null) {
                if (y != null && height != null) {
                    verticalCenter = sum(y, scale(height, 0.5f));
                } else if (south != null && y != null) {
                    verticalCenter = scale(sum(y, south), 0.5f);
                }
            }
            return verticalCenter;
        }

        /** It gives every spring back to "it is not known yet". */
        void reset() {
            Spring[] all = {x, y, width, height, east, south, horizontalCenter,
                verticalCenter, baseline};
            for (int i = 0; i < all.length; i++) {
                if (all[i] != null) {
                    all[i].setValue(Spring.UNSET);
                }
            }
        }
    }

    /**
     * Whether that spring finds itself.
     *
     * <p>It is marked while it is walked: if on asking its parts it is reached again, it is
     * cyclic. The results are kept in two sets, one of those that are and another of those that
     * are not, because the question is asked many times per layout.
     */
    boolean isCyclic(Spring s) {
        if (s == null) {
            return false;
        }
        if (cyclicSprings == null) {
            cyclicSprings = new HashSet<Spring>();
            acyclicSprings = new HashSet<Spring>();
        }
        if (cyclicSprings.contains(s)) {
            return true;
        }
        if (acyclicSprings.contains(s)) {
            return false;
        }
        cyclicSprings.add(s);
        boolean result = s.isCyclic(this);
        if (!result) {
            acyclicSprings.add(s);
            cyclicSprings.remove(s);
        }
        return result;
    }

    /** The spring that is returned in place of a cyclic one. */
    private Spring abandonCycles(Spring s) {
        return isCyclic(s) ? cyclicReference : s;
    }

    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
        componentConstraints.remove(c);
    }

    private static Dimension add(Dimension size, Insets insets) {
        return new Dimension(size.width + insets.left + insets.right,
                size.height + insets.top + insets.bottom);
    }

    /**
     * It measures the container by its width and its height, not by its east and south edges.
     *
     * <p>It looks like the same thing -- the east is the west plus the width, and the container's
     * west is zero -- and it is not: asking the east for a number forces a sum to be resolved, and
     * resolving a sum asks for its parts' preferred one, and the container's preferred one comes
     * back to this method. Asking the width directly cuts that loop off.
     */
    private Dimension measure(Container parent, int which) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        int w = value(abandonCycles(pc.getWidth()), which);
        int h = value(abandonCycles(pc.getHeight()), which);
        return add(new Dimension(w, h), parent.getInsets());
    }

    private static int value(Spring s, int which) {
        if (s == null) {
            return 0;
        }
        if (which == 0) {
            return s.getMinimumValue();
        }
        if (which == 1) {
            return s.getPreferredValue();
        }
        return s.getMaximumValue();
    }

    public Dimension minimumLayoutSize(Container parent) {
        return measure(parent, 0);
    }

    public Dimension preferredLayoutSize(Container parent) {
        return measure(parent, 1);
    }

    public Dimension maximumLayoutSize(Container parent) {
        return measure(parent, 2);
    }

    public void addLayoutComponent(Component component, Object constraints) {
        if (constraints instanceof Constraints) {
            putConstraints(component, (Constraints) constraints);
        }
    }

    public float getLayoutAlignmentX(Container p) {
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container p) {
        return 0.5f;
    }

    /**
     * It throws away what was known about the values; each spring's three numbers do not change.
     */
    public void invalidateLayout(Container p) {
        cyclicSprings = null;
        acyclicSprings = null;
    }

    /**
     * It gives the container its origin constraints.
     *
     * <p>The container's north-west is always zero: it is the origin of everything else. The width
     * and the height were already set by {@link #getConstraints}.
     */
    private void setParent(Container p) {
        Constraints pc = getConstraints(p);
        pc.setX(Spring.constant(0));
        pc.setY(Spring.constant(0));
    }

    private void putConstraints(Component component, Constraints constraints) {
        componentConstraints.put(component, constraints);
    }

    /**
     * It ties one component's edge to another's edge, at that fixed distance.
     *
     * @throws NullPointerException if some component is null
     */
    public void putConstraint(String e1, Component c1, int pad, String e2, Component c2) {
        putConstraint(e1, c1, Spring.constant(pad), e2, c2);
    }

    /**
     * The same with an elastic distance.
     *
     * <p><strong>The other component's edge is kept as a live reference</strong>, not as a
     * snapshot. It is what makes tying A to B and B to A a real cycle -- if the spring B had at
     * that moment were kept, the second tie would not close and the two positions would come out
     * of an arithmetic that looks reasonable and is not. See {@link EdgeReference}.
     *
     * @throws NullPointerException if some component is null
     */
    public void putConstraint(String e1, Component c1, Spring s, String e2, Component c2) {
        Constraints cs = getConstraints(c1);
        cs.setConstraint(e1, Spring.sum(s, new EdgeReference(e2, c2, this)));
    }

    /**
     * A spring that is "such-and-such an edge of that component", resolved each time it is
     * consulted.
     *
     * <p>Without this, tying a component to another would freeze the other's state at that
     * instant; with this, moving the other moves this one, which is what one expects of a layout
     * by constraints. And it is also what makes a cycle show: the loop really closes.
     */
    private static class EdgeReference extends Spring {

        private final String edgeName;
        private final Component c;
        private final SpringLayout l;

        EdgeReference(String edgeName, Component c, SpringLayout l) {
            this.edgeName = edgeName;
            this.c = c;
            this.l = l;
        }

        private Spring pointed() {
            return l.getConstraints(c).getConstraint(edgeName);
        }

        public int getMinimumValue() {
            return pointed().getMinimumValue();
        }

        public int getPreferredValue() {
            return pointed().getPreferredValue();
        }

        public int getMaximumValue() {
            return pointed().getMaximumValue();
        }

        public int getValue() {
            return pointed().getValue();
        }

        public void setValue(int size) {
            pointed().setValue(size);
        }

        boolean isCyclic(SpringLayout l) {
            return l.isCyclic(pointed());
        }

        public String toString() {
            return "SpringProxy for " + edgeName + " edge of " + c.getName();
        }
    }

    /**
     * That component's constraints, creating them if it had none.
     *
     * <p>It never returns null: a component with no constraints set has the ones that come out of
     * its size all the same.
     */
    public Constraints getConstraints(Component c) {
        Constraints result = componentConstraints.get(c);
        if (result == null) {
            result = new Constraints();
            componentConstraints.put(c, result);
        }
        return applyDefaults(c, result);
    }

    /**
     * It fills in whatever is missing with the usual: stuck to the origin and of the component's
     * size.
     *
     * <p>Only while fewer than two constraints are left on the axis. With two it is already
     * determined and adding a third would discard one of those the caller set.
     */
    private Constraints applyDefaults(Component c, Constraints cc) {
        if (cc.c == null) {
            cc.c = c;
        }
        if (cc.horizontalHistory.size() < 2) {
            byDefault(cc, WEST, Spring.constant(0), WIDTH, Spring.width(c),
                    cc.horizontalHistory);
        }
        if (cc.verticalHistory.size() < 2) {
            byDefault(cc, NORTH, Spring.constant(0), HEIGHT, Spring.height(c),
                    cc.verticalHistory);
        }
        return cc;
    }

    private static void byDefault(Constraints cc, String n1, Spring s1, String n2, Spring s2,
            List<String> history) {
        if (history.size() < 2 && !history.contains(n1)) {
            cc.setConstraint(n1, s1);
        }
        if (history.size() < 2 && !history.contains(n2)) {
            cc.setConstraint(n2, s2);
        }
    }

    /** That edge's spring of that component, or null if it cannot be derived. */
    public Spring getConstraint(String edgeName, Component c) {
        return abandonCycles(getConstraints(c).getConstraint(edgeName));
    }

    /** It resolves every spring and places each child. */
    public void layoutContainer(Container parent) {
        setParent(parent);
        int n = parent.getComponentCount();
        getConstraints(parent).reset();
        for (int i = 0; i < n; i++) {
            getConstraints(parent.getComponent(i)).reset();
        }
        Insets insets = parent.getInsets();
        Constraints pc = getConstraints(parent);
        // The width and the height are given a value, not the east and the south: see `measure`'s
        // note.
        setValue(abandonCycles(pc.getX()), 0);
        setValue(abandonCycles(pc.getY()), 0);
        setValue(abandonCycles(pc.getWidth()),
                parent.getWidth() - insets.left - insets.right);
        setValue(abandonCycles(pc.getHeight()),
                parent.getHeight() - insets.top - insets.bottom);
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            Constraints cc = getConstraints(c);
            int x = valueOf(abandonCycles(cc.getX()));
            int y = valueOf(abandonCycles(cc.getY()));
            int width = valueOf(abandonCycles(cc.getWidth()));
            int height = valueOf(abandonCycles(cc.getHeight()));
            c.setBounds(insets.left + x, insets.top + y, width, height);
        }
    }

    private static void setValue(Spring s, int value) {
        if (s != null) {
            s.setValue(value);
        }
    }

    /**
     * A spring's value, as it is.
     *
     * <p>{@link Spring#UNSET} is not translated into zero: a component caught in a cycle is left
     * with an absurd position, and that is visible and gets investigated. A zero is mistaken for
     * "it is at the top left" and never gets investigated.
     */
    private static int valueOf(Spring s) {
        if (s == null) {
            return 0;
        }
        return s.getValue();
    }
}
