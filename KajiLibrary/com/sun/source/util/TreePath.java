package com.sun.source.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

/**
 * Un nodo y la cadena de los que lo contienen, hasta la raiz.
 *
 * <h2>Por que hace falta</h2>
 *
 * <p>Porque un {@link Tree} <strong>no conoce a su padre</strong>. Eso es deliberado: sin punteros
 * hacia arriba, un subarbol se puede compartir entre varios lugares y el arbol entero es mas barato
 * de construir. El precio es que "en que clase esta este metodo" no se puede contestar desde el nodo.
 *
 * <p>Esta clase es ese contexto, construido para un recorrido concreto. De ahi que sea inmutable y
 * que {@link #getParentPath} devuelva otro camino en vez de un nodo: lo que se recorre hacia arriba
 * es el camino, no el arbol.
 *
 * <p>La iteracion va <strong>del nodo hacia la raiz</strong>, que es el sentido util: casi todas las
 * preguntas son "cual es el X mas cercano que me contiene".
 */
public class TreePath implements Iterable<Tree> {

    private final CompilationUnitTree unit;
    private final Tree leaf;
    private final TreePath parent;

    /**
     * El camino desde {@code unit} hasta {@code target}, o {@code null} si no esta adentro.
     *
     * <p>Recorre el arbol buscandolo, asi que no es gratis: para muchas consultas conviene un
     * {@link TreePathScanner}, que lo mantiene mientras baja.
     */
    public static TreePath getPath(CompilationUnitTree unit, Tree target) {
        return getPath(new TreePath(unit), target);
    }

    /** Igual, arrancando desde un camino ya armado. */
    public static TreePath getPath(TreePath path, Tree target) {
        if (path == null || target == null) {
            throw new NullPointerException("path y target no pueden ser null");
        }
        Buscador b = new Buscador(target);
        return b.buscar(path);
    }

    /** El camino que es solo la raiz. */
    public TreePath(CompilationUnitTree unit) {
        this.unit = unit;
        this.leaf = unit;
        this.parent = null;
    }

    /** El camino de {@code path} extendido con {@code leaf}. */
    public TreePath(TreePath path, Tree leaf) {
        this.unit = path.unit;
        this.leaf = leaf;
        this.parent = path;
    }

    /** El archivo donde vive. */
    public CompilationUnitTree getCompilationUnit() {
        return this.unit;
    }

    /** El nodo del extremo. */
    public Tree getLeaf() {
        return this.leaf;
    }

    /** El camino hasta el padre, o {@code null} si esto ya es la raiz. */
    public TreePath getParentPath() {
        return this.parent;
    }

    /** Del nodo hacia la raiz. */
    public Iterator<Tree> iterator() {
        return new HaciaArriba(this);
    }

    /** El iterador que sube: ver la nota de la clase sobre el sentido. */
    private static final class HaciaArriba implements Iterator<Tree> {

        private TreePath actual;

        HaciaArriba(TreePath desde) {
            this.actual = desde;
        }

        public boolean hasNext() {
            return this.actual != null;
        }

        public Tree next() {
            if (this.actual == null) {
                throw new NoSuchElementException();
            }
            Tree t = this.actual.leaf;
            this.actual = this.actual.parent;
            return t;
        }
    }

    /**
     * El recorrido que arma el camino hasta un nodo.
     *
     * <p>Corta con una excepcion interna al encontrarlo, y eso no es abuso: el scanner no tiene
     * forma de detenerse a mitad de camino, y seguir recorriendo un archivo entero despues de haber
     * encontrado lo que se buscaba seria trabajo puro de mas.
     */
    private static final class Buscador extends TreePathScanner<TreePath, Void> {

        private final Tree objetivo;

        Buscador(Tree objetivo) {
            this.objetivo = objetivo;
        }

        TreePath buscar(TreePath desde) {
            try {
                scan(desde, null);
                return null;
            } catch (Encontrado e) {
                return e.camino;
            }
        }

        public TreePath scan(Tree node, Void p) {
            if (node == this.objetivo) {
                throw new Encontrado(new TreePath(getCurrentPath(), node));
            }
            return super.scan(node, p);
        }
    }

    /** El corte del recorrido; sin traza, porque no es un error sino un resultado. */
    private static final class Encontrado extends RuntimeException {

        private static final long serialVersionUID = 1L;

        final transient TreePath camino;

        Encontrado(TreePath camino) {
            super(null, null, false, false);
            this.camino = camino;
        }
    }
}
