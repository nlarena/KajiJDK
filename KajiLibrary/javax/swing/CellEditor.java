package javax.swing;

import java.util.EventObject;

import javax.swing.event.CellEditorListener;

/**
 * Quien edita una celda de una tabla, un arbol o una lista.
 *
 * <h2>Por que la edicion es un objeto aparte</h2>
 *
 * <p>Editar una celda tiene un ciclo de vida que no cabe en un componente: empieza con un gesto
 * —un clic, dos, una tecla—, sigue mientras el usuario tipea, y termina de dos maneras distintas.
 * Esta interfaz es ese ciclo.
 *
 * <h2>Terminar no es una sola cosa</h2>
 *
 * <p>{@link #stopCellEditing} acepta el valor y <strong>puede negarse</strong>: devuelve
 * {@code false} si lo que se escribio no es valido, y la edicion sigue abierta con el foco donde
 * estaba. {@link #cancelCellEditing} descarta y no puede fallar.
 *
 * <p>Esa asimetria es lo que permite validar sin perder lo que el usuario tipeo. Un solo metodo
 * {@code stop} que no pudiera negarse obligaria a aceptar basura o a borrarla.
 *
 * <h2>{@link #isCellEditable} recibe el evento, no la celda</h2>
 *
 * <p>Porque la pregunta no es "¿esta celda se puede editar?" sino "¿<em>este gesto</em> empieza una
 * edicion?". Un solo clic selecciona y dos editan, y solo el evento distingue los dos casos.
 */
public interface CellEditor {

    /** Lo que el usuario dejo escrito. */
    Object getCellEditorValue();

    /** Si {@code anEvent} debe empezar una edicion; ver la nota de la interfaz. */
    boolean isCellEditable(EventObject anEvent);

    /** Si el gesto que empieza la edicion tambien debe seleccionar la celda. */
    boolean shouldSelectCell(EventObject anEvent);

    /** Termina aceptando; {@code false} si el valor no es valido y la edicion sigue. */
    boolean stopCellEditing();

    /** Termina descartando. No puede fallar. */
    void cancelCellEditing();

    /** Agrega un oyente. */
    void addCellEditorListener(CellEditorListener l);

    /** Saca un oyente. */
    void removeCellEditorListener(CellEditorListener l);
}
