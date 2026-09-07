package javax.swing.table;

import java.util.Comparator;

import javax.swing.DefaultRowSorter;

/**
 * El ordenador de filas de una tabla.
 *
 * <h2>Lo que agrega sobre {@link DefaultRowSorter}</h2>
 *
 * <p>Dos cosas, y las dos vienen de que una tabla si sabe de que tipo es cada columna:
 *
 * <ul>
 * <li><strong>Elige el comparador por el tipo de la columna.</strong> Una columna de numeros se
 *     ordena como numeros y no como texto -- "9" antes que "10" --, y cualquier tipo comparable por
 *     su orden natural. Una de texto va por el {@link java.text.Collator} del idioma, que es lo que
 *     hace que "arbol" venga antes que "Barco".</li>
 * <li><strong>Deja poner un convertidor a texto</strong> ({@link #setStringConverter}), para cuando
 *     el {@code toString} de la celda no es lo que se muestra.</li>
 * </ul>
 *
 * <p>El identificador de fila es el indice de modelo, un {@link Integer}: en una tabla las filas no
 * tienen otra identidad.
 */
public class TableRowSorter<M extends TableModel> extends DefaultRowSorter<M, Integer> {

    /** Compara lo que sea comparable, por su orden natural. */
    private static final Comparator<Object> COMPARADOR_NATURAL = new ComparadorNatural();

    private TableStringConverter stringConverter;

    /** Sin modelo. */
    public TableRowSorter() {
        this(null);
    }

    /** Sobre ese modelo. */
    public TableRowSorter(M model) {
        setModel(model);
    }

    /** Cambia el modelo; sin modelo, uno vacio. */
    public void setModel(M model) {
        setModelWrapper(new EnvoltorioDeTabla<M>(this, model));
    }

    /** Como se convierte una celda a texto; nulo usa {@code toString}. */
    public void setStringConverter(TableStringConverter stringConverter) {
        this.stringConverter = stringConverter;
    }

    public TableStringConverter getStringConverter() {
        return stringConverter;
    }

    /**
     * El comparador de esa columna.
     *
     * <p>Si nadie puso uno, lo elige por el tipo que declara la columna; ver la nota de la clase.
     *
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
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
            return COMPARADOR_NATURAL;
        }
        return java.text.Collator.getInstance();
    }

    /**
     * Si esa columna se compara por su texto.
     *
     * <p><strong>Casi nunca.</strong> Una columna de texto se compara con el {@link
     * java.text.Collator} del idioma sobre los valores -- que ya son texto -- y una de cualquier
     * tipo comparable, por su orden natural. Solo cae en el texto una columna cuyo tipo no es
     * comparable y no tiene comparador propio.
     *
     * <p>Es distinto de {@link javax.swing.DefaultRowSorter#useToString}, que dice que si cuando no
     * hay comparador: alla no hay de donde saber el tipo de la columna, aca si.
     *
     * @throws IndexOutOfBoundsException si la columna esta fuera de rango
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

    /** Compara por el orden natural del tipo. */
    private static class ComparadorNatural implements Comparator<Object>, java.io.Serializable {

        @SuppressWarnings("unchecked")
        public int compare(Object a, Object b) {
            return ((Comparable<Object>) a).compareTo(b);
        }
    }

    /** Le dice al ordenador de donde salen las filas de una tabla. */
    private static class EnvoltorioDeTabla<M extends TableModel>
            extends DefaultRowSorter.ModelWrapper<M, Integer> {

        private final TableRowSorter<M> orden;
        private final M model;

        EnvoltorioDeTabla(TableRowSorter<M> orden, M model) {
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
         * El texto de una celda, pasando por el convertidor si hay uno.
         *
         * <p>Es el unico lugar donde el convertidor se usa, y por eso el envoltorio necesita
         * conocer a su ordenador.
         */
        public String getStringValueAt(int row, int column) {
            TableStringConverter converter = orden.getStringConverter();
            if (converter != null) {
                String value = converter.toString(model, row, column);
                return (value == null) ? "" : value;
            }
            return super.getStringValueAt(row, column);
        }

        /** El identificador de una fila es su indice de modelo; ver la nota de la clase. */
        public Integer getIdentifier(int row) {
            return Integer.valueOf(row);
        }
    }
}
