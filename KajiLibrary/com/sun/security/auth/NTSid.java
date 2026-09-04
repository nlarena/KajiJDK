package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * Un identificador de seguridad de Windows: el {@code S-1-5-21-...} que identifica de verdad a una
 * cuenta o a un grupo.
 *
 * <h2>Por que el SID y no el nombre</h2>
 *
 * <p>Porque el nombre <strong>se puede reusar</strong>. Borrar la cuenta {@code juan} y crear otra
 * con el mismo nombre da una cuenta distinta, y una politica escrita contra el nombre se le
 * aplicaria a la persona equivocada. El SID no se reusa nunca.
 *
 * <p>Es tambien por lo que las subclases existen: {@link NTSidUserPrincipal},
 * {@link NTSidGroupPrincipal}, {@link NTSidDomainPrincipal} y
 * {@link NTSidPrimaryGroupPrincipal} son todas SIDs, y lo que las distingue es <em>que</em>
 * identifican. Como la comparacion es por clase exacta, un SID de grupo nunca satisface una politica
 * escrita para un usuario aunque el texto coincida.
 */
public class NTSid implements Principal, Serializable {

    private static final long serialVersionUID = 4412290580770249885L;

    private final String sid;

    /**
     * @throws NullPointerException si es {@code null}
     * @throws IllegalArgumentException si esta vacio — un SID vacio no identifica nada, y aceptarlo
     *     produciria un principal que iguala a cualquier otro vacio
     */
    public NTSid(String stringSid) {
        if (stringSid == null) {
            throw new NullPointerException("el SID no puede ser null");
        }
        if (stringSid.isEmpty()) {
            throw new IllegalArgumentException("el SID no puede estar vacio");
        }
        this.sid = stringSid;
    }

    /** El SID en su forma de texto. */
    public String getName() {
        return this.sid;
    }

    public String toString() {
        return "NTSid:  " + this.sid;
    }

    /** Por clase exacta y SID. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.sid.equals(((NTSid) o).getName());
    }

    public int hashCode() {
        return this.sid.hashCode();
    }
}
