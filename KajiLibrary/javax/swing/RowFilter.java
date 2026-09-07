package javax.swing;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decide que filas se ven y cuales no.
 *
 * <h2>Filtrar no es borrar</h2>
 *
 * <p>Una fila que el filtro deja afuera sigue en el modelo: lo unico que cambia es que la vista no
 * la muestra. Por eso el filtro se le pone al ordenador de filas y no al modelo, y por eso los
 * indices de vista y de modelo dejan de coincidir en cuanto hay un filtro puesto.
 *
 * <h2>La entrada, no la fila</h2>
 *
 * <p>{@link #include} no recibe una fila sino una {@link Entry}, que es una vista de solo lectura
 * de una fila: sus valores, cuantos son, y el identificador con que el modelo la reconoce. Asi el
 * mismo filtro sirve para una tabla y para un arbol, que guardan sus filas de maneras distintas.
 *
 * <h2>Las columnas de mas</h2>
 *
 * <p>Las fabricas reciben un {@code int...} de columnas. <strong>Sin ninguna, miran todas</strong>,
 * y alcanza con que una encaje. Es al reves de lo que uno espera de una lista vacia, y es lo comodo:
 * un buscador de tabla se escribe con una sola llamada.
 */
public abstract class RowFilter<M, I> {

    /** Para las subclases. */
    protected RowFilter() {
    }

    /**
     * Como comparar contra el valor de referencia.
     *
     * <p>{@link #BEFORE} y {@link #AFTER} se llaman asi por las fechas, pero valen igual para los
     * numeros: son "menor" y "mayor".
     */
    public enum ComparisonType {

        /** Anterior, o menor. */
        BEFORE,

        /** Posterior, o mayor. */
        AFTER,

        /** Igual. */
        EQUAL,

        /** Distinto. */
        NOT_EQUAL;
    }

    /**
     * Deja pasar las filas donde la expresion regular encuentra algo.
     *
     * <p>Es {@code find}, no {@code matches}: alcanza con que la expresion aparezca en alguna
     * parte del texto, no hace falta que lo cubra entero. De ahi que {@code "ar"} sirva para buscar
     * "Argentina" sin comodines.
     *
     * <p>No se comprueba que la expresion no sea nula: se la pasa a {@code Pattern.compile}, que
     * revienta sola. Es lo que hace el JDK y el mensaje sale de alli.
     *
     * @throws NullPointerException si la expresion es nula
     * @throws java.util.regex.PatternSyntaxException si no compila
     * @throws IllegalArgumentException si alguna columna es negativa
     */
    public static <M, I> RowFilter<M, I> regexFilter(String regex, int... indices) {
        return new RegexFilter<M, I>(Pattern.compile(regex), indices);
    }

    /**
     * Deja pasar las filas cuya fecha compara asi contra la de referencia.
     *
     * <p>La fecha se lee en la fabrica, antes de llegar al filtro, asi que una fecha nula sale como
     * {@link NullPointerException} y un tipo nulo como {@link IllegalArgumentException}. La
     * asimetria es del JDK y esta medida.
     *
     * @throws NullPointerException si la fecha es nula
     * @throws IllegalArgumentException si el tipo es nulo o alguna columna es negativa
     */
    public static <M, I> RowFilter<M, I> dateFilter(ComparisonType type, Date date,
            int... indices) {
        return new DateFilter<M, I>(type, date.getTime(), indices);
    }

    /**
     * Deja pasar las filas cuyo numero compara asi contra el de referencia.
     *
     * <p>La comparacion es por valor y no por tipo: un {@code Integer} de 3 y un {@code Double} de
     * 3.0 dan iguales. Sin eso, un filtro escrito con un literal entero no encontraria nada en una
     * columna de dobles.
     *
     * @throws IllegalArgumentException si el tipo o el numero son nulos, o si alguna columna es
     *     negativa
     */
    public static <M, I> RowFilter<M, I> numberFilter(ComparisonType type, Number number,
            int... indices) {
        return new NumberFilter<M, I>(type, number, indices);
    }

    /**
     * Deja pasar lo que pase alguno de esos filtros.
     *
     * @throws NullPointerException si la coleccion es nula
     * @throws IllegalArgumentException si trae un nulo
     */
    public static <M, I> RowFilter<M, I> orFilter(
            Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new OrFilter<M, I>(filters);
    }

    /**
     * Deja pasar lo que pase todos esos filtros.
     *
     * @throws NullPointerException si la coleccion es nula
     * @throws IllegalArgumentException si trae un nulo
     */
    public static <M, I> RowFilter<M, I> andFilter(
            Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new AndFilter<M, I>(filters);
    }

    /**
     * Da vuelta un filtro.
     *
     * @throws IllegalArgumentException si el filtro es nulo
     */
    public static <M, I> RowFilter<M, I> notFilter(RowFilter<M, I> filter) {
        return new NotFilter<M, I>(filter);
    }

    /** Si esa fila se ve. */
    public abstract boolean include(Entry<? extends M, ? extends I> entry);

    /**
     * Una fila vista desde el filtro: sus valores y con que la reconoce el modelo.
     *
     * <p>Es de solo lectura a proposito. Un filtro que pudiera tocar la fila que esta evaluando
     * cambiaria lo que se esta filtrando mientras se filtra.
     */
    public abstract static class Entry<M, I> {

        /** Para las subclases. */
        public Entry() {
        }

        /** El modelo del que sale esta fila. */
        public abstract M getModel();

        /** Cuantos valores tiene la fila. */
        public abstract int getValueCount();

        /**
         * El valor de esa columna.
         *
         * @throws IndexOutOfBoundsException si el indice esta fuera de rango
         */
        public abstract Object getValue(int index);

        /**
         * El valor de esa columna como texto.
         *
         * <p>Nulo se convierte en cadena vacia, no en {@code "null"}: un filtro de texto que
         * encontrara la palabra "null" en las celdas vacias seria una sorpresa desagradable.
         *
         * @throws IndexOutOfBoundsException si el indice esta fuera de rango
         */
        public String getStringValue(int index) {
            Object value = getValue(index);
            return (value == null) ? "" : value.toString();
        }

        /** Con que reconoce el modelo a esta fila. */
        public abstract I getIdentifier();
    }

    /** La parte comun de los filtros que miran ciertas columnas; ver la nota de la clase. */
    private abstract static class GeneralFilter<M, I> extends RowFilter<M, I> {

        private final int[] columns;

        GeneralFilter(int[] columns) {
            checkIndices(columns);
            this.columns = columns;
        }

        static void checkIndices(int[] columns) {
            if (columns == null) {
                return;
            }
            for (int i = columns.length - 1; i >= 0; i--) {
                if (columns[i] < 0) {
                    throw new IllegalArgumentException("Index must be >= 0");
                }
            }
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            int count = value.getValueCount();
            if (columns.length > 0) {
                for (int i = columns.length - 1; i >= 0; i--) {
                    int index = columns[i];
                    // Una columna pedida que la fila no tiene se saltea, no revienta: distintas
                    // filas de un arbol pueden tener distinta cantidad de valores.
                    if (index < count && include(value, index)) {
                        return true;
                    }
                }
            } else {
                while (--count >= 0) {
                    if (include(value, count)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected abstract boolean include(Entry<? extends M, ? extends I> value, int index);
    }

    /** Ver {@link RowFilter#regexFilter}. */
    private static class RegexFilter<M, I> extends GeneralFilter<M, I> {

        private final Matcher matcher;

        RegexFilter(Pattern regex, int[] columns) {
            super(columns);
            matcher = regex.matcher("");
        }

        protected boolean include(Entry<? extends M, ? extends I> value, int index) {
            matcher.reset(value.getStringValue(index));
            return matcher.find();
        }
    }

    /** Ver {@link RowFilter#dateFilter}. */
    private static class DateFilter<M, I> extends GeneralFilter<M, I> {

        private final long date;
        private final ComparisonType type;

        DateFilter(ComparisonType type, long date, int[] columns) {
            super(columns);
            if (type == null) {
                throw new IllegalArgumentException("type must be non-null");
            }
            this.type = type;
            this.date = date;
        }

        protected boolean include(Entry<? extends M, ? extends I> value, int index) {
            Object v = value.getValue(index);
            if (v instanceof Date) {
                long vDate = ((Date) v).getTime();
                if (type == ComparisonType.BEFORE) {
                    return (vDate < date);
                }
                if (type == ComparisonType.AFTER) {
                    return (vDate > date);
                }
                if (type == ComparisonType.EQUAL) {
                    return (vDate == date);
                }
                if (type == ComparisonType.NOT_EQUAL) {
                    return (vDate != date);
                }
            }
            return false;
        }
    }

    /** Ver {@link RowFilter#numberFilter}. */
    private static class NumberFilter<M, I> extends GeneralFilter<M, I> {

        private final boolean isComparable;
        private final Number number;
        private final ComparisonType type;

        NumberFilter(ComparisonType type, Number number, int[] columns) {
            super(columns);
            if (type == null || number == null) {
                throw new IllegalArgumentException("type and number must be non-null");
            }
            this.type = type;
            this.number = number;
            isComparable = (number instanceof Comparable);
        }

        @SuppressWarnings("unchecked")
        protected boolean include(Entry<? extends M, ? extends I> value, int index) {
            Object v = value.getValue(index);
            if (v instanceof Number) {
                boolean compared = true;
                int compareResult;
                Class<?> vClass = v.getClass();
                if (number.getClass() == vClass && isComparable) {
                    compareResult = ((Comparable<Number>) number).compareTo((Number) v);
                } else {
                    // Distinto tipo: se comparan los valores en doble. Ver la nota de la fabrica.
                    compareResult = compare(number, (Number) v);
                }
                if (compared) {
                    if (type == ComparisonType.BEFORE) {
                        return (compareResult > 0);
                    }
                    if (type == ComparisonType.AFTER) {
                        return (compareResult < 0);
                    }
                    if (type == ComparisonType.EQUAL) {
                        return (compareResult == 0);
                    }
                    if (type == ComparisonType.NOT_EQUAL) {
                        return (compareResult != 0);
                    }
                }
            }
            return false;
        }

        private static int compare(Number a, Number b) {
            double da = a.doubleValue();
            double db = b.doubleValue();
            if (da < db) {
                return -1;
            }
            if (da > db) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * La parte comun de los filtros que combinan otros.
     *
     * <p><strong>El arreglo se lee por {@link #partes} y no directo.</strong> Su tipo natural es el
     * que esta declarado, pero este compilador pierde la cota inferior del comodin al mirar un campo
     * <em>heredado</em> --hallazgo #516--, y entonces las subclases no pueden ni copiarlo a una
     * variable local. El mismo tipo devuelto por un metodo heredado si se sustituye bien, asi que el
     * rodeo es un accesor y no un cambio de tipo ni un descarte de generico.
     */
    private abstract static class CompoundFilter<M, I> extends RowFilter<M, I> {

        private final RowFilter<? super M, ? super I>[] filters;

        @SuppressWarnings("unchecked")
        CompoundFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            // La coleccion nula no se comprueba: revienta sola al pedirle el iterador, que es lo
            // que hace el JDK. Un elemento nulo si, y con mayuscula, que es como esta escrito alla.
            List<RowFilter<? super M, ? super I>> l =
                    new ArrayList<RowFilter<? super M, ? super I>>();
            Iterator<? extends RowFilter<? super M, ? super I>> it = filters.iterator();
            while (it.hasNext()) {
                RowFilter<? super M, ? super I> f = it.next();
                if (f == null) {
                    throw new IllegalArgumentException("Filter must be non-null");
                }
                l.add(f);
            }
            this.filters = l.toArray(new RowFilter[l.size()]);
        }

        /** Los filtros combinados; ver la nota de la clase. */
        RowFilter<? super M, ? super I>[] partes() {
            return filters;
        }
    }

    /** Ver {@link RowFilter#orFilter}. */
    private static class OrFilter<M, I> extends CompoundFilter<M, I> {

        OrFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            super(filters);
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            RowFilter<? super M, ? super I>[] fs = partes();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].include(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Ver {@link RowFilter#andFilter}. */
    private static class AndFilter<M, I> extends CompoundFilter<M, I> {

        AndFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            super(filters);
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            RowFilter<? super M, ? super I>[] fs = partes();
            for (int i = 0; i < fs.length; i++) {
                if (!fs[i].include(value)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Ver {@link RowFilter#notFilter}. */
    private static class NotFilter<M, I> extends RowFilter<M, I> {

        private final RowFilter<M, I> filter;

        NotFilter(RowFilter<M, I> filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter must be non-null");
            }
            this.filter = filter;
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            return !filter.include(value);
        }
    }
}
