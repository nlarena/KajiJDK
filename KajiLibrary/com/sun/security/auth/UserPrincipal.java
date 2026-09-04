package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * Un usuario, sin decir de que sistema.
 *
 * <p>Es el generico de este paquete: los demas dicen de donde salio la identidad —Windows, Unix,
 * LDAP— y este no. Sirve cuando esa procedencia no importa o no se conoce.
 *
 * <p>Como todo principal de este paquete: inmutable, comparado por nombre y por clase exacta. Lo
 * segundo importa mas de lo que parece — un {@code UnixPrincipal} y un {@code NTUserPrincipal} con
 * el mismo texto <strong>no</strong> son la misma identidad, y compararlos solo por nombre haria que
 * una politica escrita para uno se aplicara al otro.
 */
public final class UserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 892106070870210969L;

    private final String name;

    /**
     * @throws NullPointerException si el nombre es {@code null}
     */
    public UserPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("el nombre no puede ser null");
        }
        this.name = name;
    }

    /** El nombre. */
    public String getName() {
        return this.name;
    }

    public String toString() {
        return "UserPrincipal:  " + this.name;
    }

    /** Por clase exacta y nombre; ver la nota de la clase. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.name.equals(((UserPrincipal) o).getName());
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}
