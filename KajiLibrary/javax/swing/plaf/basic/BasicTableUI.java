package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.LookAndFeel;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.TableUI;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * El aspecto basico de una tabla.
 *
 * <h2>El alto sale de la ultima fila, no de una multiplicacion</h2>
 *
 * <p>Podria ser {@code filas * altoDeFila}, y no lo es: {@link #getPreferredSize} le pregunta a la
 * tabla donde termina la ultima fila. La diferencia importa apenas las filas dejan de medir todas
 * lo mismo -- una tabla con alturas por fila, que es lo que hace {@code JTable.setRowHeight(fila,
 * alto)} --, y ahi la multiplicacion daria cualquier cosa.
 *
 * <p>El ancho si es una suma: los anchos preferidos de las columnas. El minimo y el maximo son la
 * misma cuenta con los minimos y los maximos, y los tres comparten el alto: una tabla no se estira
 * verticalmente por su cuenta, la estira quien la contiene.
 *
 * <p>Una tabla sin filas ni columnas mide cero por cero en los tres.
 *
 * <h2>El dibujante de la linea de base es uno solo</h2>
 *
 * <p>Y no tiene nada que ver con el contenido: se lo carga con una letra y se le pregunta. Tiene que
 * ser asi -- quien pregunta por la linea de base es un acomodador, que llama antes de que haya
 * datos --, y ademas seria carisimo hacerlo con la primera celda de verdad.
 *
 * <h2>Sin escucha de teclado</h2>
 *
 * <p>{@link #keyListener} queda en {@code null}. La navegacion con flechas de una tabla no es un
 * escucha de teclas: son cuarenta y cuatro acciones con nombre en el mapa de acciones, atadas a
 * teclas por la tabla del aspecto. Medido, y es la misma historia que en
 * {@link BasicMenuItemUI#createMenuKeyListener}.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Las teclas no estan atadas: las acciones existen en el mapa, pero que tecla dispara cual sale
 * de la tabla del aspecto, y sin tabla no hay ninguna. Es el mismo hueco que en
 * {@link BasicDesktopPaneUI}.
 */
public class BasicTableUI extends TableUI {

    protected JTable table;
    protected CellRendererPane rendererPane;
    protected KeyListener keyListener;
    protected FocusListener focusListener;
    protected MouseInputListener mouseInputListener;

    private static final ColorUIResource FONDO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final ColorUIResource SELECCION = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource CUADRICULA = new ColorUIResource(122, 138, 153);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.PLAIN, 12);

    /** El dibujante con el que se mide la linea de base; ver la nota de la clase. */
    private static Component dibujanteDeBase;

    public BasicTableUI() {
    }

    /** Uno nuevo por tabla: guarda el componente y su panel de dibujantes. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTableUI();
    }

    public void installUI(JComponent c) {
        table = (JTable) c;
        rendererPane = new CellRendererPane();
        table.add(rendererPane);
        installDefaults();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        table.remove(rendererPane);
        rendererPane = null;
        table = null;
    }

    /** Colores, fuente y cuadricula; los valores son los de {@code Table.*} en Metal. */
    protected void installDefaults() {
        Color fondo = table.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            table.setBackground(FONDO);
        }
        Color frente = table.getForeground();
        if (frente == null || frente instanceof UIResource) {
            table.setForeground(FRENTE);
        }
        Font fuente = table.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            table.setFont(FUENTE);
        }
        Color sbg = table.getSelectionBackground();
        if (sbg == null || sbg instanceof UIResource) {
            table.setSelectionBackground(SELECCION);
        }
        Color sfg = table.getSelectionForeground();
        if (sfg == null || sfg instanceof UIResource) {
            table.setSelectionForeground(FRENTE);
        }
        Color grid = table.getGridColor();
        if (grid == null || grid instanceof UIResource) {
            table.setGridColor(CUADRICULA);
        }
        LookAndFeel.installProperty(table, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        focusListener = createFocusListener();
        keyListener = createKeyListener();
        mouseInputListener = createMouseInputListener();
        table.addFocusListener(focusListener);
        if (keyListener != null) {
            table.addKeyListener(keyListener);
        }
        table.addMouseListener(mouseInputListener);
        table.addMouseMotionListener(mouseInputListener);
    }

    protected void uninstallListeners() {
        table.removeFocusListener(focusListener);
        if (keyListener != null) {
            table.removeKeyListener(keyListener);
        }
        table.removeMouseListener(mouseInputListener);
        table.removeMouseMotionListener(mouseInputListener);
        focusListener = null;
        keyListener = null;
        mouseInputListener = null;
    }

    /** Las acciones con nombre; ver la nota de la clase sobre las teclas. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected FocusListener createFocusListener() {
        return new Handler();
    }

    /** Ninguno; ver la nota de la clase. */
    protected KeyListener createKeyListener() {
        return null;
    }

    protected MouseInputListener createMouseInputListener() {
        return new Handler();
    }

    /** El ancho pedido y el alto de la ultima fila; ver la nota de la clase. */
    private Dimension tamanio(long width) {
        int height = 0;
        int rowCount = table.getRowCount();
        if (rowCount > 0 && table.getColumnCount() > 0) {
            Rectangle r = table.getCellRect(rowCount - 1, 0, true);
            height = r.y + r.height;
        }
        if (width > Integer.MAX_VALUE) {
            width = Integer.MAX_VALUE;
        }
        return new Dimension((int) width, height);
    }

    public Dimension getMinimumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMinWidth();
        }
        return tamanio(width);
    }

    public Dimension getPreferredSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getPreferredWidth();
        }
        return tamanio(width);
    }

    public Dimension getMaximumSize(JComponent c) {
        long width = 0;
        Enumeration<TableColumn> e = table.getColumnModel().getColumns();
        while (e.hasMoreElements()) {
            width = width + e.nextElement().getMaxWidth();
        }
        return tamanio(width);
    }

    /**
     * Donde apoya el texto de la primera fila; ver la nota de la clase.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        Component renderer = dibujanteDeBase;
        if (renderer == null) {
            javax.swing.table.DefaultTableCellRenderer tableRenderer =
                    new javax.swing.table.DefaultTableCellRenderer();
            renderer = tableRenderer.getTableCellRendererComponent(table, "a", false, false,
                    -1, -1);
            dibujanteDeBase = renderer;
        }
        renderer.setFont(table.getFont());
        int rowMargin = table.getRowMargin();
        return renderer.getBaseline(Integer.MAX_VALUE, table.getRowHeight() - rowMargin)
                + rowMargin / 2;
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

    /** La cuadricula y las celdas que se ven. */
    public void paint(Graphics g, JComponent c) {
        Rectangle clip = g.getClipBounds();
        Rectangle bounds = table.getBounds();
        bounds.x = 0;
        bounds.y = 0;
        if (table.getRowCount() <= 0 || table.getColumnCount() <= 0
                || !bounds.intersects(clip)) {
            return;
        }
        java.awt.Point upperLeft = clip.getLocation();
        java.awt.Point lowerRight = new java.awt.Point(clip.x + clip.width - 1,
                clip.y + clip.height - 1);
        int rMin = table.rowAtPoint(upperLeft);
        int rMax = table.rowAtPoint(lowerRight);
        if (rMin == -1) {
            rMin = 0;
        }
        if (rMax == -1) {
            rMax = table.getRowCount() - 1;
        }
        int cMin = table.columnAtPoint(upperLeft);
        int cMax = table.columnAtPoint(lowerRight);
        if (cMin == -1) {
            cMin = 0;
        }
        if (cMax == -1) {
            cMax = table.getColumnCount() - 1;
        }

        pintarCuadricula(g, rMin, rMax, cMin, cMax);
        pintarCeldas(g, rMin, rMax, cMin, cMax);
        rendererPane.removeAll();
    }

    private void pintarCuadricula(Graphics g, int rMin, int rMax, int cMin, int cMax) {
        g.setColor(table.getGridColor());
        Rectangle minCell = table.getCellRect(rMin, cMin, true);
        Rectangle maxCell = table.getCellRect(rMax, cMax, true);
        Rectangle damaged = minCell.union(maxCell);

        if (table.getShowHorizontalLines()) {
            int tableWidth = damaged.x + damaged.width;
            int y = damaged.y;
            for (int row = rMin; row <= rMax; row++) {
                y += table.getRowHeight(row);
                g.drawLine(damaged.x, y - 1, tableWidth - 1, y - 1);
            }
        }
        if (table.getShowVerticalLines()) {
            int tableHeight = damaged.y + damaged.height;
            int x = damaged.x;
            for (int column = cMin; column <= cMax; column++) {
                x += table.getColumnModel().getColumn(column).getWidth();
                g.drawLine(x - 1, damaged.y, x - 1, tableHeight - 1);
            }
        }
    }

    private void pintarCeldas(Graphics g, int rMin, int rMax, int cMin, int cMax) {
        for (int row = rMin; row <= rMax; row++) {
            for (int column = cMin; column <= cMax; column++) {
                Rectangle cellRect = table.getCellRect(row, column, false);
                pintarCelda(g, cellRect, row, column);
            }
        }
    }

    private void pintarCelda(Graphics g, Rectangle cellRect, int row, int column) {
        if (table.isEditing() && table.getEditingRow() == row
                && table.getEditingColumn() == column) {
            Component component = table.getEditorComponent();
            component.setBounds(cellRect);
            component.validate();
            return;
        }
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        Component component = table.prepareRenderer(renderer, row, column);
        rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                cellRect.width, cellRect.height, true);
    }

    /**
     * El que escucha el foco y el mouse.
     *
     * <p>La seleccion con el mouse la resuelve aca y no en la tabla, porque depende de las teclas
     * que esten apretadas al mismo tiempo -- control agrega, mayusculas extiende -- y eso es
     * convencion de plataforma, no del modelo.
     */
    private class Handler implements FocusListener, MouseInputListener {

        public void focusGained(FocusEvent e) {
            repintarSeleccion();
        }

        public void focusLost(FocusEvent e) {
            repintarSeleccion();
        }

        private void repintarSeleccion() {
            int fila = table.getSelectionModel().getLeadSelectionIndex();
            int col = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
            if (fila >= 0 && col >= 0) {
                table.repaint(table.getCellRect(fila, col, false));
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!table.isEnabled()) {
                return;
            }
            if (!table.hasFocus() && table.isRequestFocusEnabled()) {
                table.requestFocus();
            }
            java.awt.Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            if (row == -1 || column == -1) {
                return;
            }
            if (table.editCellAt(row, column, e)) {
                Component editor = table.getEditorComponent();
                if (editor != null) {
                    editor.requestFocus();
                }
                return;
            }
            if (e.isControlDown()) {
                table.changeSelection(row, column, true, false);
            } else if (e.isShiftDown()) {
                table.changeSelection(row, column, false, true);
            } else {
                table.changeSelection(row, column, false, false);
            }
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (!table.isEnabled()) {
                return;
            }
            java.awt.Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int column = table.columnAtPoint(p);
            if (row == -1 || column == -1) {
                return;
            }
            table.changeSelection(row, column, false, true);
        }

        public void mouseMoved(MouseEvent e) {
        }
    }
}
