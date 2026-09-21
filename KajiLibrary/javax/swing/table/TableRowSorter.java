package javax.swing.table;

import java.util.Comparator;

import javax.swing.DefaultRowSorter;

/**
 * A table's row sorter.
 *
 * <h2>What it adds over {@link DefaultRowSorter}</h2>
 *
 * <p>Two things, and both come from a table knowing which type each column is:
 *
 * <ul>
 * <li><strong>It picks the comparator by the column's type.</strong> A column of numbers is
 *     sorted as numbers and not as text -- "9" before "10" --, and any type comparable by its
 *     natural order likewise. One of text goes through the language's
 *     {@link java.text.Collator}, which is what makes "tree" sort before "Wood".</li>
 * <li><strong>It allows setting a converter to text</strong> ({@link #setStringConverter}), for
 *     when the cell's {@code toString} is not what is shown.</li>
 * </ul>
 *
 * <p>The row identifier is the model index, an {@link Integer}: in a table the rows have no other
 * identity.
 */
public class TableRowSorter<M extends TableModel> extends DefaultRowSorter<M, Integer> {

    /** Compares whatever is comparable, by its natural order. */
    private static final Comparator<Object> NATURAL_COMPARATOR = new NaturalComparator();

    private TableStringConverter stringConverter;

    /** With no model. */
    public TableRowSorter() {
        this(null);
    }

    /** Over that model. */
    public TableRowSorter(M model) {
        setModel(model);
    }

    /** Changes the model; with no model, an empty one. */
    public void setModel(M model) {
        setModelWrapper(new TableWrapper<M>(this, model));
    }

    /** How a cell is converted to text; null uses {@code toString}. */
    public void setStringConverter(TableStringConverter stringConverter) {
        this.stringConverter = stringConverter;
    }

    public TableStringConverter getStringConverter() {
        return stringConverter;
    }

    /**
     * That column's comparator.
     *
     * <p>If nobody set one, it picks it by the type the column declares; see the class note.
     *
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    public Comparator<?> getComparator(int column) {
        Comparator<?> comparator = super.getComparator(column);
        if (comparator != null) {
            return comparator;
        }
        Class<?> columnClass = getModel().getColumnClass(column);
        if (columnClass == String.class) {
            return java.text.Collator.getInstance();
        }
        if (Comparable.class.isAssignableFrom(columnClass)) {
            return NATURAL_COMPARATOR;
        }
        return java.text.Collator.getInstance();
    }

    /**
     * Whether that column is compared by its text.
     *
     * <p><strong>Almost never.</strong> A column of text is compared with the language's
     * {@link java.text.Collator} over the values -- which are already text -- and one of any
     * comparable type, by its natural order. Only a column whose type is not comparable and has no
     * comparator of its own falls back on the text.
     *
     * <p>It is different from {@link javax.swing.DefaultRowSorter#useToString}, which says yes when
     * there is no comparator: over there there is nowhere to learn the column's type from, here
     * there is.
     *
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    protected boolean useToString(int column) {
        Comparator<?> comparator = super.getComparator(column);
        if (comparator != null) {
            return false;
        }
        Class<?> columnClass = getModel().getColumnClass(column);
        if (columnClass == String.class) {
            return false;
        }
        return !Comparable.class.isAssignableFrom(columnClass);
    }

    /** Compares by the type's natural order. */
    private static class NaturalComparator implements Comparator<Object>, java.io.Serializable {

        @SuppressWarnings("unchecked")
        public int compare(Object a, Object b) {
            return ((Comparable<Object>) a).compareTo(b);
        }
    }

    /** Tells the sorter where a table's rows come from. */
    private static class TableWrapper<M extends TableModel>
            extends DefaultRowSorter.ModelWrapper<M, Integer> {

        private final TableRowSorter<M> orden;
        private final M model;

        TableWrapper(TableRowSorter<M> orden, M model) {
            this.orden = orden;
            this.model = model;
        }

        public M getModel() {
            return model;
        }

        public int getColumnCount() {
            return (model == null) ? 0 : model.getColumnCount();
        }

        public int getRowCount() {
            return (model == null) ? 0 : model.getRowCount();
        }

        public Object getValueAt(int row, int column) {
            return model.getValueAt(row, column);
        }

        /**
         * A cell's text, going through the converter if there is one.
         *
         * <p>It is the only place where the converter is used, and that is why the wrapper needs to
         * know its sorter.
         */
        public String getStringValueAt(int row, int column) {
            TableStringConverter converter = orden.getStringConverter();
            if (converter != null) {
                String value = converter.toString(model, row, column);
                return (value == null) ? "" : value;
            }
            return super.getStringValueAt(row, column);
        }

        /** A row's identifier is its model index; see the class note. */
        public Integer getIdentifier(int row) {
            return Integer.valueOf(row);
        }
    }
}
