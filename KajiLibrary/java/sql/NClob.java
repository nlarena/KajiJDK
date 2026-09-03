package java.sql;

/**
 * KajiLibrary's java.sql.NClob -- un {@link Clob} en el juego de caracteres **nacional**.
 *
 * <p>No agrega ni un miembro, y esa es toda su funcion: existe para que el tipo diga cual de los dos
 * juegos de caracteres usa la columna. En bases donde el juego normal no es Unicode, `NCHAR` es el
 * que si lo es, y meter uno donde va el otro corrompe el texto en silencio. La distincion vive en el
 * sistema de tipos justamente porque en tiempo de ejecucion no se nota.
 */
public interface NClob extends Clob {
}
