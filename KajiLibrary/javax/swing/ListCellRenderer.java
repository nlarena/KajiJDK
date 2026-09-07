package javax.swing;

import java.awt.Component;

/**
 * Quien convierte un elemento del modelo en algo que se pueda dibujar.
 *
 * <h2>Un solo componente para toda la lista</h2>
 *
 * <p>El metodo devuelve un componente, y lo normal es que devuelva <em>el mismo</em> cada vez, con
 * los valores cambiados. Una lista de mil renglones no arma mil componentes: arma uno y lo usa de
 * sello.
 *
 * <p>Eso explica por que {@link DefaultListCellRenderer} deja vacios sus metodos de repintado: un
 * componente que no esta en ninguna ventana no tiene nada que repintar, y avisar seria puro costo.
 *
 * @param <E> el tipo de los elementos.
 */
public interface ListCellRenderer<E> {

    /**
     * El componente que dibuja ese elemento.
     *
     * @param isSelected si el renglon esta elegido.
     * @param cellHasFocus si el renglon tiene el foco.
     */
    Component getListCellRendererComponent(JList<? extends E> list, E value, int index,
            boolean isSelected, boolean cellHasFocus);
}
