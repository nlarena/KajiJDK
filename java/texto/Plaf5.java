import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicDesktopPaneUI;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;

/**
 * El encabezado de tabla y el escritorio, contra el JDK.
 *
 * <p>Los anchos de columna si se comparan crudos: no salen del texto sino de
 * {@code TableColumn.getPreferredWidth}, que es un numero puesto a mano. El alto no, porque sale
 * del dibujante de titulo, que en el JDK es una clase de {@code sun.swing} y aca es la nuestra.
 */
public class Plaf5 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static class Encabezado extends BasicTableHeaderUI {
        String estado() {
            return "panel=" + (rendererPane != null) + " mouse=" + (mouseInputListener != null)
                    + " encabezado=" + (header != null) + " columna bajo el mouse="
                    + getRolloverColumn();
        }
    }

    static class Tabla extends javax.swing.plaf.basic.BasicTableUI {
        String estado() {
            return "panel=" + (rendererPane != null) + " mouse=" + (mouseInputListener != null)
                    + " foco=" + (focusListener != null) + " teclas=" + (keyListener != null)
                    + " tabla=" + (table != null);
        }
    }

    static void tablas() {
        linea("--- BasicTableUI ---");
        JTable t = new JTable(new Object[][] {{"a", "b"}, {"c", "d"}},
                new Object[] {"uno", "dos"});
        Tabla u = new Tabla();
        linea("comparte instancia="
                + (javax.swing.plaf.basic.BasicTableUI.createUI(t)
                        == javax.swing.plaf.basic.BasicTableUI.createUI(t)));
        u.installUI(t);
        linea(u.estado());
        linea("fondo=" + col(t.getBackground()) + " frente=" + col(t.getForeground())
                + " fuente=" + fue(t.getFont()) + " opaca=" + t.isOpaque()
                + " borde=" + t.getBorder());
        linea("seleccion=" + col(t.getSelectionBackground())
                + " sobre " + col(t.getSelectionForeground())
                + " cuadricula=" + col(t.getGridColor()));
        linea("alto de fila=" + t.getRowHeight() + " margen=" + t.getRowMargin()
                + " intercelda=" + t.getIntercellSpacing()
                + " lineas h=" + t.getShowHorizontalLines()
                + " v=" + t.getShowVerticalLines());
        Dimension pref = u.getPreferredSize(t);
        Dimension min = u.getMinimumSize(t);
        Dimension max = u.getMaximumSize(t);
        linea("ancho preferido=" + pref.width + " minimo=" + min.width + " maximo=" + max.width);
        linea("los tres comparten el alto="
                + (pref.height == min.height && min.height == max.height)
                + " y es el fin de la ultima fila="
                + (pref.height == t.getCellRect(t.getRowCount() - 1, 0, true).y
                        + t.getCellRect(t.getRowCount() - 1, 0, true).height));
        linea("linea de base=" + u.getBaseline(t, 200, 40)
                + " al cambiar de tamano=" + u.getBaselineResizeBehavior(t));
        JTable vacia = new JTable();
        Tabla uv = new Tabla();
        uv.installUI(vacia);
        linea("vacia: preferido=" + uv.getPreferredSize(vacia)
                + " minimo=" + uv.getMinimumSize(vacia)
                + " maximo=" + uv.getMaximumSize(vacia)
                + " linea de base=" + uv.getBaseline(vacia, 200, 40));
    }

    static class Escritorio extends BasicDesktopPaneUI {
        String teclas() {
            return "cerrar=" + closeKey + " navegar=" + navigateKey + " navegar2=" + navigateKey2
                    + " minimizar=" + minimizeKey + " maximizar=" + maximizeKey;
        }

        boolean hayAdministrador() {
            return desktopManager != null;
        }

        boolean esDelAspecto() {
            return desktopManager instanceof javax.swing.plaf.UIResource;
        }
    }

    static void encabezados() {
        linea("--- BasicTableHeaderUI ---");
        JTable t = new JTable(new Object[][] {{"a", "b"}, {"c", "d"}},
                new Object[] {"uno", "dos"});
        JTableHeader h = t.getTableHeader();
        Encabezado u = new Encabezado();
        linea("comparte instancia="
                + (BasicTableHeaderUI.createUI(h) == BasicTableHeaderUI.createUI(h)));
        u.installUI(h);
        linea(u.estado());
        linea("fondo=" + col(h.getBackground()) + " frente=" + col(h.getForeground())
                + " fuente=" + fue(h.getFont()) + " opaco=" + h.isOpaque()
                + " borde=" + h.getBorder() + " hay dibujante="
                + (h.getDefaultRenderer() != null));

        Dimension pref = u.getPreferredSize(h);
        Dimension min = u.getMinimumSize(h);
        Dimension max = u.getMaximumSize(h);
        linea("ancho preferido=" + pref.width + " minimo=" + min.width + " maximo=" + max.width);
        linea("los tres comparten el alto="
                + (pref.height == min.height && min.height == max.height));
        // La separacion entre columnas no entra en la cuenta; ver la nota del UI.
        linea("el ancho preferido es la suma pelada de las columnas="
                + (pref.width == h.getColumnModel().getColumn(0).getPreferredWidth()
                        + h.getColumnModel().getColumn(1).getPreferredWidth()));
        linea("separacion entre columnas=" + h.getColumnModel().getColumnMargin());

        JTableHeader vacio = new JTableHeader();
        Encabezado uv = new Encabezado();
        uv.installUI(vacio);
        linea("sin columnas: preferido=" + uv.getPreferredSize(vacio)
                + " minimo=" + uv.getMinimumSize(vacio)
                + " maximo=" + uv.getMaximumSize(vacio)
                + " linea de base=" + uv.getBaseline(vacio, 100, 20));

        try {
            u.getBaseline(h, -1, -1);
            linea("tamano negativo aceptado");
        } catch (IllegalArgumentException e) {
            linea("tamano negativo rechazado");
        }
    }

    static void escritorios() {
        linea("--- BasicDesktopPaneUI ---");
        JDesktopPane d = new JDesktopPane();
        Escritorio u = new Escritorio();
        linea("comparte instancia="
                + (BasicDesktopPaneUI.createUI(d) == BasicDesktopPaneUI.createUI(d)));
        u.installUI(d);
        linea("teclas: " + u.teclas());
        linea("hay administrador=" + u.hayAdministrador() + " es del aspecto=" + u.esDelAspecto()
                + " es el del escritorio=" + (d.getDesktopManager() != null));
        linea("fondo=" + col(d.getBackground()) + " opaco=" + d.isOpaque());
        linea("preferido=" + u.getPreferredSize(d) + " minimo=" + u.getMinimumSize(d)
                + " maximo=" + u.getMaximumSize(d));
        javax.swing.InputMap im = d.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        linea("claves atadas=" + ((im == null || im.allKeys() == null) ? 0 : im.allKeys().length));
        linea("acciones=" + ordenadas(d.getActionMap().allKeys()));
    }

    static String ordenadas(Object[] o) {
        if (o == null) {
            return "-";
        }
        String[] s = new String[o.length];
        for (int i = 0; i < o.length; i++) {
            s[i] = String.valueOf(o[i]);
        }
        java.util.Arrays.sort(s);
        return java.util.Arrays.toString(s);
    }

    public static int run() {
        encabezados();
        tablas();
        escritorios();
        return 0;
    }
}
