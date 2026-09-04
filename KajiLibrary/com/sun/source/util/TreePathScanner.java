package com.sun.source.util;

import com.sun.source.tree.Tree;

/**
 * Un {@link TreeScanner} que ademas lleva la cuenta de por donde va.
 *
 * <h2>Que aporta</h2>
 *
 * <p>{@link #getCurrentPath}. Un {@code TreeScanner} pelado ve un nodo por vez y no sabe que lo
 * contiene; con esto, cualquier visita puede preguntar en que clase o en que metodo esta parada.
 *
 * <p>Y es barato: el camino se arma mientras se baja, en vez de recorrer el arbol otra vez con
 * {@link TreePath#getPath}. Cualquier consulta que necesite contexto en mas de un nodo conviene
 * hacerla asi.
 *
 * <p>La unica regla al extenderlo: si se sobrescribe {@code scan}, hay que llamar al de la clase
 * base — es el que empuja y saca del camino.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra
 */
public class TreePathScanner<R, P> extends TreeScanner<R, P> {

    private TreePath path;

    public TreePathScanner() {
    }

    /** Arranca el recorrido desde ese camino, que queda como contexto inicial. */
    public R scan(TreePath path, P p) {
        this.path = path.getParentPath();
        try {
            return path.getLeaf().accept(this, p);
        } finally {
            this.path = null;
        }
    }

    /**
     * Visita un nodo, empujandolo al camino mientras dura.
     *
     * <p>El {@code finally} no es decoracion: una visita puede tirar, y sin restaurar el camino el
     * scanner quedaria mintiendo sobre donde esta para todo lo que siga.
     */
    public R scan(Tree tree, P p) {
        if (tree == null) {
            return null;
        }
        TreePath anterior = this.path;
        this.path = new TreePath(anterior == null ? new TreePath(
                (com.sun.source.tree.CompilationUnitTree) tree) : anterior, tree);
        try {
            return tree.accept(this, p);
        } finally {
            this.path = anterior;
        }
    }

    /** Donde esta parado el recorrido ahora. */
    public TreePath getCurrentPath() {
        return this.path;
    }
}
