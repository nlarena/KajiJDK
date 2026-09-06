package javax.crypto;

/**
 * A cipher that does not encrypt: it hands the bytes over as it received them.
 *
 * <h2>What something that does nothing is for</h2>
 *
 * <p>For writing the program once. A protocol that may negotiate encryption or no encryption --and
 * many do-- would otherwise have two different paths, one with {@link Cipher} and one without, and
 * those two paths drift apart. With this there is one, and the decision to encrypt sits in one
 * place: which cipher is built.
 *
 * <p>It is also good for measuring what the machinery costs without the algorithm, and for testing
 * the rest of the program without needing keys.
 *
 * <h2>It needs no configuring</h2>
 *
 * <p>It is born ready. That is the only visible difference from any other {@link Cipher}, which
 * throws if used without {@code init}, and it makes sense: there is nothing to configure when there
 * is no key.
 *
 * @since 1.4
 */
public class NullCipher extends Cipher {

    /** One. */
    public NullCipher() {
        super(new NullCipherSpi(), null, null);
        this.unconfigured = true;
    }
}
