package com.sun.source.tree;

import java.util.List;

/**
 * The classic `for` of three parts. The three of them may be missing.
 */
public interface ForLoopTree extends StatementTree {

    List<? extends StatementTree> getInitializer();

    ExpressionTree getCondition();

    List<? extends ExpressionStatementTree> getUpdate();

    StatementTree getStatement();
}
