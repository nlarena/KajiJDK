package jdk.dynalink;

/**
 * Los cinco verbos que dynalink sabe enlazar.
 *
 * <p>Solos no dicen nada: un `GET` sin {@link Namespace} no especifica que se lee. La forma
 * completa se arma componiendo — `StandardOperation.GET.withNamespace(StandardNamespace.PROPERTY)`
 * — y por eso el enum no tiene ni un metodo propio.
 *
 * @since 9
 */
public enum StandardOperation implements Operation {

    /** Leer un valor del espacio de nombres indicado. */
    GET,

    /** Escribir un valor en el espacio de nombres indicado. */
    SET,

    /** Quitar un miembro del espacio de nombres indicado. */
    REMOVE,

    /** Invocar el objeto receptor. */
    CALL,

    /** Construir una instancia con el objeto receptor como constructor. */
    NEW
}
