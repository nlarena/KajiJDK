package com.sun.source.doctree;

/**
 * Una referencia a un elemento de Java, como la que va adentro de un `{@link}`.
 *
 * <p>Devuelve la **firma cruda**, sin resolver: convertir `Foo#bar(int)` en el metodo que nombra
 * necesita el contexto de compilacion, que este arbol no tiene. Resolverla es trabajo de
 * `DocTrees`, no de este nodo.
 */
public interface ReferenceTree extends DocTree {

    /** La firma tal cual se escribio, sin resolver. */
    String getSignature();
}
