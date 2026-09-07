package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * Una vista de tabla: filas de celdas alineadas en columnas.
 *
 * <h2>El problema que resuelve</h2>
 *
 * <p>Una tabla no es una caja de cajas. Si cada fila se maquetara sola, las celdas de una columna
 * quedarian de distinto ancho. El ancho lo decide la <em>tabla</em>, mirando todas las filas a la
 * vez, y despues se lo impone a cada fila.
 *
 * <h2>Celdas que ocupan varias columnas</h2>
 *
 * <p>Una celda puede abarcar varias columnas o varias filas. Eso rompe la cuenta simple de "el
 * ancho de la columna es el maximo de sus celdas": una celda que abarca tres columnas no manda
 * sobre ninguna de las tres por si sola. Aca se hace lo mismo que en el JDK: las celdas de una sola
 * columna fijan los anchos, y despues se reparte lo que falte entre las columnas que una celda
 * ancha necesite.
 *
 * <p>Las filas ocupadas por una celda que abarca varias se anotan en un {@link BitSet} por columna,
 * asi la fila de abajo sabe que esa columna ya esta tomada y no pone nada ahi.
 *
 * <h2>Es abstracta a proposito</h2>
 *
 * <p>No sabe de que elementos sale una tabla: eso depende del formato. Quien la use dice cuales
 * elementos son filas sobrescribiendo {@link #createTableRow}.
 */
public abstract class TableView extends BoxView {

    int[] columnSpans;
    int[] columnOffsets;
    SizeRequirements[] columnRequirements;
    Vector<TableRow> rows;
    boolean gridValid;
    static final BitSet EMPTY = new BitSet();

    /** Una tabla sobre ese elemento; las filas son sus hijos. */
    public TableView(Element elem) {
        super(elem, View.Y_AXIS);
        rows = new Vector<TableRow>();
        gridValid = false;
    }

    /** Una fila; sobrescribir para decidir que elementos son filas. */
    protected TableRow createTableRow(Element elem) {
        return new TableRow(this, elem);
    }

    /**
     * Una celda.
     *
     * @deprecated Las celdas ahora salen de la fabrica de vistas, como cualquier otra vista.
     */
    @Deprecated
    protected TableCell createTableCell(Element elem) {
        return new TableCell(this, elem);
    }

    int getColumnCount() {
        return columnSpans.length;
    }

    /** Donde empieza esa columna, en coordenadas de la tabla. */
    int getColumnOffset(int col) {
        return columnOffsets[col];
    }

    int getColumnSpan(int col) {
        return columnSpans[col];
    }

    int getRowCount() {
        return rows.size();
    }

    int getRowOffset(int row) {
        return getOffset(Y_AXIS, row);
    }

    int getRowSpan(int row) {
        return getSpan(Y_AXIS, row);
    }

    TableRow getRow(int row) {
        if (row < rows.size()) {
            return rows.elementAt(row);
        }
        return null;
    }

    int getRow(TableRow row) {
        return rows.indexOf(row);
    }

    /**
     * Rearma la grilla: cuenta columnas y ubica cada celda.
     *
     * <p>Se hace en dos pasadas porque no se sabe cuantas columnas hay hasta haber mirado todas las
     * filas, y no se puede ubicar una celda sin saber cuantas columnas hay.
     */
    void updateGrid() {
        if (!gridValid) {
            // Primera pasada: contar columnas.
            rows.removeAllElements();
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                if (v instanceof TableRow) {
                    rows.addElement((TableRow) v);
                }
            }

            int maxColumns = 0;
            int nrows = rows.size();
            for (int row = 0; row < nrows; row++) {
                TableRow rv = getRow(row);
                int col = 0;
                for (int cell = 0; cell < rv.getViewCount(); cell++) {
                    View cv = rv.getView(cell);
                    col = col + getColumnsOccupied(cv);
                }
                maxColumns = Math.max(col, maxColumns);
            }

            int[] columnCounts = new int[nrows];

            // Segunda pasada: ubicar cada celda salteando lo que ya ocupa una celda alta.
            for (int row = 0; row < nrows; row++) {
                TableRow rv = getRow(row);
                rv.clearFilledColumns();
                rv.setRow(row);
                int col = 0;
                for (int cell = 0; cell < rv.getViewCount(); cell++, col++) {
                    View cv = rv.getView(cell);
                    for (; rv.isFilled(col); col++) {
                        // Ya la ocupa una celda que baja de una fila de arriba.
                    }
                    int rowSpan = getRowsOccupied(cv);
                    int colSpan = getColumnsOccupied(cv);
                    if ((colSpan > 1) || (rowSpan > 1)) {
                        rv.fillColumns(col, colSpan, rowSpan);
                        for (int i = 0; i < rowSpan; i++) {
                            if ((row + i) < nrows) {
                                TableRow rv2 = getRow(row + i);
                                rv2.fillColumns(col, colSpan, rowSpan - i);
                            }
                        }
                        col = col + colSpan - 1;
                    }
                    columnCounts[row] = col + 1;
                }
                maxColumns = Math.max(maxColumns, columnCounts[row]);
            }

            columnSpans = new int[maxColumns];
            columnOffsets = new int[maxColumns];
            columnRequirements = new SizeRequirements[maxColumns];
            for (int i = 0; i < maxColumns; i++) {
                columnRequirements[i] = new SizeRequirements();
            }
            gridValid = true;
        }
    }

    /** Cuantas columnas ocupa esa celda. */
    protected int getColumnsOccupied(View v) {
        return 1;
    }

    /** Cuantas filas ocupa esa celda. */
    protected int getRowsOccupied(View v) {
        return 1;
    }

    protected void invalidateGrid() {
        gridValid = false;
    }

    protected void forwardUpdate(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a,
            ViewFactory f) {
        super.forwardUpdate(ec, e, a, f);
        // Un cambio en una fila puede cambiar toda la tabla, no solo esa fila.
        if (a != null) {
            Rectangle alloc = a.getBounds();
            java.awt.Component c = getContainer();
            if (c != null) {
                c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
            }
        }
    }

    public void replace(int offset, int length, View[] views) {
        super.replace(offset, length, views);
        invalidateGrid();
    }

    /**
     * Reparte el ancho entre las columnas.
     *
     * <p>El reparto es proporcional a lo que cada columna prefiere, con el minimo como piso: dar de
     * menos a una columna la haria mostrar texto cortado.
     */
    protected void layoutColumns(int targetSpan, int[] offsets, int[] spans,
            SizeRequirements[] reqs) {
        SizeRequirements.calculateTiledPositions(targetSpan, null, reqs, offsets, spans);
    }

    /** Maqueta el eje menor: primero las columnas, despues cada fila con esos anchos. */
    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        updateGrid();
        calculateColumnRequirements(axis);
        layoutColumns(targetSpan, columnOffsets, columnSpans, columnRequirements);
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            if (v instanceof TableRow) {
                TableRow row = (TableRow) v;
                row.layoutChanged(axis);
            }
            spans[i] = targetSpan;
            offsets[i] = 0;
        }
    }

    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        updateGrid();
        calculateColumnRequirements(axis);
        if (r == null) {
            r = new SizeRequirements();
        }
        long min = 0;
        long pref = 0;
        int n = columnRequirements.length;
        for (int i = 0; i < n; i++) {
            SizeRequirements req = columnRequirements[i];
            min = min + req.minimum;
            pref = pref + req.preferred;
        }
        r.minimum = (int) min;
        r.preferred = (int) pref;
        r.maximum = Integer.MAX_VALUE;
        r.alignment = 0;
        return r;
    }

    /**
     * Junta lo que pide cada columna.
     *
     * <p>Las celdas anchas se dejan para el final: primero se sabe cuanto piden las columnas por su
     * cuenta, y recien despues se ve si a una celda ancha le falta lugar. Al reves, una celda ancha
     * inflaria la primera columna que toque.
     */
    void calculateColumnRequirements(int axis) {
        for (int i = 0; i < columnRequirements.length; i++) {
            SizeRequirements req = columnRequirements[i];
            req.minimum = 0;
            req.preferred = 0;
            req.maximum = Integer.MAX_VALUE;
        }

        Vector<View> multiColumn = new Vector<View>();
        int nrows = getRowCount();
        for (int i = 0; i < nrows; i++) {
            TableRow row = getRow(i);
            int col = 0;
            int ncells = row.getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = row.getView(cell);
                for (; row.isFilled(col); col++) {
                    // Columna tomada por una celda de otra fila.
                }
                int rowSpan = getRowsOccupied(cv);
                int colSpan = getColumnsOccupied(cv);
                if (colSpan == 1) {
                    checkSingleColumnCell(axis, col, cv);
                } else {
                    multiColumn.addElement(cv);
                    col = col + colSpan - 1;
                }
            }
        }

        for (int i = 0; i < multiColumn.size(); i++) {
            View cv = multiColumn.elementAt(i);
            int col = getCellColumn(cv);
            if (col >= 0) {
                checkMultiColumnCell(axis, col, getColumnsOccupied(cv), cv);
            }
        }
    }

    private int getCellColumn(View cv) {
        View parent = cv.getParent();
        if (!(parent instanceof TableRow)) {
            return -1;
        }
        TableRow row = (TableRow) parent;
        int col = 0;
        int ncells = row.getViewCount();
        for (int cell = 0; cell < ncells; cell++, col++) {
            for (; row.isFilled(col); col++) {
                // Igual que arriba.
            }
            if (row.getView(cell) == cv) {
                return col;
            }
            col = col + getColumnsOccupied(row.getView(cell)) - 1;
        }
        return -1;
    }

    /** Una celda de una sola columna manda directamente sobre esa columna. */
    void checkSingleColumnCell(int axis, int col, View v) {
        SizeRequirements req = columnRequirements[col];
        req.minimum = Math.max((int) v.getMinimumSpan(axis), req.minimum);
        req.preferred = Math.max((int) v.getPreferredSpan(axis), req.preferred);
    }

    /**
     * Reparte lo que le falta a una celda ancha entre las columnas que abarca.
     *
     * <p>Solo agrega lo que falta: si las columnas ya suman lo suficiente, la celda no cambia nada.
     */
    void checkMultiColumnCell(int axis, int col, int ncols, View v) {
        long min = 0;
        long pref = 0;
        long max = 0;
        for (int i = 0; i < ncols; i++) {
            SizeRequirements req = columnRequirements[col + i];
            min = min + req.minimum;
            pref = pref + req.preferred;
            max = max + req.maximum;
        }

        int cmin = (int) v.getMinimumSpan(axis);
        int cpref = (int) v.getPreferredSpan(axis);
        long cmax = (long) v.getMaximumSpan(axis);

        if (cmin > min) {
            expandColumns(col, ncols, (int) (cmin - min), true);
        }
        if (cpref > pref) {
            expandColumns(col, ncols, (int) (cpref - pref), false);
        }
    }

    /** Reparte un sobrante en partes iguales, con el resto en las primeras columnas. */
    private void expandColumns(int col, int ncols, int extra, boolean minimo) {
        int perCol = extra / ncols;
        int resto = extra - (perCol * ncols);
        for (int i = 0; i < ncols; i++) {
            SizeRequirements req = columnRequirements[col + i];
            int add = perCol + ((i < resto) ? 1 : 0);
            if (minimo) {
                req.minimum = req.minimum + add;
                req.preferred = Math.max(req.preferred, req.minimum);
            } else {
                req.preferred = req.preferred + add;
            }
        }
    }

    protected View getViewAtPosition(int pos, Rectangle a) {
        int n = getViewCount();
        for (int i = 0; i < n; i++) {
            View v = getView(i);
            int p0 = v.getStartOffset();
            int p1 = v.getEndOffset();
            if ((pos >= p0) && (pos < p1)) {
                if (a != null) {
                    childAllocation(i, a);
                }
                return v;
            }
        }
        if (pos == getEndOffset()) {
            View v = getView(n - 1);
            if (a != null) {
                this.childAllocation(n - 1, a);
            }
            return v;
        }
        return null;
    }

    /**
     * Una fila de la tabla.
     *
     * <p>En el JDK es una clase interna; aca es estatica y recibe la tabla como primer parametro,
     * que es exactamente la firma que el JDK genera en el archivo compilado. El compilador de esta
     * biblioteca todavia no arma clases internas que se creen entre hermanas (hallazgos #507 y
     * #508 en <code>COMPILER_FINDINGS.md</code>).
     */
    public static class TableRow extends BoxView {

        private final TableView tabla;
        private BitSet fillColumns;
        private int row;

        /** Una fila de esa tabla sobre ese elemento. */
        public TableRow(TableView tabla, Element elem) {
            super(elem, View.X_AXIS);
            this.tabla = tabla;
            fillColumns = new BitSet();
        }

        void clearFilledColumns() {
            fillColumns.and(EMPTY);
        }

        /** Anota que esas columnas quedan tomadas por una celda que las abarca. */
        void fillColumns(int col, int ncols, int nrows) {
            for (int i = 0; i < ncols; i++) {
                fillColumns.set(col + i);
            }
        }

        boolean isFilled(int col) {
            return fillColumns.get(col);
        }

        int getRow() {
            return row;
        }

        void setRow(int row) {
            this.row = row;
        }

        /** Cuantas columnas hay antes de esa celda, contando las tomadas. */
        int getColumnCount() {
            int nfill = 0;
            int n = fillColumns.size();
            for (int i = 0; i < n; i++) {
                if (fillColumns.get(i)) {
                    nfill++;
                }
            }
            return getViewCount() + nfill;
        }

        public void replace(int offset, int length, View[] views) {
            super.replace(offset, length, views);
            if (tabla != null) {
                tabla.invalidateGrid();
            }
        }

        /** El ancho de la fila es el de la tabla, no la suma de sus celdas. */
        protected SizeRequirements calculateMajorAxisRequirements(int axis,
                SizeRequirements r) {
            return tabla.calculateMinorAxisRequirements(axis, r);
        }

        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) {
                return tabla.getMinimumSpan(axis);
            }
            return super.getMinimumSpan(axis);
        }

        public float getMaximumSpan(int axis) {
            if (axis == View.X_AXIS) {
                return Integer.MAX_VALUE;
            }
            return super.getMaximumSpan(axis);
        }

        public float getPreferredSpan(int axis) {
            if (axis == View.X_AXIS) {
                return tabla.getPreferredSpan(axis);
            }
            return super.getPreferredSpan(axis);
        }

        /** Las celdas van donde diga la tabla: ver la nota de la clase que la contiene. */
        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            tabla.updateGrid();
            int col = 0;
            int ncells = getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = getView(cell);
                for (; isFilled(col); col++) {
                    // Columna tomada.
                }
                int ncols = tabla.getColumnsOccupied(cv);
                offsets[cell] = tabla.columnOffsets[col];
                spans[cell] = tabla.columnSpans[col];
                if (ncols > 1) {
                    int n = tabla.getColumnCount();
                    for (int j = 1; j < ncols; j++) {
                        if ((col + j) < n) {
                            spans[cell] = spans[cell] + tabla.columnSpans[col + j];
                        }
                    }
                    col = col + ncols - 1;
                }
            }
        }

        /** Todas las celdas de una fila tienen el alto de la fila. */
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            super.layoutMinorAxis(targetSpan, axis, offsets, spans);
            int col = 0;
            int ncells = getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = getView(cell);
                for (; isFilled(col); col++) {
                    // Columna tomada.
                }
                int nrows = tabla.getRowsOccupied(cv);
                if (nrows > 1) {
                    int rowSpan = spans[cell];
                    int r = tabla.getRow(this);
                    for (int j = 1; j < nrows; j++) {
                        if ((r + j) < tabla.getRowCount()) {
                            rowSpan = rowSpan + tabla.getRowSpan(r + j);
                        }
                    }
                    spans[cell] = rowSpan;
                }
                col = col + tabla.getColumnsOccupied(cv) - 1;
            }
        }

        /** Una fila no se estira por su cuenta: el ancho lo pone la tabla. */
        public int getResizeWeight(int axis) {
            return 1;
        }

        protected View getViewAtPosition(int pos, Rectangle a) {
            int n = getViewCount();
            for (int i = 0; i < n; i++) {
                View v = getView(i);
                int p0 = v.getStartOffset();
                int p1 = v.getEndOffset();
                if ((pos >= p0) && (pos < p1)) {
                    if (a != null) {
                        childAllocation(i, a);
                    }
                    return v;
                }
            }
            if (pos == getEndOffset()) {
                View v = getView(n - 1);
                if (a != null) {
                    this.childAllocation(n - 1, a);
                }
                return v;
            }
            return null;
        }
    }

    /**
     * Una celda de la tabla.
     *
     * @deprecated Una celda ya no necesita una clase propia: cualquier vista sirve.
     */
    @Deprecated
    public static class TableCell extends BoxView implements GridCell {

        private int row;
        private int col;

        /** Una celda de esa tabla sobre ese elemento. */
        public TableCell(TableView tabla, Element elem) {
            super(elem, View.Y_AXIS);
        }

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return 1;
        }

        /** Donde quedo la celda en la grilla; la ubica la tabla. */
        public void setGridLocation(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getGridRow() {
            return row;
        }

        public int getGridColumn() {
            return col;
        }
    }

    /** Lo que la tabla necesita saber de una celda para ubicarla. */
    interface GridCell {

        void setGridLocation(int row, int col);

        int getGridRow();

        int getGridColumn();

        int getColumnCount();

        int getRowCount();
    }
}
