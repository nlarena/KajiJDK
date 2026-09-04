package javax.swing;

import javax.swing.event.ListSelectionListener;

/**
 * Que filas de una lista estan seleccionadas.
 *
 * <h2>Aparte del modelo de datos, a proposito</h2>
 *
 * <p>Que hay en la lista y que esta seleccionado son dos preguntas independientes, y separarlas
 * permite que dos vistas de los mismos datos tengan selecciones distintas — o que compartan una.
 *
 * <h2>{@code valueIsAdjusting}, que es lo que evita mil repintados</h2>
 *
 * <p>Arrastrar el mouse por veinte filas produce veinte cambios de seleccion. Con la bandera
 * prendida, quien escucha sabe que <em>vienen mas</em> y puede esperar: recalcular todo en cada paso
 * intermedio es trabajo tirado. Se apaga al soltar, y ese ultimo evento es el que vale.
 */
public interface ListSelectionModel {

    /** Solo una fila a la vez. */
    int SINGLE_SELECTION = 0;

    /** Un rango contiguo. */
    int SINGLE_INTERVAL_SELECTION = 1;

    /** Cualquier combinacion de filas. */
    int MULTIPLE_INTERVAL_SELECTION = 2;

    /** Selecciona el rango, descartando lo anterior. */
    void setSelectionInterval(int index0, int index1);

    /** Agrega el rango a la seleccion. */
    void addSelectionInterval(int index0, int index1);

    /** Saca el rango de la seleccion. */
    void removeSelectionInterval(int index0, int index1);

    /** La fila seleccionada mas chica, o {@code -1}. */
    int getMinSelectionIndex();

    /** La fila seleccionada mas grande, o {@code -1}. */
    int getMaxSelectionIndex();

    /** Si esa fila esta seleccionada. */
    boolean isSelectedIndex(int index);

    /** El extremo fijo del rango que se esta armando. */
    int getAnchorSelectionIndex();

    /** Fija el extremo fijo. */
    void setAnchorSelectionIndex(int index);

    /** El extremo movil del rango que se esta armando. */
    int getLeadSelectionIndex();

    /** Fija el extremo movil. */
    void setLeadSelectionIndex(int index);

    /** Deselecciona todo. */
    void clearSelection();

    /** Si no hay nada seleccionado. */
    boolean isSelectionEmpty();

    /** Avisa que se insertaron filas, para correr la seleccion. */
    void insertIndexInterval(int index, int length, boolean before);

    /** Avisa que se borraron filas. */
    void removeIndexInterval(int index0, int index1);

    /** Marca que vienen mas cambios; ver la nota de la interfaz. */
    void setValueIsAdjusting(boolean valueIsAdjusting);

    /** Si vienen mas cambios. */
    boolean getValueIsAdjusting();

    /** Cambia el modo de seleccion; una de las tres constantes. */
    void setSelectionMode(int selectionMode);

    /** El modo de seleccion. */
    int getSelectionMode();

    /** Agrega un oyente. */
    void addListSelectionListener(ListSelectionListener x);

    /** Saca un oyente. */
    void removeListSelectionListener(ListSelectionListener x);
}
