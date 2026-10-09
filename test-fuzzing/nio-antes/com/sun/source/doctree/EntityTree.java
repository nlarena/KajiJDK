package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * An HTML entity such as `&amp;` or `&#233;`, unresolved.
 */
public interface EntityTree extends DocTree {

    Name getName();
}
