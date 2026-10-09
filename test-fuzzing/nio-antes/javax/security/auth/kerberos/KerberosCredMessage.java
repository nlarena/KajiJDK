package javax.security.auth.kerberos;

import java.util.Arrays;
import java.util.Objects;
import javax.security.auth.Destroyable;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosCredMessage -- a KRB_CRED message.
 *
 * <p>It is how a client passes its credentials to a service so that it acts on its behalf: the
 * message goes encrypted and this class keeps the bytes as they are, with who sends it and who
 * receives it. It does not decrypt it.
 *
 * <p>It is destroyed like a key, because it carries one: after {@link #destroy} everything throws
 * {@link IllegalStateException}.
 */
public final class KerberosCredMessage implements Destroyable {

    /** Who sends it, or null if it was destroyed. */
    private KerberosPrincipal sender;

    /** Who receives it, or null if it was destroyed. */
    private KerberosPrincipal recipient;

    /** The bytes, or null if it was destroyed. */
    private byte[] message;

    /** Whether it was already erased. */
    private boolean destroyed = false;

    /**
     * With those three. The array is copied.
     *
     * @throws NullPointerException if any is null
     */
    public KerberosCredMessage(KerberosPrincipal sender, KerberosPrincipal recipient,
                               byte[] message) {
        this.sender = Objects.requireNonNull(sender);
        this.recipient = Objects.requireNonNull(recipient);
        this.message = Objects.requireNonNull(message).clone();
    }

    /**
     * The bytes. A copy.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public byte[] getEncoded() {
        checkAlive();
        return this.message.clone();
    }

    /**
     * Who sends it.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public KerberosPrincipal getSender() {
        checkAlive();
        return this.sender;
    }

    /**
     * Who receives it.
     *
     * @throws IllegalStateException if it is destroyed
     */
    public KerberosPrincipal getRecipient() {
        checkAlive();
        return this.recipient;
    }

    /** Erases the bytes. Destroying twice does nothing. */
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

    /** Whether it was already erased. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** From whom to whom; never the bytes. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed KerberosCredMessage";
        }
        return "KRB_CRED from " + this.sender + " to " + this.recipient;
    }

    /** A destroyed one is -1. */
    @Override
    public int hashCode() {
        if (this.destroyed) {
            return -1;
        }
        return Objects.hash(this.sender, this.recipient, Arrays.hashCode(this.message));
    }

    /** Equal if the three match; a destroyed one is only equal to itself. */
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

    /** Throws if it was already destroyed. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This object is no longer valid");
        }
    }
}
