import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicIconFactory;
import javax.swing.plaf.basic.BasicPanelUI;
import javax.swing.plaf.basic.BasicPopupMenuSeparatorUI;
import javax.swing.plaf.basic.BasicRootPaneUI;
import javax.swing.plaf.basic.BasicSeparatorUI;
import javax.swing.plaf.basic.BasicToolBarSeparatorUI;
import javax.swing.plaf.basic.BasicToolTipUI;

/**
 * Los aspectos basicos que no dibujan casi nada, contra el JDK.
 *
 * <p>Cada UI se instancia y se instala a mano en vez de dejar que lo ponga el aspecto: del otro
 * lado hay Metal instalado y aca no hay ninguno, asi que pedir {@code c.getUI()} compararia dos
 * cosas distintas. Instalando el basico a mano los dos lados corren el mismo codigo.
 *
 * <p>Queda afuera lo que dibuja, que se compara en las pruebas de pintura, y los colores que el
 * aspecto instalado ya habia puesto antes: lo que se compara es lo que instala el basico.
 */
public class Plaf1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static String col(Color c) {
        if (c == null) {
            return "-";
        }
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue()
                + (c instanceof UIResource ? " (aspecto)" : "");
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static void paneles() {
        linea("--- BasicPanelUI ---");
        JPanel p = new JPanel();
        BasicPanelUI u = (BasicPanelUI) BasicPanelUI.createUI(p);
        linea("comparte instancia=" + (BasicPanelUI.createUI(p) == BasicPanelUI.createUI(new JPanel())));
        u.installUI(p);
        linea("fondo=" + col(p.getBackground()) + " frente=" + col(p.getForeground())
                + " fuente=" + fue(p.getFont()) + " opaco=" + p.isOpaque()
                + " borde=" + p.getBorder());
        linea("linea de base=" + u.getBaseline(p, 100, 50)
                + " al cambiar de tamano=" + u.getBaselineResizeBehavior(p));
        try {
            u.getBaseline(p, -1, -1);
            linea("tamano negativo aceptado");
        } catch (IllegalArgumentException e) {
            linea("tamano negativo rechazado");
        }
        try {
            u.getBaseline(null, 1, 1);
            linea("componente nulo aceptado");
        } catch (NullPointerException e) {
            linea("componente nulo revienta");
        }
        // Un color puesto por el usuario no se pisa; uno del aspecto si.
        JPanel mio = new JPanel();
        mio.setBackground(new Color(1, 2, 3));
        ((BasicPanelUI) BasicPanelUI.createUI(mio)).installUI(mio);
        linea("fondo del usuario=" + col(mio.getBackground()));
    }

    static void separadores() {
        linea("--- los separadores ---");
        JSeparator h = new JSeparator();
        BasicSeparatorUI uh = new BasicSeparatorUI();
        linea("comparte instancia=" + (BasicSeparatorUI.createUI(h) == BasicSeparatorUI.createUI(h)));
        uh.installUI(h);
        linea("horizontal pref=" + uh.getPreferredSize(h) + " min=" + uh.getMinimumSize(h)
                + " max=" + uh.getMaximumSize(h));
        linea("fondo=" + col(h.getBackground()) + " frente=" + col(h.getForeground())
                + " opaco=" + h.isOpaque());
        JSeparator v = new JSeparator(SwingConstants.VERTICAL);
        BasicSeparatorUI uv = new BasicSeparatorUI();
        uv.installUI(v);
        linea("vertical pref=" + uv.getPreferredSize(v) + " max=" + uv.getMaximumSize(v));

        JPopupMenu.Separator ps = new JPopupMenu.Separator();
        BasicPopupMenuSeparatorUI up = new BasicPopupMenuSeparatorUI();
        up.installUI(ps);
        linea("de menu pref=" + up.getPreferredSize(ps) + " min=" + up.getMinimumSize(ps));

        JToolBar.Separator ts = new JToolBar.Separator();
        BasicToolBarSeparatorUI ut = new BasicToolBarSeparatorUI();
        ut.installUI(ts);
        linea("de barra pref=" + ut.getPreferredSize(ts)
                + " tamano del separador=" + ts.getSeparatorSize());
        JToolBar.Separator mio = new JToolBar.Separator(new Dimension(4, 5));
        new BasicToolBarSeparatorUI().installUI(mio);
        linea("de barra con tamano propio=" + mio.getSeparatorSize());
    }

    static void carteles() {
        linea("--- BasicToolTipUI ---");
        JToolTip t = new JToolTip();
        linea("comparte instancia="
                + (BasicToolTipUI.createUI(t) == BasicToolTipUI.createUI(new JToolTip())));
        BasicToolTipUI u = (BasicToolTipUI) BasicToolTipUI.createUI(t);
        u.installUI(t);
        linea("fondo=" + col(t.getBackground()) + " frente=" + col(t.getForeground())
                + " fuente=" + fue(t.getFont()) + " opaco=" + t.isOpaque());
        linea("borde=" + corto(t.getBorder()) + " margenes=" + t.getInsets());
        linea("sin texto pref=" + u.getPreferredSize(t));
        t.setTipText("hola");
        linea("con texto pref=" + u.getPreferredSize(t) + " min=" + u.getMinimumSize(t)
                + " max=" + u.getMaximumSize(t));
    }

    static void iconos() {
        linea("--- BasicIconFactory ---");
        Icon[] ic = {
            BasicIconFactory.getCheckBoxIcon(),
            BasicIconFactory.getRadioButtonIcon(),
            BasicIconFactory.getCheckBoxMenuItemIcon(),
            BasicIconFactory.getRadioButtonMenuItemIcon(),
            BasicIconFactory.getMenuItemCheckIcon(),
            BasicIconFactory.getMenuItemArrowIcon(),
            BasicIconFactory.getMenuArrowIcon(),
            BasicIconFactory.createEmptyFrameIcon(),
        };
        String[] nm = {"casilla", "redondel", "tilde de menu", "punto de menu",
            "hueco del tilde", "flecha del item", "flecha del menu", "ventana vacia"};
        for (int i = 0; i < ic.length; i++) {
            linea(nm[i] + " " + ic[i].getIconWidth() + "x" + ic[i].getIconHeight()
                    + " es del aspecto=" + (ic[i] instanceof UIResource));
        }
        linea("comparte instancia="
                + (BasicIconFactory.getCheckBoxIcon() == BasicIconFactory.getCheckBoxIcon()));
    }

    static void html() {
        linea("--- BasicHTML ---");
        linea("clave=" + BasicHTML.propertyKey + " base=" + BasicHTML.documentBaseKey);
        String[] pruebas = {"<html>hola</html>", "<HTML>x", "hola", "<html", "  <html>x", ""};
        for (int i = 0; i < pruebas.length; i++) {
            linea("es html(" + pruebas[i] + ")=" + BasicHTML.isHTMLString(pruebas[i]));
        }
        linea("es html(nulo)=" + BasicHTML.isHTMLString(null));

        JLabel l = new JLabel();
        BasicHTML.updateRenderer(l, "<html><b>hola</b></html>");
        linea("hay vista=" + (l.getClientProperty(BasicHTML.propertyKey) != null));
        BasicHTML.updateRenderer(l, "sin etiquetas");
        linea("tras texto plano hay vista=" + (l.getClientProperty(BasicHTML.propertyKey) != null));
        try {
            BasicHTML.getHTMLBaseline(null, -1, 10);
            linea("tamano negativo aceptado");
        } catch (IllegalArgumentException e) {
            linea("tamano negativo rechazado");
        }
    }

    static void panelesRaiz() {
        linea("--- BasicRootPaneUI ---");
        JRootPane r = new JRootPane();
        linea("comparte instancia="
                + (BasicRootPaneUI.createUI(r) == BasicRootPaneUI.createUI(new JRootPane())));
        BasicRootPaneUI u = (BasicRootPaneUI) BasicRootPaneUI.createUI(r);
        u.installUI(r);
        InputMap im = r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        linea("mapa=" + corto(im) + " padre=" + corto(im.getParent()));
        linea("claves sin boton=" + cuantas(im));
        linea("acciones=" + ordenadas(r.getActionMap().allKeys()));
        r.setDefaultButton(new JButton("x"));
        im = r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        linea("claves con boton=" + cuantas(im));
        linea("atajos=" + atajos(im));
        r.setDefaultButton(null);
        linea("claves tras sacarlo=" + cuantas(r.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)));
    }

    static int cuantas(InputMap im) {
        KeyStroke[] k = im.allKeys();
        return (k == null) ? 0 : k.length;
    }

    static String atajos(InputMap im) {
        KeyStroke[] k = im.allKeys();
        if (k == null) {
            return "-";
        }
        String[] s = new String[k.length];
        for (int i = 0; i < k.length; i++) {
            s[i] = k[i] + "=" + im.get(k[i]);
        }
        java.util.Arrays.sort(s);
        return java.util.Arrays.toString(s);
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
        paneles();
        separadores();
        carteles();
        iconos();
        html();
        panelesRaiz();
        return 0;
    }
}
