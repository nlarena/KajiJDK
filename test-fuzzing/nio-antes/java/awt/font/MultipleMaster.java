package java.awt.font;

import java.awt.Font;

/**
 * A Multiple Master font: the kind that can be **interpolated** instead of picked from a list.
 *
 * <p>An ordinary family ships the light, the regular and the bold as separate files. A Multiple
 * Master ships the extremes and a rule for generating everything in between: weight 1.37 is asked
 * for and out comes a face that did not exist as a file.
 *
 * <p>The axes are the quantities that can be asked for --weight, width, optical size-- and each one
 * has its range. {@link #deriveMMFont(float[])} asks for a face by its axis values.
 */
public interface MultipleMaster {

    /** How many axes the font has. */
    int getNumDesignAxes();

    /** The minimum and maximum pairs of each axis, in order. */
    float[] getDesignAxisRanges();

    /** Each axis's value in the default face. */
    float[] getDesignAxisDefaults();

    /** Each axis's name. */
    String[] getDesignAxisNames();

    /**
     * A face with those axis values.
     *
     * @throws IllegalArgumentException if some value falls outside its range
     */
    Font deriveMMFont(float[] axes);

    /**
     * A face asked for by quantities rather than by axes.
     *
     * <p>It is the shortcut for when the font's axes are not known: what is wanted is declared and
     * the font translates it into whichever axes it has.
     */
    Font deriveMMFont(float[] glyphWidths, float avgStemWidth, float typicalCapHeight,
            float typicalXHeight, float italicAngle);
}
