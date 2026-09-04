package com.sun.source.tree;

import java.util.List;

/**
 * La directiva `opens` de un modulo, que habilita reflexion en vez de compilacion.
 */
public interface OpensTree extends DirectiveTree {

    ExpressionTree getPackageName();

    List<? extends ExpressionTree> getModuleNames();
}
