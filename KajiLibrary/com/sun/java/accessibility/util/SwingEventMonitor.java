package com.sun.java.accessibility.util;

/**
 * Lo mismo que {@link AWTEventMonitor}, para los eventos que agrega Swing.
 *
 * <h2>Por que es una clase aparte y no mas metodos en aquella</h2>
 *
 * <p>Porque AWT esta en {@code java.desktop} sin depender de Swing, y {@code AWTEventMonitor} no
 * puede nombrar tipos de {@code javax.swing.event} sin arrastrarlo. Separarlo deja que una
 * aplicacion de AWT puro no cargue Swing.
 *
 * <p>Hereda de {@link AWTEventMonitor} —y comparte su lista de oyentes— asi que registrarse aca
 * para un evento de AWT funciona igual: es la clase que una herramienta de accesibilidad usa cuando
 * ya sabe que hay Swing.
 */
public class SwingEventMonitor extends AWTEventMonitor {

    /**
     * La lista de oyentes de Swing, que <strong>tapa</strong> la de {@link AWTEventMonitor}.
     *
     * <p>Es de {@code javax.swing.event.EventListenerList} y no de
     * {@link AccessibilityListenerList}, y esa asimetria es del JDK: cuando se escribio esta clase
     * Swing ya traia su propia lista de oyentes con el mismo diseno, y reusarla evito una segunda
     * implementacion de lo mismo.
     *
     * <p>La consecuencia es que los oyentes de AWT y los de Swing viven en <strong>dos listas
     * distintas</strong>. Da igual para quien se registra —los metodos saben en cual poner cada
     * cosa— pero explica por que el campo esta repetido.
     */
    protected static final javax.swing.event.EventListenerList listenerList =
            new javax.swing.event.EventListenerList();

    public SwingEventMonitor() {
    }

    /** Escucha los eventos de tipo {@code Ancestor} de cualquier componente. */
    public static void addAncestorListener(javax.swing.event.AncestorListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.AncestorListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeAncestorListener(javax.swing.event.AncestorListener l) {
        listenerList.remove(javax.swing.event.AncestorListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Caret} de cualquier componente. */
    public static void addCaretListener(javax.swing.event.CaretListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.CaretListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeCaretListener(javax.swing.event.CaretListener l) {
        listenerList.remove(javax.swing.event.CaretListener.class, l);
    }

    /** Escucha los eventos de tipo {@code CellEditor} de cualquier componente. */
    public static void addCellEditorListener(javax.swing.event.CellEditorListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.CellEditorListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeCellEditorListener(javax.swing.event.CellEditorListener l) {
        listenerList.remove(javax.swing.event.CellEditorListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Change} de cualquier componente. */
    public static void addChangeListener(javax.swing.event.ChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.ChangeListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeChangeListener(javax.swing.event.ChangeListener l) {
        listenerList.remove(javax.swing.event.ChangeListener.class, l);
    }

    /** Escucha los eventos de tipo {@code ColumnModel} de cualquier componente. */
    public static void addColumnModelListener(javax.swing.event.TableColumnModelListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TableColumnModelListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeColumnModelListener(javax.swing.event.TableColumnModelListener l) {
        listenerList.remove(javax.swing.event.TableColumnModelListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Document} de cualquier componente. */
    public static void addDocumentListener(javax.swing.event.DocumentListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.DocumentListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeDocumentListener(javax.swing.event.DocumentListener l) {
        listenerList.remove(javax.swing.event.DocumentListener.class, l);
    }

    /** Escucha los eventos de tipo {@code ListData} de cualquier componente. */
    public static void addListDataListener(javax.swing.event.ListDataListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.ListDataListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeListDataListener(javax.swing.event.ListDataListener l) {
        listenerList.remove(javax.swing.event.ListDataListener.class, l);
    }

    /** Escucha los eventos de tipo {@code ListSelection} de cualquier componente. */
    public static void addListSelectionListener(javax.swing.event.ListSelectionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.ListSelectionListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeListSelectionListener(javax.swing.event.ListSelectionListener l) {
        listenerList.remove(javax.swing.event.ListSelectionListener.class, l);
    }

    /** Escucha los eventos de tipo {@code Menu} de cualquier componente. */
    public static void addMenuListener(javax.swing.event.MenuListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.MenuListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeMenuListener(javax.swing.event.MenuListener l) {
        listenerList.remove(javax.swing.event.MenuListener.class, l);
    }

    /** Escucha los eventos de tipo {@code PopupMenu} de cualquier componente. */
    public static void addPopupMenuListener(javax.swing.event.PopupMenuListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.PopupMenuListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removePopupMenuListener(javax.swing.event.PopupMenuListener l) {
        listenerList.remove(javax.swing.event.PopupMenuListener.class, l);
    }

    /** Escucha los eventos de tipo {@code TableModel} de cualquier componente. */
    public static void addTableModelListener(javax.swing.event.TableModelListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TableModelListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeTableModelListener(javax.swing.event.TableModelListener l) {
        listenerList.remove(javax.swing.event.TableModelListener.class, l);
    }

    /** Escucha los eventos de tipo {@code TreeExpansion} de cualquier componente. */
    public static void addTreeExpansionListener(javax.swing.event.TreeExpansionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TreeExpansionListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeTreeExpansionListener(javax.swing.event.TreeExpansionListener l) {
        listenerList.remove(javax.swing.event.TreeExpansionListener.class, l);
    }

    /** Escucha los eventos de tipo {@code TreeModel} de cualquier componente. */
    public static void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TreeModelListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        listenerList.remove(javax.swing.event.TreeModelListener.class, l);
    }

    /** Escucha los eventos de tipo {@code TreeSelection} de cualquier componente. */
    public static void addTreeSelectionListener(javax.swing.event.TreeSelectionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TreeSelectionListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeTreeSelectionListener(javax.swing.event.TreeSelectionListener l) {
        listenerList.remove(javax.swing.event.TreeSelectionListener.class, l);
    }

    /** Escucha los eventos de tipo {@code UndoableEdit} de cualquier componente. */
    public static void addUndoableEditListener(javax.swing.event.UndoableEditListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.UndoableEditListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeUndoableEditListener(javax.swing.event.UndoableEditListener l) {
        listenerList.remove(javax.swing.event.UndoableEditListener.class, l);
    }

    /** Escucha los eventos de tipo {@code InternalFrame} de cualquier componente. */
    public static void addInternalFrameListener(javax.swing.event.InternalFrameListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.InternalFrameListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeInternalFrameListener(javax.swing.event.InternalFrameListener l) {
        listenerList.remove(javax.swing.event.InternalFrameListener.class, l);
    }

    /** Escucha los eventos de tipo {@code PropertyChange} de cualquier componente. */
    public static void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.beans.PropertyChangeListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        listenerList.remove(java.beans.PropertyChangeListener.class, l);
    }

    /** Escucha los eventos de tipo {@code VetoableChange} de cualquier componente. */
    public static void addVetoableChangeListener(java.beans.VetoableChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.beans.VetoableChangeListener.class, l);
    }

    /** Deja de escucharlos. */
    public static void removeVetoableChangeListener(java.beans.VetoableChangeListener l) {
        listenerList.remove(java.beans.VetoableChangeListener.class, l);
    }
}
