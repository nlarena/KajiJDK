package com.sun.security.auth;

/**
 * Un {@link NTSid} que identifica el grupo primario del usuario.
 *
 * <p>No agrega comportamiento: lo que aporta es el <strong>tipo</strong>. Como {@link NTSid#equals}
 * compara por clase exacta, este nunca va a satisfacer una politica escrita para otra de las cuatro
 * subclases, aunque el texto del SID coincida.
 */
public class NTSidPrimaryGroupPrincipal extends NTSid {

    private static final long serialVersionUID = 8011978367305190527L;

    /**
     * @throws IllegalArgumentException si el SID esta vacio
     */
    public NTSidPrimaryGroupPrincipal(String name) {
        super(name);
    }

    public String toString() {
        return "NTSidPrimaryGroupPrincipal:  " + getName();
    }

    /** Heredado de {@link NTSid}: por clase exacta y SID. */
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
