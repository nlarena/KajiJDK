package java.awt.color;

/**
 * A greyscale ICC profile: a white point and a single curve.
 *
 * <p>One component instead of three, and that is why {@link #getGamma} and {@link #getTRC} take no
 * argument. The same note as in {@link ICC_ProfileRGB} applies: one of the two throws depending on
 * how the curve is stored in that profile.
 */
public final class ICC_ProfileGray extends ICC_Profile {

    private static final long serialVersionUID = -1124721290732002649L;

    ICC_ProfileGray(byte[] data) {
        super(data);
    }

    /** The medium's white point. */
    public float[] getMediaWhitePoint() {
        return super.getMediaWhitePoint();
    }

    /**
     * The curve's gamma.
     *
     * @throws ProfileDataException if the curve is a table
     */
    public float getGamma() {
        return super.getGamma(icSigGrayTRCTag);
    }

    /**
     * The curve's table.
     *
     * @throws ProfileDataException if the curve is a gamma
     */
    public short[] getTRC() {
        return super.getTRC(icSigGrayTRCTag);
    }
}
