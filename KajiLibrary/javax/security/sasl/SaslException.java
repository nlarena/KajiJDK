package javax.security.sasl;

import java.io.IOException;

/**
 * KajiLibrary's javax.security.sasl.SaslException -- fallo la negociacion SASL.
 *
 * <p>Extiende {@link IOException} y no {@code Exception}, que es la decision de diseno que hay que
 * mirar: SASL siempre va adentro de un protocolo --LDAP, IMAP, SMTP-- y quien lo usa ya esta
 * atajando errores de entrada y salida. Colgarla de ahi evita que cada llamada tenga dos
 * {@code catch} que hacen lo mismo.
 *
 * <p>El {@link #toString} es propio y agrega la causa entre corchetes, en vez de dejar que aparezca
 * recien en el rastro de pila. Es util aca: lo que falla en SASL casi siempre es el mecanismo de
 * abajo, y el mensaje de arriba solo no dice nada.
 */
public class SaslException extends IOException {

    private static final long serialVersionUID = 4579784287983423626L;

    /** Sin detalle. */
    public SaslException() {
        super();
    }

    /** Con un mensaje. */
    public SaslException(String detail) {
        super(detail);
    }

    /**
     * Con la causa de abajo.
     *
     * @param ex lo que fallo de verdad; null si no hay
     */
    public SaslException(String detail, Throwable ex) {
        super(detail);
        if (ex != null) {
            initCause(ex);
        }
    }

    /** La causa, o null. */
    public Throwable getCause() {
        return super.getCause();
    }

    /**
     * Fija la causa.
     *
     * @throws IllegalStateException si ya tenia una
     */
    public Throwable initCause(Throwable cause) {
        return super.initCause(cause);
    }

    /** Con la causa entre corchetes si la hay; ver la nota de la clase. */
    public String toString() {
        Throwable cause = getCause();
        String head = super.toString();
        if (cause == null) {
            return head;
        }
        return head + " [Caused by " + cause.toString() + "]";
    }
}
