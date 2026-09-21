package javax.swing;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The row sorter and filterer the tables use.
 *
 * <h2>Two numberings, and they must not be confused</h2>
 *
 * <p>The <em>model</em> numbers the rows as they are kept; the <em>view</em> numbers them as
 * they are seen, already sorted and filtered. With a sort set or a filter active the two
 * numberings stop matching, and {@link #convertRowIndexToModel} and
 * {@link #convertRowIndexToView} are the only bridge. Using a view index against the model is
 * the classic mistake with sortable tables: it does not fail, it returns another row.
 *
 * <h2>It sorts by several columns at once</h2>
 *
 * <p>{@link #setSortKeys} receives a list: the first key rules, the second breaks ties, and so
 * on. {@link #setMaxSortKeys} sets the cap -- three by default -- so that clicking on one column
 * after another does not pile up an endless order.
 *
 * <p>The last tie-breaker is always the model index, and that is why the order is
 * <strong>stable</strong>: two equal rows are left in the order they were in.
 *
 * <h2>How the values are compared</h2>
 *
 * <p>If the column has a comparator set, that one is used over the values as they are. If not,
 * their texts are compared with a {@link Collator}, which is what makes "arbol" come before
 * "Barco" -- a raw {@code String} comparison would put every upper-case one first. Which of
 * the two paths is taken is decided by {@link #useToString}, and a subclass may change it by
 * looking at the column's type.
 *
 * <p>Nulls go first, always, and before the comparator sees anything. A comparator that received
 * a null would have to know what to do with it, and almost none does.
 */
public abstract class DefaultRowSorter<M, I> extends RowSorter<M> {

    private ModelWrapper<M, I> modelWrapper;
    private List<SortKey> sortKeys = new ArrayList<SortKey>();
    private int maxSortKeys = 3;
    private boolean sortsOnUpdates = false;
    private RowFilter<? super M, ? super I> filter;
    private Comparator<?>[] comparators = new Comparator<?>[0];
    private boolean[] sortableColumns = new boolean[0];

    /** From view to model; null when there is neither sort nor filter and identity is enough. */
    private Row[] viewToModel;

    /** From model to view; -1 in the rows the filter left out. */
    private int[] modelToView;

    /** With no model wrapper set yet. */
    public DefaultRowSorter() {
    }

    /**
     * It tells it where the rows come from.
     *
     * @throws IllegalArgumentException if it is null
     */
    protected final void setModelWrapper(ModelWrapper<M, I> modelWrapper) {
        if (modelWrapper == null) {
            throw new IllegalArgumentException("modelWrapper most be non-null");
        }
        ModelWrapper<M, I> last = this.modelWrapper;
        this.modelWrapper = modelWrapper;
        if (last != null) {
            modelStructureChanged();
        } else {
            allRowsChanged();
        }
    }

    protected final ModelWrapper<M, I> getModelWrapper() {
        return modelWrapper;
    }

    public final M getModel() {
        return getModelWrapper().getModel();
    }

    /**
     * Whether that column can be sorted.
     *
     * <p>A column that cannot be sorted is ignored when cycling the order, but it is
     * <strong>not</strong> rejected if somebody sets it by hand in {@link #setSortKeys}: the JDK
     * only looks at it in {@link #toggleSortOrder}.
     *
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    public void setSortable(int column, boolean sortable) {
        checkColumn(column);
        growSortableColumns(column + 1);
        sortableColumns[column] = sortable;
    }

    /**
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    public boolean isSortable(int column) {
        checkColumn(column);
        if (column >= sortableColumns.length) {
            return true;
        }
        return sortableColumns[column];
    }

    /**
     * The sort keys, the first rules.
     *
     * <p>Null or empty leave the table with no sort. A copy is kept: changing the list afterwards
     * does not change the sort.
     */
    public void setSortKeys(List<? extends SortKey> sortKeys) {
        List<SortKey> old = this.sortKeys;
        if (sortKeys != null && sortKeys.size() > 0) {
            int max = getModelWrapper().getColumnCount();
            for (int i = 0; i < sortKeys.size(); i++) {
                SortKey key = sortKeys.get(i);
                if (key == null || key.getColumn() < 0 || key.getColumn() >= max) {
                    throw new IllegalArgumentException("Invalid SortKey");
                }
            }
            this.sortKeys = new ArrayList<SortKey>(sortKeys);
        } else {
            this.sortKeys = new ArrayList<SortKey>();
        }
        if (!this.sortKeys.equals(old)) {
            fireSortOrderChanged();
            sort();
        }
    }

    public List<? extends SortKey> getSortKeys() {
        return new ArrayList<SortKey>(sortKeys);
    }

    /**
     * How many keys pile up when cycling.
     *
     * <p><strong>It does not clip those that are already there</strong>, and this is measured:
     * lowering the cap with three keys set leaves all three. The cap is applied only at the next
     * {@link #toggleSortOrder}. Clipping them here would reorder the table as an effect of an
     * adjustment that does not speak about the current order.
     *
     * @throws IllegalArgumentException if it is less than one
     */
    public void setMaxSortKeys(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Invalid max");
        }
        maxSortKeys = max;
    }

    public int getMaxSortKeys() {
        return maxSortKeys;
    }

    /**
     * Whether changing a row relocates it at once.
     *
     * <p>Switched off by default, and on purpose: with this switched on, editing a cell of the
     * column that is sorted by makes the row that is being edited jump somewhere else on the
     * screen.
     */
    public void setSortsOnUpdates(boolean sortsOnUpdates) {
        this.sortsOnUpdates = sortsOnUpdates;
    }

    public boolean getSortsOnUpdates() {
        return sortsOnUpdates;
    }

    /** The filter; null shows everything. See {@link RowFilter}. */
    public void setRowFilter(RowFilter<? super M, ? super I> filter) {
        this.filter = filter;
        sort();
    }

    public RowFilter<? super M, ? super I> getRowFilter() {
        return filter;
    }

    /**
     * What a click on a column's header does.
     *
     * <p>If that column was already the main one, it turns the direction round. If not, it becomes
     * the main one ascending and those that were there are left behind as tie-breakers, clipped to
     * {@link #getMaxSortKeys}.
     *
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    public void toggleSortOrder(int column) {
        checkColumn(column);
        if (!isSortable(column)) {
            return;
        }
        List<SortKey> keys = new ArrayList<SortKey>(getSortKeys());
        SortKey sortKey = null;
        int sortIndex = -1;
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).getColumn() == column) {
                sortIndex = i;
                sortKey = keys.get(i);
                i = keys.size();
            }
        }
        if (sortIndex == 0 && sortKey.getSortOrder() == SortOrder.ASCENDING) {
            keys.set(0, new SortKey(column, SortOrder.DESCENDING));
        } else if (sortIndex == 0) {
            keys.set(0, new SortKey(column, SortOrder.ASCENDING));
        } else {
            if (sortIndex != -1) {
                keys.remove(sortIndex);
            }
            keys.add(0, new SortKey(column, SortOrder.ASCENDING));
            while (keys.size() > getMaxSortKeys()) {
                keys.remove(keys.size() - 1);
            }
        }
        setSortKeys(keys);
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int convertRowIndexToView(int index) {
        if (modelToView == null) {
            if (index < 0 || index >= getModelWrapper().getRowCount()) {
                throw new IndexOutOfBoundsException("Invalid index");
            }
            return index;
        }
        return modelToView[index];
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int convertRowIndexToModel(int index) {
        if (viewToModel == null) {
            if (index < 0 || index >= getModelWrapper().getRowCount()) {
                throw new IndexOutOfBoundsException("Invalid index");
            }
            return index;
        }
        return viewToModel[index].modelIndex;
    }

    /**
     * It rebuilds the sort and the filter, and gives notice.
     *
     * <p>With neither keys nor filter no translation table is kept: the view and the model are the
     * same numbering and keeping the identity would be memory for nothing.
     */
    public void sort() {
        int[] previous = lastToModel();
        int rows = getModelWrapper().getRowCount();
        if (sortKeys.isEmpty() && filter == null) {
            viewToModel = null;
            modelToView = null;
            fireRowSorterChanged(previous);
            return;
        }
        List<Row> included = new ArrayList<Row>();
        for (int i = 0; i < rows; i++) {
            if (include(i)) {
                included.add(new Row(i));
            }
        }
        Row[] array = new Row[included.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = included.get(i);
        }
        if (!sortKeys.isEmpty()) {
            prepareComparators();
            Arrays.sort(array, new RowComparator(this));
        }
        viewToModel = array;
        modelToView = new int[rows];
        for (int i = 0; i < rows; i++) {
            modelToView[i] = -1;
        }
        for (int i = 0; i < array.length; i++) {
            modelToView[array[i].modelIndex] = i;
        }
        fireRowSorterChanged(previous);
    }

    /** The translation that was there before reordering, which is what the notice carries. */
    private int[] lastToModel() {
        if (viewToModel == null) {
            return null;
        }
        int[] a = new int[viewToModel.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = viewToModel[i].modelIndex;
        }
        return a;
    }

    /** Whether the filter lets that model row through. */
    private boolean include(int modelIndex) {
        if (filter == null) {
            return true;
        }
        return filter.include(new RowEntry<M, I>(this, modelIndex));
    }

    /**
     * Whether that column is compared by its text and not by its value.
     *
     * <p>By default, when it has no comparator of its own. A subclass that knows the column's type
     * may say no and let the values be compared with each other.
     *
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    protected boolean useToString(int column) {
        return (getComparator(column) == null);
    }

    /**
     * That column's comparator; null if it has none.
     *
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    public void setComparator(int column, Comparator<?> comparator) {
        checkColumn(column);
        growComparators(column + 1);
        comparators[column] = comparator;
    }

    public Comparator<?> getComparator(int column) {
        checkColumn(column);
        if (column >= comparators.length) {
            return null;
        }
        return comparators[column];
    }

    /** How many rows are seen. */
    public int getViewRowCount() {
        if (viewToModel != null) {
            return viewToModel.length;
        }
        return getModelWrapper().getRowCount();
    }

    /** How many rows there are in the model, filtered or not. */
    public int getModelRowCount() {
        return getModelWrapper().getRowCount();
    }

    /** The model changed shape: everything is forgotten, the sort included. */
    public void modelStructureChanged() {
        sortKeys = new ArrayList<SortKey>();
        comparators = new Comparator<?>[0];
        sortableColumns = new boolean[0];
        viewToModel = null;
        modelToView = null;
    }

    /** Every row changed. */
    public void allRowsChanged() {
        modelToView = null;
        viewToModel = null;
        sort();
    }

    /**
     * @throws IndexOutOfBoundsException if the range is outside the model
     */
    public void rowsInserted(int firstRow, int endRow) {
        checkAgainstModel(firstRow, endRow);
        sort();
    }

    /**
     * @throws IndexOutOfBoundsException if the range is invalid
     */
    public void rowsDeleted(int firstRow, int endRow) {
        if (firstRow < 0 || endRow < firstRow) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
        sort();
    }

    /**
     * Those rows changed; it only reorders if {@link #getSortsOnUpdates}.
     *
     * @throws IndexOutOfBoundsException if the range is outside the model
     */
    public void rowsUpdated(int firstRow, int endRow) {
        checkAgainstModel(firstRow, endRow);
        if (getSortsOnUpdates()) {
            sort();
        }
    }

    /**
     * That column of those rows changed.
     *
     * @throws IndexOutOfBoundsException if the range or the column are outside the model
     */
    public void rowsUpdated(int firstRow, int endRow, int column) {
        checkColumn(column);
        rowsUpdated(firstRow, endRow);
    }

    private void checkColumn(int column) {
        if (column < 0 || column >= getModelWrapper().getColumnCount()) {
            throw new IndexOutOfBoundsException(
                    "column beyond range of TableModel");
        }
    }

    private void checkAgainstModel(int firstRow, int endRow) {
        if (firstRow > endRow || firstRow < 0 || endRow < 0
                || firstRow > getModelWrapper().getRowCount()) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
    }

    private void growComparators(int n) {
        if (comparators.length < n) {
            Comparator<?>[] newValue = new Comparator<?>[n];
            System.arraycopy(comparators, 0, newValue, 0, comparators.length);
            comparators = newValue;
        }
    }

    private void growSortableColumns(int n) {
        if (sortableColumns.length < n) {
            boolean[] newValue = new boolean[n];
            for (int i = 0; i < n; i++) {
                newValue[i] = true;
            }
            System.arraycopy(sortableColumns, 0, newValue, 0, sortableColumns.length);
            sortableColumns = newValue;
        }
    }

    /** The comparators and each key's mode, resolved once per sorting. */
    private Comparator<?>[] used;
    private boolean[] byText;

    private void prepareComparators() {
        used = new Comparator<?>[sortKeys.size()];
        byText = new boolean[sortKeys.size()];
        for (int i = 0; i < sortKeys.size(); i++) {
            int column = sortKeys.get(i).getColumn();
            byText[i] = useToString(column);
            Comparator<?> c = getComparator(column);
            used[i] = (c != null) ? c : Collator.getInstance();
        }
    }

    /** An included row, identified by its model index. */
    private static class Row {

        final int modelIndex;

        Row(int modelIndex) {
            this.modelIndex = modelIndex;
        }
    }

    /** It sorts by the keys and breaks ties by the model index; see the class note. */
    private static class RowComparator implements Comparator<Row> {

        private final DefaultRowSorter<?, ?> orden;

        RowComparator(DefaultRowSorter<?, ?> orden) {
            this.orden = orden;
        }

        @SuppressWarnings("unchecked")
        public int compare(Row a, Row b) {
            List<? extends SortKey> keys = orden.keys();
            for (int i = 0; i < keys.size(); i++) {
                SortKey key = keys.get(i);
                int column = key.getColumn();
                Object v1;
                Object v2;
                if (orden.isByText(i)) {
                    v1 = orden.getModelWrapper().getStringValueAt(a.modelIndex, column);
                    v2 = orden.getModelWrapper().getStringValueAt(b.modelIndex, column);
                } else {
                    v1 = orden.getModelWrapper().getValueAt(a.modelIndex, column);
                    v2 = orden.getModelWrapper().getValueAt(b.modelIndex, column);
                }
                int result;
                // Nulls go first and do not reach the comparator; see the class note.
                if (v1 == null && v2 == null) {
                    result = 0;
                } else if (v1 == null) {
                    result = -1;
                } else if (v2 == null) {
                    result = 1;
                } else {
                    result = ((Comparator<Object>) orden.comparatorUsed(i)).compare(v1, v2);
                }
                if (key.getSortOrder() == SortOrder.DESCENDING) {
                    result = result * -1;
                }
                if (result != 0) {
                    return result;
                }
            }
            return a.modelIndex - b.modelIndex;
        }
    }

    List<? extends SortKey> keys() {
        return sortKeys;
    }

    boolean isByText(int i) {
        return byText[i];
    }

    Comparator<?> comparatorUsed(int i) {
        return used[i];
    }

    /** The row the filter sees; see {@link RowFilter.Entry}. */
    private static class RowEntry<M, I> extends RowFilter.Entry<M, I> {

        private final DefaultRowSorter<M, I> orden;
        private final int modelIndex;

        RowEntry(DefaultRowSorter<M, I> orden, int modelIndex) {
            this.orden = orden;
            this.modelIndex = modelIndex;
        }

        public M getModel() {
            return orden.getModel();
        }

        public int getValueCount() {
            return orden.getModelWrapper().getColumnCount();
        }

        public Object getValue(int index) {
            return orden.getModelWrapper().getValueAt(modelIndex, index);
        }

        public String getStringValue(int index) {
            return orden.getModelWrapper().getStringValueAt(modelIndex, index);
        }

        public I getIdentifier() {
            return orden.getModelWrapper().getIdentifier(modelIndex);
        }
    }

    /**
     * Where the rows come from.
     *
     * <p>It exists so that the same sorter serves over any model: a table, a list, whatever. The
     * sorter does not know about {@code TableModel}; it knows about rows, columns and values.
     *
     * <p>It is <strong>protected</strong>, not public -- `javap` shows it public because that is
     * the class file's modifier, but the inner classes attribute says protected and it is what the
     * compiler enforces. Only a subclass of the sorter may name it, which is consistent with
     * {@code setModelWrapper} being protected too.
     */
    protected abstract static class ModelWrapper<M, I> {

        /** For the subclasses. */
        protected ModelWrapper() {
        }

        /** The real model. */
        public abstract M getModel();

        public abstract int getColumnCount();

        public abstract int getRowCount();

        public abstract Object getValueAt(int row, int column);

        /**
         * The value as text.
         *
         * <p>Null gives the empty string -- and so does a {@code toString} that returns null, which
         * exists.
         */
        public String getStringValueAt(int row, int column) {
            Object o = getValueAt(row, column);
            if (o == null) {
                return "";
            }
            String string = o.toString();
            if (string == null) {
                return "";
            }
            return string;
        }

        /** What the model recognizes that row by. */
        public abstract I getIdentifier(int row);
    }
}
