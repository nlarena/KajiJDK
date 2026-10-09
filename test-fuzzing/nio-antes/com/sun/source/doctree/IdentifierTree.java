package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * A Java identifier inside a tag: a parameter's name in
 * `@param`, a field's in `@serialField`.
 */
public interface IdentifierTree extends DocTree {

    Name getName();
}
