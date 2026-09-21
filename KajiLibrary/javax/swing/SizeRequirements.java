package javax.swing;

import java.io.Serializable;

/**
 * How much something wants to measure: minimum, preferred, maximum and where its alignment
 * line is.
 *
 * <h2>One dimension at a time</h2>
 *
 * <p>This class does not talk about widths or heights: it talks about <em>one</em> dimension. A
 * component in a horizontal box contributes two of these objects, one per axis, and each axis is
 * resolved separately. It is what allows the same pair of routines to serve for laying out in a
 * row or in a column: what changes is which of the two axes is shared out and which is aligned.
 *
 * <h2>Sharing out and aligning</h2>
 *
 * <p>They are the two operations, and they are different:
 *
 * <ul>
 * <li><strong>Sharing out</strong> ({@link #calculateTiledPositions}) puts the children one
 * after another and gives each one a part of the space. If there is space left over, each one
 * grows in proportion to how much it can grow; if there is not enough, each one shrinks in
 * proportion to how much it can shrink. They never overlap.
 * <li><strong>Aligning</strong> ({@link #calculateAlignedPositions}) puts them all in the same
 * place, each one hanging from its alignment line. It is what makes a row of buttons end up
 * centred, or their baselines agree.
 * </ul>
 *
 * <p>The alignment goes from 0 to 1: 0 is "my hook point is at my top (or left) edge", 1 at the
 * bottom (or right) one, and 0.5 in the middle. A component that wants to align by its baseline
 * says what fraction of its height is above it.
 *
 * <p>The sums are done in {@code long} and clipped to the maximum integer: adding three children
 * that say "no cap" must not give a negative number, which is what would happen with a silent
 * overflow.
 */
public class SizeRequirements implements Serializable {

    /** The least it may measure. */
    public int minimum;

    /** What it wants to measure. */
    public int preferred;

    /** The most it may measure. */
    public int maximum;

    /** Where its hook line is, from 0 to 1; see the class note. */
    public float alignment;

    /** A size request of zero, aligned to the centre. */
    public SizeRequirements() {
        minimum = 0;
        preferred = 0;
        maximum = 0;
        alignment = 0.5f;
    }

    public SizeRequirements(int min, int pref, int max, float a) {
        minimum = min;
        preferred = pref;
        maximum = max;
        alignment = a > 1.0f ? 1.0f : a < 0.0f ? 0.0f : a;
    }

    public String toString() {
        return "[" + minimum + "," + preferred + "," + maximum + "]@" + alignment;
    }

    /**
     * What they ask for between them all if they go one after another: the sum of each thing.
     *
     * <p>The total's alignment is 0.5 and not anybody's in particular: a row does not inherit its
     * children's hook, it is decided by whoever places it.
     */
    public static SizeRequirements getTiledSizeRequirements(SizeRequirements[] children) {
        SizeRequirements total = new SizeRequirements();
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            total.minimum = (int) Math.min((long) total.minimum + (long) req.minimum,
                    Integer.MAX_VALUE);
            total.preferred = (int) Math.min((long) total.preferred + (long) req.preferred,
                    Integer.MAX_VALUE);
            total.maximum = (int) Math.min((long) total.maximum + (long) req.maximum,
                    Integer.MAX_VALUE);
        }
        return total;
    }

    /**
     * What they ask for between them all if they go overlapped and aligned.
     *
     * <p>What sticks out on each side of the hook line is measured separately and the worst of each
     * side is taken: the set needs what the one that goes up most needs plus what the one that goes
     * down most needs. The resulting alignment is the one that leaves that line in its place,
     * computed over the preferred sizes.
     */
    public static SizeRequirements getAlignedSizeRequirements(SizeRequirements[] children) {
        SizeRequirements totalAscent = new SizeRequirements();
        SizeRequirements totalDescent = new SizeRequirements();
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];

            int ascent = (int) (req.alignment * req.minimum);
            int descent = req.minimum - ascent;
            totalAscent.minimum = Math.max(ascent, totalAscent.minimum);
            totalDescent.minimum = Math.max(descent, totalDescent.minimum);

            ascent = (int) (req.alignment * req.preferred);
            descent = req.preferred - ascent;
            totalAscent.preferred = Math.max(ascent, totalAscent.preferred);
            totalDescent.preferred = Math.max(descent, totalDescent.preferred);

            ascent = (int) (req.alignment * req.maximum);
            descent = req.maximum - ascent;
            totalAscent.maximum = Math.max(ascent, totalAscent.maximum);
            totalDescent.maximum = Math.max(descent, totalDescent.maximum);
        }
        int min = (int) Math.min((long) totalAscent.minimum + (long) totalDescent.minimum,
                Integer.MAX_VALUE);
        int pref = (int) Math.min((long) totalAscent.preferred + (long) totalDescent.preferred,
                Integer.MAX_VALUE);
        int max = (int) Math.min((long) totalAscent.maximum + (long) totalDescent.maximum,
                Integer.MAX_VALUE);
        // The alignment comes from the minimum and not from the preferred one: it is the only one
                // that always holds, because the set never measures less than its minimum. With the
                // preferred one, a tight box would hook by a line it does not have.
        float alignment = 0.0f;
        if (min > 0) {
            alignment = (float) totalAscent.minimum / min;
            alignment = alignment > 1.0f ? 1.0f : alignment < 0.0f ? 0.0f : alignment;
        }
        return new SizeRequirements(min, pref, max, alignment);
    }

    /** It shares out front to back; see {@link #calculateTiledPositions(int,
         * SizeRequirements, SizeRequirements[], int[], int[], boolean)}. */
    public static void calculateTiledPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans) {
        calculateTiledPositions(allocated, total, children, offsets, spans, true);
    }

    /**
     * It shares {@code allocated} out among the children, one after another.
     *
     * <p>If the space is enough for the preferred one, each one grows; if not, each one shrinks.
     * In both cases the sharing out is proportional to each one's room for manoeuvre, not to its
     * size: a child that cannot shrink does not shrink even though it is the biggest.
     *
     * <p>{@code forward} at {@code false} fills from the end, which is how a row is laid out in a
     * language that is read right to left.
     */
    public static void calculateTiledPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans, boolean forward) {
        // The sums go in long: the maximums are usually enormous and the sum would overflow.
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < children.length; i++) {
            min += children[i].minimum;
            pref += children[i].preferred;
            max += children[i].maximum;
        }
        if (allocated >= pref) {
            expandedTile(allocated, min, pref, max, children, offsets, spans, forward);
        } else {
            compressedTile(allocated, min, pref, max, children, offsets, spans, forward);
        }
    }

    /** There is not enough space: each one gives up a fraction of what it can give up. */
    private static void compressedTile(int allocated, long min, long pref, long max,
            SizeRequirements[] request, int[] offsets, int[] spans, boolean forward) {
        float totalPlay = Math.min(pref - allocated, pref - min);
        float useableSpace = pref - min;
        float factor = (useableSpace == 0.0f) ? 0.0f : totalPlay / useableSpace;

        int totalOffset;
        if (forward) {
            totalOffset = 0;
            for (int i = 0; i < spans.length; i++) {
                offsets[i] = totalOffset;
                SizeRequirements req = request[i];
                float play = factor * (req.preferred - req.minimum);
                spans[i] = (int) (req.preferred - play);
                totalOffset = (int) Math.min((long) totalOffset + (long) spans[i],
                        Integer.MAX_VALUE);
            }
        } else {
            totalOffset = allocated;
            for (int i = 0; i < spans.length; i++) {
                SizeRequirements req = request[i];
                float play = factor * (req.preferred - req.minimum);
                spans[i] = (int) (req.preferred - play);
                offsets[i] = totalOffset - spans[i];
                totalOffset = (int) Math.max((long) totalOffset - (long) spans[i], 0);
            }
        }
    }

    /** There is space left over: each one takes a fraction of what it can grow. */
    private static void expandedTile(int allocated, long min, long pref, long max,
            SizeRequirements[] request, int[] offsets, int[] spans, boolean forward) {
        float totalPlay = Math.min(allocated - pref, max - pref);
        float useableSpace = max - pref;
        float factor = (useableSpace == 0.0f) ? 0.0f : totalPlay / useableSpace;

        int totalOffset;
        if (forward) {
            totalOffset = 0;
            for (int i = 0; i < spans.length; i++) {
                offsets[i] = totalOffset;
                SizeRequirements req = request[i];
                int play = (int) (factor * (req.maximum - req.preferred));
                spans[i] = (int) Math.min((long) req.preferred + (long) play, Integer.MAX_VALUE);
                totalOffset = (int) Math.min((long) totalOffset + (long) spans[i],
                        Integer.MAX_VALUE);
            }
        } else {
            totalOffset = allocated;
            for (int i = 0; i < spans.length; i++) {
                SizeRequirements req = request[i];
                int play = (int) (factor * (req.maximum - req.preferred));
                spans[i] = (int) Math.min((long) req.preferred + (long) play, Integer.MAX_VALUE);
                offsets[i] = totalOffset - spans[i];
                totalOffset = (int) Math.max((long) totalOffset - (long) spans[i], 0);
            }
        }
    }

    /** It aligns them all in the same place, each one by its hook line. */
    public static void calculateAlignedPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans) {
        calculateAlignedPositions(allocated, total, children, offsets, spans, true);
    }

    /**
     * It aligns them all in the same place; {@code normal} at {@code false} turns the axis
     * around.
     *
     * <p>The space is split in two by the total's line, and each child takes from each half what
     * its maximum allows it. A child with an enormous maximum fills; one with a small maximum
     * stays its size, hanging from the line.
     */
    public static void calculateAlignedPositions(int allocated, SizeRequirements total,
            SizeRequirements[] children, int[] offsets, int[] spans, boolean normal) {
        float totalAlignment = normal ? total.alignment : 1.0f - total.alignment;
        int totalAscent = (int) (allocated * totalAlignment);
        int totalDescent = allocated - totalAscent;
        for (int i = 0; i < children.length; i++) {
            SizeRequirements req = children[i];
            float alignment = normal ? req.alignment : 1.0f - req.alignment;
            int maxAscent = (int) (req.maximum * alignment);
            int maxDescent = req.maximum - maxAscent;
            int ascent = Math.min(totalAscent, maxAscent);
            int descent = Math.min(totalDescent, maxDescent);

            offsets[i] = totalAscent - ascent;
            spans[i] = (int) Math.min((long) ascent + (long) descent, Integer.MAX_VALUE);
        }
    }

    /**
     * It shares a change of size out among several requests.
     *
     * <p>It returns an empty array: in the JDK this is an auxiliary routine that was left
     * unimplemented -- it always returned {@code new int[0]} -- and copying its behaviour is the
     * only honest thing. Who really shares out is {@link #calculateTiledPositions}.
     */
    public static int[] adjustSizes(int delta, SizeRequirements[] children) {
        return new int[0];
    }
}
