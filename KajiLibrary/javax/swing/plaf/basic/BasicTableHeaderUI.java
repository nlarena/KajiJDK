package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.TableHeaderUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * El aspecto basico del encabezado de una tabla.
 *
 * <h2>El alto lo decide el titulo mas alto</h2>
 *
 * <p>El ancho preferido es la suma de los anchos preferidos de las columnas, y el alto es el del
 * titulo mas alto de todos. Eso quiere decir que agregar una columna con un titulo de dos renglones
 * agranda el encabezado entero, que es lo que corresponde: los titulos van alineados.
 *
 * <p>Un encabezado sin columnas mide cero por cero, no los margenes.
 *
 * <h2>Los tres tamanos no comparten el alto por casualidad</h2>
 *
 * <p>Minimo, preferido y maximo se diferencian solo en el ancho -- la suma de los minimos, de los
 * preferidos y de los maximos --; el alto es el mismo en los tres. Un encabezado no se estira ni se
 * achica verticalmente: los titulos entran o no entran.
 *
 * <h2>La columna bajo el mouse</h2>
 *
 * <p>{@link #getRolloverColumn} y {@link #rolloverColumnUpdated} son el gancho para que un aspecto
 * dibuje distinto la columna que tiene el mouse encima. El basico no dibuja nada distinto, pero
 * lleva la cuenta igual, porque el que decide es {@code paint} de la subclase.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>El arrastre de columnas -- mover una columna de lugar tirando de su titulo, y cambiarle el
 * ancho tirando del borde -- necesita el cursor del sistema y una pantalla. El escucha de mouse
 * esta y anota la columna, pero no arrastra.
 */
public class BasicTableHeaderUI extends TableHeaderUI {

    protected JTableHeader header;
    protected CellRendererPane rendererPane;
    protected MouseInputListener mouseInputListener;

    private int rolloverColumn = -1;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicTableHeaderUI() {
    }

    /** Uno nuevo por encabezado: guarda el componente y su panel de dibujantes. */
    public static ComponentUI createUI(JComponent h) {
        return new BasicTableHeaderUI();
    }

    public void installUI(JComponent c) {
        header = (JTableHeader) c;
        rendererPane = new CellRendererPane();
        header.add(rendererPane);
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        header.remove(rendererPane);
        rendererPane = null;
        header = null;
    }

    /** Colores y fuente; los valores son los de {@code TableHeader.*} en Metal. */
    protected void installDefaults() {
        Color fondo = header.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            header.setBackground(FONDO);
        }
        Color frente = header.getForeground();
        if (frente == null || frente instanceof UIResource) {
            header.setForeground(FRENTE);
        }
        Font fuente = header.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            header.setFont(FUENTE);
        }
        LookAndFeel.installProperty(header, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        mouseInputListener = createMouseInputListener();
        header.addMouseListener(mouseInputListener);
        header.addMouseMotionListener(mouseInputListener);
    }

    protected void uninstallListeners() {
        header.removeMouseListener(mouseInputListener);
        header.removeMouseMotionListener(mouseInputListener);
        mouseInputListener = null;
    }

    /** Sin atajos propios. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler();
    }

    /** La columna que tiene el mouse encima, o -1. */
    protected int getRolloverColumn() {
        return rolloverColumn;
    }

    /**
     * Aviso de que el mouse paso de una columna a otra.
     *
     * <p>El basico redibuja las dos; una subclase que las pinte distinto no tiene que hacer nada
     * mas.
     */
    protected void rolloverColumnUpdated(int oldColumn, int newColumn) {
        rolloverColumn = newColumn;
        repintarColumna(oldColumn);
        repintarColumna(newColumn);
    }

    private void repintarColumna(int column) {
        if (column < 0 || header == null) {
            return;
        }
        Rectangle r = header.getHeaderRect(column);
        header.repaint(r.x, 0, r.width, header.getHeight());
    }

    /** El dibujante de esa columna, ya cargado con su titulo. */
    private Component dibujante(int column) {
        TableColumn aColumn = header.getColumnModel().getColumn(column);
        TableCellRenderer renderer = aColumn.getHeaderRenderer();
        if (renderer == null) {
            renderer = header.getDefaultRenderer();
        }
        boolean tieneFoco = !header.isPaintingForPrint()
                && (column == getRolloverColumn())
                && (header.getDraggedColumn() == null);
        return renderer.getTableCellRendererComponent(header.getTable(),
                aColumn.getHeaderValue(), false, tieneFoco, -1, column);
    }

    /** El alto del titulo mas alto; ver la nota de la clase. */
    private int altoDelEncabezado() {
        int height = 0;
        TableColumnModel columnModel = header.getColumnModel();
        for (int column = 0; column < columnModel.getColumnCount(); column++) {
            Component comp = dibujante(column);
            height = Math.max(height, comp.getPreferredSize().height);
        }
        return height;
    }

    /**
     * El tamano a partir de un ancho ya sumado.
     *
     * <p>La separacion entre columnas <em>no</em> se suma. El JDK tiene ahi un comentario que dice
     * que la suman los que llaman, y ninguno la suma; el resultado medido es la suma pelada de los
     * anchos. Se copia asi: agregarla daria un encabezado dos pixeles mas ancho que la tabla.
     */
    private Dimension tamanio(long width) {
        if (width > Integer.MAX_VALUE) {
            width = Integer.MAX_VALUE;
        }
        return new Dimension((int) width, altoDelEncabezado());
    }

    /** La suma de los minimos de las columnas. */
    public Dimension getMinimumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = header.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMinWidth();
        }
        return tamanio(width);
    }

    /** La suma de los preferidos. */
    public Dimension getPreferredSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = header.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getPreferredWidth();
        }
        return tamanio(width);
    }

    /** La suma de los maximos, con tope. */
    public Dimension getMaximumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = header.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMaxWidth();
        }
        return tamanio(width);
    }

    /**
     * Donde apoya el texto de los titulos.
     *
     * <p>-1 si dos columnas no coinciden: un encabezado con titulos que apoyan a distinta altura no
     * tiene una linea de base, y contestar la de la primera seria mentir.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int baseline = -1;
        TableColumnModel columnModel = header.getColumnModel();
        for (int column = 0; column < columnModel.getColumnCount(); column++) {
            Component comp = dibujante(column);
            Dimension pref = comp.getPreferredSize();
            int columnBaseline = comp.getBaseline(pref.width, height);
            if (columnBaseline >= 0) {
                if (baseline == -1) {
                    baseline = columnBaseline;
                } else if (baseline != columnBaseline) {
                    baseline = -1;
                    break;
                }
            }
        }
        return baseline;
    }

    /** Los titulos de las columnas que se ven. */
    public void paint(Graphics g, JComponent c) {
        if (header.getColumnModel().getColumnCount() <= 0) {
            return;
        }
        Rectangle clip = g.getClipBounds();
        TableColumnModel cm = header.getColumnModel();
        int columnMargin = cm.getColumnMargin();
        Rectangle cellRect = header.getHeaderRect(0);
        cellRect.y = 0;
        cellRect.height = header.getHeight();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            TableColumn aColumn = cm.getColumn(column);
            cellRect.width = aColumn.getWidth();
            if (cellRect.intersects(clip)) {
                Component comp = dibujante(column);
                rendererPane.paintComponent(g, comp, header,
                        cellRect.x, cellRect.y,
                        cellRect.width - columnMargin, cellRect.height, true);
            }
            cellRect.x += cellRect.width;
        }
        rendererPane.removeAll();
    }

    /** Sigue por que columna anda el mouse; ver la nota de la clase. */
    private class Handler implements MouseInputListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            actualizar(e);
        }

        public void mouseExited(MouseEvent e) {
            int viejo = rolloverColumn;
            if (viejo != -1) {
                rolloverColumnUpdated(viejo, -1);
            }
        }

        public void mouseMoved(MouseEvent e) {
            actualizar(e);
        }

        public void mouseDragged(MouseEvent e) {
        }

        private void actualizar(MouseEvent e) {
            int columna = header.columnAtPoint(e.getPoint());
            if (columna != rolloverColumn) {
                rolloverColumnUpdated(rolloverColumn, columna);
            }
        }
    }
}
