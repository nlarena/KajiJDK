package java.awt;

/**
 * A video mode: width, height, bits per pixel and refresh rate.
 *
 * <p>Four immutable integers. Two of them have a special value meaning "does not apply", and they
 * are not interchangeable: {@code BIT_DEPTH_MULTI} is -1 and says the device accepts several depths
 * at once, while {@code REFRESH_RATE_UNKNOWN} is 0 and says the rate could not be found out. A 0 in
 * the depth would be a depth of zero bits, not an "unknown".
 */
public final class DisplayMode {

    public static final int BIT_DEPTH_MULTI = -1;

    public static final int REFRESH_RATE_UNKNOWN = 0;

    private Dimension size;

    private int bitDepth;

    private int refreshRate;

    public DisplayMode(int width, int height, int bitDepth, int refreshRate) {
        this.size = new Dimension(width, height);
        this.bitDepth = bitDepth;
        this.refreshRate = refreshRate;
    }

    public int getHeight() {
        return size.height;
    }

    public int getWidth() {
        return size.width;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    /** Typed overload: avoids the cast when the other is already known to be a DisplayMode. */
    public boolean equals(DisplayMode dm) {
        if (dm == null) {
            return false;
        }
        return (getHeight() == dm.getHeight()
                && getWidth() == dm.getWidth()
                && getBitDepth() == dm.getBitDepth()
                && getRefreshRate() == dm.getRefreshRate());
    }

    public boolean equals(Object dm) {
        if (dm instanceof DisplayMode) {
            return equals((DisplayMode) dm);
        } else {
            return false;
        }
    }

    /**
     * The JDK's formula: width and height added, depth and refresh rate weighted by 7 and 13. This
     * note said the weights keep 800x600 and 600x800 from sharing a hash; they do not —width and
     * height are simply added, so those two collide— and only keep depth and refresh rate from
     * being swapped unnoticed.
     */
    public int hashCode() {
        return getWidth() + getHeight() + getBitDepth() * 7 + getRefreshRate() * 13;
    }

    public String toString() {
        return getWidth() + "x" + getHeight() + "x"
                + (getBitDepth() == BIT_DEPTH_MULTI ? "[Multi depth]" : getBitDepth() + "bpp")
                + "@"
                + (getRefreshRate() == REFRESH_RATE_UNKNOWN
                        ? "[Unknown refresh rate]" : getRefreshRate() + "Hz");
    }
}
