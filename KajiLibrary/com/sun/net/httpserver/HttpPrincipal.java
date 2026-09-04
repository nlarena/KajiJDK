package com.sun.net.httpserver;

import java.security.Principal;

/**
 * Quien mando un pedido autenticado: un usuario dentro de un reino.
 *
 * <h2>Por que el reino forma parte de la identidad</h2>
 *
 * <p>Porque el nombre de usuario solo no es unico. El reino es el ambito donde ese nombre significa
 * algo, y el mismo servidor puede tener varios —uno para la administracion, otro para el area
 * publica— con un {@code admin} distinto en cada uno. De ahi que {@link #getName} devuelva
 * {@code "reino/usuario"} y no solo el usuario: comparar identidades por el nombre suelto
 * confundiria dos personas distintas.
 */
public class HttpPrincipal implements Principal {

    private final String username;
    private final String realm;

    /**
     * @throws NullPointerException si alguno es {@code null}
     */
    public HttpPrincipal(String username, String realm) {
        if (username == null || realm == null) {
            throw new NullPointerException("username y realm no pueden ser null");
        }
        this.username = username;
        this.realm = realm;
    }

    /** Sobre los dos componentes: ver la nota de la clase. */
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (another instanceof HttpPrincipal) {
            HttpPrincipal o = (HttpPrincipal) another;
            return this.username.equals(o.username) && this.realm.equals(o.realm);
        }
        return false;
    }

    /** El nombre contextualizado: {@code "reino/usuario"}. */
    public String getName() {
        return this.realm + "/" + this.username;
    }

    /** Solo el usuario. */
    public String getUsername() {
        return this.username;
    }

    /** Solo el reino. */
    public String getRealm() {
        return this.realm;
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String toString() {
        return getName();
    }
}
