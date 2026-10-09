package java.awt;

import java.io.Serializable;

/**
 * The already computed grid of a {@link GridBagLayout}: how many rows and columns there are, how
 * big each one is and how much weight it has.
 *
 * <p>The last grid laid out is kept, and {@link GridBagLayout#getLayoutOrigin} and company read it.
 * This note said the grid is computed once and reused for the minimum size, the preferred size and
 * the layout; here the size queries compute it again on every call.
 *
 * <p>All its fields are package-private and it has no public member: it is an intermediate result
 * of {@link GridBagLayout}, not something to work with from outside. The class is public only
 * because it appears as the type of a protected field of the layout, and a subclass has to be able
 * to name it.
 */
public final class GridBagLayoutInfo implements Serializable {

    private static final long serialVersionUID = -4899416460737170217L;

    /** How many columns the grid has. */
    int width;

    /** How many rows. */
    int height;

    /** Where the grid starts in X. */
    int startx;

    /** Where it starts in Y. */
    int starty;

    /** What each column measures. */
    int[] minWidth;

    /** What each row measures. */
    int[] minHeight;

    /** How much of the spare width each column takes. */
    double[] weightX;

    /** How much of the spare height each row takes. */
    double[] weightY;

    /** With a grid of that size. */
    GridBagLayoutInfo(int width, int height) {
        this.width = width;
        this.height = height;
        this.minWidth = new int[width];
        this.minHeight = new int[height];
        this.weightX = new double[width];
        this.weightY = new double[height];
    }
}
