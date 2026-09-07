import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI;
import javax.swing.plaf.basic.BasicIconFactory;
import javax.swing.plaf.basic.BasicMenuBarUI;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import javax.swing.plaf.basic.BasicRadioButtonMenuItemUI;
import javax.swing.plaf.basic.DefaultMenuLayout;

/**
 * Los seis aspectos basicos de menu, contra el JDK.
 *
 * <p>Los UI se instalan a mano por el mismo motivo que en las otras dos pruebas de aspecto.
 *
 * <h2>Por que el ancho no se compara crudo</h2>
 *
 * <p>Un item de menu escribe en Dialog negrita 12, y esta VM dibuja toda fuente con la misma cara
 * -- ver {@code KajiFontMetrics} --, asi que "Abrir" en negrita mide 25 aca y 28 en el JDK. Comparar
 * el ancho seria comparar las tipografias, no la cuenta.
 *
 * <p>Lo que si se compara es la <em>formula</em>: la prueba la rehace con las metricas del propio
 * componente y pregunta si el UI dio ese numero. Eso tiene que dar {@code true} de los dos lados, y
 * si la cuenta del UI se equivoca en un gap, deja de darlo.
 *
 * <p>El alto si se compara crudo: sale del alto de linea, que es el mismo.
 */
public class Plaf3 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String col(Color c) {
        return (c == null) ? "-" : c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    static String fue(Font f) {
        return (f == null) ? "-" : f.getFamily() + "/" + f.getStyle() + "/" + f.getSize();
    }

    static String ic(Icon i) {
        return (i == null) ? "-" : i.getIconWidth() + "x" + i.getIconHeight();
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    /** Expone los campos protegidos, que es donde esta casi todo lo que instala. */
    static class Item extends BasicMenuItemUI {
        String pre() {
            return getPropertyPrefix();
        }

        Icon tilde() {
            return checkIcon;
        }

        void volcar() {
            linea("  seleccion=" + col(selectionBackground) + " sobre " + col(selectionForeground));
            linea("  apagado=" + col(disabledForeground)
                    + " acelerador=" + col(acceleratorForeground)
                    + " elegido=" + col(acceleratorSelectionForeground));
            linea("  fuente del acelerador=" + fue(acceleratorFont)
                    + " separador=" + acceleratorDelimiter + " gap=" + defaultTextIconGap);
            linea("  tilde=" + ic(checkIcon) + " flecha=" + ic(arrowIcon));
            linea("  escuchas: mouse=" + (mouseInputListener != null)
                    + " arrastre=" + (menuDragMouseListener != null)
                    + " teclas=" + (menuKeyListener != null)
                    + " propiedad=" + (propertyChangeListener != null));
        }
    }

    static class Menu extends BasicMenuUI {
        String pre() {
            return getPropertyPrefix();
        }

        Icon tilde() {
            return checkIcon;
        }

        String escuchas() {
            return "cambio=" + (changeListener != null) + " menu=" + (menuListener != null);
        }
    }

    static class Casilla extends BasicCheckBoxMenuItemUI {
        String pre() {
            return getPropertyPrefix();
        }

        Icon tilde() {
            return checkIcon;
        }
    }

    static class Opcion extends BasicRadioButtonMenuItemUI {
        String pre() {
            return getPropertyPrefix();
        }

        Icon tilde() {
            return checkIcon;
        }
    }

    /** Rehace la cuenta del ancho y del alto; ver la nota de la clase. */
    static boolean esperado(JMenuItem b, BasicMenuItemUI u, Icon tilde) {
        int gap = 4;
        String text = b.getText();
        boolean hayTexto = text != null && !text.equals("");
        FontMetrics fm = b.getFontMetrics(b.getFont());
        FontMetrics fa = b.getFontMetrics(new Font("Dialog", Font.PLAIN, 10));
        boolean columnas = !(b instanceof JMenu && ((JMenu) b).isTopLevelMenu());
        Insets in = b.getInsets();
        int w = in.left + in.right;
        int alto = hayTexto ? fm.getHeight() : 0;
        if (columnas && tilde != null) {
            w += tilde.getIconWidth() + gap;
            alto = Math.max(alto, tilde.getIconHeight());
        }
        if (b.getIcon() != null) {
            w += b.getIcon().getIconWidth() + gap;
            alto = Math.max(alto, b.getIcon().getIconHeight());
        }
        if (hayTexto) {
            w += fm.stringWidth(text) + gap;
        }
        KeyStroke k = b.getAccelerator();
        if (k != null) {
            String t = "";
            if (k.getModifiers() > 0) {
                t = java.awt.event.KeyEvent.getKeyModifiersText(k.getModifiers()) + "-";
            }
            t += java.awt.event.KeyEvent.getKeyText(k.getKeyCode());
            w += fa.stringWidth(t) + gap;
            alto = Math.max(alto, fa.getHeight());
        }
        if (columnas) {
            // La flecha del basico y la de Metal miden las dos 4 x 8.
            w += 4 + gap;
            alto = Math.max(alto, 8);
        }
        w += gap;
        int h = alto + in.top + in.bottom;
        if (w % 2 == 0) {
            w++;
        }
        if (h % 2 == 0) {
            h++;
        }
        Dimension d = u.getPreferredSize(b);
        return d.width == w && d.height == h;
    }

    static boolean masAncho(JMenuItem conAtajo, BasicMenuItemUI u) {
        JMenuItem sin = new JMenuItem(conAtajo.getText());
        Item us = new Item();
        us.installUI(sin);
        return u.getPreferredSize(conAtajo).width > us.getPreferredSize(sin).width;
    }

    static void items() {
        linea("--- BasicMenuItemUI ---");
        JMenuItem mi = new JMenuItem("Abrir");
        Item u = new Item();
        linea("comparte instancia="
                + (BasicMenuItemUI.createUI(mi) == BasicMenuItemUI.createUI(mi)));
        u.installUI(mi);
        linea("prefijo=" + u.pre());
        u.volcar();
        linea("  fondo=" + col(mi.getBackground()) + " frente=" + col(mi.getForeground())
                + " fuente=" + fue(mi.getFont()) + " opaco=" + mi.isOpaque());
        linea("  margen=" + mi.getMargin() + " insets=" + mi.getInsets());
        linea("  alto preferido=" + u.getPreferredSize(mi).height
                + " minimo=" + u.getMinimumSize(mi) + " maximo=" + u.getMaximumSize(mi));
        linea("  camino sin menu abierto=" + u.getPath().length);
        // El UI prende el borde. Que queda al sacarlo no se compara: `uninstallDefaults` lo
        // devuelve a como estaba antes de instalar, y del otro lado ya habia pasado Metal.
        JMenuItem sinBorde = new JMenuItem("x");
        Item ub = new Item();
        ub.installUI(sinBorde);
        linea("  el UI prende el borde=" + sinBorde.isBorderPainted());

        String[] textos = {"", "A", "Abrir", "Guardar como..."};
        for (int i = 0; i < textos.length; i++) {
            JMenuItem x = new JMenuItem(textos[i]);
            Item ux = new Item();
            ux.installUI(x);
            linea("  '" + textos[i] + "' da la formula=" + esperado(x, ux, ux.tilde())
                    + " alto=" + ux.getPreferredSize(x).height);
        }
        String[] atajos = {"ctrl O", "F5", "ctrl shift S"};
        for (int i = 0; i < atajos.length; i++) {
            JMenuItem x = new JMenuItem("Abrir");
            x.setAccelerator(KeyStroke.getKeyStroke(atajos[i]));
            Item ux = new Item();
            ux.installUI(x);
            linea("  con " + atajos[i] + " da la formula=" + esperado(x, ux, ux.tilde())
                    + " y es mas ancho=" + masAncho(x, ux));
        }
        JMenuItem conIcono = new JMenuItem("Abrir", BasicIconFactory.getCheckBoxIcon());
        Item ui = new Item();
        ui.installUI(conIcono);
        linea("  con icono de 13x13 da la formula=" + esperado(conIcono, ui, ui.tilde())
                + " alto=" + ui.getPreferredSize(conIcono).height);
    }

    static void aceleradores() {
        linea("--- el texto de un acelerador ---");
        String[] atajos = {"ctrl O", "F5", "ctrl shift S", "alt F4"};
        for (int i = 0; i < atajos.length; i++) {
            KeyStroke k = KeyStroke.getKeyStroke(atajos[i]);
            linea("  " + atajos[i] + " modificadores=" + k.getModifiers()
                    + " texto=" + java.awt.event.KeyEvent.getKeyModifiersText(k.getModifiers())
                    + "-" + java.awt.event.KeyEvent.getKeyText(k.getKeyCode()));
        }
    }

    static void marcables() {
        linea("--- items con estado ---");
        JCheckBoxMenuItem cb = new JCheckBoxMenuItem("Abrir");
        Casilla uc = new Casilla();
        uc.installUI(cb);
        linea("prefijo=" + uc.pre() + " hay tilde=" + (uc.tilde() != null)
                + " da la formula=" + esperado(cb, uc, uc.tilde())
                + " alto=" + uc.getPreferredSize(cb).height);
        JRadioButtonMenuItem rb = new JRadioButtonMenuItem("Abrir");
        Opcion uo = new Opcion();
        uo.installUI(rb);
        linea("prefijo=" + uo.pre() + " hay tilde=" + (uo.tilde() != null)
                + " da la formula=" + esperado(rb, uo, uo.tilde())
                + " alto=" + uo.getPreferredSize(rb).height);
        linea("comparten instancia="
                + (BasicCheckBoxMenuItemUI.createUI(cb) == BasicCheckBoxMenuItemUI.createUI(cb)));
    }

    static void menus() {
        linea("--- BasicMenuUI ---");
        JMenu m = new JMenu("Archivo");
        Menu u = new Menu();
        u.installUI(m);
        linea("prefijo=" + u.pre() + " demora=" + m.getDelay() + " escuchas=" + u.escuchas());
        linea("suelto: da la formula=" + esperado(m, u, u.tilde())
                + " alto=" + u.getPreferredSize(m).height
                + " minimo=" + u.getMinimumSize(m) + " maximo=" + u.getMaximumSize(m));
        int anchoSuelto = u.getPreferredSize(m).width;
        JMenuBar mb = new JMenuBar();
        mb.add(m);
        Dimension max = u.getMaximumSize(m);
        linea("de barra: da la formula=" + esperado(m, u, u.tilde())
                + " maximo alto=" + max.height
                + " maximo ancho es el preferido del componente="
                + (max.width == m.getPreferredSize().width));
        linea("de barra es mas angosto que suelto="
                + (u.getPreferredSize(m).width < anchoSuelto));
    }

    static void barras() {
        linea("--- BasicMenuBarUI y BasicPopupMenuUI ---");
        JMenuBar mb = new JMenuBar();
        BasicMenuBarUI u = (BasicMenuBarUI) BasicMenuBarUI.createUI(mb);
        u.installUI(mb);
        linea("barra fondo=" + col(mb.getBackground()) + " frente=" + col(mb.getForeground())
                + " fuente=" + fue(mb.getFont()) + " opaca=" + mb.isOpaque());
        linea("barra acomodador=" + corto(mb.getLayout())
                + " insets=" + mb.getInsets()
                + " preferido=" + u.getPreferredSize(mb)
                + " minimo=" + u.getMinimumSize(mb) + " maximo=" + u.getMaximumSize(mb));

        JPopupMenu pm = new JPopupMenu();
        BasicPopupMenuUI up = (BasicPopupMenuUI) BasicPopupMenuUI.createUI(pm);
        up.installUI(pm);
        linea("desplegable fondo=" + col(pm.getBackground())
                + " fuente=" + fue(pm.getFont()) + " opaco=" + pm.isOpaque()
                + " acomodador=" + corto(pm.getLayout()));

        linea("vacios: barra=" + new DefaultMenuLayout(mb, BoxLayout.LINE_AXIS)
                .preferredLayoutSize(mb)
                + " desplegable=" + new DefaultMenuLayout(pm, BoxLayout.Y_AXIS)
                        .preferredLayoutSize(pm));
        linea("el acomodador es del aspecto="
                + (mb.getLayout() instanceof javax.swing.plaf.UIResource));
    }

    public static int run() {
        items();
        aceleradores();
        marcables();
        menus();
        barras();
        return 0;
    }
}
