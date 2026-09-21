package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

// What a provider has to write in order to offer a signature algorithm.
//
// The cycle is always the same: initialise with a key, feed data with `engineUpdate`, and close
// with `engineSign` or `engineVerify`. That it is an object with state and not a loose function is
// what allows signing something that does not fit in memory.
//
// ===============================================================================================
// THE NONCE, WHICH IS WHAT MATTERS MOST ABOUT THIS CLASS
// ===============================================================================================
//
// It has to be said because it is not a small thing: the signature of DSA and that of ECDSA need an
// unpredictable nonce per signature, and with no source of randomness there is no way of producing
// it. A repeated nonce in ECDSA **reveals the private key** with two signatures and a little
// arithmetic; it is how Sony's console was broken in 2010. A signature provider for this library
// would have to use a deterministic scheme —RFC 6979, or Ed25519, which is one by design— or bring
// its own source.
public abstract class SignatureSpi {

    /**
     * The source of randomness the application set, or null if it set none.
     *
     * <p>Protected and mutable because that is how the JDK declares it: the SPI reads it when
     * signing. A provider that finds it null and needs a nonce **must not invent one**; it has to
     * ask {@code new SecureRandom()} for one, which is the operating system's.
     */
    protected SecureRandom appRandom = null;

    public SignatureSpi() {
    }

    // It prepares to verify with this public key.
    protected abstract void engineInitVerify(PublicKey publicKey) throws InvalidKeyException;

    // It prepares to sign with this private key.
    protected abstract void engineInitSign(PrivateKey privateKey) throws InvalidKeyException;

    /**
     * The same, saying where the randomness comes from.
     *
     * <p>The default keeps the source in {@link #appRandom} and forwards to the one above, which is
     * what the JDK does. A provider that needs the nonce -- DSA, ECDSA -- has to override it or
     * read the field; the one that signs deterministically -- Ed25519, RFC 6979 -- can ignore it.
     */
    protected void engineInitSign(PrivateKey privateKey, SecureRandom random)
            throws InvalidKeyException {
        this.appRandom = random;
        this.engineInitSign(privateKey);
    }

    protected abstract void engineUpdate(byte b) throws SignatureException;

    protected abstract void engineUpdate(byte[] b, int off, int len) throws SignatureException;

    // It feeds from a `ByteBuffer`. The base implementation resolves it by delegating to the array
    // version: if the buffer has a backing array it uses it directly, and if not it copies it in
    // pieces so as not to reserve memory proportional to the message.
    //
    // It consumes the whole buffer: on returning, `position` was left at `limit`. It is of the
    // contract and it surprises whoever expects to be able to reread it.
    protected void engineUpdate(ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        try {
            if (input.hasArray()) {
                byte[] b = input.array();
                int ofs = input.arrayOffset();
                int pos = input.position();
                int lim = input.limit();
                this.engineUpdate(b, ofs + pos, lim - pos);
                input.position(lim);
            } else {
                int len = input.remaining();
                byte[] b = new byte[len < 4096 ? len : 4096];
                while (len > 0) {
                    int chunk = len < b.length ? len : b.length;
                    input.get(b, 0, chunk);
                    this.engineUpdate(b, 0, chunk);
                    len = len - chunk;
                }
            }
        } catch (SignatureException e) {
            // `engineUpdate(ByteBuffer)` does not declare `SignatureException`, so the only way out
            // is wrapping it. It is what the JDK does: a failure here is the provider's, not the
            // caller's.
            throw new ProviderException("update() failed", e);
        }
    }

    protected abstract byte[] engineSign() throws SignatureException;

    // It signs leaving the result in a given buffer. The base implementation signs normally and
    // copies.
    //
    // **It does not truncate**: if the buffer is not enough for the whole signature, it throws.
    // Returning a partial signature would be returning something that looks like a signature and
    // never verifies.
    protected int engineSign(byte[] outbuf, int offset, int len) throws SignatureException {
        byte[] sig = this.engineSign();
        if (len < sig.length) {
            throw new SignatureException("partial signatures not returned");
        }
        if (outbuf.length - offset < sig.length) {
            throw new SignatureException(
                "insufficient space in the output buffer to store the signature");
        }
        System.arraycopy(sig, 0, outbuf, offset, sig.length);
        return sig.length;
    }

    // It verifies the signature. **Here there is a return value**, the other way round from
    // `Certificate.verify`: false means "it does not validate" and is not an error.
    protected abstract boolean engineVerify(byte[] sigBytes) throws SignatureException;

    protected boolean engineVerify(byte[] sigBytes, int offset, int length)
            throws SignatureException {
        byte[] copy = new byte[length];
        System.arraycopy(sigBytes, offset, copy, 0, length);
        return this.engineVerify(copy);
    }

    // The old way of passing parameters, by name. It has been discouraged since JDK 1.2 because the
    // names were never standardised: each provider understood its own.
    protected abstract void engineSetParameter(String param, Object value)
        throws InvalidParameterException;

    // The good way of passing parameters. It throws `UnsupportedOperationException` by default
    // because it arrived after the SPI and the providers that already existed could not be forced.
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        throw new UnsupportedOperationException();
    }

    // The effective parameters, including the ones the provider may have chosen by default. It
    // throws by default, for the same reason as the setter.
    protected AlgorithmParameters engineGetParameters() {
        throw new UnsupportedOperationException();
    }

    protected abstract Object engineGetParameter(String param) throws InvalidParameterException;

    // It only clones if the subclass declares `Cloneable`. That the check is explicit and not
    // automatic matters: a signature object has half-computed state, and copying it without the
    // provider having thought about it would give two objects sharing the internal buffer.
    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
