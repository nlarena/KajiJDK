package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * The declaration of a method or of a constructor. {@link #getBody} is `null` in an
 * abstract or native one, and {@link #getDefaultValue} only appears in an annotation member.
 */
public interface MethodTree extends Tree {

    ModifiersTree getModifiers();

    Name getName();

    Tree getReturnType();

    List<? extends TypeParameterTree> getTypeParameters();

    List<? extends VariableTree> getParameters();

    VariableTree getReceiverParameter();

    List<? extends ExpressionTree> getThrows();

    /** The body, or `null` if it is abstract or native. */
    BlockTree getBody();

    /** The `default` of an annotation member, or `null`. */
    Tree getDefaultValue();
}
