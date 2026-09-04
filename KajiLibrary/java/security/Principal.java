package java.security;

// Una entidad: una persona, una maquina, un rol. Lo que un sistema de seguridad puede nombrar.
//
// Es la abstraccion mas usada de `java.security` fuera del propio paquete —la citan `java.net`,
// `java.nio.file.attribute`, `javax.net.ssl` y `java.security.cert`— y es tan chica como se ve:
// un nombre, mas la igualdad para poder compararlas.
//
public interface Principal {

    // Si este principal es igual al objeto dado.
    boolean equals(Object another);

    String toString();

    int hashCode();

    // El nombre de este principal.
    String getName();

    // Si este principal es uno de los del `Subject` dado.
    //
    // El default es la respuesta chica: busca **este mismo** principal entre los del Subject. Quien
    // quiera la respuesta grande —un rol que abarca a otros, un grupo que contiene miembros— tiene
    // que sobrescribirlo, y ese es justamente el motivo por el que el metodo existe en vez de que
    // cada quien haga `subject.getPrincipals().contains(p)`.
    //
    // Un Subject null da false y no explota: "nadie" nunca implica a nadie.
    default boolean implies(javax.security.auth.Subject subject) {
        if (subject == null) {
            return false;
        }
        return subject.getPrincipals().contains(this);
    }
}
