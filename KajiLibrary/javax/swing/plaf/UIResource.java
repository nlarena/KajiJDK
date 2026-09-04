package javax.swing.plaf;

/**
 * Marca "esto lo puso el aspecto, no el usuario".
 *
 * <p>Es la respuesta a una pregunta que todo aspecto tiene que hacerse al instalarse: si un boton
 * ya tiene un borde, ¿lo eligio el programador o lo dejo el aspecto anterior? Un valor que
 * implementa esta interfaz es del aspecto y se puede pisar; uno que no, es del usuario y se
 * respeta. No tiene metodos porque no hace nada: es una etiqueta en el tipo, no en el valor.
 *
 * <p>Las clases {@code ColorUIResource}, {@code FontUIResource}, {@code InsetsUIResource} y
 * {@code BorderUIResource} son exactamente eso: el valor de siempre, con la etiqueta puesta.
 */
public interface UIResource {
}
