package java.awt;

/**
 * KajiLibrary's java.awt.BasicStroke -- how a line is drawn: width, ends, corners and dashes.
 *
 * <p>It is an immutable value and nothing more: it does not draw, it describes. What draws is the
 * {@code Graphics2D} that receives it, and that is why this class can be written whole without any
 * window system -- except one method, see below.
 *
 * <h2>The four decisions it describes</h2>
 *
 * <ul>
 *   <li><b>width</b>: in user-space units, not pixels. A width of 0 is legal and means "the
 *       thinnest line the device can draw".
 *   <li><b>cap</b> ({@code CAP_*}): what happens at the ends of an open segment.
 *   <li><b>join</b> ({@code JOIN_*}): what happens where two segments meet.
 *   <li><b>dashes</b>: the pattern of stroke and gap, plus the phase, which is how much of the
 *       pattern has already been used up at the start.
 * </ul>
 *
 * <h2>The validations, which are not the ones one expects</h2>
 *
 * <p>All four came from asking JDK 25, and in all four the intuitive answer is the opposite:
 *
 * <ol>
 *   <li>A width of <b>zero is valid</b>; only a negative one is rejected.
 *   <li>The miter limit is checked <b>only if the join is {@code JOIN_MITER}</b>. With
 *       {@code JOIN_ROUND} a value below 1 passes without complaint, because it is not used there.
 *   <li>A dash pattern with <b>some</b> zero is valid; only one with <b>all</b> zeros is rejected,
 *       which would be a pattern that never advances.
 *   <li>The zero- and one-argument constructors use {@code CAP_SQUARE}, not {@code CAP_BUTT}.
 * </ol>
 *
 * <h2>The only method not implemented</h2>
 *
 * <p>{@code createStrokedShape(Shape)} throws {@code UnsupportedOperationException}. Its job is to
 * compute the outline of the shape that results from running along a path with this pen: widening
 * each segment, closing the ends, resolving the joins -- with the miter cut when it passes the
 * limit -- and applying the dashes along Bezier curves. It is real computational geometry, and an
 * approximate version would be worse than the exception: it would return an outline that looks
 * almost right and does not match the one any other implementation draws, without warning.
 *
 * <p>It is declared anyway, rather than leaving the class out, because {@code Stroke} requires it
 * and because normal use of {@code BasicStroke} does not go through it: it is built, passed to a
 * {@code Graphics2D}, and it is the rasterizer that interprets it. Everything the class promises as
 * a <b>value</b> --the validations, the copies, equality, the hash-- is exact.
 */
public class BasicStroke implements Stroke {

    /** Corners are extended until the two edges cross. */
    public static final int JOIN_MITER = 0;

    /** Corners are rounded with an arc. */
    public static final int JOIN_ROUND = 1;

    /** Corners are cut with a straight segment. */
    public static final int JOIN_BEVEL = 2;

    /** The line ends exactly at the end point, without sticking out. */
    public static final int CAP_BUTT = 0;

    /** The line ends in a semicircle that sticks out half a width. */
    public static final int CAP_ROUND = 1;

    /** The line ends in a square that sticks out half a width. */
    public static final int CAP_SQUARE = 2;

    // Package-private and not private: it is how the JDK declares them. (This comment added that
    // code in java.awt itself reads them without the accessors; nothing here does.)
    float width;
    int join;
    int cap;
    float miterlimit;
    float[] dash;
    float dash_phase;

    /**
     * @param width      the width; 0 means the thinnest possible line
     * @param cap        one of the {@code CAP_*}
     * @param join       one of the {@code JOIN_*}
     * @param miterlimit the miter limit; only used --and only validated-- with {@code JOIN_MITER}
     * @param dash       the dash pattern, or null for a solid line
     * @param dash_phase how much of the pattern has already been used up at the start
     * @throws IllegalArgumentException if any argument is out of range; see the class note, because
     *     the limits are not the ones one assumes
     */
    public BasicStroke(float width, int cap, int join, float miterlimit,
            float[] dash, float dash_phase) {
        if (width < 0.0f) {
            throw new IllegalArgumentException("negative width");
        }
        if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
            throw new IllegalArgumentException("illegal end cap value");
        }
        if (join == JOIN_MITER) {
            // Only here: with the other two joins the limit is not used at all.
            if (miterlimit < 1.0f) {
                throw new IllegalArgumentException("miter limit < 1");
            }
        } else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
            throw new IllegalArgumentException("illegal line join value");
        }
        if (dash != null) {
            if (dash_phase < 0.0f) {
                throw new IllegalArgumentException("negative dash phase");
            }
            boolean anyPositive = false;
            int i = 0;
            while (i < dash.length) {
                if (dash[i] < 0.0f) {
                    throw new IllegalArgumentException("negative dash length");
                }
                if (dash[i] > 0.0f) {
                    anyPositive = true;
                }
                i = i + 1;
            }
            // A pattern that never advances would leave whoever draws in an infinite loop. That
            // **some** segment is zero, on the other hand, is legitimate: it is how dots are asked
            // for with CAP_ROUND.
            if (!anyPositive) {
                throw new IllegalArgumentException("dash lengths all zero");
            }
        }
        this.width = width;
        this.cap = cap;
        this.join = join;
        this.miterlimit = miterlimit;
        if (dash != null) {
            this.dash = copy(dash);
        }
        this.dash_phase = dash_phase;
    }

    /** A solid line, with the given caps and joins. */
    public BasicStroke(float width, int cap, int join, float miterlimit) {
        this(width, cap, join, miterlimit, null, 0.0f);
    }

    /** The same, with the default miter limit. */
    public BasicStroke(float width, int cap, int join) {
        this(width, cap, join, 10.0f, null, 0.0f);
    }

    /** Only the width. The caps stay {@code CAP_SQUARE} and the joins {@code JOIN_MITER}. */
    public BasicStroke(float width) {
        this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    /** The default pen: width 1, {@code CAP_SQUARE}, {@code JOIN_MITER}, limit 10. */
    public BasicStroke() {
        this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
    }

    public float getLineWidth() {
        return this.width;
    }

    public int getEndCap() {
        return this.cap;
    }

    public int getLineJoin() {
        return this.join;
    }

    public float getMiterLimit() {
        return this.miterlimit;
    }

    /** The dash pattern, or null if the line is solid. A copy. */
    public float[] getDashArray() {
        if (this.dash == null) {
            return null;
        }
        return copy(this.dash);
    }

    public float getDashPhase() {
        return this.dash_phase;
    }

    /**
     * Not implemented: see the class note.
     *
     * @throws UnsupportedOperationException always
     */
    public Shape createStrokedShape(Shape p) {
        throw new UnsupportedOperationException("createStrokedShape needs a geometry engine "
            + "this library does not have; an approximate outline would not match "
            + "the one of any other implementation");
    }

    private static float[] copy(float[] a) {
        float[] c = new float[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    /**
     * The JDK's hash, bit for bit.
     *
     * <p>The bit representation of the floats is mixed and not their value, and the phase and
     * dashes only enter if there is a pattern. It is worth reproducing exactly: a {@code
     * BasicStroke} is a reasonable key in a cache of pens, and a different hash breaks nothing but
     * keeps two libraries from sharing that cache.
     */
    @Override
    public int hashCode() {
        int hash = Float.floatToIntBits(this.width);
        hash = hash * 31 + this.join;
        hash = hash * 31 + this.cap;
        hash = hash * 31 + Float.floatToIntBits(this.miterlimit);
        if (this.dash != null) {
            hash = hash * 31 + Float.floatToIntBits(this.dash_phase);
            int i = 0;
            while (i < this.dash.length) {
                hash = hash * 31 + Float.floatToIntBits(this.dash[i]);
                i = i + 1;
            }
        }
        return hash;
    }

    /** Two pens are the same if they would draw alike. */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicStroke)) {
            return false;
        }
        BasicStroke other = (BasicStroke) obj;
        if (this.width != other.width || this.join != other.join || this.cap != other.cap
                || this.miterlimit != other.miterlimit) {
            return false;
        }
        if (this.dash == null) {
            return other.dash == null;
        }
        if (other.dash == null || this.dash_phase != other.dash_phase
                || this.dash.length != other.dash.length) {
            return false;
        }
        int i = 0;
        while (i < this.dash.length) {
            if (this.dash[i] != other.dash[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }
}
