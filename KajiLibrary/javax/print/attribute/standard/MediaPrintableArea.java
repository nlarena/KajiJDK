package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * The rectangle of the sheet where one can really print.
 *
 * <p>Almost no printer reaches the edge of the paper: there is a physical margin the mechanism
 * imposes. This attribute says where that rectangle starts --{@code x}, {@code y} from the top-left
 * corner-- and how big it is. Asking for it smaller than the physical one is legitimate and is how
 * margins are asked for; asking for it bigger does not enlarge it.
 *
 * <p>It does not extend {@link javax.print.attribute.Size2DSyntax Size2DSyntax} although it looks
 * like it, because they are four numbers and not two. It repeats the same idea all the same: it
 * keeps everything in integer micrometres, which is what makes the same measures expressed in
 * inches and in millimetres come out equal. The unit it was built with is <b>not</b> kept --{@code
 * toString()} always prints in millimetres and {@code equals()} compares micrometres--, so {@code
 * new MediaPrintableArea(1, 1, 1, 1, INCH)} is equal to the same area declared in MM.
 *
 * <p>Width and height have to be strictly positive: an empty printing area describes nothing. The
 * origin may be zero.
 */
public final class MediaPrintableArea
    implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -1597171464050795793L;

    /** Micrometres per inch. */
    public static final int INCH = 25400;

    /** Micrometres per millimetre. */
    public static final int MM = 1000;

    private int x;
    private int y;
    private int w;
    private int h;

    public MediaPrintableArea(float x, float y, float w, float h, int units) {
        if (x < 0.0f || y < 0.0f || w <= 0.0f || h <= 0.0f || units < 1) {
            throw new IllegalArgumentException("0 or negative value argument");
        }
        // The half micrometre is for rounding to the nearest and not truncating.
        this.x = (int) (x * units + 0.5f);
        this.y = (int) (y * units + 0.5f);
        this.w = (int) (w * units + 0.5f);
        this.h = (int) (h * units + 0.5f);
    }

    /** The integer variant does not round because the product is already exact. */
    public MediaPrintableArea(int x, int y, int w, int h, int units) {
        if (x < 0 || y < 0 || w <= 0 || h <= 0 || units < 1) {
            throw new IllegalArgumentException("0 or negative value argument");
        }
        this.x = x * units;
        this.y = y * units;
        this.w = w * units;
        this.h = h * units;
    }

    /**
     * The four numbers together: {@code [x, y, width, height]}. It is not {@code [x0, y0, x1, y1]}.
     */
    public float[] getPrintableArea(int units) {
        return new float[] {getX(units), getY(units), getWidth(units), getHeight(units)};
    }

    public float getX(int units) {
        return convertFromMicrometers(this.x, units);
    }

    public float getY(int units) {
        return convertFromMicrometers(this.y, units);
    }

    public float getWidth(int units) {
        return convertFromMicrometers(this.w, units);
    }

    public float getHeight(int units) {
        return convertFromMicrometers(this.h, units);
    }

    public boolean equals(Object object) {
        if (!(object instanceof MediaPrintableArea)) {
            return false;
        }
        MediaPrintableArea other = (MediaPrintableArea) object;
        return this.x == other.x && this.y == other.y
            && this.w == other.w && this.h == other.h;
    }

    public final Class<? extends Attribute> getCategory() {
        return MediaPrintableArea.class;
    }

    public final String getName() {
        return "media-printable-area";
    }

    /**
     * {@code "(x,y)->(width,height)"} with the suffix attached; a null {@code unitsName} omits it.
     */
    public String toString(int units, String unitsName) {
        if (unitsName == null) {
            unitsName = "";
        }
        float[] vals = getPrintableArea(units);
        return "(" + vals[0] + "," + vals[1] + ")->(" + vals[2] + "," + vals[3] + ")" + unitsName;
    }

    public String toString() {
        return toString(MM, "mm");
    }

    // The four components with different prime weights, so that swapping x and y changes the hash.
    public int hashCode() {
        return this.x + 37 * this.y + 43 * this.w + 47 * this.h;
    }

    private static float convertFromMicrometers(int um, int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        return ((float) um) / ((float) units);
    }
}
