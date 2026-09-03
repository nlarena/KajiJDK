package java.nio.file;

// Como configurar un recorrido del arbol de archivos. Tiene una sola constante desde que existe.
//
// KajiJDK no recorre arboles --no hay nativo que liste un directorio-- asi que ningun metodo la
// recibe. El enum esta porque es parte de la API y porque codigo que lo nombra tiene que compilar.
public enum FileVisitOption {

    /** Seguir los enlaces simbolicos al bajar. */
    FOLLOW_LINKS
}
