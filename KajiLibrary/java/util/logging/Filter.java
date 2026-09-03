package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Filter -- el segundo criterio, despues del nivel.
 *
 * <p>El nivel decide por **importancia** y este por lo que sea: el nombre de la clase, el contenido
 * del mensaje, la hora. Se aplica despues del nivel a proposito, porque comparar dos enteros es
 * mucho mas barato que llamar a codigo del usuario.
 */
public interface Filter {

    /** Si ese registro debe publicarse. */
    boolean isLoggable(LogRecord record);
}
