package javax.swing;

import javax.swing.event.ChangeListener;

/**
 * Un indice elegido, o ninguno.
 *
 * <h2>Cuando alcanza con esto</h2>
 *
 * <p>Una solapa abierta, una opcion de menu resaltada, un panel visible. Todo lo que tiene
 * exactamente un elegido usa este modelo y no {@link ListSelectionModel}, que sabe de tramos y de
 * ancla y guia: esas tres cosas no significan nada cuando solo puede haber uno.
 *
 * <p>{@link #isSelected} y {@code getSelectedIndex() != -1} dicen lo mismo. Los dos estan porque el
 * segundo obliga a saber que el -1 es el valor especial.
 */
public interface SingleSelectionModel {

    /** El indice elegido, o -1. */
    int getSelectedIndex();

    /** Elige ese indice; con -1 no queda ninguno. */
    void setSelectedIndex(int index);

    void clearSelection();

    /** Si hay alguno elegido. */
    boolean isSelected();

    void addChangeListener(ChangeListener listener);

    void removeChangeListener(ChangeListener listener);
}
