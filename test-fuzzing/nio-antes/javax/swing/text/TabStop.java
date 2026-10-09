package javax.swing.text;

import java.io.Serializable;

/**
 * A tab stop: where the text stops and how it is arranged against that point.
 *
 * <p>Immutable, and that is why it can be shared between paragraphs. The <em>alignment</em> says
 * which part of the text ends up at the position: to the left is the common one, and the decimal
 * one is what aligns a column of numbers by their point. The <em>leader</em> is what is drawn in
 * the gap left before the stop, those little dots of an index.
 */
public class TabStop implements Serializable {

    /** The text starts at the stop. */
    public static final int ALIGN_LEFT = 0;

    /** The text ends at the stop. */
    public static final int ALIGN_RIGHT = 1;

    /** The text is centred on the stop. */
    public static final int ALIGN_CENTER = 2;

    /** The decimal point ends up at the stop. */
    public static final int ALIGN_DECIMAL = 4;

    /** A bar is drawn at the stop; the text carries on. */
    public static final int ALIGN_BAR = 5;

    /** With no leader. */
    public static final int LEAD_NONE = 0;

    public static final int LEAD_DOTS = 1;

    public static final int LEAD_HYPHENS = 2;

    public static final int LEAD_UNDERLINE = 3;

    public static final int LEAD_THICKLINE = 4;

    public static final int LEAD_EQUALS = 5;

    private int alignment;
    private float position;
    private int leader;

    /** A left stop with no leader at that position. */
    public TabStop(float pos) {
        this(pos, ALIGN_LEFT, LEAD_NONE);
    }

    public TabStop(float pos, int align, int leader) {
        alignment = align;
        this.leader = leader;
        position = pos;
    }

    public float getPosition() {
        return position;
    }

    public int getAlignment() {
        return alignment;
    }

    public int getLeader() {
        return leader;
    }

    /** Equal if they match in all three things; it is used when comparing sets of stops. */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof TabStop) {
            TabStop o = (TabStop) other;
            return ((alignment == o.alignment) && (leader == o.leader)
                    && (position == o.position));
        }
        return false;
    }

    public int hashCode() {
        return alignment ^ leader ^ Math.round(position);
    }

    public String toString() {
        String buf = "";
        if (alignment == ALIGN_RIGHT) {
            buf = "right ";
        } else if (alignment == ALIGN_CENTER) {
            buf = "center ";
        } else if (alignment == ALIGN_DECIMAL) {
            buf = "decimal ";
        } else if (alignment == ALIGN_BAR) {
            buf = "bar ";
        }
        buf = buf + "tab @" + String.valueOf(position);
        if (leader != LEAD_NONE) {
            buf = buf + " (w/leaders)";
        }
        return buf;
    }
}
