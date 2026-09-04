package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * El UID de Unix: el numero que de verdad identifica a un usuario.
 *
 * <h2>Por que el numero y no el nombre</h2>
 *
 * <p>Porque el nombre es una etiqueta que vive en {@code /etc/passwd} y el nucleo no la conoce:
 * todos los permisos del sistema de archivos se resuelven contra el UID. Dos nombres pueden apuntar
 * al mismo UID —y ahi son la misma identidad, aunque se escriban distinto—; un nombre reciclado
 * apunta a un UID nuevo.
 *
 * <p>De ahi que exista este principal aparte de {@link UnixPrincipal}: uno es como se llama, el otro
 * es quien es.
 *
 * <p>Los dos constructores son el mismo dato en dos formas, y {@link #getName} devuelve el texto
 * mientras {@link #longValue} devuelve el numero — util porque el UID llega como cadena de casi
 * todos lados.
 */
public class UnixNumericUserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -4329764253802397821L;

    private final String name;

    /**
     * @throws NullPointerException si es {@code null}
     * @throws NumberFormatException si no es un numero
     */
    public UnixNumericUserPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("el UID no puede ser null");
        }
        Long.parseLong(name);
        this.name = name;
    }

    /** Desde el numero. */
    public UnixNumericUserPrincipal(long name) {
        this.name = Long.toString(name);
    }

    /** El UID como texto. */
    public String getName() {
        return this.name;
    }

    /** El UID como numero. */
    public long longValue() {
        return Long.parseLong(this.name);
    }

    public String toString() {
        return "UnixNumericUserPrincipal: " + this.name;
    }

    /** Por clase exacta y UID. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.name.equals(((UnixNumericUserPrincipal) o).getName());
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}
