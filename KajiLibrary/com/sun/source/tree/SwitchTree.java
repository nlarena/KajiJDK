package com.sun.source.tree;

import java.util.List;

/**
 * Un `switch` usado como sentencia.
 */
public interface SwitchTree extends StatementTree {

    ExpressionTree getExpression();

    List<? extends CaseTree> getCases();
}
