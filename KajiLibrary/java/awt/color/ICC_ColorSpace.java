package java.awt.color;

/**
 * A colour space defined by an ICC profile.
 *
 * <p>It is what {@link ColorSpace#getInstance} returns for the five standard spaces, and what to use
 * for a profile of one's own: `new ICC_ColorSpace(ICC_Profile.getInstance("myprofile.icc"))`.
 *
 * <p><strong>How it converts, and what it does not do.</strong> A complete colour management engine
 * applies the whole profile, interpolation tables included. Here the conversion is done with the
 * profile's **matrix and curves**, which is exactly right for a matrix profile --the five built-in
 * ones and the vast majority of monitor profiles are-- and is not enough for one based on tables,
 * like a CMYK printer's.
 *
 * <p>A table profile can be **read** all the same --{@link ICC_Profile} parses it whole-- but asking
 * this space for a conversion throws {@link ProfileDataException} instead of returning an invented
 * number. It is the difference between not having a table engine and pretending to have one.
 */
public class ICC_ColorSpace extends ColorSpace {

    private static final long serialVersionUID = 3455889114070431483L;

    private final ICC_Profile profile;

    // The matrix and its inverse are computed once: converting a pixel is the most repeated operation
    // in all of this, and recomputing the inverse on every call would be paying a determinant per
    // pixel.
    private transient float[] toXyz;
    private transient float[] fromXyz;
    private transient short[][] curves;
    private transient float[] gammas;

    /**
     * The space that profile describes.
     *
     * @throws IllegalArgumentException if the profile is null, or if its space does not match the
     *     number of components it declares
     */
    public ICC_ColorSpace(ICC_Profile profile) {
        super(typeOf(profile), profile.getNumComponents());
        this.profile = profile;
    }

    private static int typeOf(ICC_Profile p) {
        if (p == null) {
            throw new IllegalArgumentException("the profile cannot be null");
        }
        return p.getColorSpaceType();
    }

    /** This space's profile. */
    public ICC_Profile getProfile() {
        return this.profile;
    }

    /**
     * The ceiling of a component.
     *
     * <p>1 for almost everything; for XYZ the `2 - 1/32768` that ICC's encoding allows.
     */
    public float getMaxValue(int component) {
        this.rangeCheck(component);
        if (this.getType() == TYPE_XYZ) {
            return XYZ_MAX;
        }
        return 1.0f;
    }

    /** The floor of a component: always 0 in the spaces this class handles. */
    public float getMinValue(int component) {
        this.rangeCheck(component);
        return 0.0f;
    }

    public String getName(int idx) {
        this.rangeCheck(idx);
        int t = this.getType();
        if (t == TYPE_RGB) {
            String[] n = { "Red", "Green", "Blue" };
            return n[idx];
        }
        if (t == TYPE_GRAY) {
            return "Gray";
        }
        if (t == TYPE_XYZ) {
            String[] n = { "X", "Y", "Z" };
            return n[idx];
        }
        return super.getName(idx);
    }

    public float[] toCIEXYZ(float[] colorvalue) {
        int t = this.getType();
        if (t == TYPE_XYZ) {
            require(colorvalue, 3);
            return new float[] { colorvalue[0], colorvalue[1], colorvalue[2] };
        }
        if (t == TYPE_GRAY) {
            require(colorvalue, 1);
            float y = this.componentToLinear(0, colorvalue[0]);
            float[] white = this.profile.getMediaWhitePoint();
            return new float[] { white[0] * y, y, white[2] * y };
        }
        if (t == TYPE_RGB) {
            require(colorvalue, 3);
            float[] linear = new float[3];
            for (int i = 0; i < 3; i++) {
                linear[i] = this.componentToLinear(i, colorvalue[i]);
            }
            return multiply(this.matrixToXyz(), linear);
        }
        throw new ProfileDataException(
                "this profile is not a matrix-and-curves one: the conversion needs a table engine, "
                        + "which this library does not have");
    }

    public float[] fromCIEXYZ(float[] colorvalue) {
        int t = this.getType();
        if (t == TYPE_XYZ) {
            require(colorvalue, 3);
            return new float[] { colorvalue[0], colorvalue[1], colorvalue[2] };
        }
        if (t == TYPE_GRAY) {
            require(colorvalue, 3);
            return new float[] { this.componentFromLinear(0, colorvalue[1]) };
        }
        if (t == TYPE_RGB) {
            require(colorvalue, 3);
            float[] linear = multiply(this.matrixFromXyz(), colorvalue);
            float[] out = new float[3];
            for (int i = 0; i < 3; i++) {
                out[i] = this.componentFromLinear(i, linear[i]);
            }
            return out;
        }
        throw new ProfileDataException(
                "this profile is not a matrix-and-curves one: the conversion needs a table engine, "
                        + "which this library does not have");
    }

    /**
     * To sRGB.
     *
     * <p>It goes through CIEXYZ, unless this space is already sRGB. It is one step more than the
     * direct road and in exchange there is no second implementation that could contradict the first.
     */
    public float[] toRGB(float[] colorvalue) {
        ColorSpace srgb = ColorSpace.getInstance(CS_sRGB);
        if (this == srgb) {
            require(colorvalue, 3);
            return new float[] { colorvalue[0], colorvalue[1], colorvalue[2] };
        }
        return srgb.fromCIEXYZ(this.toCIEXYZ(colorvalue));
    }

    /** From sRGB. See {@link #toRGB}. */
    public float[] fromRGB(float[] rgbvalue) {
        ColorSpace srgb = ColorSpace.getInstance(CS_sRGB);
        if (this == srgb) {
            require(rgbvalue, 3);
            return new float[] { rgbvalue[0], rgbvalue[1], rgbvalue[2] };
        }
        return this.fromCIEXYZ(srgb.toCIEXYZ(rgbvalue));
    }

    // ---- the profile's curves ------------------------------------------------------------------
    //
    // Each component has its curve, stored as a gamma or as a table. They are read once and kept:
    // `getTRC` copies the array on every call, and doing that per pixel would be absurd.

    private void loadCurves() {
        if (this.curves != null) {
            return;
        }
        int n = this.getNumComponents();
        short[][] tables = new short[n][];
        float[] gs = new float[n];
        int[] tags = this.getType() == TYPE_GRAY
                ? new int[] { ICC_Profile.icSigGrayTRCTag }
                : new int[] { ICC_Profile.icSigRedTRCTag, ICC_Profile.icSigGreenTRCTag,
                    ICC_Profile.icSigBlueTRCTag };
        for (int i = 0; i < n && i < tags.length; i++) {
            try {
                gs[i] = this.profile.gammaOfTag(tags[i]);
                tables[i] = null;
            } catch (ProfileDataException e) {
                tables[i] = this.profile.trcOfTag(tags[i]);
                gs[i] = 0.0f;
            }
        }
        this.gammas = gs;
        this.curves = tables;
    }

    /** From an encoded value to a linear one, with that component's curve. */
    private float componentToLinear(int i, float v) {
        this.loadCurves();
        if (this.curves[i] == null) {
            if (this.gammas[i] == 1.0f) {
                return v;
            }
            return (float) Math.pow(v, this.gammas[i]);
        }
        // The table goes from linear to encoded, so for the other direction it has to be searched
        // backwards. It is done with a binary search and an interpolation, which is what gives a
        // continuous inverse without storing a second table.
        short[] t = this.curves[i];
        int target = (int) (v * 65535.0f + 0.5f);
        int lo = 0;
        int hi = t.length - 1;
        while (lo < hi - 1) {
            int mid = (lo + hi) / 2;
            if ((t[mid] & 0xFFFF) <= target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        int a = t[lo] & 0xFFFF;
        int b = t[hi] & 0xFFFF;
        float frac = b == a ? 0.0f : ((float) (target - a)) / (b - a);
        return (lo + frac) / (t.length - 1);
    }

    /** From linear to an encoded value. */
    private float componentFromLinear(int i, float v) {
        this.loadCurves();
        if (this.curves[i] == null) {
            if (this.gammas[i] == 1.0f) {
                return v;
            }
            return (float) Math.pow(v, 1.0 / this.gammas[i]);
        }
        short[] t = this.curves[i];
        float pos = v * (t.length - 1);
        if (pos <= 0) {
            return (t[0] & 0xFFFF) / 65535.0f;
        }
        if (pos >= t.length - 1) {
            return (t[t.length - 1] & 0xFFFF) / 65535.0f;
        }
        int lo = (int) pos;
        float frac = pos - lo;
        int a = t[lo] & 0xFFFF;
        int b = t[lo + 1] & 0xFFFF;
        return (a + (b - a) * frac) / 65535.0f;
    }

    private float[] matrixToXyz() {
        if (this.toXyz == null) {
            float[] r = this.profile.getXYZTag(ICC_Profile.icSigRedColorantTag);
            float[] g = this.profile.getXYZTag(ICC_Profile.icSigGreenColorantTag);
            float[] b = this.profile.getXYZTag(ICC_Profile.icSigBlueColorantTag);
            this.toXyz = new float[] {
                r[0], g[0], b[0],
                r[1], g[1], b[1],
                r[2], g[2], b[2] };
        }
        return this.toXyz;
    }

    private float[] matrixFromXyz() {
        if (this.fromXyz == null) {
            this.fromXyz = invert(this.matrixToXyz());
        }
        return this.fromXyz;
    }

    /**
     * The inverse of a 3x3 matrix, by cofactors.
     *
     * @throws ProfileDataException if it is singular -- a profile whose three primaries are coplanar
     *     describes no space, and there is no way back from XYZ
     */
    private static float[] invert(float[] m) {
        float det = m[0] * (m[4] * m[8] - m[5] * m[7])
                - m[1] * (m[3] * m[8] - m[5] * m[6])
                + m[2] * (m[3] * m[7] - m[4] * m[6]);
        if (det == 0.0f) {
            throw new ProfileDataException("the profile's matrix cannot be inverted");
        }
        float[] out = new float[9];
        out[0] = (m[4] * m[8] - m[5] * m[7]) / det;
        out[1] = (m[2] * m[7] - m[1] * m[8]) / det;
        out[2] = (m[1] * m[5] - m[2] * m[4]) / det;
        out[3] = (m[5] * m[6] - m[3] * m[8]) / det;
        out[4] = (m[0] * m[8] - m[2] * m[6]) / det;
        out[5] = (m[2] * m[3] - m[0] * m[5]) / det;
        out[6] = (m[3] * m[7] - m[4] * m[6]) / det;
        out[7] = (m[1] * m[6] - m[0] * m[7]) / det;
        out[8] = (m[0] * m[4] - m[1] * m[3]) / det;
        return out;
    }
}
