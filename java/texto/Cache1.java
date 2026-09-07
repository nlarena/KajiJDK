import java.awt.Rectangle;
import java.util.Enumeration;

import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreePath;
import javax.swing.tree.VariableHeightLayoutCache;

/**
 * La traduccion entre filas y caminos de un arbol, contra el JDK.
 *
 * <p>Es la mitad de {@code javax.swing.tree} que no necesita pantalla: cuales nodos se ven, en que
 * orden, y que fila le toca a cada uno. Lo que si necesita pantalla -- cuanto mide cada fila -- se
 * simula con un medidor de prueba que devuelve medidas fijas, para que la comparacion no dependa de
 * ninguna tipografia.
 */
public class Cache1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Un medidor de prueba: ancho segun la profundidad, alto segun el nombre. */
    static class Medidor extends AbstractLayoutCache.NodeDimensions {

        public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded,
                Rectangle bounds) {
            Rectangle r = (bounds == null) ? new Rectangle() : bounds;
            r.x = depth * 10;
            r.width = 50 + String.valueOf(value).length();
            r.y = 0;
            r.height = 10 + (String.valueOf(value).length() % 3) * 5;
            return r;
        }
    }

    /**
     * El arbol de prueba.
     *
     * <pre>
     * raiz
     *   a
     *     a1
     *     a2
     *   b
     *   c
     *     c1
     * </pre>
     */
    static DefaultTreeModel arbol() {
        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("raiz");
        DefaultMutableTreeNode a = new DefaultMutableTreeNode("a");
        a.add(new DefaultMutableTreeNode("a1"));
        a.add(new DefaultMutableTreeNode("a2"));
        DefaultMutableTreeNode b = new DefaultMutableTreeNode("b");
        DefaultMutableTreeNode c = new DefaultMutableTreeNode("c");
        c.add(new DefaultMutableTreeNode("c1"));
        raiz.add(a);
        raiz.add(b);
        raiz.add(c);
        return new DefaultTreeModel(raiz);
    }

    /** El camino a un nodo por sus nombres, desde la raiz. */
    static TreePath camino(DefaultTreeModel m, String[] nombres) {
        Object nodo = m.getRoot();
        TreePath p = new TreePath(nodo);
        for (int i = 0; i < nombres.length; i++) {
            int n = m.getChildCount(nodo);
            for (int j = 0; j < n; j++) {
                Object hijo = m.getChild(nodo, j);
                if (hijo.toString().equals(nombres[i])) {
                    nodo = hijo;
                    p = p.pathByAddingChild(hijo);
                    j = n;
                }
            }
        }
        return p;
    }

    static String filas(AbstractLayoutCache c) {
        StringBuilder b = new StringBuilder();
        b.append(c.getRowCount()).append(":");
        for (int i = 0; i < c.getRowCount(); i++) {
            TreePath p = c.getPathForRow(i);
            b.append(" ").append(p == null ? "-" : p.getLastPathComponent());
        }
        return b.toString();
    }

    static String rect(Rectangle r) {
        if (r == null) {
            return "-";
        }
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    static void comun(AbstractLayoutCache c, DefaultTreeModel m, String que) {
        linea("--- " + que + " ---");
        linea("raiz visible=" + c.isRootVisible() + " altura=" + c.getRowHeight()
                + " fija=" + (c.getRowHeight() > 0));
        linea("modelo puesto=" + (c.getModel() == m));
        linea("filas " + filas(c));

        TreePath pa = camino(m, new String[] {"a"});
        TreePath pa1 = camino(m, new String[] {"a", "a1"});
        TreePath pc = camino(m, new String[] {"c"});
        TreePath raiz = new TreePath(m.getRoot());

        linea("raiz desplegada=" + c.getExpandedState(raiz) + " marcada=" + c.isExpanded(raiz));
        linea("a desplegada=" + c.getExpandedState(pa) + " marcada=" + c.isExpanded(pa));
        linea("fila de raiz=" + c.getRowForPath(raiz) + " de a=" + c.getRowForPath(pa)
                + " de a1=" + c.getRowForPath(pa1) + " de c=" + c.getRowForPath(pc));
        linea("hijos visibles de raiz=" + c.getVisibleChildCount(raiz)
                + " de a=" + c.getVisibleChildCount(pa));

        c.setExpandedState(pa, true);
        linea("desplegada a, filas " + filas(c));
        linea("fila de a1=" + c.getRowForPath(pa1)
                + " hijos visibles de raiz=" + c.getVisibleChildCount(raiz)
                + " de a=" + c.getVisibleChildCount(pa));
        linea("a marcada=" + c.isExpanded(pa) + " a1 marcada=" + c.isExpanded(pa1));

        c.setExpandedState(pc, true);
        linea("desplegada c, filas " + filas(c));

        c.setExpandedState(pa, false);
        linea("plegada a, filas " + filas(c));
        linea("a1 fila=" + c.getRowForPath(pa1) + " a1 desplegada=" + c.getExpandedState(pa1));

        // Desplegar un nieto despliega a sus padres.
        c.setExpandedState(pa1, true);
        linea("desplegada a1, filas " + filas(c));

        c.setRootVisible(false);
        linea("sin raiz, filas " + filas(c));
        linea("fila de raiz=" + c.getRowForPath(raiz) + " de a=" + c.getRowForPath(pa));
        c.setRootVisible(true);
        linea("con raiz de nuevo, filas " + filas(c));

        linea("fila -1=" + c.getPathForRow(-1) + " fila 99=" + c.getPathForRow(99));
        linea("fila de un camino ajeno="
                + c.getRowForPath(new TreePath(new DefaultMutableTreeNode("x"))));
        linea("fila de nulo=" + c.getRowForPath(null));

        StringBuilder desde = new StringBuilder();
        Enumeration<TreePath> e = c.getVisiblePathsFrom(pa);
        if (e == null) {
            desde.append("-");
        } else {
            while (e.hasMoreElements()) {
                desde.append(" ").append(e.nextElement().getLastPathComponent());
            }
        }
        linea("desde a:" + desde);
        linea("desde uno ajeno="
                + c.getVisiblePathsFrom(new TreePath(new DefaultMutableTreeNode("x"))));

        int[] rs = c.getRowsForPaths(new TreePath[] {raiz, pa, pa1});
        linea("filas de tres caminos=" + java.util.Arrays.toString(rs));
        linea("filas de nulo=" + c.getRowsForPaths(null));

        // Sin medidor no hay medidas.
        linea("medidas sin medidor=" + rect(c.getBounds(pa, null)));
        c.setNodeDimensions(new Medidor());
        linea("medidor puesto=" + (c.getNodeDimensions() != null));
        linea("medidas de raiz=" + rect(c.getBounds(raiz, null)));
        linea("medidas de a=" + rect(c.getBounds(pa, null)));
        linea("medidas de a1=" + rect(c.getBounds(pa1, null)));
        linea("medidas de un ajeno="
                + rect(c.getBounds(new TreePath(new DefaultMutableTreeNode("x")), null)));
        linea("alto preferido=" + c.getPreferredHeight());
        linea("ancho preferido=" + c.getPreferredWidth(null));

        linea("cercano a 0,0=" + n(c.getPathClosestTo(0, 0)));
        linea("cercano a 0,15=" + n(c.getPathClosestTo(0, 15)));
        linea("cercano a 0,-5=" + n(c.getPathClosestTo(0, -5)));
        linea("cercano a 0,9999=" + n(c.getPathClosestTo(0, 9999)));
    }

    static String n(TreePath p) {
        return (p == null) ? "-" : String.valueOf(p.getLastPathComponent());
    }

    public static int run() {
        DefaultTreeModel m1 = arbol();
        FixedHeightLayoutCache f = new FixedHeightLayoutCache();
        f.setRowHeight(16);
        f.setModel(m1);
        comun(f, m1, "altura fija");
        try {
            f.setRowHeight(0);
            linea("altura 0 aceptada");
        } catch (IllegalArgumentException e) {
            linea("altura 0 rechazada: " + e.getMessage());
        }

        DefaultTreeModel m2 = arbol();
        VariableHeightLayoutCache v = new VariableHeightLayoutCache();
        v.setModel(m2);
        comun(v, m2, "altura variable");
        linea("altura variable acepta 0=" + aceptaCero(v));
        return 0;
    }

    static boolean aceptaCero(VariableHeightLayoutCache v) {
        try {
            v.setRowHeight(0);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
