package javax.swing;

import java.awt.Component;

/**
 * An elastic distance: a minimum, a preferred, a maximum, and the current value.
 *
 * <h2>Three numbers and one</h2>
 *
 * <p>The first three are what the spring <em>may</em> measure; the fourth is what it measures at
 * this moment. A layout shares the available space out by adjusting the values, and each spring
 * gives up or stretches within its three numbers.
 *
 * <h2>The tension, which is what makes the sharing out fair</h2>
 *
 * <p>A spring's <em>tension</em> is how far it moved from its preferred one, measured in
 * fractions of what it has left in order to reach its cap. It is zero at the preferred one, one
 * at the maximum and minus one at the minimum. Sharing the space out is not giving each one the
 * same: it is putting them all at the same tension, and that is why the one with the most room
 * takes the most.
 *
 * <p>A spring that cannot move -- minimum, preferred and maximum equal -- has a range of zero,
 * and its tension is a division by zero. It comes out infinite or NaN, and it is right that it
 * should: the question "how far did you stretch, in fractions of what you could" has no answer
 * when it could not at all.
 *
 * <h2>They combine, and the result is another spring</h2>
 *
 * <p>{@link #sum}, {@link #max}, {@link #minus} and {@link #scale} return springs that
 * <em>look at</em> those they were given. They do not copy their numbers: if the inner one
 * changes, the outer one changes. That is why {@link #width} and {@link #height} are useful
 * -- they are a spring that always says what the component measures now --, and that is why
 * setting a value on a sum shares it out between its two parts.
 *
 * <p>That sharing out uses the tension: the sum gives the first part its own tension and the
 * second what is left over. It is what makes stretching a row of different springs come out
 * even.
 */
public abstract class Spring {

    /**
     * "It is not known yet".
     *
     * <p>It is {@link Integer#MIN_VALUE} and not zero nor minus one because a spring may
     * legitimately measure either of the two -- and also negative, which is what {@link #minus}
     * returns.
     */
    public static final int UNSET = Integer.MIN_VALUE;

    /** For the subclasses. */
    protected Spring() {
    }

    public abstract int getMinimumValue();

    public abstract int getPreferredValue();

    public abstract int getMaximumValue();

    /** What it measures now. */
    public abstract int getValue();

    /** It gives it that value; {@link #UNSET} gives it back to "it is not known yet". */
    public abstract void setValue(int value);

    /** How much room it has left on this side of the preferred one. */
    private double range(boolean contract) {
        return contract ? getPreferredValue() - getMinimumValue()
                : getMaximumValue() - getPreferredValue();
    }

    /** See the class note. */
    double getStrain() {
        double delta = (getValue() - getPreferredValue());
        return delta / range(getValue() < getPreferredValue());
    }

    /** It puts it at the value that corresponds to that tension. */
    void setStrain(double strain) {
        setValue(getPreferredValue() + (int) (strain * range(strain < 0)));
    }

    /** Whether it depends on itself through that layout. */
    boolean isCyclic(SpringLayout l) {
        return false;
    }

    /** The part common to the springs that keep their value. */
    abstract static class AbstractSpring extends Spring {

        protected int size = UNSET;

        public int getValue() {
            return size != UNSET ? size : getPreferredValue();
        }

        public final void setValue(int size) {
            if (this.size == size) {
                return;
            }
            if (size == UNSET) {
                clear();
            } else {
                setNonClearValue(size);
            }
        }

        protected void clear() {
            size = UNSET;
        }

        protected void setNonClearValue(int size) {
            this.size = size;
        }
    }

        /** Three fixed numbers. */
    private static class StaticSpring extends AbstractSpring {

        protected int min;
        protected int pref;
        protected int max;

        StaticSpring(int pref) {
            this(pref, pref, pref);
        }

        StaticSpring(int min, int pref, int max) {
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        public String toString() {
            return "StaticSpring [" + min + ", " + pref + ", " + max + "]";
        }

        public int getMinimumValue() {
            return min;
        }

        public int getPreferredValue() {
            return pref;
        }

        public int getMaximumValue() {
            return max;
        }
    }

    /**
     * The same spring the other way round.
     *
     * <p>The minimum becomes minus the maximum, not minus the minimum: on turning the sign round,
     * the one that measured most becomes the one that measures least.
     */
    private static class NegativeSpring extends Spring {

        private final Spring s;

        NegativeSpring(Spring s) {
            this.s = s;
        }

        public int getMinimumValue() {
            return -s.getMaximumValue();
        }

        public int getPreferredValue() {
            return -s.getPreferredValue();
        }

        public int getMaximumValue() {
            return -s.getMinimumValue();
        }

        public int getValue() {
            return -s.getValue();
        }

        public void setValue(int size) {
            if (size == UNSET) {
                s.setValue(UNSET);
            } else {
                s.setValue(-size);
            }
        }

        boolean isCyclic(SpringLayout l) {
            return s.isCyclic(l);
        }
    }

    /**
     * The same spring multiplied.
     *
     * <p>With a negative factor the minimum and the maximum cross, for the same reason as in
     * {@link NegativeSpring}.
     */
    private static class ScaleSpring extends Spring {

        private final float factor;
        private final Spring s;

        ScaleSpring(Spring s, float factor) {
            this.s = s;
            this.factor = factor;
        }

        public int getMinimumValue() {
            return Math.round((factor < 0 ? s.getMaximumValue() : s.getMinimumValue()) * factor);
        }

        public int getPreferredValue() {
            return Math.round(s.getPreferredValue() * factor);
        }

        public int getMaximumValue() {
            return Math.round((factor < 0 ? s.getMinimumValue() : s.getMaximumValue()) * factor);
        }

        public int getValue() {
            return Math.round(s.getValue() * factor);
        }

        public void setValue(int value) {
            if (value == UNSET) {
                s.setValue(UNSET);
            } else {
                s.setValue(Math.round(value / factor));
            }
        }

        boolean isCyclic(SpringLayout l) {
            return s.isCyclic(l);
        }
    }

    /**
     * A component's width, always up to date.
     *
     * <p>The maximum is clipped to {@link Short#MAX_VALUE}. A component with no cap returns
     * {@link Integer#MAX_VALUE}, and no arithmetic can be done with that -- a sum of two overflows
     * and gives a negative --. Clipping is what the JDK does and it is what keeps the arithmetic
     * sane.
     */
    private static class WidthSpring extends AbstractSpring {

        private final Component c;

        WidthSpring(Component c) {
            this.c = c;
        }

        public int getMinimumValue() {
            return c.getMinimumSize().width;
        }

        public int getPreferredValue() {
            return c.getPreferredSize().width;
        }

        public int getMaximumValue() {
            return Math.min(Short.MAX_VALUE, c.getMaximumSize().width);
        }
    }

    /** A component's height; see {@link WidthSpring}. */
    private static class HeightSpring extends AbstractSpring {

        private final Component c;

        HeightSpring(Component c) {
            this.c = c;
        }

        public int getMinimumValue() {
            return c.getMinimumSize().height;
        }

        public int getPreferredValue() {
            return c.getPreferredSize().height;
        }

        public int getMaximumValue() {
            return Math.min(Short.MAX_VALUE, c.getMaximumSize().height);
        }
    }

    /**
     * The part common to those that combine two.
     *
     * <p>It inherits from {@link StaticSpring} in order to reuse its three fields as a
     * <em>cache</em>: combining is expensive when the inner ones are themselves combinations, and
     * the result does not change until somebody clears it. Hence {@link #clear} has to clear the
     * two inner ones too.
     */
    abstract static class CompoundSpring extends StaticSpring {

        protected Spring s1;
        protected Spring s2;

        /**
         * It does not call {@link #clear}: {@code super(UNSET)} already leaves the three fields at
         * UNSET, and clearing would dereference the two springs -- which is precisely what makes
         * the JDK accept a null in {@link Spring#sum} without complaining until it is used.
         */
        CompoundSpring(Spring s1, Spring s2) {
            super(UNSET);
            this.s1 = s1;
            this.s2 = s2;
        }

        public String toString() {
            return "CompoundSpring of " + s1 + " and " + s2;
        }

        protected void clear() {
            super.clear();
            min = UNSET;
            pref = UNSET;
            max = UNSET;
            s1.setValue(UNSET);
            s2.setValue(UNSET);
        }

        protected abstract int op(int x, int y);

        public int getMinimumValue() {
            if (min == UNSET) {
                min = op(s1.getMinimumValue(), s2.getMinimumValue());
            }
            return min;
        }

        public int getPreferredValue() {
            if (pref == UNSET) {
                pref = op(s1.getPreferredValue(), s2.getPreferredValue());
            }
            return pref;
        }

        public int getMaximumValue() {
            if (max == UNSET) {
                max = op(s1.getMaximumValue(), s2.getMaximumValue());
            }
            return max;
        }

        public int getValue() {
            if (size == UNSET) {
                size = op(s1.getValue(), s2.getValue());
            }
            return size;
        }

        boolean isCyclic(SpringLayout l) {
            return l.isCyclic(s1) || l.isCyclic(s2);
        }
    }

    /** One after the other. */
    private static class SumSpring extends CompoundSpring {

        SumSpring(Spring s1, Spring s2) {
            super(s1, s2);
        }

        /**
         * Plain and simple addition.
         *
         * <p>It does not respect the {@link Spring#UNSET} sentinel: adding five to "it is not
         * known" gives "it is not known plus five". It is measured against the JDK, which does the
         * same. Treating it separately would be neater and would give another number.
         */
        protected int op(int x, int y) {
            return x + y;
        }

        /**
         * It shares the value out between the two at the same tension; see the class note.
         *
         * <p>The second is given what is left over and not its tension: that way the sum of the two
         * is exactly what was asked for, with no accumulated rounding errors.
         */
        protected void setNonClearValue(int size) {
            super.setNonClearValue(size);
            s1.setStrain(this.getStrain());
            s2.setValue(size - s1.getValue());
        }
    }

    /** The greater of the two, in the three numbers. */
    private static class MaxSpring extends CompoundSpring {

        MaxSpring(Spring s1, Spring s2) {
            super(s1, s2);
        }

        protected int op(int x, int y) {
            return Math.max(x, y);
        }

        /** The same value to both: both have to reach that far. */
        protected void setNonClearValue(int size) {
            super.setNonClearValue(size);
            s1.setValue(size);
            s2.setValue(size);
        }
    }

    /** A spring that does not move. */
    public static Spring constant(int pref) {
        return new StaticSpring(pref);
    }

    /** A spring with those three numbers. */
    public static Spring constant(int min, int pref, int max) {
        return new StaticSpring(min, pref, max);
    }

    /**
     * The same one, the other way round.
     *
     * <p><strong>It does not check for null</strong>, and this is measured: the JDK accepts it and
     * builds the spring all the same. What happens afterwards is that it blows up on being used.
     * Rejecting it here would bring the error forward, which sounds better and is not the same.
     */
    public static Spring minus(Spring s) {
        return new NegativeSpring(s);
    }

    /** One after the other; it does not check for nulls either. See {@link #minus}. */
    public static Spring sum(Spring s1, Spring s2) {
        return new SumSpring(s1, s2);
    }

    /** The greater of the two; it does not check for nulls either. See {@link #minus}. */
    public static Spring max(Spring s1, Spring s2) {
        return new MaxSpring(s1, s2);
    }

    /** The distance from the second to the first; it is the sum with the second turned round. */
    static Spring difference(Spring s1, Spring s2) {
        return sum(s1, minus(s2));
    }

    /**
     * The same one, multiplied.
     *
     * @throws NullPointerException if it is null
     */
    public static Spring scale(Spring s, float factor) {
        checkArg(s);
        return new ScaleSpring(s, factor);
    }

    /**
     * That component's width, always up to date; see the class note.
     *
     * @throws NullPointerException if it is null
     */
    public static Spring width(Component c) {
        checkArg(c);
        return new WidthSpring(c);
    }

    /**
     * That component's height.
     *
     * @throws NullPointerException if it is null
     */
    public static Spring height(Component c) {
        checkArg(c);
        return new HeightSpring(c);
    }

    private static void checkArg(Object arg) {
        if (arg == null) {
            throw new NullPointerException("Argument must not be null");
        }
    }
}
