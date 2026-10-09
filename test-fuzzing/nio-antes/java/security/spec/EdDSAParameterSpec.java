package java.security.spec;

import java.security.InvalidParameterException;
import java.util.Optional;

// The parameters of an EdDSA signature: whether it is the "prehash" variant and which context to
// use.
//
// Both options change **what** is signed, not how, and that is why they are part of the signature
// and not a local preference: Ed25519, Ed25519ph and Ed25519ctx produce signatures that do not
// verify against each other even with the same key. That is deliberate, it is RFC 8032's domain
// separation: it keeps a signature issued for one purpose from being reused for another.
//
// The context is limited to 255 bytes because its length is encoded in a single byte inside the
// message that is hashed. The limit is the format's, not a policy of this class.
//
// The oddity is worth noting: going over the length throws `InvalidParameterException`, which
// inherits from `IllegalArgumentException` but lives in `java.security`. That is what the JDK does,
// and it is replicated.
public class EdDSAParameterSpec implements AlgorithmParameterSpec {

    private static final int MAX_CONTEXT_LENGTH = 255;

    private final boolean prehash;
    private final byte[] context;

    // No context: pure Ed25519 (or Ed25519ph if prehash).
    public EdDSAParameterSpec(boolean prehash) {
        this.prehash = prehash;
        this.context = null;
    }

    public EdDSAParameterSpec(boolean prehash, byte[] context) {
        if (context == null) {
            throw new NullPointerException("context may not be null");
        }
        if (context.length > MAX_CONTEXT_LENGTH) {
            throw new InvalidParameterException("context length cannot be greater than 255");
        }
        this.prehash = prehash;
        byte[] c = new byte[context.length];
        System.arraycopy(context, 0, c, 0, context.length);
        this.context = c;
    }

    // Whether the hash of the message is signed instead of the whole message. It is useful when the
    // message does not fit in memory or arrives as a stream; in exchange, security comes to depend
    // on the hash.
    public boolean isPrehash() {
        return this.prehash;
    }

    // A copy of the context, empty if there is none. It is `Optional` and not null because absent
    // and empty are different things here: a zero-byte context is a context.
    public Optional<byte[]> getContext() {
        if (this.context == null) {
            return Optional.empty();
        }
        byte[] c = new byte[this.context.length];
        System.arraycopy(this.context, 0, c, 0, this.context.length);
        return Optional.of(c);
    }
}
