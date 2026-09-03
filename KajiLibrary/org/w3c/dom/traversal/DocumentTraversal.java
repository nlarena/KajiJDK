package org.w3c.dom.traversal;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * KajiLibrary's org.w3c.dom.traversal.DocumentTraversal -- la fabrica de recorridos.
 *
 * <p>La implementa el {@code Document}, y es la unica puerta de entrada al paquete: no hay
 * constructores publicos de {@link NodeIterator} ni de {@link TreeWalker}. Tiene que ser asi porque
 * un recorrido queda <b>atado a su documento</b> -- el iterador se ajusta cuando el documento cambia,
 * y para eso el documento tiene que saber que existe.
 *
 * <p>Un {@code Document} que no soporte recorridos no implementa esta interfaz; se pregunta con
 * {@code hasFeature("Traversal", "2.0")}.
 */
public interface DocumentTraversal {

    /**
     * Un recorrido plano, en orden de documento.
     *
     * @param root                   desde donde. No puede ser null
     * @param whatToShow             un OR de las {@code SHOW_*} de {@link NodeFilter}
     * @param filter                 el filtro, o null
     * @param entityReferenceExpansion si se entra a las referencias a entidad
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si la raiz es null
     */
    NodeIterator createNodeIterator(Node root, int whatToShow, NodeFilter filter,
        boolean entityReferenceExpansion) throws DOMException;

    /**
     * Un recorrido con forma de arbol. Ver {@link TreeWalker} para en que se diferencia del plano.
     *
     * @throws DOMException {@code NOT_SUPPORTED_ERR} si la raiz es null
     */
    TreeWalker createTreeWalker(Node root, int whatToShow, NodeFilter filter,
        boolean entityReferenceExpansion) throws DOMException;
}
