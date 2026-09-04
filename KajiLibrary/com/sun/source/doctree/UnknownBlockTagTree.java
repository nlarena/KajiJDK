package com.sun.source.doctree;

import java.util.List;

/**
 * Un tag de bloque que este arbol no conoce.
 *
 * <p>Existe porque javadoc es extensible: una herramienta puede definir sus propios tags, y el
 * parser tiene que poder representarlos sin entenderlos. Sin este nodo, un tag desconocido seria
 * un error de sintaxis en vez de una extension.
 */
public interface UnknownBlockTagTree extends BlockTagTree {

    List<? extends DocTree> getContent();
}
