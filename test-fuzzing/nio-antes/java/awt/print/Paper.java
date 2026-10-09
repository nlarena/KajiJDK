package java.awt.print;

/**
 * KajiLibrary's java.awt.print.Paper -- the physical sheet and its imageable area.
 *
 * <p>Two rectangles: the size of the sheet, and inside it the part where the printer can put ink.
 * The rest is the mechanical margin, which exists because the rollers have to grab the paper
 * somewhere.
 *
 * <p>Everything is in <b>points</b>: 1/72 of an inch. A letter page is 612 by 792, and that is the
 * default, with one inch of margin on each side.
 *
 * <h2>It validates nothing</h2>
 *
 * <p>{@link #setImageableArea} accepts an area that goes outside the sheet, or a negative one. That
 * is deliberate in the JDK and we respect it: what corrects it is {@code PrinterJob.validatePage},
 * which knows which printer to validate against. A lone {@code Paper} has nothing to validate with.
 *
 * <p>It is mutable and {@link Cloneable}; that is why {@link PageFormat#getPaper} returns a copy.
 */
public class Paper implements Cloneable {

    /** One inch in points. */
    private static final int INCH = 72;

    /** Width of the sheet. */
    private double mHeight;

    /** Height of the sheet. */
    private double mWidth;

    /** The imageable area. */
    private double mImageableX;

    private double mImageableY;

    private double mImageableWidth;

    private double mImageableHeight;

    /** A letter page with one inch of margin. */
    public Paper() {
        this.mHeight = 11.0 * INCH;
        this.mWidth = 8.5 * INCH;
        this.mImageableX = INCH;
        this.mImageableY = INCH;
        this.mImageableWidth = this.mWidth - 2.0 * INCH;
        this.mImageableHeight = this.mHeight - 2.0 * INCH;
    }

    /** An independent copy. */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Paper is Cloneable, so this cannot happen.
            throw new InternalError(e);
        }
    }

    /** The width of the sheet, in points. */
    public double getWidth() {
        return this.mWidth;
    }

    /** The height of the sheet, in points. */
    public double getHeight() {
        return this.mHeight;
    }

    /** Changes the size of the sheet. It does not touch the imageable area. */
    public void setSize(double width, double height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    /** Changes the imageable area. It does not validate; see the class note. */
    public void setImageableArea(double x, double y, double width, double height) {
        this.mImageableX = x;
        this.mImageableY = y;
        this.mImageableWidth = width;
        this.mImageableHeight = height;
    }

    /** Left edge of the imageable area. */
    public double getImageableX() {
        return this.mImageableX;
    }

    /** The top edge. */
    public double getImageableY() {
        return this.mImageableY;
    }

    /** Width of the imageable area. */
    public double getImageableWidth() {
        return this.mImageableWidth;
    }

    /** Height of the imageable area. */
    public double getImageableHeight() {
        return this.mImageableHeight;
    }
}
