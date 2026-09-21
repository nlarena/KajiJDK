package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * A closing HTML tag. It appears as a sibling node of the opening one and not as
 * a structural close, because a comment's javadoc may have badly balanced HTML and the tree has
 * to be able to represent it all the same.
 */
public interface EndElementTree extends DocTree {

    Name getName();
}
