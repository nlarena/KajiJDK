package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ListUI;
import javax.swing.text.Position;

/**
 * Una lista de renglones para elegir.
 *
 * <h2>Tres objetos, no uno</h2>
 *
 * <p>Los datos van en un {@link ListModel}, lo elegido en un {@link ListSelectionModel}, y como se
 * dibuja cada renglon en un {@link ListCellRenderer}. La lista no guarda nada de eso: los junta.
 *
 * <p>Separarlos es lo que permite mostrar un millon de renglones. El modelo puede calcular cada uno
 * al pedirlo, y el dibujante es un solo componente que se usa de sello; ver la nota de
 * {@link ListCellRenderer}.
 *
 * <h2>Por que la lista no se desplaza sola</h2>
 *
 * <p>Una {@code JList} no tiene barras. Implementa {@link Scrollable}, que es la interfaz con la
 * que le explica a un {@link JScrollPane} cuanto medir y de a cuanto avanzar. Poner una lista sin
 * meterla en un panel de desplazamiento es el error mas comun con esta clase: se ve entera y sin
 * barras, por mas larga que sea.
 *
 * <h2>Tamano fijo o medido</h2>
 *
 * <p>Medir cada renglon para saber cuanto mide la lista cuesta recorrerlos todos.
 * {@link #setFixedCellHeight} y {@link #setPrototypeCellValue} son dos formas de evitarlo: la
 * primera lo dice, la segunda da un valor de ejemplo del que se deduce. Con una lista larga la
 * diferencia se nota.
 *
 * @param <E> el tipo de los elementos.
 */
public class JList<E> extends JComponent implements Scrollable, Accessible {

    private static final String uiClassID = "ListUI";

    /** Los renglones van uno abajo del otro, en una sola columna. */
    public static final int VERTICAL = 0;

    /** Van en columnas, llenando una columna antes de pasar a la siguiente. */
    public static final int VERTICAL_WRAP = 1;

    /** Van en filas, llenando una fila antes de pasar a la siguiente. */
    public static final int HORIZONTAL_WRAP = 2;

    private int fixedCellWidth = -1;
    private int fixedCellHeight = -1;
    private int horizontalScrollIncrement = -1;
    private E prototypeCellValue;
    private int visibleRowCount = 8;
    private Color selectionForeground;
    private Color selectionBackground;
    private boolean dragEnabled;
    private ListSelectionModel selectionModel;
    private ListModel<E> dataModel;
    private ListCellRenderer<? super E> cellRenderer;
    private ListSelectionListener selectionListener;
    private int layoutOrientation = VERTICAL;
    private DropMode dropMode = DropMode.USE_SELECTION;
    private transient DropLocation dropLocation;
    private AccessibleContext accessibleContext;

    /** Una lista sobre ese modelo. */
    public JList(ListModel<E> dataModel) {
        if (dataModel == null) {
            throw new IllegalArgumentException("dataModel must be non null");
        }
        layoutOrientation = VERTICAL;
        this.dataModel = dataModel;
        selectionModel = createSelectionModel();
        setAutoscrolls(true);
        setOpaque(true);
        updateUI();
    }

    /** Una lista con esos elementos, sobre un modelo de solo lectura. */
    public JList(final E[] listData) {
        this(new ArregloModelo<E>(listData));
    }

    /** Una lista con los elementos de ese vector. */
    public JList(final Vector<? extends E> listData) {
        this(new VectorModelo<E>(listData));
    }

    /** Una lista vacia. */
    public JList() {
        this(new VacioModelo<E>());
    }

    /**
     * Un modelo de solo lectura sobre un arreglo.
     *
     * <p>No copia el arreglo. Es lo que hace el JDK y es lo que permite armar una lista sobre datos
     * grandes sin duplicarlos; el precio es que cambiar el arreglo por afuera deja la lista
     * mostrando lo viejo, porque el modelo no tiene como enterarse.
     */
    static class ArregloModelo<E> extends AbstractListModel<E> {

        private final E[] datos;

        ArregloModelo(E[] datos) {
            this.datos = datos;
        }

        public int getSize() {
            return datos.length;
        }

        public E getElementAt(int i) {
            return datos[i];
        }
    }

    /** Igual, sobre un vector. */
    static class VectorModelo<E> extends AbstractListModel<E> {

        private final Vector<? extends E> datos;

        VectorModelo(Vector<? extends E> datos) {
            this.datos = datos;
        }

        public int getSize() {
            return datos.size();
        }

        public E getElementAt(int i) {
            return datos.elementAt(i);
        }
    }

    /** El modelo de una lista vacia. */
    static class VacioModelo<E> extends AbstractListModel<E> {

        public int getSize() {
            return 0;
        }

        public E getElementAt(int i) {
            return null;
        }
    }

    /** El aspecto que dibuja la lista. */
    public ListUI getUI() {
        return (ListUI) ui;
    }

    public void setUI(ListUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
        setCellRenderer(null);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Un valor de ejemplo del que se deduce el tamano de un renglon; ver la nota de la clase. */
    public E getPrototypeCellValue() {
        return prototypeCellValue;
    }

    public void setPrototypeCellValue(E prototypeCellValue) {
        E oldValue = this.prototypeCellValue;
        this.prototypeCellValue = prototypeCellValue;
        if (prototypeCellValue != null && !prototypeCellValue.equals(oldValue)) {
            firePropertyChange("prototypeCellValue", oldValue, prototypeCellValue);
        }
    }

    public int getFixedCellWidth() {
        return fixedCellWidth;
    }

    /** El ancho de todos los renglones; con -1 se mide cada uno. */
    public void setFixedCellWidth(int width) {
        int oldValue = fixedCellWidth;
        fixedCellWidth = width;
        firePropertyChange("fixedCellWidth", oldValue, fixedCellWidth);
    }

    public int getFixedCellHeight() {
        return fixedCellHeight;
    }

    public void setFixedCellHeight(int height) {
        int oldValue = fixedCellHeight;
        fixedCellHeight = height;
        firePropertyChange("fixedCellHeight", oldValue, fixedCellHeight);
    }

    /** Quien dibuja cada renglon. */
    public ListCellRenderer<? super E> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(ListCellRenderer<? super E> cellRenderer) {
        ListCellRenderer<? super E> oldValue = this.cellRenderer;
        this.cellRenderer = cellRenderer;
        if (cellRenderer != null && !cellRenderer.equals(oldValue)) {
            firePropertyChange("cellRenderer", oldValue, cellRenderer);
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    /** El color de la letra de los renglones elegidos. */
    public void setSelectionForeground(Color selectionForeground) {
        Color oldValue = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", oldValue, selectionForeground);
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        Color oldValue = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", oldValue, selectionBackground);
    }

    /** Cuantos renglones se ven sin desplazar; es lo que la lista le pide al desplazador. */
    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    public void setVisibleRowCount(int visibleRowCount) {
        int oldValue = this.visibleRowCount;
        this.visibleRowCount = Math.max(0, visibleRowCount);
        firePropertyChange("visibleRowCount", oldValue, visibleRowCount);
    }

    /** Si los renglones van en una columna o se acomodan en varias. */
    public int getLayoutOrientation() {
        return layoutOrientation;
    }

    /**
     * Como se acomodan los renglones.
     *
     * @throws IllegalArgumentException si no es uno de los tres.
     */
    public void setLayoutOrientation(int layoutOrientation) {
        int oldValue = this.layoutOrientation;
        if (layoutOrientation != VERTICAL && layoutOrientation != VERTICAL_WRAP
                && layoutOrientation != HORIZONTAL_WRAP) {
            throw new IllegalArgumentException("layoutOrientation must be one of: "
                    + "VERTICAL, HORIZONTAL_WRAP or VERTICAL_WRAP");
        }
        this.layoutOrientation = layoutOrientation;
        firePropertyChange("layoutOrientation", oldValue, layoutOrientation);
    }

    /** El primer renglon que se ve, o -1 si no se ve ninguno. */
    public int getFirstVisibleIndex() {
        Rectangle r = getVisibleRect();
        int first = (r.width > 0 && r.height > 0) ? locationToIndex(r.getLocation()) : -1;
        return first;
    }

    public int getLastVisibleIndex() {
        Rectangle r = getVisibleRect();
        if (r.width <= 0 || r.height <= 0) {
            return -1;
        }
        Point ultimo = new Point(r.x + r.width - 1, r.y + r.height - 1);
        return locationToIndex(ultimo);
    }

    /** Desplaza para que ese renglon se vea. */
    public void ensureIndexIsVisible(int index) {
        Rectangle cellBounds = getCellBounds(index, index);
        if (cellBounds != null) {
            scrollRectToVisible(cellBounds);
        }
    }

    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * Como se muestra donde va a caer lo que se esta arrastrando.
     *
     * @throws IllegalArgumentException si el modo no sirve para una lista.
     */
    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            if (dropMode == DropMode.USE_SELECTION || dropMode == DropMode.ON
                    || dropMode == DropMode.INSERT || dropMode == DropMode.ON_OR_INSERT) {
                this.dropMode = dropMode;
                return;
            }
        }
        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for list");
    }

    public final DropMode getDropMode() {
        return dropMode;
    }

    /** Donde caeria ahora lo que se esta arrastrando, o nulo. */
    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * El proximo renglon cuyo texto empieza con eso.
     *
     * <p>Es lo que hace que escribir en una lista salte al renglon. La comparacion no distingue
     * mayusculas y usa el texto que muestra el dibujante, no el objeto: es lo que el usuario ve.
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        ListModel<E> model = getModel();
        int max = model.getSize();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase(java.util.Locale.ROOT);

        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do {
            E item = model.getElementAt(index);
            if (item != null) {
                String string = item.toString();
                if (string != null
                        && string.toUpperCase(java.util.Locale.ROOT).startsWith(prefix)) {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /** El texto de ayuda del renglon que esta bajo el mouse. */
    public String getToolTipText(MouseEvent event) {
        return super.getToolTipText(event);
    }

    /** Que renglon cae en ese punto; lo contesta el aspecto. */
    public int locationToIndex(Point location) {
        ListUI ui = getUI();
        return (ui != null) ? ui.locationToIndex(this, location) : -1;
    }

    public Point indexToLocation(int index) {
        ListUI ui = getUI();
        return (ui != null) ? ui.indexToLocation(this, index) : null;
    }

    /** El rectangulo que ocupan los renglones entre esos dos indices. */
    public Rectangle getCellBounds(int index0, int index1) {
        ListUI ui = getUI();
        return (ui != null) ? ui.getCellBounds(this, index0, index1) : null;
    }

    public ListModel<E> getModel() {
        return dataModel;
    }

    /**
     * Cambia el modelo de datos.
     *
     * <p>Vacia la seleccion: los indices elegidos se referian a los datos viejos, y conservarlos
     * dejaria elegidos renglones que no tienen nada que ver.
     */
    public void setModel(ListModel<E> model) {
        if (model == null) {
            throw new IllegalArgumentException("model must be non null");
        }
        ListModel<E> oldValue = dataModel;
        dataModel = model;
        firePropertyChange("model", oldValue, dataModel);
        clearSelection();
    }

    /** Reemplaza los datos por ese arreglo. */
    public void setListData(final E[] listData) {
        setModel(new ArregloModelo<E>(listData));
    }

    /** Reemplaza los datos por ese vector. */
    public void setListData(final Vector<? extends E> listData) {
        setModel(new VectorModelo<E>(listData));
    }

    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /** Reenvia el aviso del modelo de seleccion a quien escucha a la lista. */
    protected void fireSelectionValueChanged(int firstIndex, int lastIndex,
            boolean isAdjusting) {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    /**
     * Agrega quien quiera enterarse de los cambios de seleccion.
     *
     * <p>Se escucha a la lista y no al modelo de seleccion a proposito: cambiar el modelo no
     * deberia dejar sordo a quien se anoto. La lista se encarga de reengancharse.
     */
    public void addListSelectionListener(ListSelectionListener listener) {
        if (selectionListener == null) {
            selectionListener = new EscuchaSeleccion(this);
            getSelectionModel().addListSelectionListener(selectionListener);
        }
        listenerList.add(ListSelectionListener.class, listener);
    }

    public void removeListSelectionListener(ListSelectionListener listener) {
        listenerList.remove(ListSelectionListener.class, listener);
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }

    /** El puente entre el modelo de seleccion y quien escucha a la lista. */
    static class EscuchaSeleccion implements ListSelectionListener, java.io.Serializable {

        private final JList<?> lista;

        EscuchaSeleccion(JList<?> lista) {
            this.lista = lista;
        }

        public void valueChanged(ListSelectionEvent e) {
            lista.fireSelectionValueChanged(e.getFirstIndex(), e.getLastIndex(),
                    e.getValueIsAdjusting());
        }
    }

    /** Cambia el modelo de seleccion, llevandose el puente al nuevo. */
    public void setSelectionModel(ListSelectionModel selectionModel) {
        if (selectionModel == null) {
            throw new IllegalArgumentException("selectionModel must be non null");
        }
        if (selectionListener != null) {
            this.selectionModel.removeListSelectionListener(selectionListener);
            selectionModel.addListSelectionListener(selectionListener);
        }
        ListSelectionModel oldValue = this.selectionModel;
        this.selectionModel = selectionModel;
        firePropertyChange("selectionModel", oldValue, selectionModel);
    }

    public void setSelectionMode(int selectionMode) {
        getSelectionModel().setSelectionMode(selectionMode);
    }

    public int getSelectionMode() {
        return getSelectionModel().getSelectionMode();
    }

    public int getAnchorSelectionIndex() {
        return getSelectionModel().getAnchorSelectionIndex();
    }

    public int getLeadSelectionIndex() {
        return getSelectionModel().getLeadSelectionIndex();
    }

    public int getMinSelectionIndex() {
        return getSelectionModel().getMinSelectionIndex();
    }

    public int getMaxSelectionIndex() {
        return getSelectionModel().getMaxSelectionIndex();
    }

    public boolean isSelectedIndex(int index) {
        return getSelectionModel().isSelectedIndex(index);
    }

    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    public void setSelectionInterval(int anchor, int lead) {
        getSelectionModel().setSelectionInterval(anchor, lead);
    }

    public void addSelectionInterval(int anchor, int lead) {
        getSelectionModel().addSelectionInterval(anchor, lead);
    }

    public void removeSelectionInterval(int index0, int index1) {
        getSelectionModel().removeSelectionInterval(index0, index1);
    }

    public void setValueIsAdjusting(boolean b) {
        getSelectionModel().setValueIsAdjusting(b);
    }

    public boolean getValueIsAdjusting() {
        return getSelectionModel().getValueIsAdjusting();
    }

    /** Los indices elegidos, ordenados. */
    public int[] getSelectedIndices() {
        ListSelectionModel sm = getSelectionModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return new int[0];
        }
        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                rvTmp[n] = i;
                n++;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /** Elige solo ese renglon; con -1 no queda ninguno. */
    public void setSelectedIndex(int index) {
        if (index >= getModel().getSize()) {
            return;
        }
        getSelectionModel().setSelectionInterval(index, index);
    }

    /** Elige esos renglones y ninguno mas. */
    public void setSelectedIndices(int[] indices) {
        ListSelectionModel sm = getSelectionModel();
        sm.clearSelection();
        int size = getModel().getSize();
        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < size) {
                sm.addSelectionInterval(indices[i], indices[i]);
            }
        }
    }

    /**
     * Los elementos elegidos.
     *
     * @deprecated Usar {@link #getSelectedValuesList}, que devuelve una lista con el tipo puesto.
     */
    @Deprecated
    public Object[] getSelectedValues() {
        ListSelectionModel sm = getSelectionModel();
        ListModel<E> dm = getModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return new Object[0];
        }
        Object[] rvTmp = new Object[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                rvTmp[n] = dm.getElementAt(i);
                n++;
            }
        }
        Object[] rv = new Object[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    /** Los elementos elegidos, en orden. */
    public List<E> getSelectedValuesList() {
        ListSelectionModel sm = getSelectionModel();
        ListModel<E> dm = getModel();
        int iMin = sm.getMinSelectionIndex();
        int iMax = sm.getMaxSelectionIndex();
        if ((iMin < 0) || (iMax < 0)) {
            return java.util.Collections.emptyList();
        }
        List<E> selectedItems = new ArrayList<E>();
        for (int i = iMin; i <= iMax; i++) {
            if (sm.isSelectedIndex(i)) {
                selectedItems.add(dm.getElementAt(i));
            }
        }
        return selectedItems;
    }

    /** El primero de los elegidos, o -1. */
    public int getSelectedIndex() {
        return getMinSelectionIndex();
    }

    public E getSelectedValue() {
        int i = getMinSelectionIndex();
        return (i == -1) ? null : getModel().getElementAt(i);
    }

    /** Busca ese objeto en el modelo y lo elige. */
    public void setSelectedValue(Object anObject, boolean shouldScroll) {
        if (anObject == null) {
            setSelectedIndex(-1);
        } else if (!anObject.equals(getSelectedValue())) {
            ListModel<E> dm = getModel();
            int c = dm.getSize();
            for (int i = 0; i < c; i++) {
                if (anObject.equals(dm.getElementAt(i))) {
                    setSelectedIndex(i);
                    if (shouldScroll) {
                        ensureIndexIsVisible(i);
                    }
                    repaint();
                    return;
                }
            }
            setSelectedIndex(-1);
        }
        repaint();
    }

    /**
     * Cuanto pedirle al desplazador.
     *
     * <p>El alto sale de {@link #getVisibleRowCount} renglones, no de todos: es justamente lo que
     * distingue "cuanto quiero que se vea" de "cuanto mido".
     */
    public Dimension getPreferredScrollableViewportSize() {
        if (getLayoutOrientation() != VERTICAL) {
            return getPreferredSize();
        }
        Insets insets = getInsets();
        int dx = insets.left + insets.right;
        int dy = insets.top + insets.bottom;
        int visibleRowCount = getVisibleRowCount();
        int fixedCellWidth = getFixedCellWidth();
        int fixedCellHeight = getFixedCellHeight();

        if ((fixedCellWidth > 0) && (fixedCellHeight > 0)) {
            int width = fixedCellWidth + dx;
            int height = (visibleRowCount * fixedCellHeight) + dy;
            return new Dimension(width, height);
        }
        if (getModel().getSize() > 0) {
            Rectangle r = getCellBounds(0, 0);
            if (r != null) {
                int width = getPreferredSize().width;
                int height = (visibleRowCount * r.height) + dy;
                return new Dimension(width, height);
            }
        }
        int fixedCellWidth2 = (fixedCellWidth > 0) ? fixedCellWidth : 256;
        int fixedCellHeight2 = (fixedCellHeight > 0) ? fixedCellHeight : 16;
        return new Dimension(fixedCellWidth2, fixedCellHeight2 * visibleRowCount);
    }

    /** De a cuanto avanza una rueda del mouse: un renglon. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int row = locationToIndex(visibleRect.getLocation());
            if (row == -1) {
                return 0;
            }
            Rectangle r = getCellBounds(row, row);
            if (r == null) {
                return 0;
            }
            if (direction > 0) {
                return r.height - (visibleRect.y - r.y);
            }
            return (visibleRect.y - r.y > 0) ? visibleRect.y - r.y : r.height;
        }
        return (horizontalScrollIncrement > 0) ? horizontalScrollIncrement : 20;
    }

    /** De a cuanto avanza al hacer clic en la barra: una pantalla menos un renglon. */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int inc = visibleRect.height;
            int unidad = getScrollableUnitIncrement(visibleRect, orientation, direction);
            if (inc > unidad) {
                inc = inc - unidad;
            }
            return inc;
        }
        return visibleRect.width;
    }

    /** La lista se estira al ancho del desplazador cuando los renglones van en columna. */
    public boolean getScrollableTracksViewportWidth() {
        if (getLayoutOrientation() == VERTICAL_WRAP && getVisibleRowCount() <= 0) {
            return true;
        }
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        if (getLayoutOrientation() == HORIZONTAL_WRAP && getVisibleRowCount() <= 0) {
            return true;
        }
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * Donde caeria lo que se esta arrastrando.
     *
     * <p>{@link #isInsert} distingue las dos formas: soltar <em>sobre</em> un renglon lo reemplaza,
     * soltar <em>entre</em> dos inserta. La diferencia se ve en pantalla como una raya entre
     * renglones en lugar de un renglon resaltado.
     */
    public static final class DropLocation extends TransferHandler.DropLocation {

        private final int index;
        private final boolean isInsert;

        DropLocation(Point p, int index, boolean isInsert) {
            super(p);
            this.index = index;
            this.isInsert = isInsert;
        }

        /** El renglon sobre el que caeria, o donde se insertaria. */
        public int getIndex() {
            return index;
        }

        public boolean isInsert() {
            return isInsert;
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + getDropPoint() + ","
                    + "index=" + index + ","
                    + "insert=" + isInsert + "]";
        }
    }
}
