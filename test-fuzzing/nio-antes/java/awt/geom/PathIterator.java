package java.awt.geom;

// KajiLibrary's java.awt.geom.PathIterator -- the protocol every Shape is walked segment by
// segment with. The surface is complete: five methods and seven constants.
//
// The contract to honour when implementing it: `currentSegment` writes into `coords` as many (x,y)
// pairs as the returned type calls for -- 1 for MOVETO/LINETO, 2 for QUADTO, 3 for CUBICTO, 0 for
// CLOSE -- and does not touch the rest of the array.
public interface PathIterator {

    /** Even-odd rule: a point is inside if an odd number of segments cross it. */
    public static final int WIND_EVEN_ODD = 0;

    /** Non-zero rule: a point is inside if the sum of signed crossings is not zero. */
    public static final int WIND_NON_ZERO = 1;

    public static final int SEG_MOVETO = 0;
    public static final int SEG_LINETO = 1;
    public static final int SEG_QUADTO = 2;
    public static final int SEG_CUBICTO = 3;
    public static final int SEG_CLOSE = 4;

    public abstract int getWindingRule();

    public abstract boolean isDone();

    public abstract void next();

    public abstract int currentSegment(float[] coords);

    public abstract int currentSegment(double[] coords);
}
