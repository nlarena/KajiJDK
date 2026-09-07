import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * El panel dividido y el de solapas, contra el JDK.
 *
 * <p>Se compara el estado y los avisos. Queda afuera todo lo que dependa del aspecto: donde cae la
 * division en pixeles, en cuantas filas entran las solapas, y <strong>cuantos hijos tiene el
 * panel</strong>. Ese ultimo sorprende: el JDK cuenta uno mas porque su aspecto agrega la division
 * como un hijo. No es una diferencia de esta clase sino de que alla hay un aspecto instalado y aca
 * todavia no.
 */
public class Panel1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String nombre(Component c) {
        return (c == null) ? "nulo" : ((JLabel) c).getText();
    }

    static JLabel et(String t) {
        JLabel l = new JLabel(t);
        l.setName(t);
        return l;
    }

    /** Anota cada cambio de solapa. */
    static class Espia implements ChangeListener {

        private final JTabbedPane p;

        Espia(JTabbedPane p) {
            this.p = p;
        }

        public void stateChanged(ChangeEvent e) {
            linea("  cambio a " + p.getSelectedIndex());
        }
    }

    static void estado(JTabbedPane p) {
        String s = "solapas=" + p.getTabCount() + " elegida=" + p.getSelectedIndex()
                + " comp=" + nombre(p.getSelectedComponent()) + " [";
        for (int i = 0; i < p.getTabCount(); i++) {
            s = s + p.getTitleAt(i) + ":" + nombre(p.getComponentAt(i))
                    + (p.isEnabledAt(i) ? "" : "(off)") + " ";
        }
        linea(s.trim() + "]");
    }

    public static int run() {
        // --- el panel dividido ---
        linea("=== dividido");
        JSplitPane d = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        linea("orientacion=" + d.getOrientation() + " continuo=" + d.isContinuousLayout()
                + " flechitas=" + d.isOneTouchExpandable()
                + " peso=" + d.getResizeWeight()
                + " division=" + d.getDividerLocation());
        d.setTopComponent(et("arriba"));
        d.setBottomComponent(et("abajo"));
        linea("arriba=" + nombre(d.getTopComponent()) + " abajo=" + nombre(d.getBottomComponent())
                + " izq=" + nombre(d.getLeftComponent()) + " der="
                + nombre(d.getRightComponent()));
        d.setLeftComponent(et("nuevo"));
        linea("tras reemplazar: arriba=" + nombre(d.getTopComponent()));
        d.setResizeWeight(0.25);
        d.setOneTouchExpandable(true);
        d.setLastDividerLocation(42);
        linea("peso=" + d.getResizeWeight() + " flechitas=" + d.isOneTouchExpandable()
                + " anterior=" + d.getLastDividerLocation()
                + " raiz=" + d.isValidateRoot());
        d.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        linea("orientacion=" + d.getOrientation());
        try {
            d.setOrientation(9);
            linea("orientacion mala: sin queja");
        } catch (IllegalArgumentException e) {
            linea("orientacion mala: IllegalArgumentException");
        }
        try {
            d.setResizeWeight(2.0);
            linea("peso malo: sin queja");
        } catch (IllegalArgumentException e) {
            linea("peso malo: IllegalArgumentException");
        }
        d.remove(d.getTopComponent());
        linea("tras sacar arriba: arriba=" + nombre(d.getTopComponent()));
        d.removeAll();
        linea("tras sacar todo: abajo=" + nombre(d.getBottomComponent()));

        JSplitPane d2 = new JSplitPane();
        linea("por omision: orientacion=" + d2.getOrientation());

        // --- el panel de solapas ---
        linea("=== solapas");
        JTabbedPane p = new JTabbedPane();
        p.addChangeListener(new Espia(p));
        linea("lado=" + p.getTabPlacement() + " politica=" + p.getTabLayoutPolicy());
        estado(p);
        p.addTab("uno", et("c1"));
        estado(p);
        p.addTab("dos", et("c2"));
        p.addTab("tres", et("c3"));
        estado(p);
        p.setSelectedIndex(2);
        estado(p);
        p.insertTab("medio", null, et("cm"), "ayuda", 1);
        estado(p);
        linea("busca dos=" + p.indexOfTab("dos") + " busca zz=" + p.indexOfTab("zz")
                + " ayuda de 1=" + p.getToolTipTextAt(1));
        p.setTitleAt(0, "UNO");
        p.setEnabledAt(1, false);
        p.setMnemonicAt(0, 'N');
        linea("titulo0=" + p.getTitleAt(0) + " mnem0=" + p.getMnemonicAt(0)
                + " indice mnem0=" + p.getDisplayedMnemonicIndexAt(0));
        estado(p);
        p.setComponentAt(0, et("c1b"));
        estado(p);
        linea("indice de c1b=" + p.indexOfComponent(p.getComponentAt(0)));
        p.removeTabAt(0);
        estado(p);
        p.setSelectedIndex(p.getTabCount() - 1);
        p.removeTabAt(p.getTabCount() - 1);
        estado(p);
        try {
            p.setSelectedIndex(99);
            linea("indice malo: sin queja");
        } catch (IndexOutOfBoundsException e) {
            linea("indice malo: IndexOutOfBoundsException");
        }
        try {
            p.setTabPlacement(9);
            linea("lado malo: sin queja");
        } catch (IllegalArgumentException e) {
            linea("lado malo: IllegalArgumentException");
        }
        p.setTabPlacement(SwingConstants.LEFT);
        p.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        linea("lado=" + p.getTabPlacement() + " politica=" + p.getTabLayoutPolicy());
        p.removeAll();
        estado(p);
        return 0;
    }
}
