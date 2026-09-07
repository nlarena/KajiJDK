package javax.swing;

/**
 * Como se muestra donde va a caer lo que se esta arrastrando.
 *
 * <p>La diferencia entre {@link #ON} e {@link #INSERT} es la que se ve al arrastrar sobre una
 * lista: {@code ON} marca un elemento —se va a reemplazar— e {@code INSERT} marca la raya entre
 * dos —se va a meter ahi—. {@link #USE_SELECTION} es la forma vieja, que movia la seleccion
 * mientras se arrastraba y por eso la perdia si uno se arrepentia.
 *
 * <p>Las variantes de filas y columnas son para una tabla, que tiene dos sentidos donde insertar.
 */
public enum DropMode {

    /** La seleccion misma marca el destino; deja el componente cambiado si se cancela. */
    USE_SELECTION,

    /** Se marca el elemento sobre el que se esta. */
    ON,

    /** Se marca el hueco entre dos elementos. */
    INSERT,

    INSERT_ROWS,

    INSERT_COLS,

    /** Se marca el elemento o el hueco, segun donde caiga el cursor. */
    ON_OR_INSERT,

    ON_OR_INSERT_ROWS,

    ON_OR_INSERT_COLS
}
