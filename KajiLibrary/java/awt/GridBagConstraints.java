package java.awt;

/**
 * How a component is placed inside a {@code GridBagLayout}: cell, expansion, fill, anchor and
 * insets.
 *
 * <p>It is a bag of public fields with no behaviour: the class touches no component, it only
 * describes an intention. (This note added that for that reason it could be written whole even
 * though the {@code GridBagLayout} that consumes it did not exist; it exists now.)
 *
 * <p>The anchor values are not consecutive by chance. The absolute ones --CENTER, NORTH...-- go
 * from 10 to 18; the ones relative to reading direction --PAGE_START, LINE_END...-- from 19 to 26;
 * and the ones relative to the baseline jump to multiples of 256. The jump is not aesthetic: it
 * lets a layout tell the three families apart with a range comparison instead of a thirty-case
 * switch, and leaves room to add values without renumbering anything.
 */
public class GridBagConstraints implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -1000070633030801713L;

    /** Put this one after the previous one, without saying which row or column it falls in. */
    public static final int RELATIVE = -1;

    /** This component is the last in its row or column. */
    public static final int REMAINDER = 0;

    /** Do not enlarge the component even if there is room to spare in the cell. */
    public static final int NONE = 0;

    public static final int BOTH = 1;

    public static final int HORIZONTAL = 2;

    public static final int VERTICAL = 3;

    // --- absolute anchors: the cell as the points of the compass ---

    public static final int CENTER = 10;

    public static final int NORTH = 11;

    public static final int NORTHEAST = 12;

    public static final int EAST = 13;

    public static final int SOUTHEAST = 14;

    public static final int SOUTH = 15;

    public static final int SOUTHWEST = 16;

    public static final int WEST = 17;

    public static final int NORTHWEST = 18;

    // --- anchors relative to reading direction: in Arabic "LINE_START" is the right ---

    public static final int PAGE_START = 19;

    public static final int PAGE_END = 20;

    public static final int LINE_START = 21;

    public static final int LINE_END = 22;

    public static final int FIRST_LINE_START = 23;

    public static final int FIRST_LINE_END = 24;

    public static final int LAST_LINE_START = 25;

    public static final int LAST_LINE_END = 26;

    // --- anchors relative to the text's baseline ---

    public static final int BASELINE = 0x100;

    public static final int BASELINE_LEADING = 0x200;

    public static final int BASELINE_TRAILING = 0x300;

    public static final int ABOVE_BASELINE = 0x400;

    public static final int ABOVE_BASELINE_LEADING = 0x500;

    public static final int ABOVE_BASELINE_TRAILING = 0x600;

    public static final int BELOW_BASELINE = 0x700;

    public static final int BELOW_BASELINE_LEADING = 0x800;

    public static final int BELOW_BASELINE_TRAILING = 0x900;

    public int gridx;

    public int gridy;

    public int gridwidth;

    public int gridheight;

    public double weightx;

    public double weighty;

    public int anchor;

    public int fill;

    public Insets insets;

    /**
     * What the component measures, without the internal padding.
     *
     * <p>It is package-private and {@link GridBagLayout#getLayoutInfo} fills it: the layout
     * measures once and records it here so that {@link GridBagLayout#adjustForGravity} knows what
     * size to leave the component at when the cell is too big for it and it did not ask to fill it.
     */
    int minWidth;

    /** The same, for the height. */
    int minHeight;

    public int ipadx;

    public int ipady;

    /** The defaults: a cell after the previous one, centred and not stretched. */
    public GridBagConstraints() {
        gridx = RELATIVE;
        gridy = RELATIVE;
        gridwidth = 1;
        gridheight = 1;

        weightx = 0;
        weighty = 0;
        anchor = CENTER;
        fill = NONE;

        insets = new Insets(0, 0, 0, 0);
        ipadx = 0;
        ipady = 0;
    }

    public GridBagConstraints(int gridx, int gridy, int gridwidth, int gridheight,
            double weightx, double weighty, int anchor, int fill, Insets insets,
            int ipadx, int ipady) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.gridwidth = gridwidth;
        this.gridheight = gridheight;
        this.fill = fill;
        this.ipadx = ipadx;
        this.ipady = ipady;
        this.insets = insets;
        this.anchor = anchor;
        this.weightx = weightx;
        this.weighty = weighty;
    }

    /**
     * A copy. The Insets are cloned separately: they are a mutable object, and if they were shared,
     * changing the copy's insets would change the original's, which is exactly what nobody expects
     * from a clone.
     */
    public Object clone() {
        try {
            GridBagConstraints c = (GridBagConstraints) super.clone();
            c.insets = (Insets) insets.clone();
            return c;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
