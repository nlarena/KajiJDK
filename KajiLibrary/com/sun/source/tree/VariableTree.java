package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * La declaracion de una variable, un campo o un parametro. {@link #getInitializer}
 * es `null` si no tiene.
 */
public interface VariableTree extends StatementTree {

    ModifiersTree getModifiers();

    Name getName();

    ExpressionTree getNameExpression();

    Tree getType();

    /** El valor inicial, o `null` si no tiene. */
    ExpressionTree getInitializer();
}
