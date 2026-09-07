package javax.swing;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * El ordenador y filtrador de filas que usan las tablas.
 *
 * <h2>Dos numeraciones, y no se pueden confundir</h2>
 *
 * <p>El <em>modelo</em> numera las filas como estan guardadas; la <em>vista</em> las numera como se
 * ven, ya ordenadas y filtradas. Con un orden puesto o un filtro activo las dos numeraciones dejan
 * de coincidir, y {@link #convertRowIndexToModel} y {@link #convertRowIndexToView} son el unico
 * puente. Usar un indice de vista contra el modelo es el error clasico con tablas ordenables: no
 * falla, devuelve otra fila.
 *
 * <h2>Ordena por varias columnas a la vez</h2>
 *
 * <p>{@link #setSortKeys} recibe una lista: la primera clave manda, la segunda desempata, y asi.
 * {@link #setMaxSortKeys} pone el tope -- tres por omision -- para que hacer clic en una columna
 * tras otra no acumule un orden infinito.
 *
 * <p>El ultimo desempate es siempre el indice de modelo, y por eso el orden es
 * <strong>estable</strong>: dos filas iguales quedan en el orden en que estaban.
 *
 * <h2>Como se comparan los valores</h2>
 *
 * <p>Si la columna tiene un comparador puesto, se usa ese sobre los valores tal cual. Si no, se
 * comparan sus textos con un {@link Collator}, que es lo que hace que "arbol" venga antes que
 * "Barco" -- una comparacion de {@code String} cruda pondria todas las mayusculas primero. Cual de
 * los dos caminos se toma lo decide {@link #useToString}, y una subclase puede cambiarlo mirando el
 * tipo de la columna.
 *
 * <p>Los nulos van primero, siempre, y antes de que el comparador vea nada. Un comparador que
 * recibiera un nulo tendria que saber que hacer con el, y casi ninguno sabe.
 */
public abstract class DefaultRowSorter<M, I> extends RowSorter<M> {

    private ModelWrapper<M, I> modelWrapper;
    private List<SortKey> sortKeys = new ArrayList<SortKey>();
    private int maxSortKeys = 3;
    private boolean sortsOnUpdates = false;
    private RowFilter<? super M, ? super I> filter;
    private Comparator<?>[] comparators = new Comparator<?>[0];
    private boolean[] sortables = new boolean[0];

    /** De vista a modelo; nulo cuando no hay orden ni filtro y la identidad alcanza. */
    private Fila[] viewToModel;

    /** De modelo a vista; -1 en las filas que el filtro dejo afuera. */
    private int[] modelToView;

    /** Sin envoltorio de modelo puesto todavia. */
    public DefaultRowSorter() {
    }

    /**
     * Le dice de donde salen las filas.
     *
     * @throws IllegalArgumentException si es nulo
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
     * Si esa columna se puede ordenar.
     *
     * <p>Una columna no ordenable se ignora al alternar el orden, pero <strong>no</strong> se
     * rechaza si alguien la pone a mano en {@link #setSortKeys}: el JDK solo la mira en
     * {@link #toggleSortOrder}.
     *
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
     */
    public void setSortable(int column, boolean sortable) {
        checkColumn(column);
        crecerSortables(column + 1);
        sortables[column] = sortable;
    }

    /**
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
     */
    public boolean isSortable(int column) {
        checkColumn(column);
        if (column >= sortables.length) {
            return true;
        }
        return sortables[column];
    }

    /**
     * Las claves de orden, la primera manda.
     *
     * <p>Nulo o vacio dejan la tabla sin orden. Se guarda una copia: cambiar la lista despues no
     * cambia el orden.
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
     * Cuantas claves se acumulan al alternar.
     *
     * <p><strong>No recorta las que ya hay</strong>, y esto esta medido: bajar el tope con tres
     * claves puestas las deja las tres. El tope se aplica recien en el proximo
     * {@link #toggleSortOrder}. Recortarlas aca reordenaria la tabla como efecto de un ajuste que
     * no habla del orden actual.
     *
     * @throws IllegalArgumentException si es menor que uno
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
     * Si cambiar una fila la reubica enseguida.
     *
     * <p>Apagado por omision, y a proposito: con esto prendido, editar una celda de la columna por
     * la que se ordena hace saltar la fila que se esta editando a otro lugar de la pantalla.
     */
    public void setSortsOnUpdates(boolean sortsOnUpdates) {
        this.sortsOnUpdates = sortsOnUpdates;
    }

    public boolean getSortsOnUpdates() {
        return sortsOnUpdates;
    }

    /** El filtro; nulo muestra todo. Ver {@link RowFilter}. */
    public void setRowFilter(RowFilter<? super M, ? super I> filter) {
        this.filter = filter;
        sort();
    }

    public RowFilter<? super M, ? super I> getRowFilter() {
        return filter;
    }

    /**
     * Lo que hace un clic en el encabezado de una columna.
     *
     * <p>Si esa columna ya era la principal, da vuelta el sentido. Si no, pasa a ser la principal en
     * ascendente y las que estaban quedan detras como desempate, recortadas a
     * {@link #getMaxSortKeys}.
     *
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
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
     * @throws IndexOutOfBoundsException si el indice esta fuera de rango
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
     * @throws IndexOutOfBoundsException si el indice esta fuera de rango
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
     * Rehace el orden y el filtro, y avisa.
     *
     * <p>Sin claves ni filtro no se guarda ninguna tabla de traduccion: la vista y el modelo son la
     * misma numeracion y guardar la identidad seria memoria por nada.
     */
    public void sort() {
        int[] anterior = ultimoAModelo();
        int filas = getModelWrapper().getRowCount();
        if (sortKeys.isEmpty() && filter == null) {
            viewToModel = null;
            modelToView = null;
            fireRowSorterChanged(anterior);
            return;
        }
        List<Fila> incluidas = new ArrayList<Fila>();
        for (int i = 0; i < filas; i++) {
            if (incluir(i)) {
                incluidas.add(new Fila(i));
            }
        }
        Fila[] arreglo = new Fila[incluidas.size()];
        for (int i = 0; i < arreglo.length; i++) {
            arreglo[i] = incluidas.get(i);
        }
        if (!sortKeys.isEmpty()) {
            prepararComparadores();
            Arrays.sort(arreglo, new ComparadorDeFilas(this));
        }
        viewToModel = arreglo;
        modelToView = new int[filas];
        for (int i = 0; i < filas; i++) {
            modelToView[i] = -1;
        }
        for (int i = 0; i < arreglo.length; i++) {
            modelToView[arreglo[i].modelIndex] = i;
        }
        fireRowSorterChanged(anterior);
    }

    /** La traduccion que habia antes de reordenar, que es lo que lleva el aviso. */
    private int[] ultimoAModelo() {
        if (viewToModel == null) {
            return null;
        }
        int[] a = new int[viewToModel.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = viewToModel[i].modelIndex;
        }
        return a;
    }

    /** Si el filtro deja pasar esa fila del modelo. */
    private boolean incluir(int modelIndex) {
        if (filter == null) {
            return true;
        }
        return filter.include(new EntradaDeFila<M, I>(this, modelIndex));
    }

    /**
     * Si esa columna se compara por su texto y no por su valor.
     *
     * <p>Por omision, cuando no tiene comparador propio. Una subclase que conozca el tipo de la
     * columna puede decir que no y dejar que los valores se comparen entre si.
     *
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
     */
    protected boolean useToString(int column) {
        return (getComparator(column) == null);
    }

    /**
     * El comparador de esa columna; nulo si no tiene.
     *
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
     */
    public void setComparator(int column, Comparator<?> comparator) {
        checkColumn(column);
        crecerComparadores(column + 1);
        comparators[column] = comparator;
    }

    public Comparator<?> getComparator(int column) {
        checkColumn(column);
        if (column >= comparators.length) {
            return null;
        }
        return comparators[column];
    }

    /** Cuantas filas se ven. */
    public int getViewRowCount() {
        if (viewToModel != null) {
            return viewToModel.length;
        }
        return getModelWrapper().getRowCount();
    }

    /** Cuantas filas hay en el modelo, filtradas o no. */
    public int getModelRowCount() {
        return getModelWrapper().getRowCount();
    }

    /** Cambio la forma del modelo: se olvida todo, incluido el orden. */
    public void modelStructureChanged() {
        sortKeys = new ArrayList<SortKey>();
        comparators = new Comparator<?>[0];
        sortables = new boolean[0];
        viewToModel = null;
        modelToView = null;
    }

    /** Cambiaron todas las filas. */
    public void allRowsChanged() {
        modelToView = null;
        viewToModel = null;
        sort();
    }

    /**
     * @throws IndexOutOfBoundsException si el rango esta fuera del modelo
     */
    public void rowsInserted(int firstRow, int endRow) {
        checkAgainstModel(firstRow, endRow);
        sort();
    }

    /**
     * @throws IndexOutOfBoundsException si el rango es invalido
     */
    public void rowsDeleted(int firstRow, int endRow) {
        if (firstRow < 0 || endRow < firstRow) {
            throw new IndexOutOfBoundsException("Invalid range");
        }
        sort();
    }

    /**
     * Cambiaron esas filas; solo reordena si {@link #getSortsOnUpdates}.
     *
     * @throws IndexOutOfBoundsException si el rango esta fuera del modelo
     */
    public void rowsUpdated(int firstRow, int endRow) {
        checkAgainstModel(firstRow, endRow);
        if (getSortsOnUpdates()) {
            sort();
        }
    }

    /**
     * Cambio esa columna de esas filas.
     *
     * @throws IndexOutOfBoundsException si el rango o la columna estan fuera del modelo
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

    private void crecerComparadores(int n) {
        if (comparators.length < n) {
            Comparator<?>[] nuevo = new Comparator<?>[n];
            System.arraycopy(comparators, 0, nuevo, 0, comparators.length);
            comparators = nuevo;
        }
    }

    private void crecerSortables(int n) {
        if (sortables.length < n) {
            boolean[] nuevo = new boolean[n];
            for (int i = 0; i < n; i++) {
                nuevo[i] = true;
            }
            System.arraycopy(sortables, 0, nuevo, 0, sortables.length);
            sortables = nuevo;
        }
    }

    /** Los comparadores y el modo de cada clave, resueltos una vez por ordenamiento. */
    private Comparator<?>[] usados;
    private boolean[] porTexto;

    private void prepararComparadores() {
        usados = new Comparator<?>[sortKeys.size()];
        porTexto = new boolean[sortKeys.size()];
        for (int i = 0; i < sortKeys.size(); i++) {
            int column = sortKeys.get(i).getColumn();
            porTexto[i] = useToString(column);
            Comparator<?> c = getComparator(column);
            usados[i] = (c != null) ? c : Collator.getInstance();
        }
    }

    /** Una fila incluida, identificada por su indice de modelo. */
    private static class Fila {

        final int modelIndex;

        Fila(int modelIndex) {
            this.modelIndex = modelIndex;
        }
    }

    /** Ordena por las claves y desempata por el indice de modelo; ver la nota de la clase. */
    private static class ComparadorDeFilas implements Comparator<Fila> {

        private final DefaultRowSorter<?, ?> orden;

        ComparadorDeFilas(DefaultRowSorter<?, ?> orden) {
            this.orden = orden;
        }

        @SuppressWarnings("unchecked")
        public int compare(Fila a, Fila b) {
            List<? extends SortKey> claves = orden.claves();
            for (int i = 0; i < claves.size(); i++) {
                SortKey key = claves.get(i);
                int column = key.getColumn();
                Object v1;
                Object v2;
                if (orden.esPorTexto(i)) {
                    v1 = orden.getModelWrapper().getStringValueAt(a.modelIndex, column);
                    v2 = orden.getModelWrapper().getStringValueAt(b.modelIndex, column);
                } else {
                    v1 = orden.getModelWrapper().getValueAt(a.modelIndex, column);
                    v2 = orden.getModelWrapper().getValueAt(b.modelIndex, column);
                }
                int result;
                // Los nulos van primero y no llegan al comparador; ver la nota de la clase.
                if (v1 == null && v2 == null) {
                    result = 0;
                } else if (v1 == null) {
                    result = -1;
                } else if (v2 == null) {
                    result = 1;
                } else {
                    result = ((Comparator<Object>) orden.comparadorUsado(i)).compare(v1, v2);
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

    List<? extends SortKey> claves() {
        return sortKeys;
    }

    boolean esPorTexto(int i) {
        return porTexto[i];
    }

    Comparator<?> comparadorUsado(int i) {
        return usados[i];
    }

    /** La fila que ve el filtro; ver {@link RowFilter.Entry}. */
    private static class EntradaDeFila<M, I> extends RowFilter.Entry<M, I> {

        private final DefaultRowSorter<M, I> orden;
        private final int modelIndex;

        EntradaDeFila(DefaultRowSorter<M, I> orden, int modelIndex) {
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
     * De donde salen las filas.
     *
     * <p>Existe para que el mismo ordenador sirva sobre cualquier modelo: una tabla, una lista, lo
     * que sea. El ordenador no sabe de {@code TableModel}; sabe de filas, columnas y valores.
     *
     * <p>Es <strong>protegida</strong>, no publica -- `javap` la muestra publica porque ese es el
     * modificador del archivo de clase, pero el atributo de clases internas dice protegida y es lo
     * que el compilador hace valer. Solo una subclase del ordenador puede nombrarla, que es
     * coherente con que {@code setModelWrapper} tambien sea protegido.
     */
    protected abstract static class ModelWrapper<M, I> {

        /** Para las subclases. */
        protected ModelWrapper() {
        }

        /** El modelo de verdad. */
        public abstract M getModel();

        public abstract int getColumnCount();

        public abstract int getRowCount();

        public abstract Object getValueAt(int row, int column);

        /**
         * El valor como texto.
         *
         * <p>Nulo da cadena vacia -- y tambien un {@code toString} que devuelva nulo, que existe.
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

        /** Con que reconoce el modelo a esa fila. */
        public abstract I getIdentifier(int row);
    }
}
