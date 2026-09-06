package java.security;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;

/**
 * Reads keys and certificates written in PEM.
 *
 * <h2>What it returns</h2>
 *
 * <p>The object that matches the label: a certificate for {@code CERTIFICATE}, a public key for
 * {@code PUBLIC KEY}, an encrypted private key for {@code ENCRYPTED PRIVATE KEY}. A label it does
 * not know is not an error: it returns a {@link PEMRecord}, which keeps the text as it is. That is
 * what makes it possible to read a file with blocks of every kind without losing the ones that are
 * not understood.
 *
 * <p>The version taking a {@code Class} is for asking for one in particular. {@code decode(text,
 * PEMRecord.class)} returns the raw block even when the label is known, which is how a file is read
 * without being interpreted.
 *
 * <h2>What comes before the block</h2>
 *
 * <p>It is kept, in {@link PEMRecord#leadingData}. A PEM file usually carries comments or the output
 * of the tool that produced it before the {@code -----BEGIN}, and sometimes that is signed along
 * with the rest. Throwing it away would lose data.
 *
 * <h2>It is immutable</h2>
 *
 * <p>{@link #withFactory} and {@link #withDecryption} do not change this decoder: they return
 * another one.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>Reading the format -- finding the block, splitting off the label, decoding the base64 -- works
 * in full, and with it {@link PEMRecord} and {@code ENCRYPTED PRIVATE KEY}, which is the only label
 * whose object can be built without providers. The rest need a {@link KeyFactory} or a
 * {@link CertificateFactory}, and none is registered: the lookup happens all the same and what fails
 * is wrapped in {@link IllegalArgumentException}, which is what the JDK does when it finds nothing
 * to build with. {@link #withDecryption} also needs password-based encryption, which is not there
 * either.
 *
 * @since 25
 */
public final class PEMDecoder {

    private static final PEMDecoder ONLY = new PEMDecoder(null, null);

    /** The OIDs that can be named. One that is not here is not an error: it cannot be built. */
    private static final String[][] ALGORITHMS = {
        {"1.2.840.113549.1.1.1", "RSA"},
        {"1.2.840.113549.1.1.10", "RSASSA-PSS"},
        {"1.2.840.10040.4.1", "DSA"},
        {"1.2.840.10045.2.1", "EC"},
        {"1.2.840.113549.1.3.1", "DiffieHellman"},
        {"1.3.101.110", "X25519"},
        {"1.3.101.111", "X448"},
        {"1.3.101.112", "Ed25519"},
        {"1.3.101.113", "Ed448"},
    };

    private final Provider factory;
    private final char[] password;

    private PEMDecoder(Provider factory, char[] password) {
        this.factory = factory;
        this.password = password;
    }

    /**
     * The usual decoder.
     *
     * <p>It always returns the same object.
     *
     * @return the decoder
     */
    public static PEMDecoder of() {
        return ONLY;
    }

    /**
     * Reads the first block of that text.
     *
     * @param str the text
     * @return what the block said
     * @throws NullPointerException if the text is {@code null}
     * @throws IllegalArgumentException if there is no block, or if it cannot be built
     */
    public DEREncodable decode(String str) {
        if (str == null) {
            throw new NullPointerException("str");
        }
        return build(split(str.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * The same, reading from a stream.
     *
     * @param is where to read from
     * @return what the block said
     * @throws IOException if the read fails
     * @throws NullPointerException if the stream is {@code null}
     * @throws IllegalArgumentException if there is no block, or if it cannot be built
     */
    public DEREncodable decode(InputStream is) throws IOException {
        if (is == null) {
            throw new NullPointerException("is");
        }
        return build(split(readAll(is)));
    }

    /**
     * Reads the first block and returns it as that class.
     *
     * @param <S> what is expected
     * @param str the text
     * @param tClass what is expected
     * @return what the block said
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if there is no block, or if it cannot be built
     * @throws ClassCastException if what is there is not of that class
     */
    public <S extends DEREncodable> S decode(String str, Class<S> tClass) {
        if (tClass == null) {
            throw new NullPointerException("tClass");
        }
        if (str == null) {
            throw new NullPointerException("str");
        }
        return convert(split(str.getBytes(StandardCharsets.UTF_8)), tClass);
    }

    /**
     * The same, reading from a stream.
     *
     * @param <S> what is expected
     * @param is where to read from
     * @param tClass what is expected
     * @return what the block said
     * @throws IOException if the read fails
     * @throws NullPointerException if either of the two is {@code null}
     * @throws IllegalArgumentException if there is no block, or if it cannot be built
     * @throws ClassCastException if what is there is not of that class
     */
    public <S extends DEREncodable> S decode(InputStream is, Class<S> tClass) throws IOException {
        if (tClass == null) {
            throw new NullPointerException("tClass");
        }
        if (is == null) {
            throw new NullPointerException("is");
        }
        return convert(split(readAll(is)), tClass);
    }

    /**
     * Another decoder that builds with that provider's factories.
     *
     * @param provider the provider
     * @return the other decoder
     * @throws NullPointerException if the provider is {@code null}
     */
    public PEMDecoder withFactory(Provider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        return new PEMDecoder(provider, this.password);
    }

    /**
     * Another decoder that decrypts private keys with that password.
     *
     * @param password the password
     * @return the other decoder
     * @throws NullPointerException if the password is {@code null}
     */
    public PEMDecoder withDecryption(char[] password) {
        if (password == null) {
            throw new NullPointerException("password");
        }
        return new PEMDecoder(this.factory, password.clone());
    }

    /**
     * Finds the block and splits it into label, content and whatever came before.
     *
     * <p>The work is done on bytes and not on text because what comes before the block may not be
     * valid text, and it has to be given back as it was.
     */
    private static PEMRecord split(byte[] data) {
        final String text = new String(data, StandardCharsets.UTF_8);
        if (data.length == 0) {
            throw new IllegalArgumentException(new EOFException("No data available"));
        }
        final int start = text.indexOf("-----BEGIN ");
        if (start < 0) {
            throw new IllegalArgumentException(new EOFException("No PEM data found"));
        }
        final int labelEnd = text.indexOf("-----", start + 11);
        if (labelEnd < 0) {
            throw new IllegalArgumentException(new EOFException("No PEM data found"));
        }
        final String type = text.substring(start + 11, labelEnd);
        final String closing = "-----END " + type + "-----";
        final int end = text.indexOf(closing, labelEnd);
        if (end < 0) {
            throw new IllegalArgumentException(new EOFException("No PEM data found"));
        }
        final StringBuilder body = new StringBuilder();
        for (int i = labelEnd + 5; i < end; i++) {
            final char c = text.charAt(i);
            if (c != '\n' && c != '\r' && c != ' ' && c != '\t') {
                body.append(c);
            }
        }
        byte[] before = null;
        if (start > 0) {
            before = new byte[start];
            System.arraycopy(data, 0, before, 0, start);
        }
        return new PEMRecord(type, body.toString(), before);
    }

    /** The object matching the label, or the raw block when the label is not known. */
    private DEREncodable build(PEMRecord r) {
        final String type = r.type();
        if ("ENCRYPTED PRIVATE KEY".equals(type)) {
            try {
                return new EncryptedPrivateKeyInfo(content(r));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if ("CERTIFICATE".equals(type) || "X509 CERTIFICATE".equals(type)) {
            try {
                return (DEREncodable) certFactory().generateCertificate(
                        new java.io.ByteArrayInputStream(content(r)));
            } catch (java.security.cert.CertificateException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if ("X509 CRL".equals(type)) {
            try {
                return (DEREncodable) certFactory().generateCRL(
                        new java.io.ByteArrayInputStream(content(r)));
            } catch (java.security.cert.CertificateException e) {
                throw new IllegalArgumentException(e);
            } catch (java.security.cert.CRLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        if ("PUBLIC KEY".equals(type)) {
            return key(r, true);
        }
        if ("PRIVATE KEY".equals(type)) {
            return key(r, false);
        }
        return r;
    }

    /**
     * Builds the key with the factory of the algorithm the encoding names.
     *
     * <p>The algorithm does not come in the label -- the PEM says "PUBLIC KEY" and nothing else --
     * so it has to be taken from inside: it is the object identifier of the structure's first field.
     */
    private DEREncodable key(PEMRecord r, boolean isPublic) {
        final byte[] data = content(r);
        final String oid;
        try {
            oid = oidOf(data, isPublic);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    new IOException("No recognized algorithm detected in encoding"));
        }
        final String name = nameOf(oid);
        if (name == null) {
            throw new IllegalArgumentException(
                    new IOException("No recognized algorithm detected in encoding"));
        }
        try {
            final KeyFactory kf = this.factory == null
                    ? KeyFactory.getInstance(name)
                    : KeyFactory.getInstance(name, this.factory);
            return isPublic
                    ? (DEREncodable) kf.generatePublic(
                            new java.security.spec.X509EncodedKeySpec(data))
                    : (DEREncodable) kf.generatePrivate(
                            new java.security.spec.PKCS8EncodedKeySpec(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        } catch (java.security.spec.InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private CertificateFactory certFactory() throws java.security.cert.CertificateException {
        return this.factory == null
                ? CertificateFactory.getInstance("X.509")
                : CertificateFactory.getInstance("X.509", this.factory);
    }

    private <S extends DEREncodable> S convert(PEMRecord r, Class<S> tClass) {
        if (tClass == PEMRecord.class) {
            return tClass.cast(r);
        }
        return tClass.cast(build(r));
    }

    private static byte[] content(PEMRecord r) {
        try {
            return Base64.getDecoder().decode(r.content());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(new IOException("Invalid Base64 data"));
        }
    }

    private static String nameOf(String oid) {
        for (int i = 0; i < ALGORITHMS.length; i++) {
            if (ALGORITHMS[i][0].equals(oid)) {
                return ALGORITHMS[i][1];
            }
        }
        return null;
    }

    /**
     * The identifier of the algorithm inside the encoding.
     *
     * <p>A public key is {@code SEQUENCE { AlgorithmIdentifier, BIT STRING }} and a private one is
     * {@code SEQUENCE { INTEGER, AlgorithmIdentifier, OCTET STRING }}: the only difference for this
     * purpose is that in the private one the version number has to be skipped.
     */
    private static String oidOf(byte[] b, boolean isPublic) throws IOException {
        int p = contentOf(b, 0, 0x30);
        if (!isPublic) {
            final int[] version = tlv(b, p);
            if (version[0] != 0x02) {
                throw new IOException("the version is missing");
            }
            p = version[2] + version[1];
        }
        final int alg = contentOf(b, p, 0x30);
        final int[] id = tlv(b, alg);
        if (id[0] != 0x06) {
            throw new IOException("the algorithm identifier is missing");
        }
        return oid(b, id[2], id[1]);
    }

    /** Where the content of a TLV with that tag starting there begins. */
    private static int contentOf(byte[] b, int from, int tag) throws IOException {
        final int[] t = tlv(b, from);
        if (t[0] != tag) {
            throw new IOException("expected " + tag + " and found " + t[0]);
        }
        return t[2];
    }

    /** The tag, the length and where the content begins, for the TLV starting there. */
    private static int[] tlv(byte[] b, int from) throws IOException {
        if (from + 1 >= b.length) {
            throw new EOFException("it ended before the tag");
        }
        final int tag = b[from] & 0xff;
        int p = from + 1;
        int n = b[p] & 0xff;
        p++;
        if (n >= 0x80) {
            final int octets = n - 0x80;
            if (octets == 0 || octets > 4 || p + octets > b.length) {
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
            throw new EOFException("it ended before the content");
        }
        return new int[] {tag, n, p};
    }

    private static String oid(byte[] b, int from, int length) throws IOException {
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

    private static byte[] readAll(InputStream is) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[4096];
        int n = is.read(buf);
        while (n > 0) {
            out.write(buf, 0, n);
            n = is.read(buf);
        }
        return out.toByteArray();
    }
}
