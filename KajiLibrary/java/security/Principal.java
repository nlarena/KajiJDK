package java.security;

// Una entidad: una persona, una maquina, un rol. Lo que un sistema de seguridad puede nombrar.
//
// Es la abstraccion mas usada de `java.security` fuera del propio paquete —la citan `java.net`,
// `java.nio.file.attribute`, `javax.net.ssl` y `java.security.cert`— y es tan chica como se ve:
// un nombre, mas la igualdad para poder compararlas.
//
// A KajiLibrary subset: falta el `default boolean implies(javax.security.auth.Subject)`, porque
// `javax.security.auth.Subject` no existe en esta biblioteca. Es 1 de los 5 miembros.
public interface Principal {

    // Si este principal es igual al objeto dado.
    boolean equals(Object another);

    String toString();

    int hashCode();

    // El nombre de este principal.
    String getName();
}
