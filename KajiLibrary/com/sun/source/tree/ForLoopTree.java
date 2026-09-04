package com.sun.source.tree;

import java.util.List;

/**
 * El `for` clasico de tres partes. Las tres pueden faltar.
 */
public interface ForLoopTree extends StatementTree {

    List<? extends StatementTree> getInitializer();

    ExpressionTree getCondition();

    List<? extends ExpressionStatementTree> getUpdate();

    StatementTree getStatement();
}
