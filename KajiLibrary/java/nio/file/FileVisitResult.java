package java.nio.file;

// Lo que un `FileVisitor` le contesta al recorrido para decidir como sigue.
//
// Son cuatro y no un booleano porque el corte tiene tres alcances distintos: parar todo
// (`TERMINATE`), no bajar a este directorio (`SKIP_SUBTREE`), o no mirar mas hermanos de este nivel
// (`SKIP_SIBLINGS`).
public enum FileVisitResult {

    /** Seguir normalmente. */
    CONTINUE,

    /** Terminar el recorrido entero. */
    TERMINATE,

    /** No entrar a este directorio; seguir con los hermanos. */
    SKIP_SUBTREE,

    /** No mirar los hermanos que quedan; subir un nivel. */
    SKIP_SIBLINGS
}
