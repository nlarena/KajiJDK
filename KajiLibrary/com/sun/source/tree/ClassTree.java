package com.sun.source.tree;

import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Name;

/**
 * Una declaracion de tipo: clase, interfaz, enum, record o anotacion. Cual de las
 * cinco lo dice {@link Tree#getKind}.
 */
public interface ClassTree extends StatementTree {

    ModifiersTree getModifiers();

    Name getSimpleName();

    List<? extends TypeParameterTree> getTypeParameters();

    Tree getExtendsClause();

    List<? extends Tree> getImplementsClause();

    /** La clausula `permits` de un tipo sellado; vacia si no la tiene. */
    default List<? extends Tree> getPermitsClause() {
        return Collections.<Tree>emptyList();
    }

    List<? extends Tree> getMembers();
}
