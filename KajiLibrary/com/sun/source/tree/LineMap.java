package com.sun.source.tree;

/**
 * The translation between absolute positions of the file and line/column pairs.
 *
 * <p>It is not a node of the tree: the nodes keep **absolute positions**, which are a single
 * number and are not invalidated if the file is reindented. Translating them into line and column
 * is expensive -- where all the line breaks are has to be known -- and it is only needed when
 * showing something to a person. Hence it is a separate object that is asked for once per
 * compilation unit.
 */
public interface LineMap {

    long getStartPosition(long a0);

    long getPosition(long a0, long a1);

    long getLineNumber(long a0);

    long getColumnNumber(long a0);
}
