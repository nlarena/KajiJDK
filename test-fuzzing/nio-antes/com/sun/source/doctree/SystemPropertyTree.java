package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * The node of `{@systemProperty}`, which marks a system property's name
 * so that it should be indexable.
 */
public interface SystemPropertyTree extends InlineTagTree {

    Name getPropertyName();
}
