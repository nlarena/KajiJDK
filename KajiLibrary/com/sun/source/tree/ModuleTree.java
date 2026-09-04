package com.sun.source.tree;

import java.util.List;

/**
 * La declaracion de un `module-info.java`.
 *
 * <p>Es un nodo de este arbol y no de otro lado porque un `module-info.java` es una unidad de
 * compilacion como cualquier otra: se parsea igual, y {@link CompilationUnitTree#getModule} es como
 * se pregunta si esta unidad era una de estas.
 */
public interface ModuleTree extends Tree {

    /** Si el modulo esta abierto entero a la reflexion. */
    enum ModuleKind {

        /** `open module M { ... }` — todos sus paquetes quedan abiertos. */
        OPEN,
        /** `module M { ... }` — solo lo que diga un `opens`. */
        STRONG
    }

    /** Las anotaciones de la declaracion. */
    List<? extends AnnotationTree> getAnnotations();

    /** Si es un modulo abierto o no. */
    ModuleKind getModuleType();

    /** El nombre del modulo. */
    ExpressionTree getName();

    /** Las directivas del cuerpo: `requires`, `exports`, `opens`, `provides`, `uses`. */
    List<? extends DirectiveTree> getDirectives();
}
