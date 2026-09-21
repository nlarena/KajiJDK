package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * The declaration of a variable, a field or a parameter. {@link #getInitializer}
 * is `null` if it has none.
 */
public interface VariableTree extends StatementTree {

    ModifiersTree getModifiers();

    Name getName();

    ExpressionTree getNameExpression();

    Tree getType();

    /** The initial value, or `null` if it has none. */
    ExpressionTree getInitializer();
}
