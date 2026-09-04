package jdk.dynalink;

/**
 * Los tres espacios de nombres que todo objeto del lenguaje tiene.
 *
 * <p>La separacion entre {@link #PROPERTY} y {@link #ELEMENT} es la que Java no hace y los
 * lenguajes dinamicos si: `a.x` y `a[x]` son operaciones distintas aunque el nombre coincida.
 * {@link #METHOD} existe aparte de `PROPERTY` porque en un JavaBean el metodo `getFoo` y la
 * propiedad `foo` conviven, y pedir "el miembro foo" tiene dos respuestas segun donde se mire.
 *
 * @since 9
 */
public enum StandardNamespace implements Namespace {

    /** Propiedad con nombre: `obj.foo`. */
    PROPERTY,

    /** Elemento indexado por clave o posicion: `obj[foo]`. */
    ELEMENT,

    /** Metodo: lo que se obtiene al pedir `obj.foo` esperando algo invocable. */
    METHOD;

    /**
     * El primer espacio de nombres estandar de `op`, o `null` si no tiene ninguno.
     *
     * <p>Desarma las dos capas de decoracion en el unico orden en que pueden estar (nombre
     * afuera, espacios adentro), asi que sirve tanto para `GET:PROPERTY` como para
     * `GET:PROPERTY:x`.
     */
    public static StandardNamespace findFirst(final Operation op) {
        for (final Namespace ns : NamespaceOperation.getNamespaces(NamedOperation.getBaseOperation(op))) {
            if (ns instanceof StandardNamespace) {
                return (StandardNamespace) ns;
            }
        }
        return null;
    }
}
