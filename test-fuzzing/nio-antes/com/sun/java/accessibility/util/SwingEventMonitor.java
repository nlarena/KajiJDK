package com.sun.java.accessibility.util;

/**
 * The same as {@link AWTEventMonitor}, for the events Swing adds.
 *
 * <h2>Why it is a separate class and not more methods in that one</h2>
 *
 * <p>Because AWT is in {@code java.desktop} without depending on Swing, and
 * {@code AWTEventMonitor} cannot name types of {@code javax.swing.event} without dragging it in.
 * Separating it lets an application of pure AWT not load Swing.
 *
 * <p>It inherits from {@link AWTEventMonitor} -- and shares its list of listeners -- so
 * registering here for an AWT event works the same: it is the class an accessibility tool uses
 * when it already knows that there is Swing.
 */
public class SwingEventMonitor extends AWTEventMonitor {

    /**
     * The list of Swing listeners, which <strong>covers</strong> {@link AWTEventMonitor}'s.
     *
     * <p>It is of {@code javax.swing.event.EventListenerList} and not of
     * {@link AccessibilityListenerList}, and that asymmetry is the JDK's: when this class was
     * written Swing already brought its own list of listeners with the same design, and reusing it
     * avoided a second implementation of the same thing.
     *
     * <p>The consequence is that the AWT listeners and the Swing ones live in <strong>two different
     * lists</strong>. It makes no difference for whoever registers -- the methods know which one to
     * put each thing in -- but it explains why the field is repeated.
     */
    protected static final javax.swing.event.EventListenerList listenerList =
            new javax.swing.event.EventListenerList();

    public SwingEventMonitor() {
    }

    /** It listens to the {@code Ancestor}-type events of any component. */
    public static void addAncestorListener(javax.swing.event.AncestorListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.AncestorListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeAncestorListener(javax.swing.event.AncestorListener l) {
        listenerList.remove(javax.swing.event.AncestorListener.class, l);
    }

    /** It listens to the {@code Caret}-type events of any component. */
    public static void addCaretListener(javax.swing.event.CaretListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.CaretListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeCaretListener(javax.swing.event.CaretListener l) {
        listenerList.remove(javax.swing.event.CaretListener.class, l);
    }

    /** It listens to the {@code CellEditor}-type events of any component. */
    public static void addCellEditorListener(javax.swing.event.CellEditorListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.CellEditorListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeCellEditorListener(javax.swing.event.CellEditorListener l) {
        listenerList.remove(javax.swing.event.CellEditorListener.class, l);
    }

    /** It listens to the {@code Change}-type events of any component. */
    public static void addChangeListener(javax.swing.event.ChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.ChangeListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeChangeListener(javax.swing.event.ChangeListener l) {
        listenerList.remove(javax.swing.event.ChangeListener.class, l);
    }

    /** It listens to the {@code ColumnModel}-type events of any component. */
    public static void addColumnModelListener(javax.swing.event.TableColumnModelListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TableColumnModelListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeColumnModelListener(javax.swing.event.TableColumnModelListener l) {
        listenerList.remove(javax.swing.event.TableColumnModelListener.class, l);
    }

    /** It listens to the {@code Document}-type events of any component. */
    public static void addDocumentListener(javax.swing.event.DocumentListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.DocumentListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeDocumentListener(javax.swing.event.DocumentListener l) {
        listenerList.remove(javax.swing.event.DocumentListener.class, l);
    }

    /** It listens to the {@code ListData}-type events of any component. */
    public static void addListDataListener(javax.swing.event.ListDataListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.ListDataListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeListDataListener(javax.swing.event.ListDataListener l) {
        listenerList.remove(javax.swing.event.ListDataListener.class, l);
    }

    /** It listens to the {@code ListSelection}-type events of any component. */
    public static void addListSelectionListener(javax.swing.event.ListSelectionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.ListSelectionListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeListSelectionListener(javax.swing.event.ListSelectionListener l) {
        listenerList.remove(javax.swing.event.ListSelectionListener.class, l);
    }

    /** It listens to the {@code Menu}-type events of any component. */
    public static void addMenuListener(javax.swing.event.MenuListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.MenuListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeMenuListener(javax.swing.event.MenuListener l) {
        listenerList.remove(javax.swing.event.MenuListener.class, l);
    }

    /** It listens to the {@code PopupMenu}-type events of any component. */
    public static void addPopupMenuListener(javax.swing.event.PopupMenuListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.PopupMenuListener.class, l);
    }

    /** It stops listening to them. */
    public static void removePopupMenuListener(javax.swing.event.PopupMenuListener l) {
        listenerList.remove(javax.swing.event.PopupMenuListener.class, l);
    }

    /** It listens to the {@code TableModel}-type events of any component. */
    public static void addTableModelListener(javax.swing.event.TableModelListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TableModelListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeTableModelListener(javax.swing.event.TableModelListener l) {
        listenerList.remove(javax.swing.event.TableModelListener.class, l);
    }

    /** It listens to the {@code TreeExpansion}-type events of any component. */
    public static void addTreeExpansionListener(javax.swing.event.TreeExpansionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TreeExpansionListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeTreeExpansionListener(javax.swing.event.TreeExpansionListener l) {
        listenerList.remove(javax.swing.event.TreeExpansionListener.class, l);
    }

    /** It listens to the {@code TreeModel}-type events of any component. */
    public static void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TreeModelListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        listenerList.remove(javax.swing.event.TreeModelListener.class, l);
    }

    /** It listens to the {@code TreeSelection}-type events of any component. */
    public static void addTreeSelectionListener(javax.swing.event.TreeSelectionListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.TreeSelectionListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeTreeSelectionListener(javax.swing.event.TreeSelectionListener l) {
        listenerList.remove(javax.swing.event.TreeSelectionListener.class, l);
    }

    /** It listens to the {@code UndoableEdit}-type events of any component. */
    public static void addUndoableEditListener(javax.swing.event.UndoableEditListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.UndoableEditListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeUndoableEditListener(javax.swing.event.UndoableEditListener l) {
        listenerList.remove(javax.swing.event.UndoableEditListener.class, l);
    }

    /** It listens to the {@code InternalFrame}-type events of any component. */
    public static void addInternalFrameListener(javax.swing.event.InternalFrameListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(javax.swing.event.InternalFrameListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeInternalFrameListener(javax.swing.event.InternalFrameListener l) {
        listenerList.remove(javax.swing.event.InternalFrameListener.class, l);
    }

    /** It listens to the {@code PropertyChange}-type events of any component. */
    public static void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.beans.PropertyChangeListener.class, l);
    }

    /** It stops listening to them. */
    public static void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        listenerList.remove(java.beans.PropertyChangeListener.class, l);
    }

    /** It listens to the {@code VetoableChange}-type events of any component. */
    public static void addVetoableChangeListener(java.beans.VetoableChangeListener l) {
        EventQueueMonitor.maybeInitialize();
        listenerList.add(java.beans.VetoableChangeListener.class, l);
    }

    /** It stops listening to them. */
    public static void removeVetoableChangeListener(java.beans.VetoableChangeListener l) {
        listenerList.remove(java.beans.VetoableChangeListener.class, l);
    }
}
