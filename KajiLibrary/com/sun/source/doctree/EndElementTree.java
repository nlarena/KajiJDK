package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * Una etiqueta HTML de cierre. Aparece como nodo hermano del de apertura y no como
 * un cierre estructural, porque el javadoc de un comentario puede tener HTML mal balanceado y el
 * arbol tiene que poder representarlo igual.
 */
public interface EndElementTree extends DocTree {

    Name getName();
}
