package com.sun.source.tree;

import java.util.List;

/**
 * The declaration of a `module-info.java`.
 *
 * <p>It is a node of this tree and not of somewhere else because a `module-info.java` is a
 * compilation unit like any other: it is parsed the same, and {@link CompilationUnitTree#getModule}
 * is how one asks whether this unit was one of these.
 */
public interface ModuleTree extends Tree {

    /** Whether the module is wholly open to reflection. */
    enum ModuleKind {

        /** `open module M { ... }` -- all of its packages are left open. */
        OPEN,
        /** `module M { ... }` -- only what an `opens` says. */
        STRONG
    }

    /** The declaration's annotations. */
    List<? extends AnnotationTree> getAnnotations();

    /** Whether it is an open module or not. */
    ModuleKind getModuleType();

    /** The module's name. */
    ExpressionTree getName();

    /** The body's directives: `requires`, `exports`, `opens`, `provides`, `uses`. */
    List<? extends DirectiveTree> getDirectives();
}
