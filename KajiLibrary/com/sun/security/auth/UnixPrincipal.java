package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * El nombre de usuario de Unix.
 *
 * <p>Como todo principal de este paquete: inmutable, comparado por nombre y por clase exacta. Lo
 * segundo importa mas de lo que parece — un {@code UnixPrincipal} y un {@code NTUserPrincipal} con
 * el mismo texto <strong>no</strong> son la misma identidad, y compararlos solo por nombre haria que
 * una politica escrita para uno se aplicara al otro.
 */
public class UnixPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -2951667807323493631L;

    private final String name;

    /**
     * @throws NullPointerException si el nombre es {@code null}
     */
    public UnixPrincipal(String name) {
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
        return "UnixPrincipal:  " + this.name;
    }

    /** Por clase exacta y nombre; ver la nota de la clase. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.name.equals(((UnixPrincipal) o).getName());
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}
