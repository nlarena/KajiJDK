import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

/**
 * La tabla, contra el JDK.
 *
 * <p>Lo que se compara es la maquina de estados: los cuatro modelos, la traduccion entre indices de
 * vista y de modelo, la seleccion, y el ciclo de edicion. El dibujado queda afuera -- lo decide el
 * aspecto instalado, que esta biblioteca no tiene -- y por lo mismo quedan afuera los colores y las
 * medidas que salen de el.
 */
public class Tabla2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String clase(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        int p = c.lastIndexOf('.');
        return c.substring(p + 1);
    }

    static String rect(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    /** Un modelo con tipos y con una columna que no se edita. */
    static class Datos extends AbstractTableModel {

        private final Object[][] filas = {
            {"pera", Integer.valueOf(10), Boolean.TRUE},
            {"Banana", Integer.valueOf(9), Boolean.FALSE},
            {"uva", Integer.valueOf(100), Boolean.TRUE},
        };

        private final String[] nombres = {"fruta", "cantidad", "hay"};

        public int getRowCount() {
            return filas.length;
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int r, int c) {
            return filas[r][c];
        }

        public void setValueAt(Object v, int r, int c) {
            filas[r][c] = v;
            fireTableCellUpdated(r, c);
        }

        public String getColumnName(int c) {
            return nombres[c];
        }

        public Class<?> getColumnClass(int c) {
            if (c == 0) {
                return String.class;
            }
            if (c == 1) {
                return Integer.class;
            }
            return Boolean.class;
        }

        public boolean isCellEditable(int r, int c) {
            return c != 0;
        }
    }

    static String vista(JTable t) {
        StringBuilder b = new StringBuilder();
        for (int r = 0; r < t.getRowCount(); r++) {
            b.append(" (");
            for (int c = 0; c < t.getColumnCount(); c++) {
                if (c > 0) {
                    b.append(",");
                }
                b.append(t.getValueAt(r, c));
            }
            b.append(")");
        }
        return b.toString();
    }

    static void basico() {
        linea("--- lo basico ---");
        JTable t = new JTable();
        linea("vacia filas=" + t.getRowCount() + " columnas=" + t.getColumnCount()
                + " aspecto=" + t.getUIClassID());
        linea("modelo=" + clase(t.getModel()) + " columnas=" + clase(t.getColumnModel())
                + " seleccion=" + clase(t.getSelectionModel())
                + " encabezado=" + clase(t.getTableHeader()));
        linea("alto de fila=" + t.getRowHeight() + " margen=" + t.getRowMargin()
                + " espaciado=" + t.getIntercellSpacing());
        linea("grilla h=" + t.getShowHorizontalLines() + " v=" + t.getShowVerticalLines()
                + " ajuste=" + t.getAutoResizeMode()
                + " columnas solas=" + t.getAutoCreateColumnsFromModel());
        linea("filas elegibles=" + t.getRowSelectionAllowed()
                + " columnas=" + t.getColumnSelectionAllowed()
                + " celdas=" + t.getCellSelectionEnabled());
        linea("orden solo=" + t.getAutoCreateRowSorter() + " ordenador=" + t.getRowSorter()
                + " actualiza al ordenar=" + t.getUpdateSelectionOnSort());
        linea("llena el alto=" + t.getFillsViewportHeight()
                + " cede foco=" + t.getSurrendersFocusOnKeystroke()
                + " arrastre=" + t.getDragEnabled() + " modo soltar=" + t.getDropMode()
                + " donde soltar=" + t.getDropLocation());
        linea("editando=" + t.isEditing() + " fila=" + t.getEditingRow()
                + " columna=" + t.getEditingColumn() + " editor=" + t.getEditorComponent());

        JTable dim = new JTable(2, 3);
        linea("2x3 " + dim.getRowCount() + "x" + dim.getColumnCount()
                + " modelo=" + clase(dim.getModel()) + vista(dim));

        JTable arr = new JTable(new Object[][] {{"a", "b"}, {"c", "d"}},
                new Object[] {"x", "y"});
        linea("de arreglos" + vista(arr) + " nombres=" + arr.getColumnName(0)
                + "," + arr.getColumnName(1) + " editable=" + arr.isCellEditable(0, 0));
    }

    static void columnas() {
        linea("--- columnas y traduccion de indices ---");
        Datos m = new Datos();
        JTable t = new JTable(m);
        linea("columnas armadas=" + t.getColumnCount());
        StringBuilder n = new StringBuilder();
        for (int c = 0; c < t.getColumnCount(); c++) {
            n.append(" ").append(t.getColumnName(c)).append("/")
                    .append(t.getColumnClass(c).getSimpleName());
        }
        linea("nombres y tipos" + n);
        linea("titulo de la columna 0=" + t.getColumnModel().getColumn(0).getHeaderValue());
        linea("editable 0,0=" + t.isCellEditable(0, 0) + " 0,1=" + t.isCellEditable(0, 1));
        linea("datos" + vista(t));

        // Mover una columna cambia la vista y no el modelo.
        t.moveColumn(0, 2);
        linea("movida 0 a 2, vista" + vista(t));
        StringBuilder tr = new StringBuilder();
        for (int c = 0; c < t.getColumnCount(); c++) {
            tr.append(" v").append(c).append("=m").append(t.convertColumnIndexToModel(c));
        }
        linea("vista a modelo" + tr);
        tr.setLength(0);
        for (int c = 0; c < 3; c++) {
            tr.append(" m").append(c).append("=v").append(t.convertColumnIndexToView(c));
        }
        linea("modelo a vista" + tr);
        linea("nombres ahora=" + t.getColumnName(0) + "," + t.getColumnName(1) + ","
                + t.getColumnName(2));
        linea("el modelo no cambio=" + m.getColumnName(0) + "," + m.getColumnName(1));
        t.moveColumn(2, 0);

        // Sacar una columna la saca de la vista, no del modelo.
        TableColumn c1 = t.getColumnModel().getColumn(1);
        t.removeColumn(c1);
        linea("sacada la 1, columnas=" + t.getColumnCount() + " vista" + vista(t));
        linea("modelo a vista de la 1=" + t.convertColumnIndexToView(1));
        t.addColumn(c1);
        linea("devuelta, columnas=" + t.getColumnCount() + " vista" + vista(t));

        linea("columna por identificador=" + clase(t.getColumn("fruta")));
        try {
            t.getColumn("no existe");
            linea("identificador ajeno aceptado");
        } catch (IllegalArgumentException e) {
            linea("identificador ajeno rechazado");
        }
        linea("indices negativos: modelo=" + t.convertColumnIndexToModel(-1)
                + " vista=" + t.convertColumnIndexToView(-1));
    }

    static void seleccion() {
        linea("--- la seleccion ---");
        JTable t = new JTable(new Datos());
        linea("nada elegido fila=" + t.getSelectedRow() + " columna=" + t.getSelectedColumn()
                + " filas=" + Arrays.toString(t.getSelectedRows())
                + " columnas=" + Arrays.toString(t.getSelectedColumns()));
        linea("cuantas=" + t.getSelectedRowCount() + "/" + t.getSelectedColumnCount());

        t.setRowSelectionInterval(0, 1);
        linea("filas 0-1 elegidas=" + Arrays.toString(t.getSelectedRows())
                + " primera=" + t.getSelectedRow() + " cuantas=" + t.getSelectedRowCount());
        linea("fila 0 elegida=" + t.isRowSelected(0) + " fila 2=" + t.isRowSelected(2));
        // Sin columnas elegibles, una celda no esta elegida por su columna.
        linea("celda 0,0 elegida=" + t.isCellSelected(0, 0));

        t.setColumnSelectionAllowed(true);
        linea("ahora se eligen columnas=" + t.getColumnSelectionAllowed()
                + " celdas=" + t.getCellSelectionEnabled());
        t.setColumnSelectionInterval(1, 2);
        linea("columnas 1-2=" + Arrays.toString(t.getSelectedColumns())
                + " celda 0,0=" + t.isCellSelected(0, 0)
                + " celda 0,1=" + t.isCellSelected(0, 1));

        t.addRowSelectionInterval(2, 2);
        linea("mas la 2=" + Arrays.toString(t.getSelectedRows()));
        t.removeRowSelectionInterval(0, 0);
        linea("menos la 0=" + Arrays.toString(t.getSelectedRows()));

        t.selectAll();
        linea("todo=" + Arrays.toString(t.getSelectedRows()) + " / "
                + Arrays.toString(t.getSelectedColumns()));
        t.clearSelection();
        linea("limpio=" + Arrays.toString(t.getSelectedRows()) + " / "
                + Arrays.toString(t.getSelectedColumns()));

        // changeSelection: los dos booleanos son Control y Mayusculas.
        t.changeSelection(1, 1, false, false);
        linea("clic limpio=" + Arrays.toString(t.getSelectedRows()) + " / "
                + Arrays.toString(t.getSelectedColumns()));
        t.changeSelection(2, 2, true, false);
        linea("con control=" + Arrays.toString(t.getSelectedRows()) + " / "
                + Arrays.toString(t.getSelectedColumns()));
        t.changeSelection(2, 2, true, false);
        linea("otra vez, se saca=" + Arrays.toString(t.getSelectedRows()) + " / "
                + Arrays.toString(t.getSelectedColumns()));
        t.clearSelection();
        t.changeSelection(0, 0, false, false);
        t.changeSelection(2, 2, false, true);
        linea("con mayusculas=" + Arrays.toString(t.getSelectedRows()) + " / "
                + Arrays.toString(t.getSelectedColumns()));

        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowSelectionInterval(0, 2);
        linea("modo unico=" + Arrays.toString(t.getSelectedRows()));

        try {
            t.setRowSelectionInterval(0, 9);
            linea("fila 9 aceptada");
        } catch (IllegalArgumentException e) {
            linea("fila 9 rechazada: " + e.getMessage());
        }
        try {
            t.setColumnSelectionInterval(-1, 0);
            linea("columna -1 aceptada");
        } catch (IllegalArgumentException e) {
            linea("columna -1 rechazada: " + e.getMessage());
        }
    }

    static void ordenar() {
        linea("--- con un ordenador ---");
        Datos m = new Datos();
        JTable t = new JTable(m);
        TableRowSorter<Datos> s = new TableRowSorter<Datos>(m);
        t.setRowSorter(s);
        linea("ordenador puesto=" + (t.getRowSorter() == s) + " filas=" + t.getRowCount());
        linea("sin orden" + vista(t));

        java.util.List<RowSorter.SortKey> k = new java.util.ArrayList<RowSorter.SortKey>();
        k.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        s.setSortKeys(k);
        linea("por cantidad" + vista(t));
        StringBuilder tr = new StringBuilder();
        for (int r = 0; r < t.getRowCount(); r++) {
            tr.append(" v").append(r).append("=m").append(t.convertRowIndexToModel(r));
        }
        linea("vista a modelo" + tr);
        tr.setLength(0);
        for (int r = 0; r < 3; r++) {
            tr.append(" m").append(r).append("=v").append(t.convertRowIndexToView(r));
        }
        linea("modelo a vista" + tr);

        // Cambiar una celda de la vista toca la fila del modelo que corresponde.
        t.setValueAt(Integer.valueOf(50), 0, 1);
        linea("puesto 50 en la vista 0, modelo=" + m.getValueAt(1, 1)
                + " vista ahora" + vista(t));

        // Un filtro achica la vista sin achicar el modelo.
        s.setRowFilter(javax.swing.RowFilter.<Datos, Integer>regexFilter("a", 0));
        linea("con filtro filas=" + t.getRowCount() + " modelo=" + m.getRowCount()
                + vista(t));
        s.setRowFilter(null);
        t.setRowSorter(null);
        linea("sin ordenador filas=" + t.getRowCount() + vista(t));
    }

    /** Anota los avisos del editor. */
    static class Espia implements CellEditorListener {

        private final StringBuilder log = new StringBuilder();

        public void editingStopped(ChangeEvent e) {
            log.append(" termino");
        }

        public void editingCanceled(ChangeEvent e) {
            log.append(" cancelo");
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    static void editar() {
        linea("--- editar ---");
        Datos m = new Datos();
        JTable t = new JTable(m);
        linea("dibujante de Object=" + clase(t.getDefaultRenderer(Object.class)));
        linea("dibujante de Integer=" + clase(t.getDefaultRenderer(Integer.class))
                + " (sube por la jerarquia)");
        linea("editor de Object=" + clase(t.getDefaultEditor(Object.class)));
        linea("dibujante de una celda=" + clase(t.getCellRenderer(0, 0)));
        linea("editor de una celda=" + clase(t.getCellEditor(0, 1)));
        linea("dibujante de null=" + t.getDefaultRenderer(null));

        // Una columna con dibujante propio gana sobre el del tipo.
        DefaultTableCellRenderer propio = new DefaultTableCellRenderer();
        t.getColumnModel().getColumn(0).setCellRenderer(propio);
        linea("con dibujante propio=" + (t.getCellRenderer(0, 0) == propio));
        t.getColumnModel().getColumn(0).setCellRenderer(null);

        // La columna 0 no se edita.
        linea("editar 0,0=" + t.editCellAt(0, 0) + " editando=" + t.isEditing());
        linea("editar fuera de rango=" + t.editCellAt(9, 0));

        boolean empezo = t.editCellAt(0, 1);
        linea("editar 0,1=" + empezo + " editando=" + t.isEditing()
                + " fila=" + t.getEditingRow() + " columna=" + t.getEditingColumn()
                + " editor=" + clase(t.getEditorComponent()));
        if (empezo) {
            JTextField campo = (JTextField) t.getEditorComponent();
            linea("el editor trae=" + campo.getText());
            campo.setText("77");
            t.getCellEditor().stopCellEditing();
            linea("tras terminar editando=" + t.isEditing()
                    + " valor=" + m.getValueAt(0, 1) + " clase=" + clase(m.getValueAt(0, 1)));
        }

        // Cancelar no guarda.
        t.editCellAt(1, 1);
        JTextField campo2 = (JTextField) t.getEditorComponent();
        campo2.setText("999");
        t.getCellEditor().cancelCellEditing();
        linea("tras cancelar editando=" + t.isEditing() + " valor=" + m.getValueAt(1, 1));

        t.editCellAt(2, 1);
        t.removeEditor();
        linea("tras sacar el editor editando=" + t.isEditing()
                + " fila=" + t.getEditingRow() + " editor=" + t.getEditorComponent());

        // Un editor propio por tipo de columna.
        DefaultCellEditor tilde = new DefaultCellEditor(new JCheckBox());
        t.setDefaultEditor(Boolean.class, tilde);
        linea("editor de Boolean=" + (t.getDefaultEditor(Boolean.class) == tilde)
                + " de una celda=" + (t.getCellEditor(0, 2) == tilde));
        t.editCellAt(0, 2);
        linea("editando el tilde=" + t.isEditing() + " editor=" + clase(t.getEditorComponent()));
        JCheckBox caja = (JCheckBox) t.getEditorComponent();
        linea("el tilde trae=" + caja.isSelected());
        caja.setSelected(false);
        t.getCellEditor().stopCellEditing();
        linea("tras terminar valor=" + m.getValueAt(0, 2));
    }

    static void medidas() {
        linea("--- medidas y puntos ---");
        JTable t = new JTable(new Datos());
        t.setRowHeight(20);
        t.getColumnModel().getColumn(0).setWidth(100);
        t.getColumnModel().getColumn(1).setWidth(50);
        t.getColumnModel().getColumn(2).setWidth(30);
        t.setRowMargin(2);
        t.getColumnModel().setColumnMargin(4);
        linea("alto=" + t.getRowHeight() + " de la fila 1=" + t.getRowHeight(1)
                + " espaciado=" + t.getIntercellSpacing());
        linea("celda 0,0 con margen=" + rect(t.getCellRect(0, 0, true))
                + " sin margen=" + rect(t.getCellRect(0, 0, false)));
        linea("celda 1,1=" + rect(t.getCellRect(1, 1, true)));
        linea("celda fuera de rango=" + rect(t.getCellRect(9, 9, true)));

        linea("fila en y=0 -> " + t.rowAtPoint(new Point(0, 0))
                + " y=25 -> " + t.rowAtPoint(new Point(0, 25))
                + " y=999 -> " + t.rowAtPoint(new Point(0, 999))
                + " y=-1 -> " + t.rowAtPoint(new Point(0, -1)));
        linea("columna en x=0 -> " + t.columnAtPoint(new Point(0, 0))
                + " x=120 -> " + t.columnAtPoint(new Point(120, 0))
                + " x=999 -> " + t.columnAtPoint(new Point(999, 0)));

        // Alturas por fila.
        t.setRowHeight(1, 40);
        linea("fila 1 mas alta: 0=" + t.getRowHeight(0) + " 1=" + t.getRowHeight(1)
                + " 2=" + t.getRowHeight(2));
        linea("celda 2,0 ahora=" + rect(t.getCellRect(2, 0, true)));
        linea("fila en y=25 -> " + t.rowAtPoint(new Point(0, 25))
                + " y=61 -> " + t.rowAtPoint(new Point(0, 61)));
        try {
            t.setRowHeight(0);
            linea("alto 0 aceptado");
        } catch (IllegalArgumentException e) {
            linea("alto 0 rechazado: " + e.getMessage());
        }
        try {
            t.getRowHeight(9);
            linea("fila 9 aceptada");
        } catch (IllegalArgumentException e) {
            linea("fila 9 rechazada: " + e.getMessage());
        }

        linea("scroll vertical=" + t.getScrollableUnitIncrement(
                new Rectangle(0, 0, 100, 100), SwingConstants.VERTICAL, 1));
        linea("scroll bloque=" + t.getScrollableBlockIncrement(
                new Rectangle(0, 0, 100, 100), SwingConstants.VERTICAL, 1));
        linea("sigue el ancho=" + t.getScrollableTracksViewportWidth());
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        linea("sin ajuste, sigue el ancho=" + t.getScrollableTracksViewportWidth());
        t.setAutoResizeMode(99);
        linea("modo 99 se ignora=" + t.getAutoResizeMode());
        linea("tamano preferido de vista=" + t.getPreferredScrollableViewportSize());
    }

    static void encabezado() {
        linea("--- el encabezado ---");
        JTable t = new JTable(new Datos());
        JTableHeader h = t.getTableHeader();
        linea("existe=" + (h != null) + " tabla=" + (h.getTable() == t)
                + " comparte columnas=" + (h.getColumnModel() == t.getColumnModel()));
        linea("aspecto=" + h.getUIClassID() + " reordenar=" + h.getReorderingAllowed()
                + " redimensionar=" + h.getResizingAllowed());
        linea("arrastrada=" + h.getDraggedColumn() + " distancia=" + h.getDraggedDistance()
                + " redimensionando=" + h.getResizingColumn());
        // El dibujante de titulos del JDK es una clase de `sun.swing` con borde propio; aca es el
        // de celda, centrado. Se compara que haya uno y como alinea, no cual es.
        linea("dibujante puesto=" + (h.getDefaultRenderer() != null));

        t.getColumnModel().getColumn(0).setWidth(100);
        t.getColumnModel().getColumn(1).setWidth(50);
        t.getColumnModel().getColumn(2).setWidth(30);
        linea("rectangulo de la 0=" + rect(h.getHeaderRect(0))
                + " de la 1=" + rect(h.getHeaderRect(1)));
        linea("fuera de rango: -1=" + rect(h.getHeaderRect(-1))
                + " 9=" + rect(h.getHeaderRect(9)));
        linea("columna en x=0 -> " + h.columnAtPoint(new Point(0, 0))
                + " x=120 -> " + h.columnAtPoint(new Point(120, 0)));

        h.setReorderingAllowed(false);
        h.setResizingAllowed(false);
        linea("apagados=" + h.getReorderingAllowed() + "/" + h.getResizingAllowed());

        // Sacar el encabezado lo desconecta de la tabla.
        t.setTableHeader(null);
        linea("sacado=" + t.getTableHeader() + " la vieja apunta a=" + h.getTable());
    }

    static void dibujar() {
        linea("--- el dibujante de celda ---");
        JTable t = new JTable(new Datos());
        t.setSelectionForeground(Color.WHITE);
        t.setSelectionBackground(Color.BLUE);
        t.setForeground(Color.BLACK);
        t.setBackground(Color.GRAY);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        java.awt.Component c = r.getTableCellRendererComponent(t, "hola", false, false, 0, 0);
        linea("normal texto=" + ((javax.swing.JLabel) c).getText()
                + " frente=" + ((javax.swing.JLabel) c).getForeground()
                + " fondo=" + ((javax.swing.JLabel) c).getBackground());
        c = r.getTableCellRendererComponent(t, "hola", true, false, 0, 0);
        linea("elegida frente=" + ((javax.swing.JLabel) c).getForeground()
                + " fondo=" + ((javax.swing.JLabel) c).getBackground());
        c = r.getTableCellRendererComponent(t, null, false, false, 0, 0);
        linea("nulo texto=[" + ((javax.swing.JLabel) c).getText() + "]");
        c = r.getTableCellRendererComponent(t, Integer.valueOf(5), false, false, 0, 0);
        linea("numero texto=" + ((javax.swing.JLabel) c).getText());
        linea("es opaco=" + r.isOpaque() + " es el mismo objeto=" + (c == r));

        // Un color puesto a mano gana sobre el de la tabla.
        r.setForeground(Color.RED);
        c = r.getTableCellRendererComponent(t, "hola", false, false, 0, 0);
        linea("con color propio frente=" + ((javax.swing.JLabel) c).getForeground());
        r.setForeground(null);
        c = r.getTableCellRendererComponent(t, "hola", false, false, 0, 0);
        linea("sin color propio frente=" + ((javax.swing.JLabel) c).getForeground());

        linea("recurso de aspecto=" + (new DefaultTableCellRenderer.UIResource()
                instanceof javax.swing.plaf.UIResource));
        linea("sin tabla devuelve=" + (r.getTableCellRendererComponent(null, "x", false, false,
                0, 0) == r));
    }

    static void modelos() {
        linea("--- cambiar los modelos ---");
        JTable t = new JTable(new Datos());
        linea("columnas=" + t.getColumnCount());
        t.setModel(new DefaultTableModel(2, 5));
        linea("modelo nuevo, columnas rearmadas=" + t.getColumnCount()
                + " filas=" + t.getRowCount());
        t.setAutoCreateColumnsFromModel(false);
        t.setModel(new DefaultTableModel(3, 2));
        linea("sin rearmar, columnas=" + t.getColumnCount() + " filas=" + t.getRowCount());

        DefaultTableColumnModel cm = new DefaultTableColumnModel();
        cm.addColumn(new TableColumn(0));
        t.setColumnModel(cm);
        linea("modelo de columnas nuevo=" + (t.getColumnModel() == cm)
                + " columnas=" + t.getColumnCount()
                + " el encabezado lo comparte=" + (t.getTableHeader() == null ? "sin encabezado"
                        : String.valueOf(t.getTableHeader().getColumnModel() == cm)));

        try {
            t.setModel(null);
            linea("modelo nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("modelo nulo rechazado: " + e.getMessage());
        }
        try {
            t.setColumnModel(null);
            linea("columnas nulas aceptadas");
        } catch (IllegalArgumentException e) {
            linea("columnas nulas rechazadas: " + e.getMessage());
        }
        try {
            t.setSelectionModel(null);
            linea("seleccion nula aceptada");
        } catch (IllegalArgumentException e) {
            linea("seleccion nula rechazada: " + e.getMessage());
        }
    }

    public static int run() {
        basico();
        columnas();
        seleccion();
        ordenar();
        editar();
        medidas();
        encabezado();
        dibujar();
        modelos();
        return 0;
    }
}
