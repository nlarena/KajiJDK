package java.awt.color;

/**
 * Builds the built-in ICC profiles out of the standard's constants.
 *
 * <p>The JDK ships them as resource files; here they are constructed. It is not an imitation: what
 * comes out are valid ICC.1 profiles --a 128-byte header, a tag table, well-formed `XYZType` and
 * `curveType` tags-- that another program can read. The numbers are the standard's: the sRGB matrix
 * adapted to D50 by Bradford, IEC 61966-2-1's curve, the D50 and D65 white points.
 *
 * <p>Package-private: how they are assembled is {@link ICC_Profile#getInstance(int)}'s business, not
 * the contract's.
 *
 * <p>Each profile is assembled **once** and shared, because `getInstance(int)` promises to return the
 * same object every time -- {@link ICC_Profile#readResolve} depends on that.
 */
final class BuiltinProfiles {

    private BuiltinProfiles() {
    }

    // The four-character signatures that are needed and that `ICC_Profile` does not declare as public
    // constants because they are not part of its API.
    private static final int SIG_XYZ_TYPE = signature("XYZ ");
    private static final int SIG_CURVE_TYPE = signature("curv");
    private static final int SIG_TEXT_TYPE = signature("text");
    private static final int SIG_DESC_TYPE = signature("desc");

    /** The four characters of a signature, packed into a big-endian `int`. */
    private static int signature(String s) {
        return ((s.charAt(0) & 0xFF) << 24) | ((s.charAt(1) & 0xFF) << 16)
                | ((s.charAt(2) & 0xFF) << 8) | (s.charAt(3) & 0xFF);
    }

    // The sRGB-to-XYZ matrix with a D50 white, by COLUMNS: each one is what a primary contributes.
    // It is the same one `ColorSpace` uses, transposed, because ICC stores one column per tag.
    private static final float[] COL_ROJO = { 0.4360747f, 0.2225045f, 0.0139322f };
    private static final float[] COL_VERDE = { 0.3850649f, 0.7168786f, 0.0971045f };
    private static final float[] COL_AZUL = { 0.1430804f, 0.0606169f, 0.7141733f };

    /** D50, the illuminant ICC fixes for the connection space. */
    private static final float[] D50 = { 0.9642f, 1.0f, 0.8249f };

    /** D65, an sRGB monitor's white. It is what goes in `wtpt`, not D50. */
    private static final float[] D65 = { 0.9505f, 1.0f, 1.0891f };

    /**
     * The white the matrix implies: the sum of its three columns.
     *
     * <p>It is the colour that comes out of the three primaries at maximum, that is, the space's
     * white. It is computed instead of written out so that it goes on matching if the matrix ever
     * changes.
     */
    private static float[] matrixWhite() {
        return new float[] {
            COL_ROJO[0] + COL_VERDE[0] + COL_AZUL[0],
            COL_ROJO[1] + COL_VERDE[1] + COL_AZUL[1],
            COL_ROJO[2] + COL_VERDE[2] + COL_AZUL[2] };
    }

    private static ICC_Profile srgb;
    private static ICC_Profile linear;
    private static ICC_Profile gray;
    private static ICC_Profile xyz;

    static synchronized ICC_Profile sRGB() {
        if (srgb == null) {
            // The curve goes as a 1024-point TABLE, just as in the JDK: sRGB's is piecewise --a
            // straight stretch near zero and a power afterwards-- and a lone gamma does not describe
            // it. That is why this profile's `getGamma` throws and `getTRC` answers.
            byte[][] cuerpos = {
                xyzTag(COL_ROJO), xyzTag(COL_VERDE), xyzTag(COL_AZUL),
                tableCurve(1024), tableCurve(1024), tableCurve(1024),
                xyzTag(D65), textTag("KajiJDK sRGB"), descTag("sRGB integrado") };
            int[] firmas = {
                ICC_Profile.icSigRedColorantTag, ICC_Profile.icSigGreenColorantTag,
                ICC_Profile.icSigBlueColorantTag, ICC_Profile.icSigRedTRCTag,
                ICC_Profile.icSigGreenTRCTag, ICC_Profile.icSigBlueTRCTag,
                ICC_Profile.icSigMediaWhitePointTag, ICC_Profile.icSigCopyrightTag,
                ICC_Profile.icSigProfileDescriptionTag };
            srgb = ICC_Profile.getInstance(ICC_Profile.assemble(
                    header(ICC_Profile.icSigDisplayClass, ICC_Profile.icSigRgbData),
                    firmas, cuerpos, firmas.length));
        }
        return srgb;
    }

    static synchronized ICC_Profile linearRGB() {
        if (linear == null) {
            // Gamma 1.0 as a single value, not as a table: here `getGamma` answers and `getTRC`
            // throws. It is the reverse split from sRGB's and it is the one the JDK has.
            byte[][] cuerpos = {
                xyzTag(COL_ROJO), xyzTag(COL_VERDE), xyzTag(COL_AZUL),
                gammaCurve(1.0f), gammaCurve(1.0f), gammaCurve(1.0f),
                xyzTag(D65), textTag("KajiJDK Linear RGB"), descTag("RGB linear integrado") };
            int[] firmas = {
                ICC_Profile.icSigRedColorantTag, ICC_Profile.icSigGreenColorantTag,
                ICC_Profile.icSigBlueColorantTag, ICC_Profile.icSigRedTRCTag,
                ICC_Profile.icSigGreenTRCTag, ICC_Profile.icSigBlueTRCTag,
                ICC_Profile.icSigMediaWhitePointTag, ICC_Profile.icSigCopyrightTag,
                ICC_Profile.icSigProfileDescriptionTag };
            linear = ICC_Profile.getInstance(ICC_Profile.assemble(
                    header(ICC_Profile.icSigDisplayClass, ICC_Profile.icSigRgbData),
                    firmas, cuerpos, firmas.length));
        }
        return linear;
    }

    static synchronized ICC_Profile gray() {
        if (gray == null) {
            // The grey profile's white is the SUM OF THE COLUMNS of the RGB matrix, not the
            // tabulated D50. The two numbers differ in the third figure --0.82521 against 0.8249--
            // because the published sRGB matrix rounds, and that difference is not harmless: a grey
            // converted to sRGB by way of XYZ came out with the blue one step below the red and the
            // green, that is, a grey with a tint. By making the two built-in profiles share the same
            // white, the round trip is exact and a grey stays grey.
            byte[][] cuerpos = {
                gammaCurve(1.0f), xyzTag(matrixWhite()),
                textTag("KajiJDK Gray"), descTag("gris integrado") };
            int[] firmas = {
                ICC_Profile.icSigGrayTRCTag, ICC_Profile.icSigMediaWhitePointTag,
                ICC_Profile.icSigCopyrightTag, ICC_Profile.icSigProfileDescriptionTag };
            gray = ICC_Profile.getInstance(ICC_Profile.assemble(
                    header(ICC_Profile.icSigDisplayClass, ICC_Profile.icSigGrayData),
                    firmas, cuerpos, firmas.length));
        }
        return gray;
    }

    static synchronized ICC_Profile ciexyz() {
        if (xyz == null) {
            // An abstract profile: its device space IS the connection space, so it carries neither
            // matrix nor curves -- there is nothing to convert.
            byte[][] cuerpos = {
                xyzTag(D50), textTag("KajiJDK CIEXYZ"), descTag("CIEXYZ integrado") };
            int[] firmas = {
                ICC_Profile.icSigMediaWhitePointTag, ICC_Profile.icSigCopyrightTag,
                ICC_Profile.icSigProfileDescriptionTag };
            xyz = ICC_Profile.getInstance(ICC_Profile.assemble(
                    header(ICC_Profile.icSigAbstractClass, ICC_Profile.icSigXYZData),
                    firmas, cuerpos, firmas.length));
        }
        return xyz;
    }

    /**
     * The 128-byte header.
     *
     * <p>The total size is left at zero: {@link ICC_Profile#assemble} writes it once it knows how much
     * the tags take up.
     */
    private static byte[] header(int claseDePerfil, int espacio) {
        byte[] h = new byte[128];
        escribirInt(h, ICC_Profile.icHdrCmmId, signature("Kaji"));
        // Version 2.4.0, which is the one the JDK's profiles declare.
        escribirInt(h, ICC_Profile.icHdrVersion, 0x02400000);
        escribirInt(h, ICC_Profile.icHdrDeviceClass, claseDePerfil);
        escribirInt(h, ICC_Profile.icHdrColorSpace, espacio);
        // The connection space is always XYZ here: it is the one the conversions use as their axis.
        escribirInt(h, ICC_Profile.icHdrPcs, SIG_XYZ_TYPE);
        escribirInt(h, ICC_Profile.icHdrMagic, signature("acsp"));
        escribirInt(h, ICC_Profile.icHdrPlatform, 0);
        escribirInt(h, ICC_Profile.icHdrRenderingIntent, ICC_Profile.icPerceptual);
        // The header's illuminant is fixed by the standard at D50, not at the medium's white.
        escribirXyz(h, ICC_Profile.icHdrIlluminant, D50);
        return h;
    }

    /** An `XYZType` tag: signature, reserved and three s15Fixed16. */
    private static byte[] xyzTag(float[] v) {
        byte[] t = new byte[20];
        escribirInt(t, 0, SIG_XYZ_TYPE);
        escribirXyz(t, 8, v);
        return t;
    }

    /** A single-valued `curveType`: the gamma, in u8Fixed8. */
    private static byte[] gammaCurve(float gamma) {
        byte[] t = new byte[14];
        escribirInt(t, 0, SIG_CURVE_TYPE);
        escribirInt(t, ICC_Profile.icCurveCount, 1);
        escribirShort(t, ICC_Profile.icCurveData, (int) (gamma * 256.0f + 0.5f));
        return t;
    }

    /**
     * A `curveType` as a table of `n` points with sRGB's curve.
     *
     * <p>The table goes from linear to with-gamma, which is the direction ICC defines it in: entry
     * `i` is the encoded value corresponding to the luminance `i/(n-1)`.
     */
    private static byte[] tableCurve(int n) {
        byte[] t = new byte[ICC_Profile.icCurveData + n * 2];
        escribirInt(t, 0, SIG_CURVE_TYPE);
        escribirInt(t, ICC_Profile.icCurveCount, n);
        for (int i = 0; i < n; i++) {
            float linear = ((float) i) / (n - 1);
            float withGamma = ColorSpace.toGamma(linear);
            int v = (int) (withGamma * 65535.0f + 0.5f);
            if (v < 0) {
                v = 0;
            }
            if (v > 65535) {
                v = 65535;
            }
            escribirShort(t, ICC_Profile.icCurveData + i * 2, v);
        }
        return t;
    }

    private static byte[] textTag(String s) {
        byte[] t = new byte[8 + s.length() + 1];
        escribirInt(t, 0, SIG_TEXT_TYPE);
        for (int i = 0; i < s.length(); i++) {
            t[8 + i] = (byte) s.charAt(i);
        }
        return t;
    }

    /** A `descType`: signature, reserved, length including the null, and the text. */
    private static byte[] descTag(String s) {
        byte[] t = new byte[12 + s.length() + 1 + 78];
        escribirInt(t, 0, SIG_DESC_TYPE);
        escribirInt(t, 8, s.length() + 1);
        for (int i = 0; i < s.length(); i++) {
            t[12 + i] = (byte) s.charAt(i);
        }
        return t;
    }

    private static void escribirXyz(byte[] b, int off, float[] v) {
        for (int i = 0; i < 3; i++) {
            escribirInt(b, off + i * 4, (int) (v[i] * 65536.0f + 0.5f));
        }
    }

    private static void escribirInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 24);
        b[off + 1] = (byte) (v >> 16);
        b[off + 2] = (byte) (v >> 8);
        b[off + 3] = (byte) v;
    }

    private static void escribirShort(byte[] b, int off, int v) {
        b[off] = (byte) (v >> 8);
        b[off + 1] = (byte) v;
    }
}
