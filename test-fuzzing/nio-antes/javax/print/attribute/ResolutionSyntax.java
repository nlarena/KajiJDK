package javax.print.attribute;

import java.io.Serializable;

// The syntax class of the attributes that are a print resolution: two numbers, one across the paper
// (cross feed) and another along it (feed).
//
// Inside everything is kept in **dphi** -- dots per hundred inches --, which is an integer, so that
// the comparison is exact and does not depend on which unit it was built in. The DPI and DPCM
// constants are precisely the conversion factor to dphi: 100 dphi = 1 dpi, 254 dphi = 1 dpcm. That
// is what makes `new R(300, 300, DPI)` and `new R(300, 300, DPI)` equal without floating point.
//
// Unlike the rest of the syntax classes, its constructor is **public**, not protected.
public abstract class ResolutionSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = 2706743076526672017L;

    private int crossFeedResolution;
    private int feedResolution;

    // The two factors to dphi. They are not an enum: they are the number one multiplies by.
    public static final int DPI = 100;
    public static final int DPCM = 254;

    public ResolutionSyntax(int crossFeedResolution, int feedResolution, int units) {
        if (crossFeedResolution < 1) {
            throw new IllegalArgumentException("crossFeedResolution is < 1");
        }
        if (feedResolution < 1) {
            throw new IllegalArgumentException("feedResolution is < 1");
        }
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        this.crossFeedResolution = crossFeedResolution * units;
        this.feedResolution = feedResolution * units;
    }

    // Back from dphi to the requested unit, rounding to the nearest integer.
    private static int convertFromDphi(int dphi, int units) {
        if (units < 1) {
            throw new IllegalArgumentException(": units is < 1");
        }
        int round = units / 2;
        return (dphi + round) / units;
    }

    // The two numbers together: [cross feed, feed].
    public int[] getResolution(int units) {
        int[] result = new int[2];
        result[0] = getCrossFeedResolution(units);
        result[1] = getFeedResolution(units);
        return result;
    }

    public int getCrossFeedResolution(int units) {
        return convertFromDphi(this.crossFeedResolution, units);
    }

    public int getFeedResolution(int units) {
        return convertFromDphi(this.feedResolution, units);
    }

    // "300x600 dpi". With a null `unitsName` the suffix and the space are omitted.
    public String toString(int units, String unitsName) {
        StringBuilder result = new StringBuilder();
        result.append(getCrossFeedResolution(units));
        result.append('x');
        result.append(getFeedResolution(units));
        if (unitsName != null) {
            result.append(' ');
            result.append(unitsName);
        }
        return result.toString();
    }

    // A partial order, not a total one: it asks that **both** components be less or equal. Two
    // resolutions such as 300x600 and 600x300 are not ordered with each other in either direction.
    public boolean lessThanOrEquals(ResolutionSyntax other) {
        if (other == null) {
            throw new NullPointerException("other is null");
        }
        return this.crossFeedResolution <= other.crossFeedResolution
                && this.feedResolution <= other.feedResolution;
    }

    public boolean equals(Object object) {
        if (!(object instanceof ResolutionSyntax)) {
            return false;
        }
        ResolutionSyntax other = (ResolutionSyntax) object;
        return this.crossFeedResolution == other.crossFeedResolution
                && this.feedResolution == other.feedResolution;
    }

    // The low 16 bits of each component, packed. It collides for huge resolutions, but it is the
    // JDK's and has to be replicated: a different hash breaks any shared table.
    public int hashCode() {
        return (this.crossFeedResolution & 0x0000FFFF)
                | ((this.feedResolution & 0x0000FFFF) << 16);
    }

    // In dphi, the internal unit: "30000x60000 dphi".
    public String toString() {
        return toString(1, "dphi");
    }

    protected int getCrossFeedResolutionDphi() {
        return this.crossFeedResolution;
    }

    protected int getFeedResolutionDphi() {
        return this.feedResolution;
    }
}
