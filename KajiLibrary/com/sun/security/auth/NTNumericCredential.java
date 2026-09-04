package com.sun.security.auth;

/**
 * El manejador de token de acceso que Windows le dio al proceso.
 *
 * <h2>Por que es una credencial y no un principal</h2>
 *
 * <p>Un principal dice <em>quien sos</em>; una credencial es <em>con que lo probas</em>. Este numero
 * no identifica a nadie por si mismo: es una referencia opaca a una estructura del sistema
 * operativo, que es lo que el proceso presenta para actuar en nombre del usuario.
 *
 * <p>De ahi que no implemente {@link java.security.Principal} ni sea serializable: un manejador solo
 * significa algo <strong>en la maquina y el proceso donde se creo</strong>. Mandarlo a otro lado
 * daria un numero que alla apunta a otra cosa, o a nada.
 */
public class NTNumericCredential {

    private final long impersonationToken;

    /** Con el manejador que dio el sistema. */
    public NTNumericCredential(long token) {
        this.impersonationToken = token;
    }

    /** El manejador. */
    public long getToken() {
        return this.impersonationToken;
    }

    public String toString() {
        return "NTNumericCredential: " + String.valueOf(this.impersonationToken);
    }

    /** Por clase exacta y valor. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.impersonationToken == ((NTNumericCredential) o).getToken();
    }

    public int hashCode() {
        return (int) this.impersonationToken;
    }
}
