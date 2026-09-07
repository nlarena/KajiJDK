import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/**
 * El combo y su lista desplegable, contra el JDK.
 *
 * <p>Los anchos que salen del texto no se comparan crudos --el combo escribe en Dialog negrita 12 y
 * esta VM dibuja toda fuente con la misma cara--: se compara la relacion, que el ancho total sea el
 * del item mas ancho mas el de la flechita.
 *
 * <p>Queda afuera abrir la lista, que necesita ventana.
 */
public class Plaf8 {

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

    static class Combo extends BasicComboBoxUI {
        String estado() {
            return "combo=" + (comboBox != null) + " flecha=" + (arrowButton != null)
                    + " editor=" + (editor != null) + " lista=" + (listBox != null)
                    + " desplegable=" + (popup != null) + " panel=" + (currentValuePane != null)
                    + " foco=" + hasFocus + " minimo sucio=" + isMinimumSizeDirty
                    + " cuadrado=" + squareButton + " relleno=" + padding
                    + " cache=" + cachedMinimumSize;
        }

        String escuchas() {
            return "foco=" + (focusListener != null) + " item=" + (itemListener != null)
                    + " tecla=" + (keyListener != null)
                    + " propiedad=" + (propertyChangeListener != null)
                    + " datos=" + (listDataListener != null)
                    + " mouse=" + (popupMouseListener != null)
                    + " movimiento=" + (popupMouseMotionListener != null)
                    + " tecla del desplegable=" + (popupKeyListener != null);
        }

        String creados() {
            return "flecha=" + corto(createArrowButton()) + " editor=" + corto(createEditor())
                    + " dibujante=" + corto(createRenderer()) + " desplegable="
                    + corto(createPopup()) + " acomodador=" + corto(createLayoutManager());
        }

        Dimension porOmision() {
            return getDefaultSize();
        }

        Dimension display() {
            return getDisplaySize();
        }

        Rectangle rectValor() {
            return rectangleForCurrentValue();
        }

        Insets ins() {
            return getInsets();
        }

        boolean nav(int k) {
            return isNavigationKey(k);
        }

        ComboPopup desplegable() {
            return createPopup();
        }

    }

    static void combos() {
        linea("--- BasicComboBoxUI ---");
        JComboBox<Object> cb = new JComboBox<Object>(new Object[] {"uno", "dos", "tres"});
        Combo u = new Combo();
        linea("comparte instancia="
                + (BasicComboBoxUI.createUI(cb) == BasicComboBoxUI.createUI(cb)));
        u.installUI(cb);
        linea("estado: " + u.estado());
        linea("escuchas: " + u.escuchas());
        linea("creados: " + u.creados());
        linea("fondo=" + col(cb.getBackground()) + " frente=" + col(cb.getForeground())
                + " fuente=" + fue(cb.getFont()) + " opaco=" + cb.isOpaque()
                + " borde=" + cb.getBorder() + " acomodador=" + corto(cb.getLayout()));
        linea("insets=" + u.ins() + " rectangulo del valor=" + u.rectValor());

        Dimension display = u.display();
        Dimension pref = u.getPreferredSize(cb);
        // La flechita es cuadrada: ocupa lo que mide el renglon de alto.
        linea("el ancho es el del item mas ancho mas la flecha cuadrada="
                + (pref.width == display.width + display.height));
        linea("el alto es el del item mas alto=" + (pref.height == display.height));
        linea("el preferido es el minimo=" + pref.equals(u.getMinimumSize(cb)));
        linea("maximo=" + u.getMaximumSize(cb));
        linea("tras medir, el minimo dejo de estar sucio=" + !u.estado().contains("sucio=true"));

        linea("al cambiar de tamano=" + u.getBaselineResizeBehavior(cb));
        linea("linea de base 100x25=" + u.getBaseline(cb, 100, 25));
        linea("desplegable visible=" + u.isPopupVisible(cb)
                + " navegable por foco=" + u.isFocusTraversable(cb)
                + " hijos accesibles=" + u.getAccessibleChildrenCount(cb));
        linea("navegacion: arriba=" + u.nav(KeyEvent.VK_UP)
                + " A=" + u.nav(KeyEvent.VK_A)
                + " re pag=" + u.nav(KeyEvent.VK_PAGE_UP)
                + " enter=" + u.nav(KeyEvent.VK_ENTER));

        // El editor aparece y desaparece con `setEditable`.
        cb.setEditable(true);
        linea("editable: hay editor=" + u.estado().contains("editor=true"));
        cb.setEditable(false);
        linea("no editable: hay editor=" + u.estado().contains("editor=true"));

        JComboBox<Object> vacio = new JComboBox<Object>();
        Combo uv = new Combo();
        uv.installUI(vacio);
        linea("vacio: el ancho es el de un renglon vacio mas la flecha cuadrada="
                + (uv.getPreferredSize(vacio).width
                        == uv.porOmision().width + uv.porOmision().height));
        linea("vacio: display es el de un renglon vacio="
                + uv.display().equals(uv.porOmision()));
    }

    static void desplegables() {
        linea("--- BasicComboPopup ---");
        JComboBox<Object> cb = new JComboBox<Object>(new Object[] {"uno", "dos", "tres"});
        Combo u = new Combo();
        u.installUI(cb);
        ComboPopup p = u.desplegable();
        linea("hay lista=" + (p.getList() != null) + " visible=" + p.isVisible());
        linea("escuchas: mouse=" + (p.getMouseListener() != null)
                + " movimiento=" + (p.getMouseMotionListener() != null)
                + " tecla=" + (p.getKeyListener() != null));
        BasicComboPopup bp = (BasicComboPopup) p;
        JList<Object> l = bp.getList();
        linea("lista: fondo=" + col(l.getBackground()) + " frente=" + col(l.getForeground())
                + " seleccion=" + col(l.getSelectionBackground())
                + " sobre " + col(l.getSelectionForeground()));
        linea("lista: enfocable=" + l.isFocusable() + " borde=" + l.getBorder()
                + " modo de seleccion=" + l.getSelectionMode()
                + " modelo es el del combo=" + (l.getModel() == cb.getModel()));
        linea("desplegable: borde=" + corto(bp.getBorder())
                + " acomodador=" + corto(bp.getLayout())
                + " liviano=" + bp.isLightWeightPopupEnabled()
                + " enfocable=" + bp.isFocusTraversable()
                + " nombre=" + bp.getName());
        linea("elige lo que el combo tiene elegido="
                + (l.getSelectedIndex() == cb.getSelectedIndex()));
    }

    public static int run() {
        combos();
        desplegables();
        return 0;
    }
}
