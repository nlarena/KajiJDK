import java.awt.Dimension;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;

/**
 * Los ultimos miembros del paquete basico, contra el JDK.
 *
 * <p>El reloj de repeticion de la barra, la rueda del panel, tres medidas del deslizador y tres
 * respuestas del arbol.
 *
 * <p>Ningun alto en pixeles se compara crudo: dependen de la fuente, y las metricas de esta
 * biblioteca no son todavia las del JDK. Lo que se compara son las relaciones -- que dos etiquetas
 * iguales midan lo mismo, que un punto caiga o no en la manija -- que si tienen que dar igual.
 */
public class Plaf15 {

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

    static class Barra extends BasicScrollBarUI {
        Object escucha() {
            return scrollListener;
        }

        Object reloj() {
            return scrollTimer;
        }

        Object nuevoEscucha() {
            return createScrollListener();
        }

        int espera() {
            return scrollTimer.getInitialDelay();
        }

        int periodo() {
            return scrollTimer.getDelay();
        }

        boolean repite() {
            return scrollTimer.isRepeats();
        }

        boolean anda() {
            return scrollTimer.isRunning();
        }
    }

    static class Panel extends BasicScrollPaneUI {
        Object nuevaRueda() {
            return createMouseWheelListener();
        }
    }

    static class Desliza extends BasicSliderUI {
        Desliza(JSlider s) {
            super(s);
        }

        Dimension pulgar() {
            return getThumbSize();
        }

        int altoDelBajo() {
            return getHeightOfLowValueLabel();
        }

        int altoDelAlto() {
            return getHeightOfHighValueLabel();
        }

        int altoDelMasAlto() {
            return getHeightOfTallestLabel();
        }

        boolean mismaBase() {
            return labelsHaveSameBaselines();
        }
    }

    static class Arbol extends BasicTreeUI {
        boolean enLaManija(TreePath p, int x, int y) {
            return isLocationInExpandControl(p, x, y);
        }

        void clickEnLaManija(TreePath p, int x, int y) {
            checkForClickInExpandControl(p, x, y);
        }

        void asegurar(int desde, int hasta) {
            ensureRowsAreVisible(desde, hasta);
        }

        int equis(int fila, int prof) {
            return getRowX(fila, prof);
        }

        int derecha() {
            return getRightChildIndent();
        }
    }

    /** El reloj que repite mientras el boton sigue apretado. */
    static void barra() {
        linea("--- BasicScrollBarUI: el reloj ---");
        JScrollBar b = new JScrollBar();
        Barra u = new Barra();
        b.setUI(u);
        linea("escucha=" + corto(u.escucha()) + " reloj=" + corto(u.reloj()));
        linea("espera inicial=" + u.espera() + " periodo=" + u.periodo()
                + " repite=" + u.repite() + " anda=" + u.anda());
        linea("cada llamada da uno nuevo="
                + (u.nuevoEscucha() != u.nuevoEscucha()));
        linea("y es de la misma clase que el instalado="
                + (u.nuevoEscucha().getClass() == u.escucha().getClass()));
    }

    /** La rueda del panel. */
    static void panel() {
        linea("--- BasicScrollPaneUI: la rueda ---");
        JScrollPane p = new JScrollPane();
        Panel u = new Panel();
        p.setUI(u);
        linea("rueda=" + corto(u.nuevaRueda()));
        linea("es siempre la misma=" + (u.nuevaRueda() == u.nuevaRueda()));
        linea("el panel la acepta=" + p.isWheelScrollingEnabled());
    }

    /** Las tres medidas del deslizador. */
    static void deslizador() {
        linea("--- BasicSliderUI: medidas ---");
        JSlider s = new JSlider();
        Desliza u = new Desliza(s);
        s.setUI(u);
        linea("pulgar acostado=" + u.pulgar());
        JSlider v = new JSlider(JSlider.VERTICAL);
        Desliza uv = new Desliza(v);
        v.setUI(uv);
        linea("pulgar parado=" + uv.pulgar());

        linea("sin etiquetas: alto del bajo=" + u.altoDelBajo()
                + " alto del alto=" + u.altoDelAlto()
                + " misma linea de base=" + u.mismaBase());
        s.setLabelTable(new Hashtable<Integer, JLabel>());
        linea("con la tabla vacia: misma linea de base=" + u.mismaBase());

        // Dos etiquetas de una linea comparten linea de base; los altos no se imprimen crudos.
        Hashtable<Integer, JLabel> tabla = new Hashtable<Integer, JLabel>();
        tabla.put(Integer.valueOf(0), new JLabel("cero"));
        tabla.put(Integer.valueOf(100), new JLabel("cien"));
        s.setLabelTable(tabla);
        linea("con dos etiquetas de una linea: el bajo y el alto miden igual="
                + (u.altoDelBajo() == u.altoDelAlto())
                + " y son el mas alto=" + (u.altoDelBajo() == u.altoDelMasAlto())
                + " misma linea de base=" + u.mismaBase());
        linea("el bajo es positivo=" + (u.altoDelBajo() > 0));

        // Una con dos renglones apoya el primero a la misma altura; el alto no se compara,
        // que ahi manda como se arma el HTML y cuanto mide la fuente.
        Hashtable<Integer, JLabel> otra = new Hashtable<Integer, JLabel>();
        otra.put(Integer.valueOf(0), new JLabel("cero"));
        otra.put(Integer.valueOf(100), new JLabel("<html>uno<br>dos</html>"));
        s.setLabelTable(otra);
        linea("con una de dos renglones: misma linea de base=" + u.mismaBase());

        // Y sacar la tabla vuelve a la respuesta de cuando no habia ninguna.
        s.setLabelTable(null);
        linea("y al sacar la tabla: misma linea de base=" + u.mismaBase());
    }

    /** La manija del arbol y el asegurar filas. */
    static void arbol() {
        linea("--- BasicTreeUI: manija y filas visibles ---");
        JTree t = new JTree();
        Arbol u = new Arbol();
        t.setUI(u);

        TreePath raiz = u.getPathForRow(t, 0);
        TreePath rama = u.getPathForRow(t, 1);
        TreePath hoja = u.getPathForRow(t, 3);
        linea("hoja de la fila 3=" + hoja.getLastPathComponent());

        // El borde izquierdo de la caja no depende de ningun icono.
        int borde = u.equis(1, rama.getPathCount() - 1) - u.derecha() + 1;
        linea("borde izquierdo de la manija de la fila 1=" + borde);
        linea("justo en el borde=" + u.enLaManija(rama, borde, 0)
                + " un pixel antes=" + u.enLaManija(rama, borde - 1, 0)
                + " muy a la derecha=" + u.enLaManija(rama, borde + 1000, 0));
        linea("sobre una hoja nunca=" + u.enLaManija(hoja, borde, 0));
        linea("sobre la raiz, que tiene hijos=" + u.enLaManija(raiz,
                u.equis(0, 0) - u.derecha() + 1, 0));

        // Un click en la manija de una rama cerrada la abre.
        linea("antes del click, la fila 1 esta abierta=" + t.isExpanded(rama));
        u.clickEnLaManija(rama, borde, 0);
        linea("despues=" + t.isExpanded(rama) + " filas=" + u.getRowCount(t));
        u.clickEnLaManija(rama, borde, 0);
        linea("y otro la cierra=" + t.isExpanded(rama) + " filas=" + u.getRowCount(t));
        u.clickEnLaManija(hoja, borde, 0);
        linea("un click sobre una hoja no hace nada; filas=" + u.getRowCount(t));

        // Sin panel que lo contenga, asegurar no mueve nada, pero no puede reventar.
        u.asegurar(0, 0);
        u.asegurar(0, 3);
        u.asegurar(-1, 3);
        u.asegurar(0, 999);
        linea("asegurar filas no rompe; filas=" + u.getRowCount(t));
    }

    public static int run() {
        barra();
        panel();
        deslizador();
        arbol();
        return 0;
    }
}
