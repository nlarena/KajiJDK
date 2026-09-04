package javax.swing;

/**
 * Como esta ordenada una columna.
 *
 * <p>Son tres y no dos: {@link #UNSORTED} no es "ascendente por defecto" sino
 * <strong>el orden del modelo</strong>, o sea el que tenian los datos antes de que nadie los
 * ordenara. Sin esa tercera constante no habria forma de volver atras.
 *
 * @since 1.6
 */
public enum SortOrder {

    /** De menor a mayor. */
    ASCENDING,
    /** De mayor a menor. */
    DESCENDING,
    /** Sin ordenar: el orden del modelo. */
    UNSORTED
}
