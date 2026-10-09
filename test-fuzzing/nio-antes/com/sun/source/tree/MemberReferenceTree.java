package com.sun.source.tree;

import java.util.List;

import javax.lang.model.element.Name;

/**
 * A method reference: `Foo::bar`, `Foo::new`, `expr::method`.
 *
 * <p>{@link #getMode} tells apart the two that are written alike and mean different things:
 * `Foo::bar` invokes and `Foo::new` constructs. Without that enum one would have to look at
 * whether the name is `"new"`, which is exactly the kind of comparison by string that a typed
 * tree avoids.
 */
public interface MemberReferenceTree extends ExpressionTree {

    /** Whether the reference invokes a method or calls a constructor. */
    enum ReferenceMode {

        /** `Foo::bar` -- it invokes the method. */
        INVOKE,
        /** `Foo::new` -- it constructs. */
        NEW
    }

    /** Whether it invokes or constructs. */
    ReferenceMode getMode();

    /** What goes before the `::`: a type or an expression. */
    ExpressionTree getQualifierExpression();

    /** The name after the `::`, which is `new` in the {@link ReferenceMode#NEW} mode. */
    Name getName();

    /** The explicit type arguments, if they were written. */
    List<? extends ExpressionTree> getTypeArguments();
}
