package java.awt.dnd;

import java.util.EventListener;

/**
 * Quien atiende el arrastre **desde el lado del origen**.
 *
 * <p>Es el espejo de {@link DropTargetListener}: los mismos momentos, vistos desde quien soltó el
 * dato en vez de quien lo recibe. Sirve sobre todo para dar realimentación —cambiar el cursor según
 * el destino acepte o no— y para enterarse, en {@link #dragDropEnd}, de si hubo que borrar el
 * original.
 *
 * <p>{@link #dragDropEnd} es el único que **siempre** llega, se haya soltado o cancelado, y es el
 * lugar donde va la limpieza.
 */
public interface DragSourceListener extends EventListener {

    /** El arrastre entró a un destino que lo acepta. */
    void dragEnter(DragSourceDragEvent dsde);

    /** El arrastre se mueve sobre un destino. */
    void dragOver(DragSourceDragEvent dsde);

    /** Cambió la acción elegida. */
    void dropActionChanged(DragSourceDragEvent dsde);

    /** El arrastre salió del destino. */
    void dragExit(DragSourceEvent dse);

    /** Terminó, con o sin soltado. */
    void dragDropEnd(DragSourceDropEvent dsde);
}
