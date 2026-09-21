package javax.swing.text;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * A table view: rows of cells aligned in columns.
 *
 * <h2>The problem it solves</h2>
 *
 * <p>A table is not a box of boxes. If each row laid itself out, the cells of a column would end
 * up of different widths. The width is decided by the <em>table</em>, looking at every row at
 * once, and afterwards it imposes it on each row.
 *
 * <h2>Cells that take up several columns</h2>
 *
 * <p>A cell may span several columns or several rows. That breaks the simple rule of "the
 * column's width is the maximum of its cells": a cell that spans three columns does not rule
 * over any of the three on its own. Here the same is done as in the JDK: the single-column cells
 * fix the widths, and afterwards whatever is missing is shared out among the columns a wide cell
 * needs.
 *
 * <p>The rows taken up by a cell that spans several are noted in a {@link BitSet} per column, so
 * the row below knows that that column is already taken and puts nothing there.
 *
 * <h2>It is abstract on purpose</h2>
 *
 * <p>It does not know which elements a table comes from: that depends on the format. Whoever
 * uses it says which elements are rows by overriding {@link #createTableRow}.
 */
public abstract class TableView extends BoxView {

    int[] columnSpans;
    int[] columnOffsets;
    SizeRequirements[] columnRequirements;
    Vector<TableRow> rows;
    boolean gridValid;
    static final BitSet EMPTY = new BitSet();

    /** A table over that element; the rows are its children. */
    public TableView(Element elem) {
        super(elem, View.Y_AXIS);
        rows = new Vector<TableRow>();
        gridValid = false;
    }

    /** A row; override to decide which elements are rows. */
    protected TableRow createTableRow(Element elem) {
        return new TableRow(this, elem);
    }

    /**
     * A cell.
     *
     * @deprecated Cells now come from the view factory, like any other view.
     */
    @Deprecated
    protected TableCell createTableCell(Element elem) {
        return new TableCell(this, elem);
    }

    int getColumnCount() {
        return columnSpans.length;
    }

    /** Where that column starts, in the table's coordinates. */
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
     * It rebuilds the grid: it counts columns and places each cell.
     *
     * <p>It is done in two passes because how many columns there are is not known until every row
     * has been looked at, and a cell cannot be placed without knowing how many columns there are.
     */
    void updateGrid() {
        if (!gridValid) {
        // First pass: count columns.
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

            // Second pass: place each cell, skipping what a tall cell already takes up.
            for (int row = 0; row < nrows; row++) {
                TableRow rv = getRow(row);
                rv.clearFilledColumns();
                rv.setRow(row);
                int col = 0;
                for (int cell = 0; cell < rv.getViewCount(); cell++, col++) {
                    View cv = rv.getView(cell);
                    for (; rv.isFilled(col); col++) {
                        // It is already taken by a cell coming down from a row above.
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

    /** How many columns that cell takes up. */
    protected int getColumnsOccupied(View v) {
        return 1;
    }

    /** How many rows that cell takes up. */
    protected int getRowsOccupied(View v) {
        return 1;
    }

    protected void invalidateGrid() {
        gridValid = false;
    }

    protected void forwardUpdate(DocumentEvent$ElementChange ec, DocumentEvent e, Shape a,
            ViewFactory f) {
        super.forwardUpdate(ec, e, a, f);
        // A change in one row may change the whole table, not only that row.
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
     * It shares the width out among the columns.
     *
     * <p>The sharing out is proportional to what each column prefers, with the minimum as a floor:
     * giving a column less would make it show cut text.
     */
    protected void layoutColumns(int targetSpan, int[] offsets, int[] spans,
            SizeRequirements[] reqs) {
        SizeRequirements.calculateTiledPositions(targetSpan, null, reqs, offsets, spans);
    }

    /** It lays out the minor axis: first the columns, then each row with those widths. */
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
     * It gathers what each column asks for.
     *
     * <p>The wide cells are left for the end: first what the columns ask for on their own is known,
     * and only afterwards is it seen whether a wide cell is short of room. The other way round, a
     * wide cell would inflate the first column it touched.
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
                    // A column taken by a cell of another row.
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
                // The same as above.
            }
            if (row.getView(cell) == cv) {
                return col;
            }
            col = col + getColumnsOccupied(row.getView(cell)) - 1;
        }
        return -1;
    }

    /** A single-column cell rules directly over that column. */
    void checkSingleColumnCell(int axis, int col, View v) {
        SizeRequirements req = columnRequirements[col];
        req.minimum = Math.max((int) v.getMinimumSpan(axis), req.minimum);
        req.preferred = Math.max((int) v.getPreferredSpan(axis), req.preferred);
    }

    /**
     * It shares out what a wide cell is short of among the columns it spans.
     *
     * <p>It only adds what is missing: if the columns already add up to enough, the cell changes
     * nothing.
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

    /** It shares a surplus out in equal parts, with the remainder in the first columns. */
    private void expandColumns(int col, int ncols, int extra, boolean min) {
        int perCol = extra / ncols;
        int rest = extra - (perCol * ncols);
        for (int i = 0; i < ncols; i++) {
            SizeRequirements req = columnRequirements[col + i];
            int add = perCol + ((i < rest) ? 1 : 0);
            if (min) {
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
     * A row of the table.
     *
     * <p>In the JDK it is an inner class; here it is static and takes the table as its first
     * parameter, which is exactly the signature the JDK generates in the compiled file. This
     * library's compiler does not yet build inner classes that create each other as siblings
     * (findings #507 and #508 in <code>COMPILER_FINDINGS.md</code>).
     */
    public static class TableRow extends BoxView {

        private final TableView table;
        private BitSet fillColumns;
        private int row;

        /** A row of that table over that element. */
        public TableRow(TableView table, Element elem) {
            super(elem, View.X_AXIS);
            this.table = table;
            fillColumns = new BitSet();
        }

        void clearFilledColumns() {
            fillColumns.and(EMPTY);
        }

        /** It notes that those columns are taken by a cell that spans them. */
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

        /** How many columns there are before that cell, counting the taken ones. */
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
            if (table != null) {
                table.invalidateGrid();
            }
        }

        /** The row's width is the table's, not the sum of its cells'. */
        protected SizeRequirements calculateMajorAxisRequirements(int axis,
                SizeRequirements r) {
            return table.calculateMinorAxisRequirements(axis, r);
        }

        public float getMinimumSpan(int axis) {
            if (axis == View.X_AXIS) {
                return table.getMinimumSpan(axis);
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
                return table.getPreferredSpan(axis);
            }
            return super.getPreferredSpan(axis);
        }

        /** The cells go where the table says: see the note of the class that contains it. */
        protected void layoutMajorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            table.updateGrid();
            int col = 0;
            int ncells = getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = getView(cell);
                for (; isFilled(col); col++) {
                // Column taken.
                }
                int ncols = table.getColumnsOccupied(cv);
                offsets[cell] = table.columnOffsets[col];
                spans[cell] = table.columnSpans[col];
                if (ncols > 1) {
                    int n = table.getColumnCount();
                    for (int j = 1; j < ncols; j++) {
                        if ((col + j) < n) {
                            spans[cell] = spans[cell] + table.columnSpans[col + j];
                        }
                    }
                    col = col + ncols - 1;
                }
            }
        }

        /** Every cell of a row has the row's height. */
        protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
            super.layoutMinorAxis(targetSpan, axis, offsets, spans);
            int col = 0;
            int ncells = getViewCount();
            for (int cell = 0; cell < ncells; cell++, col++) {
                View cv = getView(cell);
                for (; isFilled(col); col++) {
                // Column taken.
                }
                int nrows = table.getRowsOccupied(cv);
                if (nrows > 1) {
                    int rowSpan = spans[cell];
                    int r = table.getRow(this);
                    for (int j = 1; j < nrows; j++) {
                        if ((r + j) < table.getRowCount()) {
                            rowSpan = rowSpan + table.getRowSpan(r + j);
                        }
                    }
                    spans[cell] = rowSpan;
                }
                col = col + table.getColumnsOccupied(cv) - 1;
            }
        }

        /** A row does not stretch on its own: the width is set by the table. */
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
     * A cell of the table.
     *
     * @deprecated A cell no longer needs a class of its own: any view serves.
     */
    @Deprecated
    public static class TableCell extends BoxView implements GridCell {

        private int row;
        private int col;

        /** A cell of that table over that element. */
        public TableCell(TableView table, Element elem) {
            super(elem, View.Y_AXIS);
        }

        public int getColumnCount() {
            return 1;
        }

        public int getRowCount() {
            return 1;
        }

        /** Where the cell ended up in the grid; the table places it. */
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

    /** What the table needs to know about a cell in order to place it. */
    interface GridCell {

        void setGridLocation(int row, int col);

        int getGridRow();

        int getGridColumn();

        int getColumnCount();

        int getRowCount();
    }
}
