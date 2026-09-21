package com.sun.source.tree;

import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Name;

/**
 * A type declaration: class, interface, enum, record or annotation. Which of the
 * five it is is said by {@link Tree#getKind}.
 */
public interface ClassTree extends StatementTree {

    ModifiersTree getModifiers();

    Name getSimpleName();

    List<? extends TypeParameterTree> getTypeParameters();

    Tree getExtendsClause();

    List<? extends Tree> getImplementsClause();

    /** A sealed type's `permits` clause; empty if it has none. */
    default List<? extends Tree> getPermitsClause() {
        return Collections.<Tree>emptyList();
    }

    List<? extends Tree> getMembers();
}
