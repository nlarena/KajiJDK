package com.sun.source.tree;

import java.util.List;

/**
 * A `switch` used as a statement.
 */
public interface SwitchTree extends StatementTree {

    ExpressionTree getExpression();

    List<? extends CaseTree> getCases();
}
