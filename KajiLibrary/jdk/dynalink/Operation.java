package jdk.dynalink;

/**
 * Lo que un sitio de invocacion dinamico quiere hacer.
 *
 * <p>Una operacion es un **dato**, no una accion: describe la intencion (leer, escribir, llamar,
 * construir) sin decidir sobre quien ni como. La decoracion es por composicion y en un orden
 * fijo — primero el espacio de nombres, despues el nombre — de modo que
 * `GET.withNamespace(PROPERTY).named("x")` da un `NamedOperation` que envuelve un
 * `NamespaceOperation` que envuelve `GET`. El orden inverso esta prohibido por los
 * constructores de {@link NamespaceOperation}, y por eso desarmar una operacion es siempre
 * el mismo par de pasos: {@link NamedOperation#getBaseOperation} y despues
 * {@link NamespaceOperation#getBaseOperation}.
 *
 * <p>La interfaz no tiene metodos abstractos: una implementacion solo tiene que existir y saber
 * compararse. Los cinco verbos del lenguaje estan en {@link StandardOperation}.
 *
 * @since 9
 */
public interface Operation {

    /** Esta operacion, restringida a un unico espacio de nombres. */
    default NamespaceOperation withNamespace(final Namespace namespace) {
        return withNamespaces(namespace);
    }

    /**
     * Esta operacion sobre varios espacios de nombres, **en orden de preferencia**: el enlazador
     * prueba el primero que pueda satisfacer y solo baja al siguiente si no encontro nada.
     */
    default NamespaceOperation withNamespaces(final Namespace... namespaces) {
        return new NamespaceOperation(this, namespaces);
    }

    /** Esta operacion con un nombre fijo, conocido en tiempo de enlace. */
    default NamedOperation named(final Object name) {
        return new NamedOperation(this, name);
    }
}
