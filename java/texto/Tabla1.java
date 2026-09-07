import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableStringConverter;

/**
 * Los modelos de tabla, el modelo de columnas y el ordenador, contra el JDK.
 *
 * <p>Nada de esto necesita pantalla: son datos, avisos y cuentas de indices. Lo que si la
 * necesitaria -- dibujar una celda, la barra de encabezados -- queda para cuando exista
 * {@code JTable}.
 */
public class Tabla1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota los avisos de un modelo de tabla. */
    static class Espia implements TableModelListener {

        private final StringBuilder log = new StringBuilder();

        public void tableChanged(TableModelEvent e) {
            log.append(" [").append(e.getFirstRow()).append("-").append(e.getLastRow())
                    .append(" col=").append(e.getColumn()).append(" tipo=").append(e.getType())
                    .append("]");
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Anota los avisos de un modelo de columnas. */
    static class EspiaCols implements TableColumnModelListener {

        private final StringBuilder log = new StringBuilder();

        public void columnAdded(TableColumnModelEvent e) {
            log.append(" mas(").append(e.getFromIndex()).append(",")
                    .append(e.getToIndex()).append(")");
        }

        public void columnRemoved(TableColumnModelEvent e) {
            log.append(" menos(").append(e.getFromIndex()).append(",")
                    .append(e.getToIndex()).append(")");
        }

        public void columnMoved(TableColumnModelEvent e) {
            log.append(" mueve(").append(e.getFromIndex()).append(",")
                    .append(e.getToIndex()).append(")");
        }

        public void columnMarginChanged(ChangeEvent e) {
            log.append(" margen");
        }

        public void columnSelectionChanged(ListSelectionEvent e) {
            log.append(" seleccion");
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Un modelo minimo, para ver que da la clase base. */
    static class Minimo extends AbstractTableModel {

        public int getRowCount() {
            return 3;
        }

        public int getColumnCount() {
            return 30;
        }

        public Object getValueAt(int r, int c) {
            return r + "/" + c;
        }
    }

    static String fila(TableModel m, int r) {
        StringBuilder b = new StringBuilder();
        for (int c = 0; c < m.getColumnCount(); c++) {
            b.append(" ").append(m.getValueAt(r, c));
        }
        return b.toString();
    }

    static String todo(TableModel m) {
        StringBuilder b = new StringBuilder();
        b.append(m.getRowCount()).append("x").append(m.getColumnCount()).append(":");
        for (int r = 0; r < m.getRowCount(); r++) {
            b.append(" (").append(fila(m, r).trim()).append(")");
        }
        return b.toString();
    }

    static void base() {
        linea("--- el modelo de base ---");
        Minimo m = new Minimo();
        // Los nombres de columna van como en una hoja de calculo.
        StringBuilder n = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            n.append(" ").append(m.getColumnName(i));
        }
        linea("nombres" + n);
        linea("buscar C=" + m.findColumn("C") + " AB=" + m.findColumn("AB")
                + " zz=" + m.findColumn("zz"));
        linea("clase=" + m.getColumnClass(0).getName() + " editable=" + m.isCellEditable(0, 0));
        m.setValueAt("x", 0, 0);
        linea("setValueAt no hace nada=" + m.getValueAt(0, 0));

        Espia espia = new Espia();
        m.addTableModelListener(espia);
        linea("oyentes=" + m.getTableModelListeners().length);
        m.fireTableDataChanged();
        linea("datos |" + espia.vaciar());
        m.fireTableStructureChanged();
        linea("estructura |" + espia.vaciar());
        m.fireTableRowsInserted(1, 2);
        linea("insertadas |" + espia.vaciar());
        m.fireTableRowsUpdated(1, 2);
        linea("actualizadas |" + espia.vaciar());
        m.fireTableRowsDeleted(1, 2);
        linea("borradas |" + espia.vaciar());
        m.fireTableCellUpdated(1, 2);
        linea("una celda |" + espia.vaciar());
        m.removeTableModelListener(espia);
        linea("oyentes=" + m.getTableModelListeners().length);
    }

    static void porOmision() {
        linea("--- el modelo por omision ---");
        DefaultTableModel d = new DefaultTableModel();
        linea("vacio " + todo(d));
        DefaultTableModel c = new DefaultTableModel(2, 3);
        linea("2x3 " + todo(c) + " nombres=" + c.getColumnName(0) + c.getColumnName(1)
                + c.getColumnName(2));

        DefaultTableModel t = new DefaultTableModel(
                new Object[][] {{"a", "b"}, {"c", "d"}}, new Object[] {"uno", "dos"});
        linea("con datos " + todo(t) + " nombres=" + t.getColumnName(0) + "," + t.getColumnName(1));
        linea("editable=" + t.isCellEditable(0, 0) + " clase=" + t.getColumnClass(0).getName());

        Espia espia = new Espia();
        t.addTableModelListener(espia);

        t.addRow(new Object[] {"e", "f"});
        linea("una fila mas " + todo(t) + " |" + espia.vaciar());
        // Una fila mas corta se rellena con nulos.
        t.addRow(new Object[] {"g"});
        linea("fila corta " + todo(t) + " |" + espia.vaciar());
        t.insertRow(0, new Object[] {"z", "z"});
        linea("insertada arriba " + todo(t) + " |" + espia.vaciar());
        t.removeRow(0);
        linea("sacada " + todo(t) + " |" + espia.vaciar());

        t.setValueAt("X", 0, 0);
        linea("celda cambiada " + todo(t) + " |" + espia.vaciar());

        t.addColumn("tres");
        linea("columna mas " + todo(t) + " |" + espia.vaciar());
        t.addColumn("cuatro", new Object[] {"p", "q"});
        linea("columna con datos " + todo(t) + " |" + espia.vaciar());

        t.setColumnCount(2);
        linea("dos columnas " + todo(t) + " |" + espia.vaciar());
        t.setRowCount(2);
        linea("dos filas " + todo(t) + " |" + espia.vaciar());
        t.setRowCount(4);
        linea("cuatro filas " + todo(t) + " |" + espia.vaciar());
        t.setRowCount(4);
        linea("mismas filas |" + espia.vaciar());

        // Mover un bloque de filas es rotar.
        DefaultTableModel r = new DefaultTableModel(
                new Object[][] {{"1"}, {"2"}, {"3"}, {"4"}, {"5"}}, new Object[] {"n"});
        r.moveRow(0, 1, 3);
        linea("movidas 0-1 a 3 " + todo(r));
        r.moveRow(3, 4, 0);
        linea("movidas 3-4 a 0 " + todo(r));
        try {
            r.moveRow(0, 1, 9);
            linea("mover fuera de rango aceptado");
        } catch (ArrayIndexOutOfBoundsException e) {
            linea("mover fuera de rango rechazado");
        }

        t.setColumnIdentifiers(new Object[] {"p", "q", "r"});
        linea("nombres nuevos " + todo(t) + " " + t.getColumnName(0) + t.getColumnName(1)
                + t.getColumnName(2));

        try {
            new DefaultTableModel(new Object[] {"a"}, -1);
            linea("filas negativas aceptadas");
        } catch (IllegalArgumentException e) {
            linea("filas negativas rechazadas: " + e.getMessage());
        }
        try {
            t.getValueAt(99, 0);
            linea("fila 99 aceptada");
        } catch (ArrayIndexOutOfBoundsException e) {
            linea("fila 99 rechazada");
        }

        // El vector de datos no es copia.
        Vector<Vector> datos = t.getDataVector();
        linea("filas del vector=" + datos.size());
    }

    static String cols(DefaultTableColumnModel m) {
        StringBuilder b = new StringBuilder();
        b.append(m.getColumnCount()).append(":");
        Enumeration<TableColumn> e = m.getColumns();
        while (e.hasMoreElements()) {
            b.append(" ").append(e.nextElement().getIdentifier());
        }
        return b.toString();
    }

    static TableColumn columna(int modelIndex, Object id, int ancho) {
        TableColumn c = new TableColumn(modelIndex, ancho);
        c.setIdentifier(id);
        return c;
    }

    static void columnas() {
        linea("--- el modelo de columnas ---");
        DefaultTableColumnModel m = new DefaultTableColumnModel();
        linea("vacio " + cols(m) + " margen=" + m.getColumnMargin()
                + " total=" + m.getTotalColumnWidth()
                + " se eligen=" + m.getColumnSelectionAllowed());
        linea("seleccion=" + (m.getSelectionModel() != null)
                + " elegidas=" + Arrays.toString(m.getSelectedColumns())
                + " cuantas=" + m.getSelectedColumnCount());

        EspiaCols espia = new EspiaCols();
        m.addColumnModelListener(espia);

        m.addColumn(columna(0, "uno", 50));
        m.addColumn(columna(1, "dos", 30));
        m.addColumn(columna(2, "tres", 20));
        linea("tres " + cols(m) + " total=" + m.getTotalColumnWidth() + " |" + espia.vaciar());
        linea("indice de dos=" + m.getColumnIndex("dos")
                + " columna 1=" + m.getColumn(1).getIdentifier());

        linea("en x=0 -> " + m.getColumnIndexAtX(0) + " en 49 -> " + m.getColumnIndexAtX(49)
                + " en 50 -> " + m.getColumnIndexAtX(50) + " en 79 -> " + m.getColumnIndexAtX(79)
                + " en 100 -> " + m.getColumnIndexAtX(100) + " en -1 -> "
                + m.getColumnIndexAtX(-1));

        m.moveColumn(0, 2);
        linea("movida 0 a 2 " + cols(m) + " |" + espia.vaciar());
        m.moveColumn(1, 1);
        linea("movida a si misma " + cols(m) + " |" + espia.vaciar());

        TableColumn sacar = m.getColumn(0);
        m.removeColumn(sacar);
        linea("sacada " + cols(m) + " total=" + m.getTotalColumnWidth() + " |" + espia.vaciar());
        m.removeColumn(sacar);
        linea("sacada de nuevo " + cols(m) + " |" + espia.vaciar());

        // Cambiarle el ancho a una columna cambia el total y avisa.
        m.getColumn(0).setWidth(99);
        linea("ancho cambiado total=" + m.getTotalColumnWidth() + " |" + espia.vaciar());

        m.setColumnMargin(5);
        linea("margen 5=" + m.getColumnMargin() + " |" + espia.vaciar());
        m.setColumnMargin(5);
        linea("mismo margen |" + espia.vaciar());

        m.setColumnSelectionAllowed(true);
        m.getSelectionModel().setSelectionInterval(0, 1);
        linea("elegidas=" + Arrays.toString(m.getSelectedColumns())
                + " cuantas=" + m.getSelectedColumnCount() + " |" + espia.vaciar());

        try {
            m.addColumn(null);
            linea("columna nula aceptada");
        } catch (IllegalArgumentException e) {
            linea("columna nula rechazada: " + e.getMessage());
        }
        try {
            m.getColumnIndex("no existe");
            linea("identificador ajeno aceptado");
        } catch (IllegalArgumentException e) {
            linea("identificador ajeno rechazado: " + e.getMessage());
        }
        try {
            m.getColumnIndex(null);
            linea("identificador nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("identificador nulo rechazado: " + e.getMessage());
        }
        try {
            m.moveColumn(0, 9);
            linea("mover fuera de rango aceptado");
        } catch (IllegalArgumentException e) {
            linea("mover fuera de rango rechazado: " + e.getMessage());
        }
        try {
            m.setSelectionModel(null);
            linea("seleccion nula aceptada");
        } catch (IllegalArgumentException e) {
            linea("seleccion nula rechazada: " + e.getMessage());
        }
        ListSelectionModel otra = new DefaultListSelectionModel();
        m.setSelectionModel(otra);
        linea("seleccion cambiada=" + (m.getSelectionModel() == otra));
    }

    /** Un modelo con tipos declarados, para ver como ordena. */
    static class ConTipos extends AbstractTableModel {

        private final Object[][] datos = {
            {"pera", Integer.valueOf(10), Boolean.TRUE},
            {"Banana", Integer.valueOf(9), Boolean.FALSE},
            {"uva", Integer.valueOf(100), Boolean.TRUE},
        };

        public int getRowCount() {
            return datos.length;
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int r, int c) {
            return datos[r][c];
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
    }

    static String vista(RowSorter<?> s, TableModel m) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.getViewRowCount(); i++) {
            b.append(" ").append(m.getValueAt(s.convertRowIndexToModel(i), 0));
        }
        return b.toString();
    }

    static void ordenar() {
        linea("--- el ordenador de tabla ---");
        ConTipos m = new ConTipos();
        Orden s = new Orden(m);
        linea("modelo=" + (s.getModel() == m) + " filas=" + s.getModelRowCount()
                + " vistas=" + s.getViewRowCount());
        linea("convertidor=" + s.getStringConverter());
        linea("usa texto col0=" + s.porTexto(0) + " col1=" + s.porTexto(1)
                + " col2=" + s.porTexto(2));

        // La columna de numeros se ordena por valor, no por texto: 9 antes que 10 y que 100.
        List<RowSorter.SortKey> k = new java.util.ArrayList<RowSorter.SortKey>();
        k.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        s.setSortKeys(k);
        linea("por numero" + vista(s, m));
        k.clear();
        k.add(new RowSorter.SortKey(1, SortOrder.DESCENDING));
        s.setSortKeys(k);
        linea("al reves" + vista(s, m));

        // La de texto va por el idioma, asi que "Banana" no queda primera por ser mayuscula.
        k.clear();
        k.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        s.setSortKeys(k);
        linea("por texto" + vista(s, m));

        // La de booleanos por su orden natural: falso antes que verdadero.
        k.clear();
        k.add(new RowSorter.SortKey(2, SortOrder.ASCENDING));
        s.setSortKeys(k);
        linea("por booleano" + vista(s, m));

        // Un convertidor propio cambia el orden sin tocar el modelo.
        s.setStringConverter(new AlReves());
        linea("convertidor puesto=" + (s.getStringConverter() != null));
        k.clear();
        k.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        s.setSortKeys(null);
        s.setSortKeys(k);
        linea("por texto al reves" + vista(s, m));
        s.setStringConverter(null);

        Orden vacio = new Orden();
        linea("sin modelo filas=" + vacio.getModelRowCount()
                + " vistas=" + vacio.getViewRowCount() + " modelo=" + vacio.getModel());
    }

    /** Expone {@code useToString}, que es protegido. */
    static class Orden extends TableRowSorter<ConTipos> {

        Orden() {
            super();
        }

        Orden(ConTipos m) {
            super(m);
        }

        boolean porTexto(int columna) {
            return useToString(columna);
        }
    }

    /** Devuelve el texto de la celda dado vuelta. */
    static class AlReves extends TableStringConverter {

        public String toString(TableModel model, int row, int column) {
            Object v = model.getValueAt(row, column);
            return new StringBuilder(String.valueOf(v)).reverse().toString();
        }
    }

    public static int run() {
        base();
        porOmision();
        columnas();
        ordenar();
        return 0;
    }
}
