package javax.swing.tree;

/**
 * El camino desde la raiz de un arbol hasta un nodo.
 *
 * <h2>Por que un camino y no el nodo</h2>
 *
 * <p>Porque un mismo objeto puede estar colgado en dos lugares del arbol. Si la seleccion fuera "el
 * nodo", no habria forma de decir <em>cual</em> de sus apariciones. El camino desambigua, y por eso
 * todo el modelo de arbol de Swing habla en caminos y no en nodos.
 *
 * <h2>Inmutable, y compartiendo la cola</h2>
 *
 * <p>Un {@code TreePath} no cambia nunca. {@link #pathByAddingChild} devuelve uno nuevo que
 * <strong>guarda al anterior como padre</strong> en vez de copiar el arreglo: un arbol de mil nodos
 * de profundidad diez no aloca diez mil elementos, sino que comparte los prefijos. Es la misma idea
 * que una lista enlazada persistente.
 *
 * <p>De ahi que {@link #getPath} tenga que reconstruir el arreglo recorriendo la cadena: el arreglo
 * completo no existe hasta que alguien lo pide.
 */
public class TreePath implements java.io.Serializable {

    private static final long serialVersionUID = 4380089275673032332L;

    /** El ultimo componente: el nodo al que este camino apunta. */
    private Object lastPathComponent;

    /** El camino hasta el padre, o {@code null} si este es la raiz. */
    private TreePath parentPath;

    /**
     * Un camino con esos componentes, de la raiz al nodo.
     *
     * @throws IllegalArgumentException si el arreglo es {@code null} o vacio
     */
    public TreePath(Object[] path) {
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("El camino no puede ser null ni vacio");
        }
        this.lastPathComponent = path[path.length - 1];
        if (path.length > 1) {
            this.parentPath = new TreePath(path, path.length - 1);
        }
    }

    /**
     * Un camino de un solo componente, o sea la raiz.
     *
     * @throws IllegalArgumentException si es {@code null}
     */
    public TreePath(Object singlePath) {
        if (singlePath == null) {
            throw new IllegalArgumentException("El componente no puede ser null");
        }
        this.lastPathComponent = singlePath;
        this.parentPath = null;
    }

    /** El camino de {@code parent} mas un hijo. Es lo que comparte el prefijo. */
    protected TreePath(TreePath parent, Object lastElement) {
        if (lastElement == null) {
            throw new IllegalArgumentException("El componente no puede ser null");
        }
        this.parentPath = parent;
        this.lastPathComponent = lastElement;
    }

    /** Los primeros {@code length} componentes de {@code path}. */
    protected TreePath(Object[] path, int length) {
        this.lastPathComponent = path[length - 1];
        if (length > 1) {
            this.parentPath = new TreePath(path, length - 1);
        }
    }

    /** Sin componentes. Para las subclases que los guardan de otra forma. */
    protected TreePath() {
    }

    /** El camino como arreglo, de la raiz al nodo. */
    public Object[] getPath() {
        int n = getPathCount();
        Object[] resultado = new Object[n];
        TreePath actual = this;
        for (int i = n - 1; i >= 0; i--) {
            resultado[i] = actual.getLastPathComponent();
            actual = actual.getParentPath();
        }
        return resultado;
    }

    /** El nodo al que apunta. */
    public Object getLastPathComponent() {
        return this.lastPathComponent;
    }

    /** Cuantos componentes tiene, contando la raiz. */
    public int getPathCount() {
        int n = 0;
        TreePath actual = this;
        while (actual != null) {
            n = n + 1;
            actual = actual.getParentPath();
        }
        return n;
    }

    /**
     * El componente numero {@code element}, contando desde la raiz.
     *
     * @throws IllegalArgumentException si el indice esta fuera de rango
     */
    public Object getPathComponent(int element) {
        int n = getPathCount();
        if (element < 0 || element >= n) {
            throw new IllegalArgumentException("Indice fuera de rango: " + String.valueOf(element));
        }
        TreePath actual = this;
        for (int i = n - 1; i != element; i--) {
            actual = actual.getParentPath();
        }
        return actual.getLastPathComponent();
    }

    /**
     * Iguales si tienen los mismos componentes en el mismo orden.
     *
     * <p>Compara con {@code equals} y no por identidad, asi que dos caminos armados por separado
     * sobre los mismos nodos son iguales — que es lo que hace falta para que una seleccion
     * reconstruida siga siendo la misma.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TreePath) {
            TreePath otro = (TreePath) o;
            if (getPathCount() != otro.getPathCount()) {
                return false;
            }
            TreePath a = this;
            TreePath b = otro;
            while (a != null) {
                if (!a.getLastPathComponent().equals(b.getLastPathComponent())) {
                    return false;
                }
                a = a.getParentPath();
                b = b.getParentPath();
            }
            return true;
        }
        return false;
    }

    /**
     * El hash del ultimo componente.
     *
     * <p>Alcanza, y es lo que hace el JDK: dos caminos iguales terminan en el mismo nodo, asi que
     * la propiedad que un hash necesita se cumple. Recorrer la cadena entera solo haria mas caro
     * el calculo sin separar mejor.
     */
    public int hashCode() {
        return this.lastPathComponent.hashCode();
    }

    /** Si {@code aTreePath} cuelga de este camino, o es este mismo. */
    public boolean isDescendant(TreePath aTreePath) {
        if (aTreePath == this) {
            return true;
        }
        if (aTreePath == null) {
            return false;
        }
        int miLargo = getPathCount();
        int suLargo = aTreePath.getPathCount();
        if (suLargo < miLargo) {
            return false;
        }
        // Se sube por el candidato hasta ponerlo a la misma altura, y recien ahi se comparan.
        TreePath candidato = aTreePath;
        while (suLargo > miLargo) {
            candidato = candidato.getParentPath();
            suLargo = suLargo - 1;
        }
        return equals(candidato);
    }

    /** Este camino mas un hijo. Comparte el prefijo; ver la nota de la clase. */
    public TreePath pathByAddingChild(Object child) {
        if (child == null) {
            throw new NullPointerException("El hijo no puede ser null");
        }
        return new TreePath(this, child);
    }

    /** El camino hasta el padre, o {@code null} si este es la raiz. */
    public TreePath getParentPath() {
        return this.parentPath;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        int n = getPathCount();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getPathComponent(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
