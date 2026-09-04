package java.awt;

import java.awt.event.ItemListener;

/**
 * Algo con elementos que se pueden elegir: una lista, un desplegable, una casilla.
 *
 * <p>Lo que tienen en común no es cómo se ven sino qué anuncian: cuáles de sus elementos están
 * elegidos, y a quién avisarle cuando eso cambia.
 */
public interface ItemSelectable {

    /** Los elementos elegidos, o `null` si no hay ninguno. */
    Object[] getSelectedObjects();

    /** Suma alguien a quien avisarle de los cambios. */
    void addItemListener(ItemListener l);

    /** Saca a ese oyente. */
    void removeItemListener(ItemListener l);
}
