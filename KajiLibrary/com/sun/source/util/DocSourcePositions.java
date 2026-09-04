package com.sun.source.util;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.tree.CompilationUnitTree;

/**
 * Lo mismo que {@link SourcePositions}, para los nodos de un comentario de documentacion.
 *
 * <p>Hacen falta <strong>tres</strong> argumentos y no dos, y la razon es la misma que alla llevada
 * un nivel mas: un nodo de documentacion vive dentro de un comentario, y el comentario dentro de un
 * archivo. Sin el comentario del medio no se puede ubicar nada, porque el mismo arbol de
 * documentacion puede estar heredado y aparecer en varios lugares.
 */
public interface DocSourcePositions extends SourcePositions {

    /** Donde empieza el nodo de documentacion, o {@code NOPOS}. */
    long getStartPosition(CompilationUnitTree file, DocCommentTree comment, DocTree tree);

    /** Donde termina, o {@code NOPOS}. */
    long getEndPosition(CompilationUnitTree file, DocCommentTree comment, DocTree tree);
}
