import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * El panel de solapas, contra el JDK.
 *
 * <p>Los anchos de solapa salen del texto y esta VM dibuja toda fuente con la misma cara, asi que no
 * se comparan crudos. Los altos si: salen del alto de linea, que es el mismo.
 */
public class Plaf12 {

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

    static class Solapas extends BasicTabbedPaneUI {
        String estado() {
            return "panel=" + (tabPane != null) + " corridas=" + runCount
                    + " corrida elegida=" + selectedRun
                    + " tabla=" + (tabRuns == null ? -1 : tabRuns.length)
                    + " maximo alto=" + maxTabHeight + " maximo ancho=" + maxTabWidth
                    + " solape=" + tabRunOverlay + " gap=" + textIconGap
                    + " rectangulo de trabajo=" + calcRect;
        }

        String insets() {
            return "solapa=" + tabInsets + " elegida=" + selectedTabPadInsets
                    + " area=" + tabAreaInsets + " contenido=" + contentBorderInsets;
        }

        String colores() {
            return "sombra=" + col(shadow) + " oscura=" + col(darkShadow)
                    + " brillo=" + col(highlight) + " claro=" + col(lightHighlight)
                    + " foco=" + col(focus);
        }

        String escuchas() {
            return "mouse=" + (mouseListener != null) + " foco=" + (focusListener != null)
                    + " propiedad=" + (propertyChangeListener != null)
                    + " cambio=" + (tabChangeListener != null)
                    + " teclas=" + upKey + "/" + downKey + "/" + leftKey + "/" + rightKey;
        }

        int alto(int i) {
            return calculateTabHeight(tabPane.getTabPlacement(), i, getFontMetrics().getHeight());
        }

        int maxAlto() {
            return calculateMaxTabHeight(tabPane.getTabPlacement());
        }

        int areaAlto(int corridas) {
            return calculateTabAreaHeight(tabPane.getTabPlacement(), corridas, maxAlto());
        }

        int base(int i) {
            return getBaseline(i);
        }

        int solape() {
            return getTabRunOverlay(tabPane.getTabPlacement());
        }

        boolean rotar() {
            return shouldRotateTabRuns(tabPane.getTabPlacement());
        }

        Icon icono(int i) {
            return getIconForTab(i);
        }

        Insets pad() {
            return getSelectedTabPadInsets(tabPane.getTabPlacement());
        }

        Insets solapaInsets(int i) {
            return getTabInsets(tabPane.getTabPlacement(), i);
        }

        String vecinos() {
            return "siguiente de 0=" + getNextTabIndex(0) + " anterior de 0=" + getPreviousTabIndex(0)
                    + " siguiente de 1=" + getNextTabIndex(1)
                    + " en la corrida: siguiente de 0=" + getNextTabIndexInRun(2, 0)
                    + " anterior de 0=" + getPreviousTabIndexInRun(2, 0);
        }

        String girados() {
            Insets destino = new Insets(0, 0, 0, 0);
            rotateInsets(new Insets(1, 2, 3, 4), destino, SwingConstants.LEFT);
            String izq = destino.toString();
            rotateInsets(new Insets(1, 2, 3, 4), destino, SwingConstants.BOTTOM);
            String abajo = destino.toString();
            rotateInsets(new Insets(1, 2, 3, 4), destino, SwingConstants.RIGHT);
            return "izquierda=" + izq + " abajo=" + abajo + " derecha=" + destino;
        }
    }

    static void solapas() {
        linea("--- BasicTabbedPaneUI ---");
        JTabbedPane tp = new JTabbedPane();
        tp.addTab("uno", new JPanel());
        tp.addTab("dos", new JPanel());
        Solapas u = new Solapas();
        linea("comparte instancia="
                + (BasicTabbedPaneUI.createUI(tp) == BasicTabbedPaneUI.createUI(tp)));
        tp.setUI(u);
        linea("recien instalado: " + u.estado());
        // Con tamano de verdad: sin el, las dos solapas caen en corridas distintas y las cuentas
        // de vecinos quedan en un caso degenerado que no dice nada.
        tp.setSize(300, 200);
        linea("insets: " + u.insets());
        linea("colores: " + u.colores());
        linea("escuchas: " + u.escuchas());
        linea("fondo=" + col(tp.getBackground()) + " frente=" + col(tp.getForeground())
                + " fuente=" + fue(tp.getFont()) + " opaco=" + tp.isOpaque()
                + " borde=" + tp.getBorder() + " acomodador=" + corto(tp.getLayout()));

        linea("corridas tras preguntar=" + u.getTabRunCount(tp)
                + " solape=" + u.solape() + " rota=" + u.rotar());
        linea("alto de la solapa 0=" + u.alto(0) + " maximo=" + u.maxAlto());
        linea("el alto es el de linea mas los insets mas dos="
                + (u.alto(0) == 16 + u.solapaInsets(0).top + u.solapaInsets(0).bottom + 2));
        linea("alto del area con 2 corridas=" + u.areaAlto(2)
                + " con 0 corridas=" + u.areaAlto(0));
        linea("linea de base de la solapa 0=" + u.base(0));
        linea("al cambiar de tamano=" + u.getBaselineResizeBehavior(tp));
        linea("minimo=" + u.getMinimumSize(tp) + " maximo=" + u.getMaximumSize(tp));
        linea("icono de la 0=" + u.icono(0) + " relleno de la elegida=" + u.pad());
        linea("vecinos: " + u.vecinos());
        linea("insets girados: " + u.girados());

        JTabbedPane vacio = new JTabbedPane();
        Solapas uv = new Solapas();
        vacio.setUI(uv);
        linea("vacio: corridas=" + uv.getTabRunCount(vacio)
                + " maximo alto=" + uv.maxAlto()
                + " alto del area=" + uv.areaAlto(uv.getTabRunCount(vacio)));
    }

    public static int run() {
        solapas();
        return 0;
    }
}
