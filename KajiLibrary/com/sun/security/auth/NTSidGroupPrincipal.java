package com.sun.security.auth;

/**
 * Un {@link NTSid} que identifica un grupo al que el usuario pertenece.
 *
 * <p>No agrega comportamiento: lo que aporta es el <strong>tipo</strong>. Como {@link NTSid#equals}
 * compara por clase exacta, este nunca va a satisfacer una politica escrita para otra de las cuatro
 * subclases, aunque el texto del SID coincida.
 */
public class NTSidGroupPrincipal extends NTSid {

    private static final long serialVersionUID = -1358357160952943269L;

    /**
     * @throws IllegalArgumentException si el SID esta vacio
     */
    public NTSidGroupPrincipal(String name) {
        super(name);
    }

    public String toString() {
        return "NTSidGroupPrincipal:  " + getName();
    }

    /** Heredado de {@link NTSid}: por clase exacta y SID. */
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
