package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * `continue`, con etiqueta o sin ella.
 */
public interface ContinueTree extends StatementTree {

    Name getLabel();
}
