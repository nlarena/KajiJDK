package javax.crypto;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.DEREncodable;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * A private key kept encrypted, with the label of how it was encrypted.
 *
 * <h2>What it is</h2>
 *
 * <p>The PKCS#8 structure: an algorithm identification and a block of encrypted bytes. It is what
 * lives inside a password-protected private key file --what shows up as
 * {@code -----BEGIN ENCRYPTED PRIVATE KEY-----}.
 *
 * <p>The identification travels in the clear and has to: without it there would be no way to know
 * what to decrypt with. It includes the parameters --the salt and the iteration count, if it is a
 * password-- which are public by design.
 *
 * <h2>Why the algorithm is kept as text</h2>
 *
 * <p>Because in the file it is an object identifier, a list of numbers. {@link #getAlgName}
 * translates it into the name when it knows it, and otherwise returns the numbers with dots.
 * Returning the numbers is better than failing: the file can go on being read and written even when
 * this library does not know that algorithm.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>Reading, writing and translating identifiers really works, and it is what is needed to handle
 * the file. Decrypting does not: {@link #getKeySpec(Cipher)} needs an already built cipher, and the
 * rest need to build one, for which a provider offering ciphers is needed --see {@link Cipher}'s
 * note. The name table holds the PKCS#5 and PKCS#12 identifiers, which are the ones that turn up in
 * these files; a name that is not there is a {@link NoSuchAlgorithmException} and not an invented
 * identifier.
 *
 * @since 1.4
 */
public class EncryptedPrivateKeyInfo implements DEREncodable {

    private static final Map<String, String> NAME_TO_OID = new HashMap<String, String>();
    private static final Map<String, String> OID_TO_NAME = new HashMap<String, String>();

    static {
        // The names and the identifiers are taken from JDK 25, one by one. Where the JDK accepts one
        // name but returns another on reading back --DES gives DES/CBC-- it is written down all the
        // same, because that asymmetry is part of the behaviour too.
        register("PBEWithMD5AndDES", "1.2.840.113549.1.5.3", true);
        register("PBEWithSHA1AndDESede", "1.2.840.113549.1.12.1.3", true);
        register("PBEWithSHA1AndRC2_40", "1.2.840.113549.1.12.1.6", true);
        register("PBEWithSHA1AndRC2_128", "1.2.840.113549.1.12.1.5", true);
        register("PBEWithSHA1AndRC4_40", "1.2.840.113549.1.12.1.2", true);
        register("PBEWithSHA1AndRC4_128", "1.2.840.113549.1.12.1.1", true);
        register("PBES2", "1.2.840.113549.1.5.13", true);
        register("PBKDF2WithHmacSHA1", "1.2.840.113549.1.5.12", true);
        register("AES", "2.16.840.1.101.3.4.1", true);
        register("AES_128/CBC/NoPadding", "2.16.840.1.101.3.4.1.2", true);
        register("AES_256/CBC/NoPadding", "2.16.840.1.101.3.4.1.42", true);
        register("DESede", "1.3.14.3.2.17", true);
        register("DES/CBC", "1.3.14.3.2.7", true);
        register("DES", "1.3.14.3.2.7", false);
        register("RC2/CBC/PKCS5Padding", "1.2.840.113549.3.2", true);
        register("RC2", "1.2.840.113549.3.2", false);
        register("Blowfish", "1.3.6.1.4.1.3029.1.1.2", true);
        register("HmacSHA256", "1.2.840.113549.2.9", true);
        register("EC", "1.2.840.10045.2.1", true);
        register("DiffieHellman", "1.2.840.113549.1.3.1", true);
    }

    private final String oid;
    private final byte[] paramsDer;
    private final byte[] encryptedData;

    /**
     * One read from its encoding.
     *
     * @param encoded the bytes
     * @throws IOException if it is not a valid structure
     * @throws NullPointerException if the bytes are {@code null}
     */
    public EncryptedPrivateKeyInfo(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new NullPointerException("the encoded parameter must be non-null");
        }
        final byte[] copy = encoded.clone();
        final Der root = Der.read(copy, 0);
        if (root.tag != 0x30) {
            throw new IOException("not a SEQUENCE");
        }
        final Der alg = Der.read(copy, root.content);
        if (alg.tag != 0x30) {
            throw new IOException("the AlgorithmIdentifier is not a SEQUENCE");
        }
        final Der id = Der.read(copy, alg.content);
        if (id.tag != 0x06) {
            throw new IOException("the algorithm identifier is missing");
        }
        this.oid = Der.oid(copy, id.content, id.length);
        final int algEnd = alg.content + alg.length;
        final int paramsFrom = id.content + id.length;
        if (paramsFrom < algEnd) {
            this.paramsDer = new byte[algEnd - paramsFrom];
            System.arraycopy(copy, paramsFrom, this.paramsDer, 0, this.paramsDer.length);
        } else {
            this.paramsDer = null;
        }
        final Der data = Der.read(copy, algEnd);
        if (data.tag != 0x04) {
            throw new IOException("the encrypted data is not an OCTET STRING");
        }
        this.encryptedData = new byte[data.length];
        System.arraycopy(copy, data.content, this.encryptedData, 0, data.length);
    }

    /**
     * One with that algorithm and that data.
     *
     * @param algName the algorithm's name, or its identifier with dots
     * @param encryptedData the encrypted data
     * @throws NoSuchAlgorithmException if the name is not in the table and is not an identifier
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if the data is empty
     */
    public EncryptedPrivateKeyInfo(String algName, byte[] encryptedData)
            throws NoSuchAlgorithmException {
        if (algName == null) {
            throw new NullPointerException("the algName parameter must be non-null");
        }
        if (encryptedData == null) {
            throw new NullPointerException("the encryptedData parameter must be non-null");
        }
        if (encryptedData.length == 0) {
            throw new IllegalArgumentException("the encryptedData parameter must not be empty");
        }
        this.oid = toOid(algName);
        this.paramsDer = null;
        this.encryptedData = encryptedData.clone();
    }

    /**
     * One with those parameters and that data.
     *
     * @param algParams the parameters, which the algorithm and its configuration come from
     * @param encryptedData the encrypted data
     * @throws NoSuchAlgorithmException if the parameters' algorithm is not in the table
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if the data is empty
     */
    public EncryptedPrivateKeyInfo(AlgorithmParameters algParams, byte[] encryptedData)
            throws NoSuchAlgorithmException {
        if (algParams == null) {
            throw new NullPointerException("algParams must be non-null");
        }
        if (encryptedData == null) {
            throw new NullPointerException("encryptedData must be non-null");
        }
        if (encryptedData.length == 0) {
            throw new IllegalArgumentException("the encryptedData parameter must not be empty");
        }
        this.oid = toOid(algParams.getAlgorithm());
        byte[] p;
        try {
            p = algParams.getEncoded();
        } catch (IOException e) {
            p = null;
        }
        this.paramsDer = p;
        this.encryptedData = encryptedData.clone();
    }

    /**
     * Which algorithm it was encrypted with.
     *
     * @return the name if it is in the table, or the identifier with dots
     */
    public String getAlgName() {
        final String n = OID_TO_NAME.get(this.oid);
        return n == null ? this.oid : n;
    }

    /**
     * The algorithm's parameters.
     *
     * @return the parameters, or {@code null} if there are none or they could not be interpreted
     */
    public AlgorithmParameters getAlgParameters() {
        if (this.paramsDer == null) {
            return null;
        }
        try {
            final AlgorithmParameters p = AlgorithmParameters.getInstance(getAlgName());
            p.init(this.paramsDer);
            return p;
        } catch (NoSuchAlgorithmException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * The encrypted bytes.
     *
     * @return a copy
     */
    public byte[] getEncryptedData() {
        return this.encryptedData.clone();
    }

    /**
     * Decrypts and returns the private key in its encoded form.
     *
     * @param cipher the cipher, already configured to decrypt
     * @return the encoded private key
     * @throws InvalidKeySpecException if what was decrypted is not an encoded private key, which is
     *     what happens when the decryption key is wrong
     * @throws NullPointerException if the cipher is {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Cipher cipher) throws InvalidKeySpecException {
        if (cipher == null) {
            throw new NullPointerException("cipher must be non-null");
        }
        final byte[] plain;
        try {
            plain = cipher.doFinal(this.encryptedData);
        } catch (GeneralSecurityException e) {
            throw new InvalidKeySpecException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        }
        return toKeySpec(plain);
    }

    /**
     * Encrypts a private key with a password.
     *
     * @param key the key to encrypt
     * @param password the password
     * @param algorithm with which algorithm, or {@code null} for the default one
     * @param spec the parameters, or {@code null}
     * @param provider which provider to use, or {@code null} to search among all of them
     * @return the encrypted key
     * @throws IllegalArgumentException if the key or the password are {@code null}
     * @throws UnsupportedOperationException always: password-based encryption is needed, and no
     *     registered provider offers ciphers
     */
    public static EncryptedPrivateKeyInfo encryptKey(PrivateKey key, char[] password,
            String algorithm, AlgorithmParameterSpec spec, Provider provider) {
        if (key == null || password == null) {
            throw new IllegalArgumentException("key and password must be non-null");
        }
        throw new UnsupportedOperationException(NO_CIPHER);
    }

    /**
     * The same, with the default algorithm and parameters.
     *
     * @param key the key to encrypt
     * @param password the password
     * @return the encrypted key
     * @throws IllegalArgumentException if the key or the password are {@code null}
     * @throws UnsupportedOperationException always: see the other version
     */
    public static EncryptedPrivateKeyInfo encryptKey(PrivateKey key, char[] password) {
        return encryptKey(key, password, null, null, null);
    }

    /**
     * Encrypts a private key with another key.
     *
     * @param key the key to encrypt
     * @param encKey which key to encrypt it with
     * @param algorithm with which algorithm, or {@code null} for the default one
     * @param spec the parameters, or {@code null}
     * @param provider which provider to use, or {@code null} to search among all of them
     * @param random where to take the randomness from, or {@code null}
     * @return the encrypted key
     * @throws IllegalArgumentException if either of the two keys is {@code null}
     * @throws UnsupportedOperationException always: no registered provider offers ciphers
     */
    public static EncryptedPrivateKeyInfo encryptKey(PrivateKey key, Key encKey, String algorithm,
            AlgorithmParameterSpec spec, Provider provider, SecureRandom random) {
        if (key == null || encKey == null) {
            throw new IllegalArgumentException("key and encKey must be non-null");
        }
        throw new UnsupportedOperationException(NO_CIPHER);
    }

    /**
     * Decrypts the private key with a password.
     *
     * @param password the password
     * @return the private key
     * @throws GeneralSecurityException if it cannot be decrypted; in this library, always, because
     *     there is no provider offering password-based encryption
     * @throws NullPointerException if the password is {@code null}
     */
    public PrivateKey getKey(char[] password) throws GeneralSecurityException {
        if (password == null) {
            throw new NullPointerException("password must be non-null");
        }
        throw new NoSuchAlgorithmException(getAlgName() + " Cipher not available");
    }

    /**
     * Decrypts the private key with another key.
     *
     * @param decryptKey which key to decrypt it with
     * @param provider which provider to use, or {@code null} to search among all of them
     * @return the private key
     * @throws GeneralSecurityException if it cannot be decrypted
     * @throws NullPointerException if the key is {@code null}
     */
    public PrivateKey getKey(Key decryptKey, Provider provider) throws GeneralSecurityException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey must be non-null");
        }
        throw new NoSuchAlgorithmException(getAlgName() + " Cipher not available");
    }

    /**
     * Decrypts and returns the encoded private key, building the cipher itself.
     *
     * @param decryptKey which key to decrypt it with
     * @return the encoded private key
     * @throws NoSuchAlgorithmException if no provider has that cipher
     * @throws InvalidKeyException if the key is no good
     * @throws NullPointerException if the key is {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        return withCipher(cipherFor(decryptKey, (Provider) null), decryptKey);
    }

    /**
     * The same, with a named provider.
     *
     * @param decryptKey which key to decrypt it with
     * @param providerName the provider's name
     * @return the encoded private key
     * @throws NoSuchProviderException if there is no provider by that name
     * @throws NoSuchAlgorithmException if that provider does not have that cipher
     * @throws InvalidKeyException if the key is no good
     * @throws NullPointerException if the key or the name are {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey, String providerName)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException {
        if (providerName == null) {
            throw new NullPointerException("providerName must be non-null");
        }
        final Provider p = java.security.Security.getProvider(providerName);
        if (p == null) {
            throw new NoSuchProviderException("No such provider: " + providerName);
        }
        return getKeySpec(decryptKey, p);
    }

    /**
     * The same, with a provider.
     *
     * @param decryptKey which key to decrypt it with
     * @param provider the provider
     * @return the encoded private key
     * @throws NoSuchAlgorithmException if that provider does not have that cipher
     * @throws InvalidKeyException if the key is no good
     * @throws NullPointerException if the key or the provider are {@code null}
     */
    public PKCS8EncodedKeySpec getKeySpec(Key decryptKey, Provider provider)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (provider == null) {
            throw new NullPointerException("provider must be non-null");
        }
        return withCipher(cipherFor(decryptKey, provider), decryptKey);
    }

    /**
     * The encoding of this structure.
     *
     * <p>The algorithm's parameters are written back exactly as they arrived, byte for byte. It is
     * not laziness: interpreting them and assembling them again could change the encoding, and this
     * structure often travels signed.
     *
     * @return the bytes
     * @throws IOException if it cannot be encoded
     */
    public byte[] getEncoded() throws IOException {
        final byte[] idDer = Der.writeOid(this.oid);
        final int algLength = idDer.length + (this.paramsDer == null ? 0 : this.paramsDer.length);
        final byte[] alg = Der.wrap(0x30, join(idDer, this.paramsDer), algLength);
        final byte[] data = Der.wrap(0x04, this.encryptedData, this.encryptedData.length);
        return Der.wrap(0x30, join(alg, data), alg.length + data.length);
    }

    private static final String NO_CIPHER =
            "there is no encryption: no registered provider offers the Cipher service";

    private PKCS8EncodedKeySpec withCipher(Cipher c, Key decryptKey)
            throws NoSuchAlgorithmException, InvalidKeyException {
        c.init(Cipher.DECRYPT_MODE, decryptKey);
        try {
            return toKeySpec(c.doFinal(this.encryptedData));
        } catch (GeneralSecurityException e) {
            throw new InvalidKeyException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        }
    }

    private Cipher cipherFor(Key decryptKey, Provider provider)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (decryptKey == null) {
            throw new NullPointerException("decryptKey must be non-null");
        }
        try {
            return provider == null
                    ? Cipher.getInstance(getAlgName())
                    : Cipher.getInstance(getAlgName(), provider);
        } catch (NoSuchPaddingException e) {
            throw new NoSuchAlgorithmException(e.getMessage());
        }
    }

    /** What was decrypted has to be an encoded private key, or the key was wrong. */
    private static PKCS8EncodedKeySpec toKeySpec(byte[] plain) throws InvalidKeySpecException {
        try {
            final Der d = Der.read(plain, 0);
            if (d.tag != 0x30 || d.content + d.length != plain.length) {
                throw new InvalidKeySpecException("Cannot retrieve the PKCS8EncodedKeySpec");
            }
        } catch (IOException e) {
            throw new InvalidKeySpecException("Cannot retrieve the PKCS8EncodedKeySpec", e);
        }
        return new PKCS8EncodedKeySpec(plain);
    }

    private static void register(String name, String oid, boolean canonical) {
        NAME_TO_OID.put(name, oid);
        if (canonical) {
            OID_TO_NAME.put(oid, name);
        }
    }

    /** That name's identifier, or the name itself when it already is an identifier. */
    private static String toOid(String algName) throws NoSuchAlgorithmException {
        final String oid = NAME_TO_OID.get(algName);
        if (oid != null) {
            return oid;
        }
        if (isOid(algName)) {
            return algName;
        }
        throw new NoSuchAlgorithmException("unrecognized algorithm name: " + algName);
    }

    private static boolean isOid(String s) {
        if (s.isEmpty()) {
            return false;
        }
        final String[] parts = s.split("[.]", -1);
        if (parts.length < 2) {
            return false;
        }
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                return false;
            }
            for (int j = 0; j < parts[i].length(); j++) {
                final char c = parts[i].charAt(j);
                if (c < '0' || c > '9') {
                    return false;
                }
            }
        }
        return true;
    }

    private static byte[] join(byte[] a, byte[] b) {
        if (b == null) {
            return a;
        }
        final byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /**
     * The least DER this structure needs.
     *
     * <p>Definite forms only and three tags only: SEQUENCE, OBJECT IDENTIFIER and OCTET STRING. The
     * algorithm's parameters are not interpreted --they are copied-- so nothing more is needed.
     */
    private static final class Der {

        final int tag;
        final int length;
        final int content;

        private Der(int tag, int length, int content) {
            this.tag = tag;
            this.length = length;
            this.content = content;
        }

        /** Reads the tag and the length starting at that position. */
        static Der read(byte[] b, int from) throws IOException {
            if (from + 1 >= b.length) {
                throw new java.io.EOFException("it ended before the tag");
            }
            final int tag = b[from] & 0xff;
            int p = from + 1;
            int n = b[p] & 0xff;
            p++;
            if (n == 0x80) {
                throw new IOException("indefinite form: not DER");
            }
            if (n > 0x80) {
                final int octets = n - 0x80;
                if (octets > 4 || p + octets > b.length) {
                    throw new IOException("length out of range");
                }
                n = 0;
                for (int i = 0; i < octets; i++) {
                    n = (n << 8) | (b[p] & 0xff);
                    p++;
                }
                if (n < 0) {
                    throw new IOException("length out of range");
                }
            }
            if (p + n > b.length) {
                throw new java.io.EOFException("it ended before the content");
            }
            return new Der(tag, n, p);
        }

        /** The object identifier in that slice, with dots. */
        static String oid(byte[] b, int from, int length) throws IOException {
            if (length == 0) {
                throw new IOException("empty identifier");
            }
            final StringBuilder s = new StringBuilder();
            final int first = b[from] & 0xff;
            s.append(first / 40).append('.').append(first % 40);
            long v = 0;
            for (int i = from + 1; i < from + length; i++) {
                final int c = b[i] & 0xff;
                v = (v << 7) | (c & 0x7f);
                if ((c & 0x80) == 0) {
                    s.append('.').append(v);
                    v = 0;
                }
            }
            return s.toString();
        }

        /** Writes an object identifier given with dots. */
        static byte[] writeOid(String oid) {
            final String[] parts = oid.split("[.]", -1);
            final java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
            body.write(Integer.parseInt(parts[0]) * 40 + Integer.parseInt(parts[1]));
            for (int i = 2; i < parts.length; i++) {
                writeBase128(body, Long.parseLong(parts[i]));
            }
            final byte[] c = body.toByteArray();
            return wrap(0x06, c, c.length);
        }

        /** A value in base 128, with the top bit set except in the last octet. */
        private static void writeBase128(java.io.ByteArrayOutputStream out, long v) {
            int octets = 1;
            long t = v >>> 7;
            while (t != 0) {
                octets++;
                t = t >>> 7;
            }
            for (int i = octets - 1; i >= 0; i--) {
                int c = (int) ((v >>> (7 * i)) & 0x7f);
                if (i != 0) {
                    c = c | 0x80;
                }
                out.write(c);
            }
        }

        /** Wraps the first bytes of an array with that tag and its length. */
        static byte[] wrap(int tag, byte[] content, int length) {
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            out.write(tag);
            if (length < 128) {
                out.write(length);
            } else {
                int octets = 1;
                int t = length >>> 8;
                while (t != 0) {
                    octets++;
                    t = t >>> 8;
                }
                out.write(0x80 | octets);
                for (int i = octets - 1; i >= 0; i--) {
                    out.write((length >>> (8 * i)) & 0xff);
                }
            }
            out.write(content, 0, length);
            return out.toByteArray();
        }
    }
}
