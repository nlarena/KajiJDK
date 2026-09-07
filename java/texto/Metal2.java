import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.plaf.metal.MetalDesktopIconUI;
import javax.swing.plaf.metal.MetalLabelUI;
import javax.swing.plaf.metal.MetalMenuBarUI;
import javax.swing.plaf.metal.MetalPopupMenuSeparatorUI;
import javax.swing.plaf.metal.MetalRootPaneUI;
import javax.swing.plaf.metal.MetalScrollPaneUI;
import javax.swing.plaf.metal.MetalSeparatorUI;
import javax.swing.plaf.metal.MetalTextFieldUI;
import javax.swing.plaf.metal.MetalToolTipUI;

/**
 * Las nueve clases simples de Metal, contra el JDK.
 *
 * <p>No se comparan anchos que dependan de la fuente: las metricas de esta biblioteca todavia no
 * son las del JDK. Si se comparan los tamanos fijos, los colores del tema, que instancias se
 * comparten y como contesta cada UI recien instalado.
 */
public class Metal2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String d(Dimension x) {
        return (x == null) ? "-" : x.width + "x" + x.height;
    }

    static String c(Color x) {
        return (x == null) ? "-" : x.getRed() + "," + x.getGreen() + "," + x.getBlue();
    }

    /** Instalar un UI de globo pide pasar por el propio UI: {@code setUI} es protegido. */
    static class Globo extends MetalToolTipUI {
        void poner(JToolTip t) {
            installUI(t);
        }

        boolean oculto() {
            return isAcceleratorHidden();
        }
    }

    static class Panel extends MetalScrollPaneUI {
        Object cambio() {
            return createScrollBarSwapListener();
        }
    }

    static void compartidos() {
        linea("--- que instancias se comparten ---");
        JLabel l = new JLabel("hola");
        linea("etiqueta=" + (MetalLabelUI.createUI(l) == MetalLabelUI.createUI(l)));
        linea("la etiqueta la comparte incluso entre componentes distintos="
                + (MetalLabelUI.createUI(l) == MetalLabelUI.createUI(new JLabel())));
        JSeparator s = new JSeparator();
        linea("separador=" + (MetalSeparatorUI.createUI(s) == MetalSeparatorUI.createUI(s)));
        linea("separador de menu=" + (MetalPopupMenuSeparatorUI.createUI(s)
                == MetalPopupMenuSeparatorUI.createUI(s)));
        JToolTip t = new JToolTip();
        linea("globo=" + (MetalToolTipUI.createUI(t) == MetalToolTipUI.createUI(t)));
        JTextField f = new JTextField();
        linea("campo=" + (MetalTextFieldUI.createUI(f) == MetalTextFieldUI.createUI(f)));
        JMenuBar m = new JMenuBar();
        linea("barra de menu=" + (MetalMenuBarUI.createUI(m) == MetalMenuBarUI.createUI(m)));
        JScrollPane p = new JScrollPane();
        linea("panel=" + (MetalScrollPaneUI.createUI(p) == MetalScrollPaneUI.createUI(p)));
        JRootPane r = new JRootPane();
        linea("raiz=" + (MetalRootPaneUI.createUI(r) == MetalRootPaneUI.createUI(r)));
    }

    static void separadores() {
        linea("--- separadores ---");
        JSeparator h = new JSeparator(SwingConstants.HORIZONTAL);
        MetalSeparatorUI uh = new MetalSeparatorUI();
        h.setUI(uh);
        linea("horizontal: preferido=" + d(uh.getPreferredSize(h))
                + " fondo=" + c(h.getBackground())
                + " frente=" + c(h.getForeground()));
        JSeparator v = new JSeparator(SwingConstants.VERTICAL);
        MetalSeparatorUI uv = new MetalSeparatorUI();
        v.setUI(uv);
        linea("vertical: preferido=" + d(uv.getPreferredSize(v)));
        JSeparator p = new JSeparator();
        MetalPopupMenuSeparatorUI up = new MetalPopupMenuSeparatorUI();
        p.setUI(up);
        linea("de menu: preferido=" + d(up.getPreferredSize(p))
                + " fondo=" + c(p.getBackground())
                + " frente=" + c(p.getForeground()));
        // Y el de menu no mira la orientacion.
        JSeparator pv = new JSeparator(SwingConstants.VERTICAL);
        MetalPopupMenuSeparatorUI upv = new MetalPopupMenuSeparatorUI();
        pv.setUI(upv);
        linea("de menu parado: preferido=" + d(upv.getPreferredSize(pv)));
    }

    static void globo() {
        linea("--- el globo de ayuda ---");
        linea("separacion=" + MetalToolTipUI.padSpaceBetweenStrings);
        JToolTip t = new JToolTip();
        t.setTipText("hola");
        Globo u = new Globo();
        u.poner(t);
        linea("acelerador escondido=" + u.oculto()
                + " acelerador='" + u.getAcceleratorString() + "'");
        linea("el preferido tiene alto positivo=" + (u.getPreferredSize(t).height > 0));

        // Un globo sin componente no tiene de donde sacar un atajo.
        JToolTip suelto = new JToolTip();
        Globo us = new Globo();
        us.poner(suelto);
        linea("sin componente: acelerador='" + us.getAcceleratorString() + "'");
    }

    static void campo() {
        linea("--- el campo de texto ---");
        JTextField f = new JTextField("hola");
        MetalTextFieldUI u = new MetalTextFieldUI();
        f.setUI(u);
        linea("editable=" + f.isEditable() + " fondo=" + c(f.getBackground()));
        f.setEditable(false);
        linea("de solo lectura: fondo=" + c(f.getBackground()));
        f.setEditable(true);
        linea("y de vuelta: fondo=" + c(f.getBackground()));

        // Un fondo puesto por el programa no se pisa.
        JTextField propio = new JTextField("x");
        MetalTextFieldUI up = new MetalTextFieldUI();
        propio.setUI(up);
        propio.setBackground(new Color(1, 2, 3));
        propio.setEditable(false);
        linea("con fondo propio: " + c(propio.getBackground()));
    }

    static void icono() {
        linea("--- el icono de escritorio ---");
        JDesktopPane d = new JDesktopPane();
        JInternalFrame f = new JInternalFrame("t", true, true, true, true);
        d.add(f);
        JInternalFrame.JDesktopIcon i = f.getDesktopIcon();
        MetalDesktopIconUI u = new MetalDesktopIconUI();
        i.setUI(u);
        Dimension pref = u.getPreferredSize(i);
        linea("ancho preferido=" + pref.width);
        linea("el minimo es el preferido=" + u.getMinimumSize(i).equals(pref)
                + " y el maximo tambien=" + u.getMaximumSize(i).equals(pref));
        linea("opaco=" + i.isOpaque());
    }

    static void panel() {
        linea("--- el panel de desplazamiento ---");
        JScrollPane p = new JScrollPane(new JLabel("x"));
        Panel u = new Panel();
        p.setUI(u);
        linea("cada llamada da un escucha nuevo=" + (u.cambio() != u.cambio()));
        linea("la barra vertical sabe que no esta suelta="
                + p.getVerticalScrollBar().getClientProperty("JScrollBar.isFreeStanding"));
        linea("y la horizontal tambien="
                + p.getHorizontalScrollBar().getClientProperty("JScrollBar.isFreeStanding"));

        // Cambiar una barra tiene que dejarle la marca a la nueva y sacarsela a la vieja.
        javax.swing.JScrollBar vieja = p.getVerticalScrollBar();
        javax.swing.JScrollBar nueva = new javax.swing.JScrollBar();
        p.setVerticalScrollBar(nueva);
        linea("tras cambiarla: nueva=" + nueva.getClientProperty("JScrollBar.isFreeStanding")
                + " vieja=" + vieja.getClientProperty("JScrollBar.isFreeStanding"));
    }

    public static int run() {
        compartidos();
        separadores();
        globo();
        campo();
        icono();
        panel();
        return 0;
    }
}
