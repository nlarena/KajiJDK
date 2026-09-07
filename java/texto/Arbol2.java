import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * El modelo de arbol y su seleccion, contra el JDK.
 *
 * <p>Como en las listas, se comparan los avisos y no solo el estado: un modelo que cambia sin
 * avisar deja la vista mostrando lo viejo, y eso no se ve mirando el resultado.
 *
 * <p>Las filas no entran en la comparacion: sin aspecto instalado no hay quien traduzca caminos a
 * filas, y las dos bibliotecas contestan lo mismo por la misma razon -- que no hay nada que
 * contestar --, asi que no dirian nada.
 */
public class Arbol2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota cada aviso del modelo. */
    static class EspiaModelo implements TreeModelListener {

        private String camino(TreeModelEvent e) {
            TreePath p = e.getTreePath();
            String s = (p == null) ? "-" : p.toString();
            Object[] h = e.getChildren();
            int[] i = e.getChildIndices();
            s = s + " hijos=" + ((h == null) ? "-" : "" + h.length);
            s = s + " indices=";
            if (i == null) {
                s = s + "-";
            } else {
                for (int k = 0; k < i.length; k++) {
                    s = s + i[k] + ",";
                }
            }
            return s;
        }

        public void treeNodesChanged(TreeModelEvent e) {
            linea("  cambiaron " + camino(e));
        }

        public void treeNodesInserted(TreeModelEvent e) {
            linea("  insertaron " + camino(e));
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            linea("  sacaron " + camino(e));
        }

        public void treeStructureChanged(TreeModelEvent e) {
            linea("  estructura " + camino(e));
        }
    }

    /** Anota cada cambio de seleccion. */
    static class EspiaSeleccion implements TreeSelectionListener {

        public void valueChanged(TreeSelectionEvent e) {
            String s = "  seleccion";
            TreePath[] p = e.getPaths();
            for (int i = 0; i < p.length; i++) {
                s = s + " " + (e.isAddedPath(i) ? "+" : "-") + p[i].getLastPathComponent();
            }
            s = s + " guia=" + (e.getNewLeadSelectionPath() == null ? "-"
                    : e.getNewLeadSelectionPath().getLastPathComponent());
            linea(s);
        }
    }

    static DefaultMutableTreeNode n(String s) {
        return new DefaultMutableTreeNode(s);
    }

    static String elegidos(TreeSelectionModel m) {
        TreePath[] p = m.getSelectionPaths();
        String s = "";
        for (int i = 0; i < p.length; i++) {
            s = s + p[i].getLastPathComponent() + " ";
        }
        return "[" + s.trim() + "] cantidad=" + m.getSelectionCount()
                + " vacia=" + m.isSelectionEmpty()
                + " guia=" + (m.getLeadSelectionPath() == null ? "-"
                        : m.getLeadSelectionPath().getLastPathComponent());
    }

    public static int run() {
        DefaultMutableTreeNode raiz = n("raiz");
        DefaultMutableTreeNode a = n("a");
        DefaultMutableTreeNode b = n("b");
        DefaultMutableTreeNode a1 = n("a1");
        DefaultMutableTreeNode a2 = n("a2");
        raiz.add(a);
        raiz.add(b);
        a.add(a1);
        a.add(a2);

        linea("=== modelo");
        DefaultTreeModel m = new DefaultTreeModel(raiz);
        m.addTreeModelListener(new EspiaModelo());
        linea("raiz=" + m.getRoot() + " hijos=" + m.getChildCount(raiz)
                + " a es hoja=" + m.isLeaf(a) + " a1 es hoja=" + m.isLeaf(a1)
                + " indice de b=" + m.getIndexOfChild(raiz, b));

        // Vacio, pero permite hijos: las dos formas de decidir dan distinto.
        DefaultMutableTreeNode vacio = new DefaultMutableTreeNode("vacio", true);
        m.insertNodeInto(vacio, raiz, 2);
        linea("vacio es hoja=" + m.isLeaf(vacio) + " pregunta permitir="
                + m.asksAllowsChildren());
        m.setAsksAllowsChildren(true);
        linea("con permitir: vacio es hoja=" + m.isLeaf(vacio));
        m.setAsksAllowsChildren(false);

        DefaultMutableTreeNode c = n("c");
        m.insertNodeInto(c, a, 1);
        linea("hijos de a=" + m.getChildCount(a));
        m.nodeChanged(a1);
        m.removeNodeFromParent(c);
        m.reload(a);

        String camino = "";
        TreeNode[] p = m.getPathToRoot(a2);
        for (int i = 0; i < p.length; i++) {
            camino = camino + p[i] + "/";
        }
        linea("camino a a2=" + camino);
        m.valueForPathChanged(new TreePath(m.getPathToRoot(a2)), "A2");
        linea("a2 ahora=" + a2 + " hijos de a=" + m.getChildCount(a));

        linea("=== seleccion");
        DefaultTreeSelectionModel s = new DefaultTreeSelectionModel();
        s.addTreeSelectionListener(new EspiaSeleccion());
        TreePath pa = new TreePath(new Object[] {raiz, a});
        TreePath pb = new TreePath(new Object[] {raiz, b});
        TreePath pa1 = new TreePath(new Object[] {raiz, a, a1});
        TreePath pa2 = new TreePath(new Object[] {raiz, a, a2});

        linea("modo=" + s.getSelectionMode());
        linea("inicio " + elegidos(s));
        s.setSelectionPath(pa);
        linea("solo a " + elegidos(s));
        s.addSelectionPath(pb);
        linea("mas b " + elegidos(s));
        s.addSelectionPaths(new TreePath[] {pa1, pa2});
        linea("mas a1 a2 " + elegidos(s));
        linea("a elegido=" + s.isPathSelected(pa) + " raiz elegida="
                + s.isPathSelected(new TreePath(raiz)));
        s.removeSelectionPath(pb);
        linea("menos b " + elegidos(s));
        s.addSelectionPath(pa);
        linea("agregar repetido " + elegidos(s));
        s.setSelectionPaths(new TreePath[] {pa1, pa1, pa2});
        linea("con repetidos " + elegidos(s));
        s.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        linea("modo unico=" + s.getSelectionMode());
        s.setSelectionPaths(new TreePath[] {pa, pb});
        linea("dos en modo unico " + elegidos(s));
        s.clearSelection();
        linea("limpia " + elegidos(s));
        s.setSelectionPath(null);
        linea("nulo " + elegidos(s));

        linea("filas=" + s.getSelectionRows().length + " min=" + s.getMinSelectionRow()
                + " max=" + s.getMaxSelectionRow() + " guia fila=" + s.getLeadSelectionRow()
                + " traductor=" + s.getRowMapper());
        return 0;
    }
}
