import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.plaf.basic.BasicEditorPaneUI;
import javax.swing.plaf.basic.BasicFormattedTextFieldUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;

/**
 * Los seis aspectos basicos de texto, contra el JDK.
 *
 * <p>Cada uno se instala a mano por el mismo motivo que en {@code Plaf1}: del otro lado hay Metal y
 * aca no hay aspecto, y lo que se quiere comparar es el basico.
 *
 * <p>Lo que se compara es que arme la vista que corresponde, donde queda la linea de base, y los
 * tamanos. Queda afuera lo que dibuja.
 */
public class Plaf2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Las subclases existen solo para llegar a {@code getPropertyPrefix}, que es protegido. */
    static class Campo extends BasicTextFieldUI {
        String pre() {
            return getPropertyPrefix();
        }
    }

    static class Area extends BasicTextAreaUI {
        String pre() {
            return getPropertyPrefix();
        }
    }

    static class Editor extends BasicEditorPaneUI {
        String pre() {
            return getPropertyPrefix();
        }
    }

    static class Estilos extends BasicTextPaneUI {
        String pre() {
            return getPropertyPrefix();
        }
    }

    static class Clave extends BasicPasswordFieldUI {
        String pre() {
            return getPropertyPrefix();
        }
    }

    static class ConFormato extends BasicFormattedTextFieldUI {
        String pre() {
            return getPropertyPrefix();
        }
    }

    static String vista(javax.swing.plaf.basic.BasicTextUI u, JTextComponent c) {
        View v = u.create(c.getDocument().getDefaultRootElement());
        if (v == null) {
            return "-";
        }
        String n = v.getClass().getName();
        return n.substring(n.lastIndexOf('.') + 1);
    }

    static void prefijos() {
        linea("--- los prefijos ---");
        linea(new Campo().pre() + " " + new Area().pre() + " " + new Editor().pre()
                + " " + new Estilos().pre() + " " + new Clave().pre()
                + " " + new ConFormato().pre());
    }

    static void campos() {
        linea("--- BasicTextFieldUI ---");
        JTextField f = new JTextField("hola");
        BasicTextFieldUI u = new BasicTextFieldUI();
        linea("comparte instancia="
                + (BasicTextFieldUI.createUI(f) == BasicTextFieldUI.createUI(f)));
        u.installUI(f);
        linea("vista=" + vista(u, f));
        linea("linea de base con 20=" + u.getBaseline(f, 100, 20)
                + " con 30=" + u.getBaseline(f, 100, 30)
                + " con 0=" + u.getBaseline(f, 100, 0));
        linea("al cambiar de tamano=" + u.getBaselineResizeBehavior(f));
        try {
            u.getBaseline(f, -1, -1);
            linea("tamano negativo aceptado");
        } catch (IllegalArgumentException e) {
            linea("tamano negativo rechazado");
        }

        JFormattedTextField ff = new JFormattedTextField();
        BasicFormattedTextFieldUI uf = new BasicFormattedTextFieldUI();
        uf.installUI(ff);
        linea("con formato vista=" + vista(uf, ff));

        JPasswordField p = new JPasswordField("hola");
        BasicPasswordFieldUI up = new BasicPasswordFieldUI();
        up.installUI(p);
        linea("eco despues=" + (int) p.getEchoChar() + " vista=" + vista(up, p));
        // Un eco puesto a mano no se pisa.
        JPasswordField mio = new JPasswordField();
        mio.setEchoChar('#');
        new BasicPasswordFieldUI().installUI(mio);
        linea("eco del usuario=" + mio.getEchoChar());
    }

    static void areas() {
        linea("--- BasicTextAreaUI ---");
        JTextArea t = new JTextArea("hola\nchau");
        BasicTextAreaUI u = new BasicTextAreaUI();
        u.installUI(t);
        linea("vista sin corte=" + vista(u, t));
        t.setLineWrap(true);
        linea("vista con corte=" + vista(u, t));
        t.setLineWrap(false);
        linea("linea de base con 30=" + u.getBaseline(t, 100, 30)
                + " con 0=" + u.getBaseline(t, 100, 0)
                + " al cambiar de tamano=" + u.getBaselineResizeBehavior(t));
        Dimension pref = u.getPreferredSize(t);
        Dimension min = u.getMinimumSize(t);
        linea("preferido=" + pref + " minimo=" + min);
        View raiz = u.getRootView(t);
        linea("el ancho es el de la vista mas uno="
                + (pref.width == (int) raiz.getPreferredSpan(View.X_AXIS) + 1));
        t.putClientProperty("caretWidth", Integer.valueOf(5));
        linea("con cursor de cinco crece cuatro="
                + (u.getPreferredSize(t).width == pref.width + 4));
    }

    static void paneles() {
        linea("--- BasicEditorPaneUI y BasicTextPaneUI ---");
        JEditorPane e = new JEditorPane();
        BasicEditorPaneUI ue = new BasicEditorPaneUI();
        ue.installUI(e);
        linea("el juego es el del panel=" + (ue.getEditorKit(e) == e.getEditorKit()));
        linea("tipo de contenido=" + e.getContentType());

        JTextPane p = new JTextPane();
        BasicTextPaneUI up = new BasicTextPaneUI();
        up.installUI(p);
        linea("el juego es el del panel=" + (up.getEditorKit(p) == p.getEditorKit()));
        javax.swing.text.Style base = p.getStyledDocument()
                .getStyle(javax.swing.text.StyleContext.DEFAULT_STYLE);
        linea("hay estilo de omision=" + (base != null));
        if (base != null) {
            linea("familia=" + javax.swing.text.StyleConstants.getFontFamily(base)
                    + " cuerpo=" + javax.swing.text.StyleConstants.getFontSize(base));
        }
    }

    public static int run() {
        prefijos();
        campos();
        areas();
        paneles();
        return 0;
    }
}
