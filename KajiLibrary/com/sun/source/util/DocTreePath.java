package com.sun.source.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;

/**
 * Un nodo de documentacion y todo lo que lo contiene: hasta el comentario, y de ahi hasta el codigo.
 *
 * <h2>Los dos arboles encadenados</h2>
 *
 * <p>Es lo que distingue a esta clase de {@link TreePath}: un camino de documentacion termina en un
 * {@link TreePath}, no en una raiz propia. Tiene que ser asi porque la pregunta que se hace sobre un
 * nodo de documentacion casi siempre es sobre el codigo — "a que metodo documenta este
 * {@code @param}" — y esa respuesta esta del otro lado de la frontera entre los dos arboles.
 */
public class DocTreePath implements Iterable<DocTree> {

    private final TreePath treePath;
    private final DocCommentTree docComment;
    private final DocTree leaf;
    private final DocTreePath parent;

    /** El camino hasta {@code target} dentro de ese comentario, o {@code null} si no esta. */
    public static DocTreePath getPath(TreePath treePath, DocCommentTree comment, DocTree target) {
        return getPath(new DocTreePath(treePath, comment), target);
    }

    /** Igual, arrancando desde un camino ya armado. */
    public static DocTreePath getPath(DocTreePath path, DocTree target) {
        if (path == null || target == null) {
            throw new NullPointerException("path y target no pueden ser null");
        }
        return new Buscador(target).buscar(path);
    }

    /** El camino que es solo el comentario. */
    public DocTreePath(TreePath treePath, DocCommentTree t) {
        if (treePath == null || t == null) {
            throw new NullPointerException("treePath y comment no pueden ser null");
        }
        this.treePath = treePath;
        this.docComment = t;
        this.leaf = t;
        this.parent = null;
    }

    /** El camino de {@code p} extendido con {@code t}. */
    public DocTreePath(DocTreePath p, DocTree t) {
        if (t.getKind() == DocTree.Kind.DOC_COMMENT) {
            throw new IllegalArgumentException("un DocCommentTree es la raiz, no una hoja");
        }
        this.treePath = p.treePath;
        this.docComment = p.docComment;
        this.leaf = t;
        this.parent = p;
    }

    /** El camino en el arbol de codigo donde vive este comentario. */
    public TreePath getTreePath() {
        return this.treePath;
    }

    /** El comentario entero. */
    public DocCommentTree getDocComment() {
        return this.docComment;
    }

    /** El nodo del extremo. */
    public DocTree getLeaf() {
        return this.leaf;
    }

    /** El camino hasta el padre, o {@code null} si esto es el comentario. */
    public DocTreePath getParentPath() {
        return this.parent;
    }

    /** Del nodo hacia el comentario. */
    public Iterator<DocTree> iterator() {
        return new HaciaArriba(this);
    }

    private static final class HaciaArriba implements Iterator<DocTree> {

        private DocTreePath actual;

        HaciaArriba(DocTreePath desde) {
            this.actual = desde;
        }

        public boolean hasNext() {
            return this.actual != null;
        }

        public DocTree next() {
            if (this.actual == null) {
                throw new NoSuchElementException();
            }
            DocTree t = this.actual.leaf;
            this.actual = this.actual.parent;
            return t;
        }
    }

    /** Ver la nota del buscador de {@link TreePath}: corta con una excepcion al encontrarlo. */
    private static final class Buscador extends DocTreePathScanner<DocTreePath, Void> {

        private final DocTree objetivo;

        Buscador(DocTree objetivo) {
            this.objetivo = objetivo;
        }

        DocTreePath buscar(DocTreePath desde) {
            try {
                scan(desde, null);
                return null;
            } catch (Encontrado e) {
                return e.camino;
            }
        }

        public DocTreePath scan(DocTree node, Void p) {
            if (node == this.objetivo) {
                throw new Encontrado(new DocTreePath(getCurrentPath(), node));
            }
            return super.scan(node, p);
        }
    }

    private static final class Encontrado extends RuntimeException {

        private static final long serialVersionUID = 1L;

        final transient DocTreePath camino;

        Encontrado(DocTreePath camino) {
            super(null, null, false, false);
            this.camino = camino;
        }
    }
}
