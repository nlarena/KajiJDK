package javax.swing;

/**
 * Marca una clave de propiedad de cliente como propiedad del aspecto.
 *
 * <h2>Para que sirve una interfaz sin metodos</h2>
 *
 * <p>Las propiedades de cliente de un {@link JComponent} son un mapa abierto: cualquiera pone lo que
 * quiere. El aspecto tambien las usa, y al cambiar de aspecto hay que limpiar las suyas sin tocar
 * las del programa. La unica manera de distinguirlas es por el <em>tipo de la clave</em>, y para eso
 * esta esta interfaz: una clave que la implementa es del aspecto y se va con el.
 *
 * <p>De ahi que no tenga metodos. No hay nada que preguntarle a la clave; alcanza con saber de que
 * tipo es.
 */
public interface UIClientPropertyKey {
}
