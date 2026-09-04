package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 * Una identidad nombrada por su nombre distinguido de LDAP.
 *
 * <h2>Por que compara por {@link LdapName} y no por texto</h2>
 *
 * <p>Porque el mismo DN se escribe de muchas maneras. {@code CN=Juan, DC=com} y
 * {@code cn=Juan,dc=com} son <strong>la misma identidad</strong> —los tipos no distinguen
 * mayusculas, los espacios alrededor de la coma no cuentan— y compararlas como cadenas diria que no.
 *
 * <p>Un principal que responde "no sos vos" a la misma persona escrita distinto rompe cualquier
 * politica de acceso, y por eso esta clase parsea en el constructor. El nombre que devuelve
 * {@link #getName} es el original, sin normalizar: la normalizacion es para comparar, no para
 * mostrar.
 */
public final class LdapPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 6820120005580754861L;

    private final String nameString;
    private final transient LdapName name;

    /**
     * @throws InvalidNameException si no es un nombre distinguido valido
     * @throws NullPointerException si es {@code null}
     */
    public LdapPrincipal(String name) throws InvalidNameException {
        if (name == null) {
            throw new NullPointerException("el nombre no puede ser null");
        }
        this.name = new LdapName(name);
        this.nameString = name;
    }

    /** El nombre tal como se escribio. */
    public String getName() {
        return this.nameString;
    }

    public String toString() {
        return this.nameString;
    }

    /** Por nombre distinguido, no por texto; ver la nota de la clase. */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LdapPrincipal)) {
            return false;
        }
        return this.name.equals(((LdapPrincipal) object).name);
    }

    /** Sobre el nombre normalizado, coherente con {@link #equals}. */
    public int hashCode() {
        return this.name.hashCode();
    }
}
