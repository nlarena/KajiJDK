package java.awt;

/**
 * A colour in sRGB with alpha, packed into a single integer.
 *
 * <p>The packed state is 32 bits: {@code 0xAARRGGBB}. That it is an integer and not four fields is
 * not an implementation detail but API: {@code getRGB()} returns exactly that integer,
 * {@code hashCode()} too, and the channel order is the one any video buffer expects. (This note
 * called those 32 bits the whole state; the transient floats and colour space described below are
 * state too.)
 *
 * <h2>The floats are kept separately, and there is a reason</h2>
 *
 * <p>A colour built with floats --{@code new Color(0.1f, 0.2f, 0.3f)}-- also keeps the three
 * original floats. It looks redundant and is not: going through integers loses precision, and if
 * {@code getRGBColorComponents()} returned {@code getRed()/255f} the value coming out would not be
 * the one that went in. With the floats kept, whoever built with floats gets them back intact and
 * whoever built with integers gets the division; both answers are exact for their origin.
 *
 * <h2>The colour space</h2>
 *
 * <p>A Color built by any of the integer or float constructors is sRGB. The constructor {@code
 * Color(ColorSpace, float[], float)} allows another, and there the components kept are those of the
 * given space --not sRGB's--: {@code getColorComponents(null)} returns what was passed, and {@code
 * getRed()} returns the conversion to sRGB. That asymmetry is part of the contract, and it is what
 * makes a greyscale colour keep its single component instead of degrading to three.
 *
 * <p>As a {@link Paint}, it is the degenerate case: {@code createContext} returns a context that
 * answers the same colour at every point. That is what makes drawing with a colour and drawing with
 * a gradient the same operation for whoever draws.
 */
public class Color implements Paint, java.io.Serializable {

    private static final long serialVersionUID = 118526816881161077L;

    public static final Color white = new Color(255, 255, 255);

    public static final Color WHITE = white;

    public static final Color lightGray = new Color(192, 192, 192);

    public static final Color LIGHT_GRAY = lightGray;

    public static final Color gray = new Color(128, 128, 128);

    public static final Color GRAY = gray;

    public static final Color darkGray = new Color(64, 64, 64);

    public static final Color DARK_GRAY = darkGray;

    public static final Color black = new Color(0, 0, 0);

    public static final Color BLACK = black;

    public static final Color red = new Color(255, 0, 0);

    public static final Color RED = red;

    /**
     * Not light red: 255,175,175. The blue goes along with the green so it does not turn orange.
     */
    public static final Color pink = new Color(255, 175, 175);

    public static final Color PINK = pink;

    /** 255,200,0 and not 255,165,0: AWT's orange is yellower than the web's "orange". */
    public static final Color orange = new Color(255, 200, 0);

    public static final Color ORANGE = orange;

    public static final Color yellow = new Color(255, 255, 0);

    public static final Color YELLOW = yellow;

    public static final Color green = new Color(0, 255, 0);

    public static final Color GREEN = green;

    public static final Color magenta = new Color(255, 0, 255);

    public static final Color MAGENTA = magenta;

    public static final Color cyan = new Color(0, 255, 255);

    public static final Color CYAN = cyan;

    public static final Color blue = new Color(0, 0, 255);

    public static final Color BLUE = blue;

    /** 0xAARRGGBB. The packed state of the class. */
    int value;

    /**
     * The three floats it was built with, or null if it was built with integers. It is transient on
     * purpose: on deserialization only {@code value} arrives, and making them again by dividing
     * would give numbers that were never the originals.
     */
    private transient float[] frgbvalue;

    private transient float falpha;

    /**
     * The components in the colour's own space, or null if this colour is sRGB.
     *
     * <p>It differs from {@code frgbvalue}: that one is always three numbers in sRGB and this one
     * has as many as the space says. A grey has a single one, and keeping it converted to three
     * would make it unrecoverable.
     */
    private transient float[] fvalue;

    /** This colour's space, or null while nobody has asked for it and it is sRGB. */
    private transient java.awt.color.ColorSpace cs;

    /**
     * When brightening, channels at zero do not move --dividing zero by 0.7 still gives zero-- so a
     * pure black would never brighten. That is why there is a floor: a channel between 1 and 2 is
     * raised to 3 before dividing, and all-black becomes (3,3,3). Without that floor, {@code
     * brighter()} applied many times to a very dark grey would stay put.
     */
    private static final double FACTOR = 0.7;

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24)
                | ((r & 0xFF) << 16)
                | ((g & 0xFF) << 8)
                | ((b & 0xFF) << 0);
        testColorValueRange(r, g, b, a);
    }

    /** The top 8 bits are ignored: this constructor always gives an opaque colour. */
    public Color(int rgb) {
        value = 0xff000000 | rgb;
    }

    public Color(int rgba, boolean hasalpha) {
        if (hasalpha) {
            value = rgba;
        } else {
            value = 0xff000000 | rgba;
        }
    }

    public Color(float r, float g, float b) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
        testColorValueRange(r, g, b, 1.0f);
        frgbvalue = new float[3];
        frgbvalue[0] = r;
        frgbvalue[1] = g;
        frgbvalue[2] = b;
        falpha = 1.0f;
    }

    public Color(float r, float g, float b, float a) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5),
                (int) (a * 255 + 0.5));
        frgbvalue = new float[3];
        frgbvalue[0] = r;
        frgbvalue[1] = g;
        frgbvalue[2] = b;
        falpha = a;
    }

    /**
     * The message lists <b>all</b> the channels out of range, not the first: whoever passes red and
     * blue wrong has usually made a mistake in the integer conversion, and seeing both saves a
     * second trip.
     */
    private static void testColorValueRange(int r, int g, int b, int a) {
        boolean rangeError = false;
        String badComponentString = "";

        if (a < 0 || a > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if (r < 0 || r > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if (g < 0 || g > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if (b < 0 || b > 255) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                    "Color parameter outside of expected range:" + badComponentString);
        }
    }

    private static void testColorValueRange(float r, float g, float b, float a) {
        boolean rangeError = false;
        String badComponentString = "";
        if (a < 0.0 || a > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Alpha";
        }
        if (r < 0.0 || r > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Red";
        }
        if (g < 0.0 || g > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Green";
        }
        if (b < 0.0 || b > 1.0) {
            rangeError = true;
            badComponentString = badComponentString + " Blue";
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                    "Color parameter outside of expected range:" + badComponentString);
        }
    }

    public int getRed() {
        return (getRGB() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getRGB() >> 8) & 0xFF;
    }

    public int getBlue() {
        return (getRGB() >> 0) & 0xFF;
    }

    public int getAlpha() {
        return (getRGB() >> 24) & 0xff;
    }

    public int getRGB() {
        return value;
    }

    public Color brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int alpha = getAlpha();

        int i = (int) (1.0 / (1.0 - FACTOR));
        if (r == 0 && g == 0 && b == 0) {
            return new Color(i, i, i, alpha);
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }

        return new Color(Math.min((int) (r / FACTOR), 255),
                Math.min((int) (g / FACTOR), 255),
                Math.min((int) (b / FACTOR), 255),
                alpha);
    }

    /**
     * Darkening needs no floor: multiplying by 0.7 always goes down, and zero is already the
     * bottom.
     */
    public Color darker() {
        return new Color(Math.max((int) (getRed() * FACTOR), 0),
                Math.max((int) (getGreen() * FACTOR), 0),
                Math.max((int) (getBlue() * FACTOR), 0),
                getAlpha());
    }

    public int hashCode() {
        return value;
    }

    /** Alpha counts: {@code getRGB()} includes it, so two colours with different alpha differ. */
    public boolean equals(Object obj) {
        return obj instanceof Color && ((Color) obj).getRGB() == this.getRGB();
    }

    /**
     * Alpha is not printed, not even when it is not 255. It has been so since 1.1 and cannot
     * change.
     */
    public String toString() {
        return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue()
                + "]";
    }

    /**
     * Accepts what {@code Integer.decode} does: "#RRGGBB", "0xRRGGBB", "0RRGGBB" in octal and a
     * plain decimal. Any alpha in the top 8 bits is discarded.
     */
    public static Color decode(String nm) throws NumberFormatException {
        Integer intval = Integer.decode(nm);
        int i = intval.intValue();
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    public static Color getColor(String nm) {
        return getColor(nm, null);
    }

    public static Color getColor(String nm, Color v) {
        Integer intval = Integer.getInteger(nm);
        if (intval == null) {
            return v;
        }
        int i = intval.intValue();
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
    }

    public static Color getColor(String nm, int v) {
        Integer intval = Integer.getInteger(nm);
        int i = (intval != null) ? intval.intValue() : v;
        return new Color((i >> 16) & 0xFF, (i >> 8) & 0xFF, (i >> 0) & 0xFF);
    }

    /**
     * Hue, saturation and brightness to RGB.
     *
     * <p>The hue is taken modulo 1 --{@code hue - floor(hue)}-- so 1.25 and 0.25 give the same
     * colour and a negative hue works too: it is an angle, not a bounded fraction.
     */
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0) {
            r = (int) (brightness * 255.0f + 0.5f);
            g = r;
            b = r;
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (t * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 1:
                    r = (int) (q * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (p * 255.0f + 0.5f);
                    break;
                case 2:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (brightness * 255.0f + 0.5f);
                    b = (int) (t * 255.0f + 0.5f);
                    break;
                case 3:
                    r = (int) (p * 255.0f + 0.5f);
                    g = (int) (q * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 4:
                    r = (int) (t * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (brightness * 255.0f + 0.5f);
                    break;
                case 5:
                    r = (int) (brightness * 255.0f + 0.5f);
                    g = (int) (p * 255.0f + 0.5f);
                    b = (int) (q * 255.0f + 0.5f);
                    break;
                default:
                    break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
    }

    /**
     * RGB to hue, saturation and brightness.
     *
     * <p>When the saturation is zero the hue stays at 0 by convention: a grey has no hue, and any
     * other value would be invented.
     */
    public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
        float hue;
        float saturation;
        float brightness;
        if (hsbvals == null) {
            hsbvals = new float[3];
        }
        int cmax = (r > g) ? r : g;
        if (b > cmax) {
            cmax = b;
        }
        int cmin = (r < g) ? r : g;
        if (b < cmin) {
            cmin = b;
        }

        brightness = ((float) cmax) / 255.0f;
        if (cmax != 0) {
            saturation = ((float) (cmax - cmin)) / ((float) cmax);
        } else {
            saturation = 0;
        }
        if (saturation == 0) {
            hue = 0;
        } else {
            float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
            float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
            float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
            if (r == cmax) {
                hue = bluec - greenc;
            } else if (g == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue = hue / 6.0f;
            if (hue < 0) {
                hue = hue + 1.0f;
            }
        }
        hsbvals[0] = hue;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static Color getHSBColor(float h, float s, float b) {
        return new Color(HSBtoRGB(h, s, b));
    }

    public float[] getRGBComponents(float[] compArray) {
        float[] f;
        if (compArray == null) {
            f = new float[4];
        } else {
            f = compArray;
        }
        if (frgbvalue == null) {
            f[0] = ((float) getRed()) / 255f;
            f[1] = ((float) getGreen()) / 255f;
            f[2] = ((float) getBlue()) / 255f;
            f[3] = ((float) getAlpha()) / 255f;
        } else {
            f[0] = frgbvalue[0];
            f[1] = frgbvalue[1];
            f[2] = frgbvalue[2];
            f[3] = falpha;
        }
        return f;
    }

    public float[] getRGBColorComponents(float[] compArray) {
        float[] f;
        if (compArray == null) {
            f = new float[3];
        } else {
            f = compArray;
        }
        if (frgbvalue == null) {
            f[0] = ((float) getRed()) / 255f;
            f[1] = ((float) getGreen()) / 255f;
            f[2] = ((float) getBlue()) / 255f;
        } else {
            f[0] = frgbvalue[0];
            f[1] = frgbvalue[1];
            f[2] = frgbvalue[2];
        }
        return f;
    }

    /**
     * The components in this colour's space, plus the alpha at the end.
     *
     * <p>For an sRGB colour --most of them-- it is exactly {@code getRGBComponents}. For one built
     * with another space they are those of **that** space, and they need not be three: a greyscale
     * colour returns two numbers, the grey and the alpha.
     */
    public float[] getComponents(float[] compArray) {
        if (this.fvalue == null) {
            return this.getRGBComponents(compArray);
        }
        float[] f;
        if (compArray == null) {
            f = new float[this.fvalue.length + 1];
        } else {
            f = compArray;
        }
        for (int i = 0; i < this.fvalue.length; i++) {
            f[i] = this.fvalue[i];
        }
        f[this.fvalue.length] = this.falpha;
        return f;
    }

    /** The components in this colour's space, without the alpha. See {@link #getComponents}. */
    public float[] getColorComponents(float[] compArray) {
        if (this.fvalue == null) {
            return this.getRGBColorComponents(compArray);
        }
        float[] f;
        if (compArray == null) {
            f = new float[this.fvalue.length];
        } else {
            f = compArray;
        }
        for (int i = 0; i < this.fvalue.length; i++) {
            f[i] = this.fvalue[i];
        }
        return f;
    }

    /**
     * An alpha of 0 gives BITMASK and not TRANSLUCENT: the colour is completely invisible, so
     * whoever composites can skip blending instead of multiplying by zero pixel by pixel.
     */
    public int getTransparency() {
        int alpha = getAlpha();
        if (alpha == 0xff) {
            return Transparency.OPAQUE;
        } else if (alpha == 0) {
            return Transparency.BITMASK;
        } else {
            return Transparency.TRANSLUCENT;
        }
    }

    /**
     * A colour in the given space.
     *
     * <p>The components are kept **in that space**, not converted to sRGB: that is what makes
     * {@code getColorComponents(null)} return them intact. What is converted, and only once here,
     * is the packed sRGB value that {@code getRed()} and company return -- otherwise every call
     * would pay for the conversion.
     *
     * @throws NullPointerException if the space or the components are null
     * @throws IllegalArgumentException if a component falls outside the range the space declares,
     *     or if the alpha is not between 0 and 1. This javadoc also said it throws for too many or
     *     too few components; see the comment in the body: too few gives an {@code
     *     ArrayIndexOutOfBoundsException} and too many are accepted
     */
    public Color(java.awt.color.ColorSpace cspace, float[] components, float alpha) {
        if (cspace == null) {
            throw new NullPointerException("color space cannot be null");
        }
        if (components == null) {
            throw new NullPointerException("components cannot be null");
        }
        int n = cspace.getNumComponents();
        // An array shorter than the space asks for is **not** checked: it is walked and
        // `ArrayIndexOutOfBoundsException` comes out by itself. That is what JDK 25 does
        // --checked-- and although an `IllegalArgumentException` would be more informative,
        // changing it breaks whoever catches the one the contract produces. A longer one is
        // accepted and the extra is left over.
        boolean rangeError = false;
        StringBuilder badComponentString = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (components[i] < cspace.getMinValue(i) || components[i] > cspace.getMaxValue(i)) {
                rangeError = true;
                badComponentString.append(" Component ").append(i);
            }
        }
        if (alpha < 0.0f || alpha > 1.0f) {
            rangeError = true;
            badComponentString.append(" Alpha");
        }
        if (rangeError) {
            throw new IllegalArgumentException(
                    "Color parameter outside of expected range:" + badComponentString.toString());
        }
        this.fvalue = new float[n];
        for (int i = 0; i < n; i++) {
            this.fvalue[i] = components[i];
        }
        this.falpha = alpha;
        this.cs = cspace;
        // The packed sRGB comes out of the space's conversion; the rounding is the same `+0.5` the
        // float constructors use.
        float[] rgb = cspace.toRGB(this.fvalue);
        this.frgbvalue = new float[] { rgb[0], rgb[1], rgb[2] };
        this.value = ((((int) (alpha * 255 + 0.5)) & 0xFF) << 24)
                | ((((int) (rgb[0] * 255 + 0.5)) & 0xFF) << 16)
                | ((((int) (rgb[1] * 255 + 0.5)) & 0xFF) << 8)
                | (((int) (rgb[2] * 255 + 0.5)) & 0xFF);
    }

    /** This colour's space; sRGB unless it was built with another. */
    public java.awt.color.ColorSpace getColorSpace() {
        if (this.cs == null) {
            this.cs = java.awt.color.ColorSpace.getInstance(
                    java.awt.color.ColorSpace.CS_sRGB);
        }
        return this.cs;
    }

    /**
     * The components of this colour **in the requested space**, plus the alpha at the end.
     *
     * @throws NullPointerException if the space is null
     */
    public float[] getComponents(java.awt.color.ColorSpace cspace, float[] compArray) {
        if (cspace == null) {
            throw new NullPointerException("color space cannot be null");
        }
        float[] color = this.getColorComponents(cspace, null);
        float[] f;
        if (compArray == null) {
            f = new float[color.length + 1];
        } else {
            f = compArray;
        }
        for (int i = 0; i < color.length; i++) {
            f[i] = color[i];
        }
        f[color.length] = this.getAlpha() / 255f;
        if (this.fvalue != null) {
            f[color.length] = this.falpha;
        }
        return f;
    }

    /**
     * The components of this colour in the requested space, **without** the alpha.
     *
     * <p>The conversion goes through CIEXYZ, which is how any two spaces are converted: from this
     * space to XYZ and from XYZ to the requested one. If the requested one is its own, the
     * components are returned as they are and nothing is converted -- a round trip through XYZ
     * would lose precision for nothing.
     *
     * @throws NullPointerException if the space is null
     */
    public float[] getColorComponents(java.awt.color.ColorSpace cspace, float[] compArray) {
        if (cspace == null) {
            throw new NullPointerException("color space cannot be null");
        }
        float[] own = this.getColorComponents(null);
        float[] converted;
        if (cspace == this.getColorSpace()) {
            converted = own;
        } else {
            converted = cspace.fromCIEXYZ(this.getColorSpace().toCIEXYZ(own));
        }
        float[] f;
        if (compArray == null) {
            f = new float[converted.length];
        } else {
            f = compArray;
        }
        for (int i = 0; i < converted.length; i++) {
            f[i] = converted[i];
        }
        return f;
    }

    /**
     * Builds the machine that generates the pixels: the simplest of all.
     *
     * <p>A flat colour answers the same at every point, so the context needs neither to invert the
     * transformation nor to look at the coordinates.
     */
    public java.awt.PaintContext createContext(java.awt.image.ColorModel cm,
            java.awt.Rectangle r, java.awt.geom.Rectangle2D r2d,
            java.awt.geom.AffineTransform xform, java.awt.RenderingHints hints) {
        return new ColorPaintContext(this.getRGB());
    }
}
