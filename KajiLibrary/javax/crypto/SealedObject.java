package javax.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * An object kept encrypted.
 *
 * <h2>What it does</h2>
 *
 * <p>It serializes the object and encrypts the bytes. What is left is an object that can go on being
 * treated like any other --stored, sent, serialized again-- but whose content cannot be read without
 * the key.
 *
 * <h2>What it does not do</h2>
 *
 * <p>It does not authenticate. If the cipher is not an authenticated one, somebody can change the
 * encrypted bytes and {@link #getObject} will deserialize whatever comes out. And deserializing data
 * one does not control is dangerous in itself: deserialization builds arbitrary objects before the
 * program can look at them.
 *
 * <p>That is why it is best to seal with an authenticated cipher, or to keep a {@link Mac} of what
 * was sealed alongside it.
 *
 * <h2>{@link #getAlgorithm}</h2>
 *
 * <p>It keeps what it was sealed with, so that whoever opens it can build the matching cipher. It
 * does not keep the key, obviously; it does keep the parameters, in {@link #encodedParams}, because
 * without them --without the initialization vector, for instance-- it could not be decrypted.
 *
 * @since 1.4
 */
public class SealedObject implements Serializable {

    private static final long serialVersionUID = 4482838265551344752L;

    /** The parameters it was encrypted with, encoded, or {@code null} if there were none. */
    protected byte[] encodedParams;

    private final byte[] encryptedContent;
    private final String sealAlg;

    /**
     * Seals that object with that cipher.
     *
     * @param object what is being kept
     * @param c the cipher, already configured to encrypt
     * @throws IOException if the object cannot be serialized
     * @throws IllegalBlockSizeException if the cipher cannot cope with what was serialized
     * @throws NullPointerException if the cipher is {@code null}
     */
    public SealedObject(Serializable object, Cipher c)
            throws IOException, IllegalBlockSizeException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bytes);
        oos.writeObject(object);
        oos.flush();
        oos.close();
        try {
            this.encryptedContent = c.doFinal(bytes.toByteArray());
        } catch (BadPaddingException e) {
            // Encrypting it cannot happen: the padding is put there by the cipher.
            throw new RuntimeException(e.getMessage());
        }
        this.sealAlg = c.getAlgorithm();
        final java.security.AlgorithmParameters p = c.getParameters();
        this.encodedParams = p == null ? null : p.getEncoded();
    }

    /**
     * A copy of another one.
     *
     * @param so the original
     */
    protected SealedObject(SealedObject so) {
        this.encryptedContent = so.encryptedContent == null ? null
                : so.encryptedContent.clone();
        this.sealAlg = so.sealAlg;
        this.encodedParams = so.encodedParams == null ? null : so.encodedParams.clone();
    }

    /**
     * What it was sealed with.
     *
     * @return the algorithm
     */
    public final String getAlgorithm() {
        return this.sealAlg;
    }

    /**
     * Opens it with that key.
     *
     * <p>It builds the cipher itself, out of the stored algorithm and parameters. It is the
     * convenient version; {@link #getObject(Cipher)} is for when the cipher is already built.
     *
     * @param key the key
     * @return the object
     * @throws IOException if it cannot be deserialized
     * @throws ClassNotFoundException if the object's class is not there
     * @throws NoSuchAlgorithmException if there is no provider with that algorithm
     * @throws InvalidKeyException if the key is no good
     */
    public final Object getObject(Key key)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            InvalidKeyException {
        try {
            final Cipher c = Cipher.getInstance(this.sealAlg);
            return open(c, key);
        } catch (NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /**
     * Opens it with that cipher.
     *
     * @param c the cipher, already configured to decrypt
     * @return the object
     * @throws IOException if it cannot be deserialized
     * @throws ClassNotFoundException if the object's class is not there
     * @throws IllegalBlockSizeException if what was kept is not a multiple of the block
     * @throws BadPaddingException if the padding does not close, nearly always because the key is
     *     wrong
     * @throws NullPointerException if the cipher is {@code null}
     */
    public final Object getObject(Cipher c)
            throws IOException, ClassNotFoundException, IllegalBlockSizeException,
            BadPaddingException {
        return read(c.doFinal(this.encryptedContent));
    }

    /**
     * Opens it with that key, using that provider.
     *
     * @param key the key
     * @param provider the provider's name
     * @return the object
     * @throws IOException if it cannot be deserialized
     * @throws ClassNotFoundException if the object's class is not there
     * @throws NoSuchAlgorithmException if that provider does not have that algorithm
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws InvalidKeyException if the key is no good
     * @throws IllegalArgumentException if the provider's name is {@code null} or empty
     */
    public final Object getObject(Key key, String provider)
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        try {
            final Cipher c = Cipher.getInstance(this.sealAlg, provider);
            return open(c, key);
        } catch (NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /** Configures the cipher with what was stored and opens it. */
    private Object open(Cipher c, Key key)
            throws IOException, ClassNotFoundException, InvalidKeyException,
            NoSuchAlgorithmException {
        try {
            if (this.encodedParams == null) {
                c.init(Cipher.DECRYPT_MODE, key);
            } else {
                final java.security.AlgorithmParameters p =
                        java.security.AlgorithmParameters.getInstance(withoutMode(this.sealAlg));
                p.init(this.encodedParams);
                c.init(Cipher.DECRYPT_MODE, key, p);
            }
            return read(c.doFinal(this.encryptedContent));
        } catch (java.security.InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e.getMessage());
        } catch (BadPaddingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /** The algorithm without the mode or the padding, which is how the parameters are named. */
    private static String withoutMode(String alg) {
        if (alg == null) {
            return null;
        }
        final int slash = alg.indexOf('/');
        return slash < 0 ? alg : alg.substring(0, slash);
    }

    private static Object read(byte[] data) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        try {
            return ois.readObject();
        } finally {
            ois.close();
        }
    }
}
