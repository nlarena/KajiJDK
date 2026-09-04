package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * El GID de Unix, con una distincion que el numero solo no lleva: si es el grupo
 * <strong>primario</strong> del usuario.
 *
 * <h2>Por que importa la distincion</h2>
 *
 * <p>Un usuario de Unix pertenece a un grupo primario y a cuantos suplementarios haga falta. Para
 * los permisos de lectura y escritura los dos valen igual, pero <strong>los archivos que crea
 * heredan el grupo primario</strong>. Sin este dato no se puede expresar una politica que dependa de
 * eso.
 *
 * <p>Por eso la bandera forma parte de la identidad y entra en {@link #equals}: el mismo GID como
 * primario y como suplementario son dos principales distintos.
 */
public class UnixNumericGroupPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 3941535899328403223L;

    private final String name;
    private final boolean primaryGroup;

    /**
     * @param primaryGroup si es el grupo primario
     * @throws NullPointerException si el GID es {@code null}
     * @throws NumberFormatException si no es un numero
     */
    public UnixNumericGroupPrincipal(String name, boolean primaryGroup) {
        if (name == null) {
            throw new NullPointerException("el GID no puede ser null");
        }
        Long.parseLong(name);
        this.name = name;
        this.primaryGroup = primaryGroup;
    }

    /** Desde el numero. */
    public UnixNumericGroupPrincipal(long name, boolean primaryGroup) {
        this.name = Long.toString(name);
        this.primaryGroup = primaryGroup;
    }

    /** El GID como texto. */
    public String getName() {
        return this.name;
    }

    /** El GID como numero. */
    public long longValue() {
        return Long.parseLong(this.name);
    }

    /** Si es el grupo primario; ver la nota de la clase. */
    public boolean isPrimaryGroup() {
        return this.primaryGroup;
    }

    public String toString() {
        return this.primaryGroup
                ? "UnixNumericGroupPrincipal [Primary Group]: " + this.name
                : "UnixNumericGroupPrincipal [Supplementary Group]: " + this.name;
    }

    /** Por clase exacta, GID <strong>y</strong> la bandera de primario. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        UnixNumericGroupPrincipal otro = (UnixNumericGroupPrincipal) o;
        return this.name.equals(otro.getName()) && this.primaryGroup == otro.isPrimaryGroup();
    }

    public int hashCode() {
        return this.primaryGroup ? this.name.hashCode() * 31 : this.name.hashCode();
    }
}
