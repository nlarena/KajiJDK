package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * El nodo de `{@systemProperty}`, que marca el nombre de una propiedad del
 * sistema para que sea indexable.
 */
public interface SystemPropertyTree extends InlineTagTree {

    Name getPropertyName();
}
