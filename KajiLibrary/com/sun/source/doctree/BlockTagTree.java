package com.sun.source.doctree;

/**
 * La mitad de la jerarquia que agrupa los tags de **bloque**: los que van solos en
 * una linea, empezando con `@`, despues del cuerpo del comentario. Ver {@link InlineTagTree} para
 * la otra mitad y por que estan separadas.
 */
public interface BlockTagTree extends DocTree {

    String getTagName();
}
