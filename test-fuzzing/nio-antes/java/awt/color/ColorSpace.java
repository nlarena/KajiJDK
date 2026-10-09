package java.awt.color;

import java.io.Serializable;

/**
 * A colour space: how many components it has, what range each of them has, and how it converts to
 * sRGB and to CIEXYZ.
 *
 * <p>CIEXYZ is the axis of the whole design and it is worth saying why: it is an **absolute**
 * space, tied to how the human eye sees and not to any device. Any pair of spaces converts between
 * each other by passing through it, and that is why every space has to know how to go to and from
 * XYZ even though it knows nothing about the others. `toRGB`/`fromRGB` exist separately because the
 * road to sRGB is the most used one and doing it in two steps would be dearer and less exact.
 *
 * <h2>The standard spaces come out of ICC profiles</h2>
 *
 * <p>{@link #getInstance} returns an {@link ICC_ColorSpace}, as the JDK does: behind each one there
 * is a real {@link ICC_Profile}, with its matrix, its curves and its white point. The difference
 * from the JDK is where the profile's bytes come from — the JDK ships them as a resource file and
 * here they are **built** out of the standard's constants. The result is a valid ICC profile that
 * can be written to a file and that another program reads.
 *
 * <p>The observable consequence is that the numbers differ in the last digits: both roads quantize
 * to 16 bits but not at the same points, so the JDK's `toRGB({0.5,0.5,0.5})` over sRGB gives
 * `0.5000076` and this one another value just as close to 0.5. A test comparing bit for bit against
 * the JDK will fail; this house's tests compare with a tolerance and verify **properties** — round
 * trips, known points — instead of digits.
 *
 * <p><strong>The one thing missing is {@link #CS_PYCC}</strong>, and the reason is concrete:
 * PhotoYCC is defined not by formulas but by 230 KB of interpolation tables that live inside its
 * profile. Without that file there is nothing to build, and assembling an empty profile with its
 * signature would be an object claiming to be PhotoYCC that does not convert like PhotoYCC.
 * `getInstance(CS_PYCC)` throws saying so.
 */
public abstract class ColorSpace implements Serializable {

    private static final long serialVersionUID = -409452704308689724L;

    /** CIEXYZ. */
    public static final int TYPE_XYZ = 0;
    /** CIELab. */
    public static final int TYPE_Lab = 1;
    /** CIELuv. */
    public static final int TYPE_Luv = 2;
    /** YCbCr. */
    public static final int TYPE_YCbCr = 3;
    /** CIEYxy. */
    public static final int TYPE_Yxy = 4;
    /** RGB. */
    public static final int TYPE_RGB = 5;
    /** Greyscale. */
    public static final int TYPE_GRAY = 6;
    /** HSV. */
    public static final int TYPE_HSV = 7;
    /** HLS. */
    public static final int TYPE_HLS = 8;
    /** CMYK. */
    public static final int TYPE_CMYK = 9;
    /** CMY. */
    public static final int TYPE_CMY = 11;
    /** Generic, 2 components. */
    public static final int TYPE_2CLR = 12;
    /** Generic, 3 components. */
    public static final int TYPE_3CLR = 13;
    /** Generic, 4 components. */
    public static final int TYPE_4CLR = 14;
    /** Generic, 5 components. */
    public static final int TYPE_5CLR = 15;
    /** Generic, 6 components. */
    public static final int TYPE_6CLR = 16;
    /** Generic, 7 components. */
    public static final int TYPE_7CLR = 17;
    /** Generic, 8 components. */
    public static final int TYPE_8CLR = 18;
    /** Generic, 9 components. */
    public static final int TYPE_9CLR = 19;
    /** Generic, 10 components. */
    public static final int TYPE_ACLR = 20;
    /** Generic, 11 components. */
    public static final int TYPE_BCLR = 21;
    /** Generic, 12 components. */
    public static final int TYPE_CCLR = 22;
    /** Generic, 13 components. */
    public static final int TYPE_DCLR = 23;
    /** Generic, 14 components. */
    public static final int TYPE_ECLR = 24;
    /** Generic, 15 components. */
    public static final int TYPE_FCLR = 25;

    /** The usual sRGB, with its gamma curve. */
    public static final int CS_sRGB = 1000;
    /** **Linear** RGB: the same primaries as sRGB but without the curve. */
    public static final int CS_LINEAR_RGB = 1004;
    /** CIEXYZ with a D50 white, which is the one ICC uses. */
    public static final int CS_CIEXYZ = 1001;
    /** PhotoYCC. **Not available here**; see the note of the class. */
    public static final int CS_PYCC = 1002;
    /** Linear greyscale. */
    public static final int CS_GRAY = 1003;

    // The twenty-odd `TYPE_` constants that are missing (there is no 10: the JDK skips the gap
    // between CMYK and CMY) are not an oversight -- the standard does not define a type 10 either.

    private final int type;
    private final int numComponents;

    /**
     * A space of that type and with that number of components.
     *
     * @throws IllegalArgumentException if the number of components is less than 1
     */
    protected ColorSpace(int type, int numComponentsArg) {
        if (numComponentsArg < 1) {
            throw new IllegalArgumentException("numComponents < 1");
        }
        this.type = type;
        this.numComponents = numComponentsArg;
    }

    // The instances are unique per identifier: `getInstance(CS_sRGB) == getInstance(CS_sRGB)`, as
    // in the JDK. They are created late because building all five on class load would cost the work
    // of the four nobody asked for.
    private static ColorSpace sRGBcs;
    private static ColorSpace linearRGBcs;
    private static ColorSpace xyzCS;
    private static ColorSpace grayCS;

    /**
     * One of the standard spaces.
     *
     * @throws IllegalArgumentException if the identifier is not one of the `CS_`, or if it is
     *     {@link #CS_PYCC} — which exists as a constant but not as a space in this library
     */
    public static ColorSpace getInstance(int colorspace) {
        if (colorspace == CS_sRGB) {
            synchronized (ColorSpace.class) {
                if (sRGBcs == null) {
                    sRGBcs = new ICC_ColorSpace(ICC_Profile.getInstance(CS_sRGB));
                }
                return sRGBcs;
            }
        }
        if (colorspace == CS_LINEAR_RGB) {
            synchronized (ColorSpace.class) {
                if (linearRGBcs == null) {
                    linearRGBcs = new ICC_ColorSpace(ICC_Profile.getInstance(CS_LINEAR_RGB));
                }
                return linearRGBcs;
            }
        }
        if (colorspace == CS_CIEXYZ) {
            synchronized (ColorSpace.class) {
                if (xyzCS == null) {
                    xyzCS = new ICC_ColorSpace(ICC_Profile.getInstance(CS_CIEXYZ));
                }
                return xyzCS;
            }
        }
        if (colorspace == CS_GRAY) {
            synchronized (ColorSpace.class) {
                if (grayCS == null) {
                    grayCS = new ICC_ColorSpace(ICC_Profile.getInstance(CS_GRAY));
                }
                return grayCS;
            }
        }
        if (colorspace == CS_PYCC) {
            // PhotoYCC is defined **as an ICC profile** and not by a formula: without the profile's
            // file there is nothing to compute. Throwing says that; returning an sRGB in disguise
            // would be the member that lies.
            throw new IllegalArgumentException(
                    "CS_PYCC is not available: it needs its ICC profile, which this library "
                            + "does not ship");
        }
        throw new IllegalArgumentException("Unknown color space");
    }

    /** Whether it is the standard sRGB. */
    public boolean isCS_sRGB() {
        return this == sRGBcs;
    }

    /**
     * This colour, in sRGB.
     *
     * @param colorvalue the components in this space
     */
    public abstract float[] toRGB(float[] colorvalue);

    /**
     * An sRGB colour, in this space.
     *
     * @param rgbvalue the three sRGB components
     */
    public abstract float[] fromRGB(float[] rgbvalue);

    /**
     * This colour, in CIEXYZ with a D50 white.
     *
     * <p>D50 and not D65 because that is what ICC uses, and with it the chained conversions need no
     * chromatic adaptation in between.
     */
    public abstract float[] toCIEXYZ(float[] colorvalue);

    /** A CIEXYZ (D50) colour, in this space. */
    public abstract float[] fromCIEXYZ(float[] colorvalue);

    /** This space's `TYPE_`. */
    public int getType() {
        return this.type;
    }

    /** How many components a colour of this space has. */
    public int getNumComponents() {
        return this.numComponents;
    }

    /**
     * The name of component `idx`.
     *
     * <p>By default, a generic name. The implementations that can say "Red" override it.
     *
     * @throws IllegalArgumentException if the index is not a component of this space
     */
    public String getName(int idx) {
        this.rangeCheck(idx);
        return "Unnamed color component(" + idx + ")";
    }

    /**
     * The minimum value of component `idx`. 0 by default.
     *
     * @throws IllegalArgumentException if the index is not a component of this space
     */
    public float getMinValue(int component) {
        this.rangeCheck(component);
        return 0.0f;
    }

    /**
     * The maximum value of component `idx`. 1 by default.
     *
     * @throws IllegalArgumentException if the index is not a component of this space
     */
    public float getMaxValue(int component) {
        this.rangeCheck(component);
        return 1.0f;
    }

    // Package-private, as in the JDK: the subclasses here use it and it is not API.
    final void rangeCheck(int component) {
        if (component < 0 || component > this.numComponents - 1) {
            throw new IllegalArgumentException(
                    "Component index out of range: " + component);
        }
    }

    // ---- the shared mathematics ------------------------------------------------------------
    //
    // The matrices are ICC's sRGB profile's, adapted to D50 by Bradford. They are written out and
    // not computed because they are the standard's constants: recomputing them on every start would
    // be work to arrive at the same numbers with fewer digits.

    static final float[] RGB_TO_XYZ = {
        0.4360747f, 0.3850649f, 0.1430804f,
        0.2225045f, 0.7168786f, 0.0606169f,
        0.0139322f, 0.0971045f, 0.7141733f };

    static final float[] XYZ_TO_RGB = {
        3.1338561f, -1.6168667f, -0.4906146f,
        -0.9787684f, 1.9161415f, 0.0334540f,
        0.0719453f, -0.2289914f, 1.4052427f };

    /** The D50 white, which is ICC's white point. */
    static final float[] WHITE_D50 = { 0.9642f, 1.0f, 0.8249f };

    /**
     * The ceiling of an XYZ component.
     *
     * <p>It is not 2 but `2 - 1/32768`: XYZ is encoded in ICC as 16-bit fixed point with the 1 at
     * 0x8000, so the largest representable value is 0xFFFF/0x8000. The JDK answers this same
     * number.
     */
    static final float XYZ_MAX = 1.0f + (32767.0f / 32768.0f);

    /** sRGB's curve: from a component with gamma to a linear one (IEC 61966-2-1). */
    static float toLinear(float c) {
        if (c <= 0.04045f) {
            return c / 12.92f;
        }
        return (float) Math.pow((c + 0.055) / 1.055, 2.4);
    }

    /** The inverse: from linear to sRGB. */
    static float toGamma(float c) {
        if (c <= 0.0031308f) {
            return c * 12.92f;
        }
        return (float) (1.055 * Math.pow(c, 1.0 / 2.4) - 0.055);
    }

    static float[] multiply(float[] m, float[] v) {
        float[] out = new float[3];
        out[0] = m[0] * v[0] + m[1] * v[1] + m[2] * v[2];
        out[1] = m[3] * v[0] + m[4] * v[1] + m[5] * v[2];
        out[2] = m[6] * v[0] + m[7] * v[1] + m[8] * v[2];
        return out;
    }

    static void require(float[] v, int n) {
        if (v == null) {
            throw new NullPointerException("the colour cannot be null");
        }
        if (v.length < n) {
            throw new ArrayIndexOutOfBoundsException(
                    "the colour needs " + n + " components and has " + v.length);
        }
    }
}
