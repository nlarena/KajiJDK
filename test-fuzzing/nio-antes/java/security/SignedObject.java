package java.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * An object serialised together with its signature.
 *
 * <h2>What it guarantees and what it does not</h2>
 *
 * <p>It guarantees <strong>integrity and origin</strong>: whoever receives one of these can check
 * that the contents did not change and that the owner of a concrete key signed it. It does
 * <strong>not</strong> guarantee confidentiality — the object travels in the clear, and anybody can
 * read it without verifying anything. Confusing the two things is the classic mistake with this
 * class.
 *
 * <h2>Why the bytes are kept and not the object</h2>
 *
 * <p>Because the signature is over <em>bytes</em>. If this class kept the reference to the original
 * object and serialised it again when verifying, two serialisations of the same object could differ
 * —a {@code HashMap} with another order, a field that changed— and the signature would stop
 * validating without anybody having tampered with anything.
 *
 * <p>Keeping the serialised copy also has the useful consequence that {@link #getObject} returns a
 * <strong>new</strong> object on each call: it is a deep copy, not the instance of origin.
 *
 * <h2>The {@link Signature} is brought by the caller</h2>
 *
 * <p>And that is what makes this class work in this library even though there is no cryptographic
 * provider installed: it asks for no algorithm on its own. Whoever builds or verifies brings their
 * signature engine already obtained, and if there is no provider the failure appears there —in
 * {@code Signature.getInstance}— and not here.
 *
 * <h2>On reusing the signature object</h2>
 *
 * <p>Both the constructor and {@link #verify} call {@code initSign}/{@code initVerify}, so the
 * previous state of the {@code Signature} passed to them is lost. It is intentional in the JDK:
 * receiving one half used and trusting its state would be fragile.
 *
 * @since 1.2
 */
public final class SignedObject implements Serializable {

    private static final long serialVersionUID = 720502720485447167L;

    /** The object, serialised already. See the note of the class about why it is kept like this. */
    private byte[] content;

    /** The signature over {@link #content}. */
    private byte[] signature;

    /** Which algorithm it was signed with. */
    private String thealgorithm;

    /**
     * It serialises {@code object} and signs it.
     *
     * @throws IOException if the object could not be serialised
     * @throws InvalidKeyException if the key does not serve for that signature engine
     * @throws SignatureException if the signing failed
     */
    public SignedObject(Serializable object, PrivateKey signingKey, Signature signingEngine)
            throws IOException, InvalidKeyException, SignatureException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        try {
            out.writeObject(object);
            out.flush();
        } finally {
            out.close();
        }
        this.content = bytes.toByteArray();
        this.thealgorithm = signingEngine.getAlgorithm();

        signingEngine.initSign(signingKey);
        signingEngine.update(this.content, 0, this.content.length);
        this.signature = signingEngine.sign().clone();
    }

    /**
     * It deserialises a copy of the object.
     *
     * <p><strong>It verifies nothing.</strong> Calling this without having gone through {@link
     * #verify} first is reading data of unknown origin — and deserialising somebody else's data is
     * precisely the vector of the deserialisation attacks. The right order is to verify first.
     *
     * @throws IOException if the bytes could not be read
     * @throws ClassNotFoundException if the class of the object is not there
     */
    public Object getObject() throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(this.content));
        try {
            return in.readObject();
        } finally {
            in.close();
        }
    }

    /** A copy of the signature; the internal array is not lent out. */
    public byte[] getSignature() {
        return this.signature.clone();
    }

    /** Which algorithm it was signed with. */
    public String getAlgorithm() {
        return this.thealgorithm;
    }

    /**
     * It checks the signature against that public key.
     *
     * @return {@code true} if the contents did not change and the owner of the key signed it
     * @throws InvalidKeyException if the key does not serve for that engine
     * @throws SignatureException if the verification failed for a reason that is not "it does not
     *     match"
     */
    public boolean verify(PublicKey verificationKey, Signature verificationEngine)
            throws InvalidKeyException, SignatureException {
        verificationEngine.initVerify(verificationKey);
        verificationEngine.update(this.content, 0, this.content.length);
        return verificationEngine.verify(this.signature);
    }
}
