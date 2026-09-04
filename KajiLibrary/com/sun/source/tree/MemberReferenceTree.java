package com.sun.source.tree;

import java.util.List;

import javax.lang.model.element.Name;

/**
 * Una referencia a metodo: `Foo::bar`, `Foo::new`, `expr::metodo`.
 *
 * <p>{@link #getMode} distingue las dos que se escriben parecido y significan cosas distintas:
 * `Foo::bar` invoca y `Foo::new` construye. Sin ese enum habria que mirar si el nombre es
 * `"new"`, que es exactamente la clase de comparacion por cadena que un arbol tipado evita.
 */
public interface MemberReferenceTree extends ExpressionTree {

    /** Si la referencia invoca un metodo o llama a un constructor. */
    enum ReferenceMode {

        /** `Foo::bar` — invoca el metodo. */
        INVOKE,
        /** `Foo::new` — construye. */
        NEW
    }

    /** Si invoca o construye. */
    ReferenceMode getMode();

    /** Lo que va antes del `::`: un tipo o una expresion. */
    ExpressionTree getQualifierExpression();

    /** El nombre despues del `::`, que es `new` en el modo {@link ReferenceMode#NEW}. */
    Name getName();

    /** Los argumentos de tipo explicitos, si se escribieron. */
    List<? extends ExpressionTree> getTypeArguments();
}
