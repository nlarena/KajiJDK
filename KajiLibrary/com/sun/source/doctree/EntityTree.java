package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * Una entidad HTML como `&amp;` o `&#233;`, sin resolver.
 */
public interface EntityTree extends DocTree {

    Name getName();
}
