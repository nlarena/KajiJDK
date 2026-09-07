import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * El nodo de arbol y los caminos, contra el JDK.
 *
 * <p>Los cuatro recorridos son lo interesante: dan ordenes distintos sobre el mismo arbol y es
 * facil confundirlos. Se comparan los cuatro sobre el mismo arbol para que la diferencia se vea.
 */
public class Arbol1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static DefaultMutableTreeNode n(String s) {
        return new DefaultMutableTreeNode(s);
    }

    static String recorrido(Enumeration<TreeNode> e) {
        String s = "";
        while (e.hasMoreElements()) {
            s = s + e.nextElement() + " ";
        }
        return s.trim();
    }

    static String camino(TreeNode[] p) {
        String s = "";
        for (int i = 0; i < p.length; i++) {
            s = s + p[i] + "/";
        }
        return s;
    }

    public static int run() {
        //        raiz
        //      /  |   \
        //     a   b    c
        //    / \   \
        //   a1 a2   b1
        //       |
        //      a2x
        DefaultMutableTreeNode raiz = n("raiz");
        DefaultMutableTreeNode a = n("a");
        DefaultMutableTreeNode b = n("b");
        DefaultMutableTreeNode c = n("c");
        DefaultMutableTreeNode a1 = n("a1");
        DefaultMutableTreeNode a2 = n("a2");
        DefaultMutableTreeNode b1 = n("b1");
        DefaultMutableTreeNode a2x = n("a2x");
        raiz.add(a);
        raiz.add(b);
        raiz.add(c);
        a.add(a1);
        a.add(a2);
        b.add(b1);
        a2.add(a2x);

        linea("hijos de raiz=" + raiz.getChildCount() + " hojas=" + raiz.getLeafCount()
                + " hondura=" + raiz.getDepth() + " nivel de a2x=" + a2x.getLevel());
        linea("preorden   " + recorrido(raiz.preorderEnumeration()));
        linea("posorden   " + recorrido(raiz.postorderEnumeration()));
        linea("niveles    " + recorrido(raiz.breadthFirstEnumeration()));
        linea("hondo      " + recorrido(raiz.depthFirstEnumeration()));
        linea("hijos de a " + recorrido((Enumeration<TreeNode>) a.children()));
        linea("camino a2x=" + camino(a2x.getPath()));
        linea("desde a    " + recorrido(a2x.pathFromAncestorEnumeration(a)));

        linea("raiz de a2x=" + a2x.getRoot() + " es raiz=" + raiz.isRoot()
                + " a2x es raiz=" + a2x.isRoot());
        linea("a es antepasado de a2x=" + a2x.isNodeAncestor(a)
                + " c lo es=" + a2x.isNodeAncestor(c)
                + " compartido(a2x,b1)=" + a2x.getSharedAncestor(b1)
                + " compartido(a1,a2)=" + a1.getSharedAncestor(a2));
        linea("a1 y a2 hermanos=" + a1.isNodeSibling(a2) + " a1 y b1=" + a1.isNodeSibling(b1)
                + " hermanos de a1=" + a1.getSiblingCount());
        linea("siguiente de a1=" + a1.getNextSibling() + " anterior de a2=" + a2.getPreviousSibling()
                + " siguiente de a2=" + a2.getNextSibling());
        linea("primera hoja=" + raiz.getFirstLeaf() + " ultima=" + raiz.getLastLeaf()
                + " siguiente hoja de a1=" + a1.getNextLeaf()
                + " anterior de b1=" + b1.getPreviousLeaf());
        linea("siguiente nodo de a=" + a.getNextNode() + " anterior de b=" + b.getPreviousNode());
        linea("indice de b en raiz=" + raiz.getIndex(b) + " de b1 en raiz=" + raiz.getIndex(b1));

        // Sacar y mover.
        raiz.remove(b);
        linea("tras sacar b: hijos=" + raiz.getChildCount() + " padre de b=" + b.getParent());
        a.add(b);
        linea("tras mover b bajo a: " + recorrido(raiz.preorderEnumeration())
                + " padre de b=" + b.getParent());
        b.removeFromParent();
        linea("tras sacarlo: " + recorrido(raiz.preorderEnumeration()));

        // Hoja y permitir hijos no son lo mismo.
        DefaultMutableTreeNode vacio = new DefaultMutableTreeNode("vacio", true);
        DefaultMutableTreeNode hoja = new DefaultMutableTreeNode("hoja", false);
        linea("vacio: hoja=" + vacio.isLeaf() + " permite=" + vacio.getAllowsChildren());
        linea("hoja: hoja=" + hoja.isLeaf() + " permite=" + hoja.getAllowsChildren());
        try {
            hoja.add(n("x"));
            linea("agregar a una hoja: sin queja");
        } catch (IllegalStateException e) {
            linea("agregar a una hoja: IllegalStateException");
        }
        try {
            a1.add(raiz);
            linea("ciclo: sin queja");
        } catch (IllegalArgumentException e) {
            linea("ciclo: IllegalArgumentException");
        }

        // La copia es superficial.
        DefaultMutableTreeNode copia = (DefaultMutableTreeNode) a.clone();
        linea("copia=" + copia + " hijos=" + copia.getChildCount()
                + " padre=" + copia.getParent());

        // Los caminos.
        TreePath p1 = new TreePath(new Object[] {"r", "x", "y"});
        TreePath p2 = p1.getParentPath();
        TreePath p3 = p2.pathByAddingChild("z");
        linea("p1=" + p1 + " largo=" + p1.getPathCount() + " ultimo=" + p1.getLastPathComponent());
        linea("p2=" + p2 + " p3=" + p3);
        linea("p2 contiene a p1=" + p2.isDescendant(p1) + " p1 contiene a p2=" + p1.isDescendant(p2)
                + " p1 igual a p1b=" + p1.equals(new TreePath(new Object[] {"r", "x", "y"})));
        linea("p1 igual a p3=" + p1.equals(p3)
                + " hash igual=" + (p1.hashCode() == new TreePath(
                        new Object[] {"r", "x", "y"}).hashCode()));
        return 0;
    }
}
