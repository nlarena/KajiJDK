package com.sun.source.doctree;

/**
 * La mitad de la jerarquia que agrupa los tags **en linea**: los que van entre
 * llaves, en medio del texto, como `{@link}` o `{@code}`.
 *
 * <p>La separacion de {@link BlockTagTree} no es de estilo sino gramatical: un tag de bloque
 * termina donde empieza el siguiente o el comentario, y uno en linea termina en su llave de cierre.
 * Son dos reglas de parseo distintas, y por eso son dos tipos.
 *
 * <p>{@link ReturnTree} implementa las dos, porque `@return` existe en las dos formas.
 */
public interface InlineTagTree extends DocTree {

    String getTagName();
}
