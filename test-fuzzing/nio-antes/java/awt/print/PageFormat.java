package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PageFormat -- a {@link Paper} plus the orientation.
 *
 * <p>The class exists because orientation is not a property of the paper: the sheet always goes
 * into the printer the same way, and what rotates is the <b>drawing</b>. That is why every accessor
 * of this class translates: {@link #getWidth} returns the width <i>as whoever draws sees it</i>,
 * which in landscape is the height of the sheet.
 *
 * <h2>The two landscape orientations</h2>
 *
 * <p>{@link #LANDSCAPE} and {@link #REVERSE_LANDSCAPE} rotate in opposite directions. Both exist
 * because platforms disagree: the JDK documents {@code LANDSCAPE} as the Windows and PostScript
 * landscape and {@code REVERSE_LANDSCAPE} as the Macintosh one. (This note gave as the reason that
 * the binding edge ends up on opposite sides.)
 *
 * <p>The values are surprising: {@code LANDSCAPE} is 0 and {@code PORTRAIT} is 1. There is no
 * reason, it ended up that way, and that is why one must never assume that 0 is portrait.
 *
 * <h2>{@link #getMatrix}</h2>
 *
 * <p>It returns the six numbers of the transformation from the coordinate system of whoever draws
 * to the sheet's. It is what a {@code Graphics2D} needs so that drawing in landscape does not
 * require thinking about rotations.
 *
 * <h2>It copies what goes in and what comes out</h2>
 *
 * <p>{@link #getPaper} returns a copy and {@link #setPaper} stores a copy. Changing the paper that
 * {@code getPaper} returned changes nothing; it has to be passed back with {@code setPaper}. It is
 * a classic stumble and it is on purpose: without it, the page format of a job in progress could
 * change underneath.
 */
public class PageFormat implements Cloneable {

    /** Landscape. Its value is 0; see the class note. */
    public static final int LANDSCAPE = 0;

    /** Portrait. It is worth 1. */
    public static final int PORTRAIT = 1;

    /** Landscape the other way. */
    public static final int REVERSE_LANDSCAPE = 2;

    /** The sheet. */
    private Paper mPaper;

    /** Which of the three. */
    private int mOrientation = PORTRAIT;

    /** A portrait letter page. */
    public PageFormat() {
        this.mPaper = new Paper();
    }

    /** An independent copy, with its own paper. */
    @Override
    public Object clone() {
        PageFormat copy;
        try {
            copy = (PageFormat) super.clone();
        } catch (CloneNotSupportedException e) {
            // PageFormat is Cloneable.
            throw new InternalError(e);
        }
        copy.mPaper = (Paper) this.mPaper.clone();
        return copy;
    }

    /** The width as whoever draws sees it. See the class note. */
    public double getWidth() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getWidth();
        }
        return this.mPaper.getHeight();
    }

    /** The height as whoever draws sees it. */
    public double getHeight() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getHeight();
        }
        return this.mPaper.getWidth();
    }

    /** Left edge of the imageable area, already rotated. */
    public double getImageableX() {
        if (this.mOrientation == LANDSCAPE) {
            return this.mPaper.getHeight()
                - (this.mPaper.getImageableY() + this.mPaper.getImageableHeight());
        }
        if (this.mOrientation == REVERSE_LANDSCAPE) {
            return this.mPaper.getImageableY();
        }
        return this.mPaper.getImageableX();
    }

    /** The top edge, already rotated. */
    public double getImageableY() {
        if (this.mOrientation == LANDSCAPE) {
            return this.mPaper.getImageableX();
        }
        if (this.mOrientation == REVERSE_LANDSCAPE) {
            return this.mPaper.getWidth()
                - (this.mPaper.getImageableX() + this.mPaper.getImageableWidth());
        }
        return this.mPaper.getImageableY();
    }

    /** Width of the imageable area, already rotated. */
    public double getImageableWidth() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getImageableWidth();
        }
        return this.mPaper.getImageableHeight();
    }

    /** Height of the imageable area, already rotated. */
    public double getImageableHeight() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getImageableHeight();
        }
        return this.mPaper.getImageableWidth();
    }

    /** A copy of the sheet. See the class note. */
    public Paper getPaper() {
        return (Paper) this.mPaper.clone();
    }

    /** Stores a copy of that sheet. */
    public void setPaper(Paper paper) {
        this.mPaper = (Paper) paper.clone();
    }

    /**
     * Changes the orientation.
     *
     * @throws IllegalArgumentException if it is not one of the three constants
     */
    public void setOrientation(int orientation) throws IllegalArgumentException {
        if (orientation < LANDSCAPE || orientation > REVERSE_LANDSCAPE) {
            throw new IllegalArgumentException();
        }
        this.mOrientation = orientation;
    }

    /** Which of the three. */
    public int getOrientation() {
        return this.mOrientation;
    }

    /**
     * The transformation from drawing coordinates to sheet coordinates.
     *
     * <p>Six numbers in {@code AffineTransform} order: x scale, y shear, x shear, y scale, x
     * translation, y translation. See the class note.
     */
    public double[] getMatrix() {
        double[] matrix = new double[6];
        if (this.mOrientation == LANDSCAPE) {
            matrix[0] = 0.0;
            matrix[1] = -1.0;
            matrix[2] = 1.0;
            matrix[3] = 0.0;
            matrix[4] = 0.0;
            matrix[5] = this.mPaper.getHeight();
        } else if (this.mOrientation == REVERSE_LANDSCAPE) {
            matrix[0] = 0.0;
            matrix[1] = 1.0;
            matrix[2] = -1.0;
            matrix[3] = 0.0;
            matrix[4] = this.mPaper.getWidth();
            matrix[5] = 0.0;
        } else {
            matrix[0] = 1.0;
            matrix[1] = 0.0;
            matrix[2] = 0.0;
            matrix[3] = 1.0;
            matrix[4] = 0.0;
            matrix[5] = 0.0;
        }
        return matrix;
    }
}
