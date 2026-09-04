package java.awt;

import java.io.Serializable;

/**
 * Reparte a los hijos en una grilla de celdas **todas iguales**.
 *
 * <p>La medida preferida de cada hijo no se respeta: todos quedan del mismo tamaño, el de la celda.
 * Es lo que la hace tan útil para una botonera y tan mala para un formulario.
 *
 * <p>Poner una de las dos dimensiones en **cero** significa "la que haga falta": con tres filas y
 * cero columnas, las columnas salen de cuántos hijos haya. Las dos en cero es un error, porque no
 * habría de dónde sacar ninguna.
 *
 * <p>Cuando las dos están dadas, **las filas ganan**: si hay más hijos que celdas, se agregan
 * columnas y la cantidad de filas se respeta.
 */
public class GridLayout implements LayoutManager, Serializable {

    private static final long serialVersionUID = -7411804673224730901L;

    private int rows;
    private int cols;
    private int hgap;
    private int vgap;

    /** Una sola fila, con tantas columnas como hijos. */
    public GridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     * Con esa cantidad de filas y columnas, sin separación.
     *
     * @throws IllegalArgumentException si las dos son cero
     */
    public GridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * Con filas, columnas y separaciones.
     *
     * @throws IllegalArgumentException si las dos dimensiones son cero: no habría de dónde deducir
     *     ninguna
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

    /** Cuántas filas, o 0 si se deducen. */
    public int getRows() {
        return this.rows;
    }

    /**
     * Cambia la cantidad de filas.
     *
     * @throws IllegalArgumentException si queda en cero y las columnas también
     */
    public void setRows(int rows) {
        if (rows == 0 && this.cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    /** Cuántas columnas, o 0 si se deducen. */
    public int getColumns() {
        return this.cols;
    }

    /**
     * Cambia la cantidad de columnas.
     *
     * @throws IllegalArgumentException si queda en cero y las filas también
     */
    public void setColumns(int cols) {
        if (cols == 0 && this.rows == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    /** Cuánto se separan horizontalmente. */
    public int getHgap() {
        return this.hgap;
    }

    /** Cambia la separación horizontal. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** Cuánto se separan verticalmente. */
    public int getVgap() {
        return this.vgap;
    }

    /** Cambia la separación vertical. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /** No hace nada: esta distribución no guarda nada por hijo. */
    public void addLayoutComponent(String name, Component comp) {
    }

    /** No hace nada, por el mismo motivo. */
    public void removeLayoutComponent(Component comp) {
    }

    /** La celda más grande, multiplicada por la grilla. */
    public Dimension preferredLayoutSize(Container parent) {
        return this.medir(parent, true);
    }

    /** Lo mismo, con las medidas mínimas. */
    public Dimension minimumLayoutSize(Container parent) {
        return this.medir(parent, false);
    }

    /** Todas las celdas miden lo que la mayor, así que se busca el máximo y se multiplica. */
    private Dimension medir(Container parent, boolean preferida) {
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
                Dimension d = preferida ? comp.getPreferredSize() : comp.getMinimumSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
            return new Dimension(
                    insets.left + insets.right + ncols * w + (ncols - 1) * this.hgap,
                    insets.top + insets.bottom + nrows * h + (nrows - 1) * this.vgap);
        }
    }

    /**
     * Reparte el espacio en celdas iguales y pone un hijo en cada una.
     *
     * <p>El sobrante de la división se reparte entre las primeras celdas, de a un píxel: sin eso, la
     * última columna quedaría más angosta y se vería.
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
