package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * Una sentencia con etiqueta.
 */
public interface LabeledStatementTree extends StatementTree {

    Name getLabel();

    StatementTree getStatement();
}
