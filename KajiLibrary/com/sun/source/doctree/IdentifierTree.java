package com.sun.source.doctree;

import javax.lang.model.element.Name;

/**
 * Un identificador de Java dentro de un tag: el nombre de un parametro en
 * `@param`, el de un campo en `@serialField`.
 */
public interface IdentifierTree extends DocTree {

    Name getName();
}
