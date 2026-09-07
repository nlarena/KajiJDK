import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.plaf.basic.BasicListUI;

/**
 * La lista y las dos piezas sueltas del combo, contra el JDK.
 *
 * <p>Los anchos que salen del texto no se comparan crudos --ver la nota de {@code Plaf3}--: la
 * lista escribe en Dialog negrita 12 y esta VM dibuja toda fuente con la misma cara. Se comparan los
 * altos, las coordenadas, y la relacion entre unos y otros.
 */
public class Plaf4 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    /** Expone lo protegido, que es donde esta la tabla de alturas. */
    static class Lista extends BasicListUI {
        String estado() {
            return "altoFijo=" + cellHeight + " alturas="
                    + (cellHeights == null ? "-" : "" + cellHeights.length)
                    + " pendiente=" + updateLayoutStateNeeded;
        }

        String escuchas() {
            return "foco=" + (focusListener != null) + " mouse=" + (mouseInputListener != null)
                    + " datos=" + (listDataListener != null)
                    + " seleccion=" + (listSelectionListener != null)
                    + " propiedad=" + (propertyChangeListener != null)
                    + " panel=" + (rendererPane != null);
        }

        int ancho() {
            return cellWidth;
        }

        int fila(int y) {
            return convertYToRow(y);
        }

        int y(int r) {
            return convertRowToY(r);
        }

        int alto(int r) {
            return getRowHeight(r);
        }

        void actualizar() {
            updateLayoutState();
        }

        static String banderas() {
            return modelChanged + "," + selectionModelChanged + "," + fontChanged + ","
                    + fixedCellWidthChanged + "," + fixedCellHeightChanged + ","
                    + prototypeCellValueChanged + "," + cellRendererChanged;
        }
    }

    static void listas() {
        linea("--- BasicListUI ---");
        linea("banderas=" + Lista.banderas());
        JList<Object> l = new JList<Object>(new Object[] {"uno", "dos", "tres"});
        Lista u = new Lista();
        linea("comparte instancia=" + (BasicListUI.createUI(l) == BasicListUI.createUI(l)));
        u.installUI(l);
        linea("recien instalada: " + u.estado());
        linea("escuchas: " + u.escuchas());
        linea("fondo=" + col(l.getBackground()) + " frente=" + col(l.getForeground())
                + " fuente=" + fue(l.getFont()) + " opaca=" + l.isOpaque()
                + " borde=" + l.getBorder());
        linea("seleccion=" + col(l.getSelectionBackground())
                + " sobre " + col(l.getSelectionForeground())
                + " dibujante=" + corto(l.getCellRenderer()));

        Dimension pref = u.getPreferredSize(l);
        linea("tras medir: " + u.estado());
        linea("alto preferido=" + pref.height + " ancho es el de la celda mas ancha="
                + (pref.width == u.ancho()));
        linea("alto de la fila 0=" + u.alto(0) + " de la 2=" + u.alto(2)
                + " de la 9=" + u.alto(9) + " de la -1=" + u.alto(-1));
        linea("el alto preferido son las tres filas="
                + (pref.height == u.alto(0) + u.alto(1) + u.alto(2)));

        linea("fila de y=0 -> " + u.fila(0) + " de y=" + u.alto(0) + " -> " + u.fila(u.alto(0))
                + " de y=1000 -> " + u.fila(1000) + " de y=-5 -> " + u.fila(-5));
        linea("y de la fila 0=" + u.y(0) + " de la 1=" + u.y(1) + " de la 9=" + u.y(9));
        linea("ubicacion de 1=" + u.indexToLocation(l, 1)
                + " de 99=" + u.indexToLocation(l, 99));
        linea("indice en (0,1000)=" + u.locationToIndex(l, new Point(0, 1000)));
        linea("celdas 0..1=" + u.getCellBounds(l, 0, 1)
                + " 1..0=" + u.getCellBounds(l, 1, 0));
        linea("celdas 5..9=" + u.getCellBounds(l, 5, 9));
        linea("linea de base=" + u.getBaseline(l, 100, 60)
                + " al cambiar de tamano=" + u.getBaselineResizeBehavior(l));

        JList<Object> vacia = new JList<Object>();
        Lista uv = new Lista();
        uv.installUI(vacia);
        linea("vacia: preferido=" + uv.getPreferredSize(vacia)
                + " celdas=" + uv.getCellBounds(vacia, 0, 0)
                + " linea de base=" + uv.getBaseline(vacia, 100, 60));

        l.setFixedCellHeight(30);
        l.setFixedCellWidth(200);
        linea("con tamano fijo: preferido=" + u.getPreferredSize(l)
                + " y de la fila 2=" + u.y(2) + " " + u.estado());
    }

    static void dibujanteDeCombo() {
        linea("--- BasicComboBoxRenderer ---");
        BasicComboBoxRenderer r = new BasicComboBoxRenderer();
        linea("opaco=" + r.isOpaque() + " alineacion=" + r.getHorizontalAlignment()
                + " borde=" + corto(r.getBorder()) + " insets=" + r.getInsets());
        JList<Object> l = new JList<Object>(new Object[] {"uno", "dos"});
        // Con el UI puesto: los colores del dibujante salen de la lista, y sin aspecto la lista
        // no tiene ninguno.
        new Lista().installUI(l);
        Component c = r.getListCellRendererComponent(l, "uno", 0, false, false);
        linea("se devuelve a si mismo=" + (c == r) + " texto=" + ((JLabel) c).getText()
                + " fondo=" + col(c.getBackground()) + " frente=" + col(c.getForeground()));
        c = r.getListCellRendererComponent(l, "uno", 0, true, false);
        linea("elegido fondo=" + col(c.getBackground()) + " frente=" + col(c.getForeground()));
        c = r.getListCellRendererComponent(l, null, -1, false, false);
        linea("valor nulo texto='" + ((JLabel) c).getText() + "'");
        Icon icono = new ImageIcon();
        c = r.getListCellRendererComponent(l, icono, 0, false, false);
        linea("valor icono: texto='" + ((JLabel) c).getText() + "' hay icono="
                + (((JLabel) c).getIcon() != null));
        // El renglon vacio igual mide alto; ver la nota de la clase.
        r.setText("");
        linea("el renglon vacio mide alto=" + (r.getPreferredSize().height > 0));
        linea("marcado por el aspecto="
                + (new BasicComboBoxRenderer.UIResource() instanceof javax.swing.plaf.UIResource));
    }

    static void editorDeCombo() {
        linea("--- BasicComboBoxEditor ---");
        BasicComboBoxEditor e = new BasicComboBoxEditor();
        JTextField tf = (JTextField) e.getEditorComponent();
        linea("columnas=" + tf.getColumns() + " borde=" + tf.getBorder());
        e.setItem("hola");
        linea("item='" + e.getItem() + "' texto='" + tf.getText() + "'");
        e.setItem(null);
        linea("con nulo item='" + e.getItem() + "' texto='" + tf.getText() + "'");
        // Un valor que no es cadena vuelve con su tipo.
        e.setItem(Integer.valueOf(42));
        Object mismo = e.getItem();
        linea("sin tocar vuelve el mismo objeto=" + (mismo instanceof Integer)
                + " valor=" + mismo);
        tf.setText("77");
        Object otro = e.getItem();
        linea("editado vuelve del mismo tipo=" + (otro instanceof Integer) + " valor=" + otro);
        tf.setText("no es un numero");
        Object roto = e.getItem();
        linea("con texto invalido vuelve=" + corto(roto) + " valor=" + roto);
        // Un borde del aspecto se rechaza y uno del programa se acepta.
        tf.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(1, 1, 1, 1));
        linea("borde del aspecto puesto="
                + (tf.getBorder() instanceof javax.swing.plaf.UIResource));
        tf.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        linea("borde del programa=" + (tf.getBorder() != null));
        linea("marcado por el aspecto="
                + (new BasicComboBoxEditor.UIResource() instanceof javax.swing.plaf.UIResource));
    }

    public static int run() {
        listas();
        dibujanteDeCombo();
        editorDeCombo();
        return 0;
    }
}
