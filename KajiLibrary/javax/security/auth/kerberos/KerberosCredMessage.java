package javax.security.auth.kerberos;

import java.util.Arrays;
import java.util.Objects;
import javax.security.auth.Destroyable;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosCredMessage -- un mensaje KRB_CRED.
 *
 * <p>Es como un cliente le pasa sus credenciales a un servicio para que actue en su nombre: el
 * mensaje va cifrado y esta clase guarda los bytes tal cual, con quien lo manda y quien lo recibe.
 * No lo descifra.
 *
 * <p>Se destruye como una clave, porque lleva una: despues de {@link #destroy} todo lanza
 * {@link IllegalStateException}.
 */
public final class KerberosCredMessage implements Destroyable {

    /** Quien lo manda, o null si se destruyo. */
    private KerberosPrincipal sender;

    /** Quien lo recibe, o null si se destruyo. */
    private KerberosPrincipal recipient;

    /** Los bytes, o null si se destruyo. */
    private byte[] message;

    /** Si ya se borro. */
    private boolean destroyed = false;

    /**
     * Con esos tres. El arreglo se copia.
     *
     * @throws NullPointerException si cualquiera es null
     */
    public KerberosCredMessage(KerberosPrincipal sender, KerberosPrincipal recipient,
                               byte[] message) {
        this.sender = Objects.requireNonNull(sender);
        this.recipient = Objects.requireNonNull(recipient);
        this.message = Objects.requireNonNull(message).clone();
    }

    /**
     * Los bytes. Una copia.
     *
     * @throws IllegalStateException si esta destruido
     */
    public byte[] getEncoded() {
        checkAlive();
        return this.message.clone();
    }

    /**
     * Quien lo manda.
     *
     * @throws IllegalStateException si esta destruido
     */
    public KerberosPrincipal getSender() {
        checkAlive();
        return this.sender;
    }

    /**
     * Quien lo recibe.
     *
     * @throws IllegalStateException si esta destruido
     */
    public KerberosPrincipal getRecipient() {
        checkAlive();
        return this.recipient;
    }

    /** Borra los bytes. Destruir dos veces no hace nada. */
    @Override
    public void destroy() {
        if (!this.destroyed) {
            Arrays.fill(this.message, (byte) 0);
            this.message = null;
            this.sender = null;
            this.recipient = null;
            this.destroyed = true;
        }
    }

    /** Si ya se borro. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** De quien a quien; nunca los bytes. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed KerberosCredMessage";
        }
        return "KRB_CRED from " + this.sender + " to " + this.recipient;
    }

    /** Uno destruido vale -1. */
    @Override
    public int hashCode() {
        if (this.destroyed) {
            return -1;
        }
        return Objects.hash(this.sender, this.recipient, Arrays.hashCode(this.message));
    }

    /** Iguales si coinciden los tres; uno destruido solo es igual a si mismo. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof KerberosCredMessage)) {
            return false;
        }
        KerberosCredMessage that = (KerberosCredMessage) other;
        if (this.destroyed || that.destroyed) {
            return false;
        }
        return this.sender.equals(that.sender) && this.recipient.equals(that.recipient)
            && Arrays.equals(this.message, that.message);
    }

    /** Lanza si ya se destruyo. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This object is no longer valid");
        }
    }
}
