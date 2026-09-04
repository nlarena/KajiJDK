package com.sun.source.tree;

import javax.lang.model.element.Name;

/**
 * `break`, con etiqueta o sin ella.
 */
public interface BreakTree extends StatementTree {

    Name getLabel();
}
