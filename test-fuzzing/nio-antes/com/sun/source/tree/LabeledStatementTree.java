package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * A statement with a label.
 */
public interface LabeledStatementTree extends StatementTree {

    Name getLabel();

    StatementTree getStatement();
}
