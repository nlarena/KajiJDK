package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.LookAndFeel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ListUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de una lista.
 *
 * <h2>La tabla de alturas, y por que no hay una sola altura</h2>
 *
 * <p>Una lista puede tener todas sus filas iguales o cada una de un alto distinto, y la diferencia
 * cambia todo lo demas. Con altura fija, saber que fila cae en una coordenada es una division; con
 * alturas distintas hay que recorrer sumando. Por eso hay dos campos y solo uno esta puesto a la
 * vez: {@link #cellHeight} vale cuando todas son iguales y {@link #cellHeights} cuando no, y el otro
 * queda en -1 o en {@code null}.
 *
 * <p>Medir cada fila cuesta: hay que armar el dibujante con el valor de la fila y preguntarle cuanto
 * mide. Para una lista de diez mil elementos eso son diez mil mediciones, y por eso no se hacen
 * hasta que hacen falta: {@link #updateLayoutStateNeeded} junta las razones por las que la tabla
 * quedo vieja --cambio el modelo, la fuente, el dibujante-- y {@link #maybeUpdateLayoutState} la
 * rehace una sola vez, justo antes de que alguien pregunte algo que dependa de ella.
 *
 * <p>Las razones son banderas de bits porque se acumulan: entre dos dibujados pueden cambiar el
 * modelo y la fuente, y las dos tienen que quedar anotadas.
 *
 * <h2>El panel que no es un panel</h2>
 *
 * <p>{@link #rendererPane} existe porque un dibujante de celda es un componente de verdad que no
 * esta en ninguna ventana. Para pintarlo hay que darle un padre --si no, la cadena de dibujado no
 * funciona-- y ese padre no tiene que participar del acomodado ni del reparto de eventos. Eso es
 * exactamente {@link CellRendererPane}.
 *
 * <h2>Coordenadas de ida y de vuelta</h2>
 *
 * <p>{@link #convertYToRow} y {@link #convertRowToY} son las dos mitades de la misma cuenta, y las
 * dos tienen bordes raros que estan medidos: una coordenada mas abajo del final devuelve la ultima
 * fila, y una mas arriba del principio <em>tambien</em> --el recorrido no encuentra nada y se queda
 * con la ultima--. Es del JDK y se copia.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Las dos orientaciones que envuelven --{@code VERTICAL_WRAP} y {@code HORIZONTAL_WRAP}-- se
 * acomodan por columnas de ancho fijo {@link #cellWidth}; el JDK ademas reparte los sobrantes de la
 * ultima columna. La diferencia se ve en una lista envuelta cuyo total no es multiplo del numero de
 * columnas.
 */
public class BasicListUI extends ListUI {

    /** Cambio el modelo de datos. */
    protected static final int modelChanged = 1 << 0;

    /** Cambio el modelo de seleccion. */
    protected static final int selectionModelChanged = 1 << 1;

    /** Cambio la tipografia. */
    protected static final int fontChanged = 1 << 2;

    /** Cambio el ancho fijo de celda. */
    protected static final int fixedCellWidthChanged = 1 << 3;

    /** Cambio el alto fijo de celda. */
    protected static final int fixedCellHeightChanged = 1 << 4;

    /** Cambio el valor de muestra con el que se mide. */
    protected static final int prototypeCellValueChanged = 1 << 5;

    /** Cambio el dibujante de celdas. */
    protected static final int cellRendererChanged = 1 << 6;

    protected JList list = null;
    protected CellRendererPane rendererPane;

    protected FocusListener focusListener;
    protected MouseInputListener mouseInputListener;
    protected ListSelectionListener listSelectionListener;
    protected ListDataListener listDataListener;
    protected PropertyChangeListener propertyChangeListener;

    /** El alto de todas las filas cuando son iguales, o -1; ver la nota de la clase. */
    protected int cellHeight = -1;

    /** El ancho de la fila mas ancha, o -1. */
    protected int cellWidth = -1;

    /** El alto de cada fila cuando no son iguales, o {@code null}; ver la nota de la clase. */
    protected int[] cellHeights = null;

    /** Las razones por las que la tabla de alturas quedo vieja; ver la nota de la clase. */
    protected int updateLayoutStateNeeded = modelChanged;

    private static final ColorUIResource FONDO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource SELECCION = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    /** El dibujante con el que se mide la linea de base; uno solo alcanza para toda la VM. */
    private static Component dibujanteDeBase;

    public BasicListUI() {
    }

    /** Uno nuevo por lista: guarda la lista, sus escuchas y la tabla de alturas. */
    public static ComponentUI createUI(JComponent list) {
        return new BasicListUI();
    }

    public void installUI(JComponent c) {
        list = (JList) c;
        rendererPane = new CellRendererPane();
        list.add(rendererPane);
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallListeners();
        uninstallDefaults();
        uninstallKeyboardActions();
        cellWidth = cellHeight = -1;
        cellHeights = null;
        list.remove(rendererPane);
        rendererPane = null;
        list = null;
    }

    /** Colores, fuente, dibujante y opacidad; los valores son los de {@code List.*} en Metal. */
    protected void installDefaults() {
        list.setLayout(null);
        Color fondo = list.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            list.setBackground(FONDO);
        }
        Color frente = list.getForeground();
        if (frente == null || frente instanceof UIResource) {
            list.setForeground(FRENTE);
        }
        Font fuente = list.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            list.setFont(FUENTE);
        }
        LookAndFeel.installProperty(list, "opaque", Boolean.TRUE);
        if (list.getCellRenderer() == null) {
            list.setCellRenderer(new javax.swing.DefaultListCellRenderer.UIResource());
        }
        Color sbg = list.getSelectionBackground();
        if (sbg == null || sbg instanceof UIResource) {
            list.setSelectionBackground(SELECCION);
        }
        Color sfg = list.getSelectionForeground();
        if (sfg == null || sfg instanceof UIResource) {
            list.setSelectionForeground(FRENTE);
        }
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        focusListener = createFocusListener();
        mouseInputListener = createMouseInputListener();
        listSelectionListener = createListSelectionListener();
        listDataListener = createListDataListener();
        propertyChangeListener = createPropertyChangeListener();

        list.addFocusListener(focusListener);
        list.addMouseListener(mouseInputListener);
        list.addMouseMotionListener(mouseInputListener);
        list.addPropertyChangeListener(propertyChangeListener);
        if (list.getModel() != null) {
            list.getModel().addListDataListener(listDataListener);
        }
        if (list.getSelectionModel() != null) {
            list.getSelectionModel().addListSelectionListener(listSelectionListener);
        }
    }

    protected void uninstallListeners() {
        list.removeFocusListener(focusListener);
        list.removeMouseListener(mouseInputListener);
        list.removeMouseMotionListener(mouseInputListener);
        list.removePropertyChangeListener(propertyChangeListener);
        if (list.getModel() != null) {
            list.getModel().removeListDataListener(listDataListener);
        }
        if (list.getSelectionModel() != null) {
            list.getSelectionModel().removeListSelectionListener(listSelectionListener);
        }
        focusListener = null;
        mouseInputListener = null;
        listSelectionListener = null;
        listDataListener = null;
        propertyChangeListener = null;
    }

    /** Sin atajos propios: la navegacion con flechas la maneja la tabla de acciones del aspecto. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected FocusListener createFocusListener() {
        return new Handler();
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler();
    }

    protected ListSelectionListener createListSelectionListener() {
        return new Handler();
    }

    protected ListDataListener createListDataListener() {
        return new Handler();
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler();
    }

    /** Rehace la tabla si quedo vieja; ver la nota de la clase. */
    protected void maybeUpdateLayoutState() {
        if (updateLayoutStateNeeded != 0) {
            updateLayoutState();
            updateLayoutStateNeeded = 0;
        }
    }

    /**
     * Mide cada fila y arma la tabla; ver la nota de la clase.
     *
     * <p>Si la lista tiene ancho o alto fijo, esa mitad no se mide: se copia el numero. Y si no
     * tiene dibujante --que puede pasar entre dos llamadas a {@code setCellRenderer}-- todo queda
     * en cero en vez de reventar.
     */
    protected void updateLayoutState() {
        int fixedCellHeight = list.getFixedCellHeight();
        int fixedCellWidth = list.getFixedCellWidth();

        cellWidth = (fixedCellWidth != -1) ? fixedCellWidth : -1;

        if (fixedCellHeight != -1) {
            cellHeight = fixedCellHeight;
            cellHeights = null;
        } else {
            cellHeight = -1;
            cellHeights = new int[list.getModel().getSize()];
        }

        if ((fixedCellWidth == -1) || (fixedCellHeight == -1)) {
            ListModel dataModel = list.getModel();
            int dataModelSize = dataModel.getSize();
            ListCellRenderer renderer = list.getCellRenderer();

            if (renderer != null) {
                for (int index = 0; index < dataModelSize; index++) {
                    Object value = dataModel.getElementAt(index);
                    Component c = renderer.getListCellRendererComponent(list, value, index,
                            false, false);
                    rendererPane.add(c);
                    Dimension cellSize = c.getPreferredSize();
                    if (fixedCellWidth == -1) {
                        cellWidth = Math.max(cellSize.width, cellWidth);
                    }
                    if (fixedCellHeight == -1) {
                        cellHeights[index] = cellSize.height;
                    }
                }
            } else {
                if (cellWidth == -1) {
                    cellWidth = 0;
                }
                if (cellHeights == null) {
                    cellHeights = new int[dataModelSize];
                }
                for (int index = 0; index < dataModelSize; index++) {
                    cellHeights[index] = 0;
                }
            }
        }
        list.invalidate();
    }

    /** El alto de esa fila: el fijo, o el de la tabla. */
    protected int getRowHeight(int row) {
        if (row < 0 || row >= list.getModel().getSize()) {
            return -1;
        }
        return (cellHeights == null) ? cellHeight
                : ((row < cellHeights.length) ? cellHeights[row] : -1);
    }

    /** Que fila cae en esa coordenada; ver la nota de la clase sobre los bordes. */
    protected int convertYToRow(int y0) {
        int rowCount = list.getModel().getSize();
        if (rowCount <= 0) {
            return -1;
        }
        Insets insets = list.getInsets();
        if (cellHeights == null) {
            int row = (cellHeight == 0) ? 0 : ((y0 - insets.top) / cellHeight);
            return (row < 0) ? -1 : ((row >= rowCount) ? -1 : row);
        }
        if (rowCount > cellHeights.length) {
            return -1;
        }
        int y = insets.top;
        int row = 0;
        for (int i = 0; i < rowCount; i++) {
            if ((y0 >= y) && (y0 < y + cellHeights[i])) {
                return row;
            }
            y += cellHeights[i];
            row += 1;
        }
        return row - 1;
    }

    /** Donde arranca esa fila; -1 si no existe. */
    protected int convertRowToY(int row) {
        if (row >= list.getModel().getSize() || row < 0) {
            return -1;
        }
        Rectangle bounds = getCellBounds(list, row, row);
        return (bounds == null) ? -1 : bounds.y;
    }

    /** El rectangulo de una fila, o {@code null} si no existe. */
    private Rectangle bandaDeFila(int index) {
        maybeUpdateLayoutState();
        if (index < 0 || index >= list.getModel().getSize()) {
            return null;
        }
        Insets insets = list.getInsets();
        int y = insets.top;
        if (cellHeights == null) {
            y += index * cellHeight;
        } else {
            for (int i = 0; i < index && i < cellHeights.length; i++) {
                y += cellHeights[i];
            }
        }
        int w = list.getWidth() - (insets.left + insets.right);
        int h = (cellHeights == null) ? cellHeight
                : ((index < cellHeights.length) ? cellHeights[index] : 0);
        return new Rectangle(insets.left, y, w, h);
    }

    /**
     * El rectangulo que abarca de un indice al otro.
     *
     * <p>El orden no importa: se ordenan solos. {@code null} si el primero de los dos ya no existe.
     */
    public Rectangle getCellBounds(JList list, int index1, int index2) {
        maybeUpdateLayoutState();
        int minIndex = Math.min(index1, index2);
        int maxIndex = Math.max(index1, index2);
        if (minIndex >= list.getModel().getSize()) {
            return null;
        }
        Rectangle minBounds = bandaDeFila(minIndex);
        if (minBounds == null) {
            return null;
        }
        if (minIndex == maxIndex) {
            return minBounds;
        }
        Rectangle maxBounds = bandaDeFila(maxIndex);
        if (maxBounds != null) {
            minBounds.add(maxBounds);
        }
        return minBounds;
    }

    /** La esquina de arriba a la izquierda de esa fila, o {@code null}. */
    public Point indexToLocation(JList list, int index) {
        maybeUpdateLayoutState();
        Rectangle rect = bandaDeFila(index);
        return (rect != null) ? new Point(rect.x, rect.y) : null;
    }

    /** Que fila cae en ese punto; -1 si no hay ninguna. */
    public int locationToIndex(JList list, Point location) {
        maybeUpdateLayoutState();
        return convertYToRow(location.y);
    }

    /**
     * El alto de todas las filas mas los margenes; el ancho, el de la mas ancha.
     *
     * <p>Una lista vacia mide cero por cero, no los margenes.
     */
    public Dimension getPreferredSize(JComponent c) {
        maybeUpdateLayoutState();
        int lastRow = list.getModel().getSize() - 1;
        if (lastRow < 0) {
            return new Dimension(0, 0);
        }
        Insets insets = list.getInsets();
        int width = cellWidth + insets.left + insets.right;
        Rectangle bounds = bandaDeFila(lastRow);
        int height = (bounds != null) ? bounds.y + bounds.height + insets.bottom : 0;
        return new Dimension(width, height);
    }

    /**
     * Donde apoya el texto de la primera fila.
     *
     * <p>Se mide con el dibujante cargado con una letra, no con el contenido: la respuesta tiene
     * que ser la misma este la lista llena o vacia, porque quien la usa es un acomodador que todavia
     * no puso los datos.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int rowHeight = list.getFixedCellHeight();
        Component renderer = dibujanteDeBase;
        if (renderer == null) {
            ListCellRenderer lcr = list.getCellRenderer();
            if (lcr == null) {
                lcr = new javax.swing.DefaultListCellRenderer();
            }
            renderer = lcr.getListCellRendererComponent(list, "a", -1, false, false);
            dibujanteDeBase = renderer;
        }
        renderer.setFont(list.getFont());
        if (rowHeight > 0) {
            return renderer.getBaseline(Integer.MAX_VALUE, rowHeight);
        }
        Dimension pref = renderer.getPreferredSize();
        return renderer.getBaseline(pref.width, pref.height);
    }

    /**
     * {@code CONSTANT_ASCENT}: la primera fila esta siempre arriba de todo.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    /** Las filas que se ven, una por una. */
    public void paint(Graphics g, JComponent c) {
        maybeUpdateLayoutState();
        ListCellRenderer renderer = list.getCellRenderer();
        ListModel dataModel = list.getModel();
        ListSelectionModel selModel = list.getSelectionModel();
        if (renderer == null || dataModel.getSize() == 0) {
            return;
        }
        Rectangle paintBounds = g.getClipBounds();
        int primera = convertYToRow(paintBounds.y);
        int ultima = convertYToRow(paintBounds.y + paintBounds.height);
        if (primera < 0) {
            primera = 0;
        }
        if (ultima < 0) {
            ultima = dataModel.getSize() - 1;
        }
        int lead = list.getLeadSelectionIndex();
        for (int row = primera; row <= ultima && row < dataModel.getSize(); row++) {
            Rectangle rowBounds = bandaDeFila(row);
            if (rowBounds == null) {
                break;
            }
            paintCell(g, row, rowBounds, renderer, dataModel, selModel, lead);
        }
        rendererPane.removeAll();
    }

    /** Una fila: se arma el dibujante con su valor y se lo pinta en su rectangulo. */
    protected void paintCell(Graphics g, int row, Rectangle rowBounds, ListCellRenderer cellRenderer,
            ListModel dataModel, ListSelectionModel selModel, int leadIndex) {
        Object value = dataModel.getElementAt(row);
        boolean cellHasFocus = list.hasFocus() && (row == leadIndex);
        boolean isSelected = selModel.isSelectedIndex(row);
        Component rendererComponent = cellRenderer.getListCellRendererComponent(
                list, value, row, isSelected, cellHasFocus);
        rendererPane.paintComponent(g, rendererComponent, list,
                rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, true);
    }

    /** Mueve la seleccion una fila para arriba. */
    protected void selectPreviousIndex() {
        int s = list.getSelectedIndex();
        if (s > 0) {
            s -= 1;
            list.setSelectedIndex(s);
            list.ensureIndexIsVisible(s);
        }
    }

    /** Y una para abajo. */
    protected void selectNextIndex() {
        int s = list.getSelectedIndex();
        if ((s + 1) < list.getModel().getSize()) {
            s += 1;
            list.setSelectedIndex(s);
            list.ensureIndexIsVisible(s);
        }
    }

    /**
     * El que escucha todo.
     *
     * <p>Cinco interfaces en un objeto por lo mismo que en {@link BasicMenuItemUI}: las cinco
     * reaccionan al mismo estado --que la tabla de alturas quedo vieja-- y separarlas obligaria a
     * compartirlo.
     */
    private class Handler implements FocusListener, MouseInputListener, ListSelectionListener,
            ListDataListener, PropertyChangeListener {

        public void focusGained(FocusEvent e) {
            repintarSeleccion();
        }

        public void focusLost(FocusEvent e) {
            repintarSeleccion();
        }

        private void repintarSeleccion() {
            int lead = list.getLeadSelectionIndex();
            if (lead != -1) {
                Rectangle r = getCellBounds(list, lead, lead);
                if (r != null) {
                    list.repaint(r.x, r.y, r.width, r.height);
                }
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!list.isEnabled()) {
                return;
            }
            if (!list.hasFocus() && list.isRequestFocusEnabled()) {
                list.requestFocus();
            }
            int row = convertYToRow(e.getY());
            if (row < 0) {
                return;
            }
            if (e.isControlDown()) {
                if (list.isSelectedIndex(row)) {
                    list.removeSelectionInterval(row, row);
                } else {
                    list.addSelectionInterval(row, row);
                }
            } else if (e.isShiftDown()) {
                list.setSelectionInterval(list.getAnchorSelectionIndex(), row);
            } else {
                list.setSelectionInterval(row, row);
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (!list.isEnabled()) {
                return;
            }
            int row = convertYToRow(e.getY());
            if (row >= 0) {
                list.setSelectionInterval(list.getAnchorSelectionIndex(), row);
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        public void valueChanged(ListSelectionEvent e) {
            maybeUpdateLayoutState();
            int first = e.getFirstIndex();
            int last = e.getLastIndex();
            if (first < 0 || last < 0) {
                list.repaint();
                return;
            }
            Rectangle r = getCellBounds(list, first, last);
            if (r != null) {
                list.repaint(r.x, r.y, r.width, r.height);
            } else {
                list.repaint();
            }
        }

        public void intervalAdded(ListDataEvent e) {
            updateLayoutStateNeeded |= modelChanged;
            list.revalidate();
            list.repaint();
        }

        public void intervalRemoved(ListDataEvent e) {
            updateLayoutStateNeeded |= modelChanged;
            list.revalidate();
            list.repaint();
        }

        public void contentsChanged(ListDataEvent e) {
            updateLayoutStateNeeded |= modelChanged;
            list.revalidate();
            list.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ("model".equals(name)) {
                ListModel viejo = (ListModel) e.getOldValue();
                if (viejo != null) {
                    viejo.removeListDataListener(listDataListener);
                }
                ListModel nuevo = (ListModel) e.getNewValue();
                if (nuevo != null) {
                    nuevo.addListDataListener(listDataListener);
                }
                updateLayoutStateNeeded |= modelChanged;
            } else if ("selectionModel".equals(name)) {
                ListSelectionModel viejo = (ListSelectionModel) e.getOldValue();
                if (viejo != null) {
                    viejo.removeListSelectionListener(listSelectionListener);
                }
                ListSelectionModel nuevo = (ListSelectionModel) e.getNewValue();
                if (nuevo != null) {
                    nuevo.addListSelectionListener(listSelectionListener);
                }
                updateLayoutStateNeeded |= selectionModelChanged;
            } else if ("font".equals(name)) {
                updateLayoutStateNeeded |= fontChanged;
            } else if ("fixedCellWidth".equals(name)) {
                updateLayoutStateNeeded |= fixedCellWidthChanged;
            } else if ("fixedCellHeight".equals(name)) {
                updateLayoutStateNeeded |= fixedCellHeightChanged;
            } else if ("prototypeCellValue".equals(name)) {
                updateLayoutStateNeeded |= prototypeCellValueChanged;
            } else if ("cellRenderer".equals(name)) {
                updateLayoutStateNeeded |= cellRendererChanged;
            } else {
                return;
            }
            list.revalidate();
            list.repaint();
        }
    }
}
