package javax.swing.tree;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Lo que {@link FixedHeightLayoutCache} y {@link VariableHeightLayoutCache} tienen en comun: que
 * nodos se ven y en que orden.
 *
 * <p>No es publica. Las dos caches del JDK son dos arboles internos distintos, optimizados cada uno
 * para su caso; aca la parte que decide <em>cuales</em> son las filas es una sola, y lo unico que
 * las distingue es como se calcula la <em>altura</em> de cada una. Separar las dos preguntas es lo
 * que permite que la traduccion entre filas y caminos se pruebe sin una pantalla.
 *
 * <p>La lista de visibles se arma entera cuando hace falta y se descarta cuando algo cambia. Es mas
 * simple que mantenerla al dia y da exactamente lo mismo desde afuera; un arbol con cien mil nodos
 * desplegados lo notaria, uno de pantalla no.
 */
final class NucleoDeCache {

    private final Set<TreePath> desplegados = new HashSet<TreePath>();
    private List<TreePath> visibles;
    private TreeModel modelo;
    private boolean raizVisible;

    void setModelo(TreeModel m) {
        this.modelo = m;
        this.desplegados.clear();
        invalidar();
    }

    TreeModel getModelo() {
        return modelo;
    }

    void setRaizVisible(boolean b) {
        this.raizVisible = b;
        invalidar();
    }

    /** Tira la lista; se rearma en la proxima consulta. */
    void invalidar() {
        visibles = null;
    }

    /**
     * Marca ese camino como desplegado o plegado.
     *
     * <p>Desplegar tambien despliega a todos sus padres: un nodo desplegado adentro de uno plegado
     * no se veria, y quien pide desplegarlo quiere verlo.
     */
    void setDesplegado(TreePath path, boolean expandir) {
        if (path == null) {
            return;
        }
        if (expandir) {
            TreePath p = path;
            while (p != null) {
                desplegados.add(p);
                p = p.getParentPath();
            }
        } else {
            desplegados.remove(path);
        }
        invalidar();
    }

    /** Si ese camino esta marcado como desplegado. */
    boolean estaMarcado(TreePath path) {
        return path != null && desplegados.contains(path);
    }

    /** Si ese camino y todos sus padres estan desplegados. */
    boolean desplegadoDeVerdad(TreePath path) {
        TreePath p = path;
        while (p != null) {
            if (!desplegados.contains(p)) {
                return false;
            }
            p = p.getParentPath();
        }
        return true;
    }

    /** Las filas, en orden. */
    List<TreePath> lista() {
        if (visibles == null) {
            visibles = new ArrayList<TreePath>();
            if (modelo != null) {
                Object raiz = modelo.getRoot();
                if (raiz != null) {
                    TreePath camino = new TreePath(raiz);
                    if (raizVisible) {
                        visibles.add(camino);
                        agregarHijos(camino);
                    } else {
                        // La raiz escondida no ocupa fila, pero sus hijos si -- y se los muestra
                        // aunque nadie la haya desplegado, porque si no el arbol se veria vacio.
                        agregarHijosDeRaizEscondida(camino);
                    }
                }
            }
        }
        return visibles;
    }

    private void agregarHijos(TreePath padre) {
        if (!desplegados.contains(padre)) {
            return;
        }
        Object nodo = padre.getLastPathComponent();
        int n = modelo.getChildCount(nodo);
        for (int i = 0; i < n; i++) {
            TreePath hijo = padre.pathByAddingChild(modelo.getChild(nodo, i));
            visibles.add(hijo);
            agregarHijos(hijo);
        }
    }

    private void agregarHijosDeRaizEscondida(TreePath raiz) {
        Object nodo = raiz.getLastPathComponent();
        int n = modelo.getChildCount(nodo);
        for (int i = 0; i < n; i++) {
            TreePath hijo = raiz.pathByAddingChild(modelo.getChild(nodo, i));
            visibles.add(hijo);
            agregarHijos(hijo);
        }
    }

    int cuantas() {
        return lista().size();
    }

    TreePath caminoDeFila(int row) {
        List<TreePath> v = lista();
        if (row < 0 || row >= v.size()) {
            return null;
        }
        return v.get(row);
    }

    int filaDeCamino(TreePath path) {
        if (path == null) {
            return -1;
        }
        List<TreePath> v = lista();
        for (int i = 0; i < v.size(); i++) {
            if (v.get(i).equals(path)) {
                return i;
            }
        }
        return -1;
    }

    /** Cuantas filas ocupan los descendientes visibles de ese nodo. */
    int hijosVisibles(TreePath path) {
        int fila = filaDeCamino(path);
        if (fila < 0) {
            if (!desplegadoDeVerdad(path)) {
                return 0;
            }
        }
        List<TreePath> v = lista();
        int cuenta = 0;
        for (int i = fila + 1; i < v.size(); i++) {
            if (esDescendiente(v.get(i), path)) {
                cuenta = cuenta + 1;
            } else {
                i = v.size();
            }
        }
        return cuenta;
    }

    private static boolean esDescendiente(TreePath posible, TreePath de) {
        TreePath p = posible.getParentPath();
        while (p != null) {
            if (p.equals(de)) {
                return true;
            }
            p = p.getParentPath();
        }
        return false;
    }

    /** Las filas desde esa hacia abajo. */
    Enumeration<TreePath> desde(TreePath path) {
        int fila = filaDeCamino(path);
        if (fila < 0) {
            return null;
        }
        return new DesdeLaFila(lista(), fila);
    }

    /** Recorre la lista desde una fila hasta el final. */
    private static class DesdeLaFila implements Enumeration<TreePath> {

        private final List<TreePath> v;
        private int i;

        DesdeLaFila(List<TreePath> v, int i) {
            this.v = v;
            this.i = i;
        }

        public boolean hasMoreElements() {
            return i < v.size();
        }

        public TreePath nextElement() {
            if (i >= v.size()) {
                throw new NoSuchElementException();
            }
            TreePath p = v.get(i);
            i = i + 1;
            return p;
        }
    }
}
