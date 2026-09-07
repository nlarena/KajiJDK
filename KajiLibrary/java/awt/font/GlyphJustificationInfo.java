package java.awt.font;

/**
 * How much and with what priority a glyph may stretch or shrink when a line is justified.
 *
 * <p>Justifying is not sharing out the slack in equal parts. There are places where text stretches
 * well --the spaces between words-- and places where stretching looks bad --between two letters of
 * the same word. Each glyph declares here which group it is in and how much it tolerates.
 *
 * <p>The algorithm works by **priorities**: first it shares out everything it can in the highest
 * priority group, and only if that is not enough does it move on to the next. An `absorb` glyph eats
 * all the slack left of its priority instead of sharing it out, which is how a kashida stroke is
 * stretched in Arabic writing.
 */
public final class GlyphJustificationInfo {

    /** The highest priority: Arabic writing's stroke lengthening. */
    public static final int PRIORITY_KASHIDA = 0;

    /** The spaces between words. */
    public static final int PRIORITY_WHITESPACE = 1;

    /** The space between letters of one and the same word. */
    public static final int PRIORITY_INTERCHAR = 2;

    /** It is not justified. */
    public static final int PRIORITY_NONE = 3;

    /** How much this glyph weighs when sharing out. */
    public final float weight;

    /** Which group it goes into when stretching. */
    public final int growPriority;

    /** Whether on stretching it eats all the slack of its priority. */
    public final boolean growAbsorb;

    /** How much it may grow on the left side. */
    public final float growLeftLimit;

    /** How much it may grow on the right side. */
    public final float growRightLimit;

    /** Which group it goes into when shrinking. */
    public final int shrinkPriority;

    /** Whether on shrinking it absorbs the whole shortfall of its priority. */
    public final boolean shrinkAbsorb;

    /** How much it may shrink on the left side. */
    public final float shrinkLeftLimit;

    /** How much it may shrink on the right side. */
    public final float shrinkRightLimit;

    /**
     * With everything given.
     *
     * @throws IllegalArgumentException if the weight or some limit is negative, or if some priority
     *     is none of the four
     */
    public GlyphJustificationInfo(float weight, boolean growAbsorb, int growPriority,
            float growLeftLimit, float growRightLimit, boolean shrinkAbsorb, int shrinkPriority,
            float shrinkLeftLimit, float shrinkRightLimit) {
        if (weight < 0) {
            throw new IllegalArgumentException("weight is negative");
        }
        if (!priorityIsValid(growPriority)) {
            throw new IllegalArgumentException("Invalid grow priority");
        }
        if (growLeftLimit < 0) {
            throw new IllegalArgumentException("growLeftLimit is negative");
        }
        if (growRightLimit < 0) {
            throw new IllegalArgumentException("growRightLimit is negative");
        }
        if (!priorityIsValid(shrinkPriority)) {
            throw new IllegalArgumentException("Invalid shrink priority");
        }
        if (shrinkLeftLimit < 0) {
            throw new IllegalArgumentException("shrinkLeftLimit is negative");
        }
        if (shrinkRightLimit < 0) {
            throw new IllegalArgumentException("shrinkRightLimit is negative");
        }
        this.weight = weight;
        this.growAbsorb = growAbsorb;
        this.growPriority = growPriority;
        this.growLeftLimit = growLeftLimit;
        this.growRightLimit = growRightLimit;
        this.shrinkAbsorb = shrinkAbsorb;
        this.shrinkPriority = shrinkPriority;
        this.shrinkLeftLimit = shrinkLeftLimit;
        this.shrinkRightLimit = shrinkRightLimit;
    }

    /** Whether the number is one of the four priorities. */
    private static boolean priorityIsValid(int priority) {
        return priority >= PRIORITY_KASHIDA && priority <= PRIORITY_NONE;
    }

    public String toString() {
        return "GlyphJustificationInfo: weight=" + this.weight + ", growAbsorb="
                + this.growAbsorb + ", growPriority=" + this.growPriority + ", shrinkAbsorb="
                + this.shrinkAbsorb + ", shrinkPriority=" + this.shrinkPriority;
    }
}
