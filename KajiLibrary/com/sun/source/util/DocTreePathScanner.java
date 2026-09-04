package com.sun.source.util;

import com.sun.source.doctree.DocTree;

/**
 * Un {@link DocTreeScanner} que lleva la cuenta de por donde va.
 *
 * <p>El equivalente de {@link TreePathScanner} para el arbol de documentacion, y con el mismo
 * proposito: que cualquier visita pueda preguntar que la contiene sin recorrer el arbol otra vez.
 *
 * @param <R> lo que devuelve cada visita
 * @param <P> el dato que se arrastra
 */
public class DocTreePathScanner<R, P> extends DocTreeScanner<R, P> {

    private DocTreePath path;

    public DocTreePathScanner() {
    }

    /** Arranca el recorrido desde ese camino. */
    public R scan(DocTreePath path, P p) {
        this.path = path.getParentPath();
        try {
            return path.getLeaf().accept(this, p);
        } finally {
            this.path = null;
        }
    }

    /** Visita un nodo, empujandolo al camino mientras dura. */
    public R scan(DocTree tree, P p) {
        if (tree == null) {
            return null;
        }
        DocTreePath anterior = this.path;
        if (anterior != null) {
            this.path = new DocTreePath(anterior, tree);
        }
        try {
            return tree.accept(this, p);
        } finally {
            this.path = anterior;
        }
    }

    /** Donde esta parado el recorrido ahora. */
    public DocTreePath getCurrentPath() {
        return this.path;
    }
}
