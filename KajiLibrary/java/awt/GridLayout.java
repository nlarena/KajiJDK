package java.awt;

import java.io.Serializable;

/**
 * Distributes the children in a grid of cells that are **all equal**.
 *
 * <p>Each child's preferred size is not respected: they all end up the same size, the cell's. That
 * is what makes it so useful for a button pad and so bad for a form.
 *
 * <p>Setting one of the two dimensions to **zero** means "as many as needed": with three rows and
 * zero columns, the columns come from how many children there are. Both at zero is an error,
 * because there would be nowhere to get either from.
 *
 * <p>When both are given, **the rows win**: if there are more children than cells, columns are
 * added and the number of rows is respected.
 */
public class GridLayout implements LayoutManager, Serializable {

    private static final long serialVersionUID = -7411804673224730901L;

    private int rows;
    private int cols;
    private int hgap;
    private int vgap;

    /** A single row, with as many columns as children. */
    public GridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     * With that number of rows and columns, and no gap.
     *
     * @throws IllegalArgumentException if both are zero
     */
    public GridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * With rows, columns and gaps.
     *
     * @throws IllegalArgumentException if both dimensions are zero: there would be nowhere to
     *     deduce either from
     */
    public GridLayout(int rows, int cols, int hgap, int vgap) {
        if (rows == 0 && cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /** How many rows, or 0 if they are deduced. */
    public int getRows() {
        return this.rows;
    }

    /**
     * Changes the number of rows.
     *
     * @throws IllegalArgumentException if it becomes zero while the columns are zero too
     */
    public void setRows(int rows) {
        if (rows == 0 && this.cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    /** How many columns, or 0 if they are deduced. */
    public int getColumns() {
        return this.cols;
    }

    /**
     * Changes the number of columns.
     *
     * @throws IllegalArgumentException if it becomes zero while the rows are zero too
     */
    public void setColumns(int cols) {
        if (cols == 0 && this.rows == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    /** How much they are separated horizontally. */
    public int getHgap() {
        return this.hgap;
    }

    /** Changes the horizontal gap. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** How much they are separated vertically. */
    public int getVgap() {
        return this.vgap;
    }

    /** Changes the vertical gap. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /** It does nothing: this layout keeps nothing per child. */
    public void addLayoutComponent(String name, Component comp) {
    }

    /** It does nothing, for the same reason. */
    public void removeLayoutComponent(Component comp) {
    }

    /** The largest cell, multiplied by the grid. */
    public Dimension preferredLayoutSize(Container parent) {
        return this.measure(parent, true);
    }

    /** The same, with the minimum sizes. */
    public Dimension minimumLayoutSize(Container parent) {
        return this.measure(parent, false);
    }

    /** All cells measure what the largest does, so the maximum is found and multiplied. */
    private Dimension measure(Container parent, boolean preferred) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = this.rows;
            int ncols = this.cols;
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            int w = 0;
            int h = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = preferred ? comp.getPreferredSize() : comp.getMinimumSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
            return new Dimension(
                    insets.left + insets.right + ncols * w + (ncols - 1) * this.hgap,
                    insets.top + insets.bottom + nrows * h + (nrows - 1) * this.vgap);
        }
    }

    /**
     * Divides the space into equal cells and puts one child in each.
     *
     * <p>The remainder of the division is split between the two sides, so the grid is centred in
     * the container, as in the JDK. This javadoc said it was handed out a pixel at a time among the
     * first cells; nothing here does that.
     */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = this.rows;
            int ncols = this.cols;
            if (ncomponents == 0) {
                return;
            }
            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            int totalGapsWidth = (ncols - 1) * this.hgap;
            int widthWOInsets = parent.getWidth() - (insets.left + insets.right);
            int widthOnComponent = (widthWOInsets - totalGapsWidth) / ncols;
            int extraWidthAvailable = (widthWOInsets - (widthOnComponent * ncols
                    + totalGapsWidth)) / 2;
            int totalGapsHeight = (nrows - 1) * this.vgap;
            int heightWOInsets = parent.getHeight() - (insets.top + insets.bottom);
            int heightOnComponent = (heightWOInsets - totalGapsHeight) / nrows;
            int extraHeightAvailable = (heightWOInsets - (heightOnComponent * nrows
                    + totalGapsHeight)) / 2;
            boolean ltr = parent.getComponentOrientation().isLeftToRight();
            int x = insets.left + extraWidthAvailable;
            if (!ltr) {
                x = parent.getWidth() - insets.right - widthOnComponent - extraWidthAvailable;
            }
            for (int c = 0; c < ncols; c++) {
                int y = insets.top + extraHeightAvailable;
                for (int r = 0; r < nrows; r++) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, widthOnComponent,
                                heightOnComponent);
                    }
                    y = y + heightOnComponent + this.vgap;
                }
                if (ltr) {
                    x = x + widthOnComponent + this.hgap;
                } else {
                    x = x - widthOnComponent - this.hgap;
                }
            }
        }
    }

    public String toString() {
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + ",rows="
                + this.rows + ",cols=" + this.cols + "]";
    }
}
