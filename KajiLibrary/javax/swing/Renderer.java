package javax.swing;

import java.awt.Component;

/**
 * Dibuja un valor con un componente prestado.
 *
 * <h2>El componente se reusa</h2>
 *
 * <p>{@link #setValue} lo carga y {@link #getComponent} lo devuelve, y lo normal es que sea siempre
 * el <em>mismo</em> componente: en una lista de mil elementos se lo configura mil veces y se lo
 * dibuja mil veces, en vez de tener mil componentes. Por eso lo que devuelve no se puede guardar
 * para despues: en la proxima llamada muestra otra cosa.
 *
 * <p><strong>No la usa nadie.</strong> Swing terminó con dos interfaces mas especificas --
 * {@link ListCellRenderer} y {@link javax.swing.table.TableCellRenderer} --, que le pasan al
 * dibujante el contexto que esta no tiene: cual es la lista, en que fila esta, si tiene el foco.
 * Queda porque es publica.
 */
public interface Renderer {

    /** Carga el valor; el segundo parametro dice si esta elegido. */
    void setValue(Object aValue, boolean isSelected);

    /** El componente cargado; ver la nota de la interfaz. */
    Component getComponent();
}
