package java.awt.color;

/**
 * An ICC profile of an RGB space with a matrix and curves.
 *
 * <p>It is the commonest form of monitor profile and the simplest to apply: converting a colour to
 * XYZ means putting it through the three curves and multiplying it by the 3x3 matrix. The profiles
 * that instead carry interpolation tables are **not** of this class --{@link ICC_Profile#getInstance}
 * returns a plain `ICC_Profile`-- and that is why the three methods here can promise an answer.
 *
 * <p>The exception is {@link #getGamma} against {@link #getTRC}: a curve is stored **either** as a
 * gamma number **or** as a table, never as both, so one of the two methods throws depending on how
 * that profile is. It is not a defect: it is the format, and asking a profile with a table what its
 * gamma is has no answer.
 */
public final class ICC_ProfileRGB extends ICC_Profile {

    private static final long serialVersionUID = 8505067385152579334L;

    /** The red component. */
    public static final int REDCOMPONENT = 0;
    /** The green component. */
    public static final int GREENCOMPONENT = 1;
    /** The blue component. */
    public static final int BLUECOMPONENT = 2;

    ICC_ProfileRGB(byte[] data) {
        super(data);
    }

    /** The medium's white point. In an sRGB monitor profile it is D65, not D50. */
    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }

    /**
     * The 3x3 matrix that takes linear RGB to XYZ.
     *
     * <p>`m[row][column]`: column 0 is what the red contributes, 1 the green and 2 the blue. It comes
     * from the three colorant tags, which is how ICC stores it -- one column per tag.
     *
     * @throws ProfileDataException if the profile is missing any of the three
     */
    public float[][] getMatrix() {
        float[] r = this.getXYZTag(icSigRedColorantTag);
        float[] g = this.getXYZTag(icSigGreenColorantTag);
        float[] b = this.getXYZTag(icSigBlueColorantTag);
        float[][] m = new float[3][3];
        for (int row = 0; row < 3; row++) {
            m[row][0] = r[row];
            m[row][1] = g[row];
            m[row][2] = b[row];
        }
        return m;
    }

    /**
     * That component's gamma.
     *
     * @throws ProfileDataException if the curve is a table; see the class's note
     * @throws IllegalArgumentException if the component is not one of the three
     */
    public float getGamma(int component) {
        return super.getGamma(tagFor(component));
    }

    /**
     * That component's table.
     *
     * @throws ProfileDataException if the curve is a gamma; see the class's note
     * @throws IllegalArgumentException if the component is not one of the three
     */
    public short[] getTRC(int component) {
        return super.getTRC(tagFor(component));
    }

    private static int tagFor(int component) {
        if (component == REDCOMPONENT) {
            return icSigRedTRCTag;
        }
        if (component == GREENCOMPONENT) {
            return icSigGreenTRCTag;
        }
        if (component == BLUECOMPONENT) {
            return icSigBlueTRCTag;
        }
        throw new IllegalArgumentException("Must be Red, Green, or Blue");
    }
}
