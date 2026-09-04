package com.sun.source.tree;

import java.util.List;

/**
 * Codigo que no se pudo parsear.
 *
 * <p>Extiende {@link ExpressionTree} y conserva en {@link #getErrorTrees} lo que si se entendio.
 * Es lo que permite que un IDE siga dando autocompletado sobre un archivo a medio escribir: el
 * arbol representa el error en vez de no existir.
 */
public interface ErroneousTree extends ExpressionTree {

    List<? extends Tree> getErrorTrees();
}
