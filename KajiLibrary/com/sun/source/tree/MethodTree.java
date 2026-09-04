package com.sun.source.tree;

import java.util.List;
import javax.lang.model.element.Name;

/**
 * La declaracion de un metodo o de un constructor. {@link #getBody} es `null` en un
 * abstracto o nativo, y {@link #getDefaultValue} solo aparece en un miembro de anotacion.
 */
public interface MethodTree extends Tree {

    ModifiersTree getModifiers();

    Name getName();

    Tree getReturnType();

    List<? extends TypeParameterTree> getTypeParameters();

    List<? extends VariableTree> getParameters();

    VariableTree getReceiverParameter();

    List<? extends ExpressionTree> getThrows();

    /** El cuerpo, o `null` si es abstracto o nativo. */
    BlockTree getBody();

    /** El `default` de un miembro de anotacion, o `null`. */
    Tree getDefaultValue();
}
