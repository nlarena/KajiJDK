package jdk.security.jarsigner;

import java.io.OutputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.zip.ZipFile;

/**
 * It signs JAR files.
 *
 * <p>It is immutable and is built with {@link Builder}. That it is immutable is what makes it
 * reusable: one same `JarSigner` signs many JARs, and signing changes nothing of it.
 *
 * <p>Signing a JAR is three files in `META-INF/`: the manifest with a digest per entry, a `.SF` with
 * a digest of the manifest, and a `.DSA`/`.RSA`/`.EC` with the PKCS#7 signature of the `.SF` plus
 * the chain of certificates. Verifying is redoing the digests and checking the signature; that is
 * why adding entries to a signed JAR invalidates it and removing the `META-INF/` folder "unsigns"
 * it without leaving a trace.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #sign} is not implemented. Producing the signature asks for PKCS#7 --DER encoding,
 * `SignerInfo`, `ContentInfo`, authenticated attributes-- and, if there is a TSA, also the RFC 3161
 * protocol over HTTP against a third party. None of that is in this library.
 *
 * <p>Everything else is: the {@link Builder} really validates the algorithms against the installed
 * providers, and the six query methods return what was configured. A badly built `JarSigner` fails
 * here, in the line that builds it, and not when signing.
 *
 * <p>{@link #sign} throws {@link JarSignerException} --which is what it already declares for any
 * failure-- with the reason inside. It writes nothing into the output before throwing.
 *
 * @since 9
 */
public final class JarSigner {

    private final PrivateKey privateKey;
    private final X509Certificate[] certChain;
    private final String[] digestalg;
    private final String sigalg;
    private final Provider digestProvider;
    private final Provider sigProvider;
    private final URI tsaUrl;
    private final String signerName;
    private final BiConsumer<String, String> handler;
    private final String tSAPolicyID;
    private final String tSADigestAlg;
    private final boolean sectionsonly;
    private final boolean internalsf;

    private JarSigner(Builder b) {
        this.privateKey = b.privateKey;
        this.certChain = b.certChain;
        this.digestalg = b.digestalg != null ? b.digestalg
                : new String[] {Builder.getDefaultDigestAlgorithm()};
        this.sigalg = b.sigalg != null ? b.sigalg
                : Builder.getDefaultSignatureAlgorithm(b.privateKey);
        this.digestProvider = b.digestProvider;
        this.sigProvider = b.sigProvider;
        this.tsaUrl = b.tsaUrl;
        this.signerName = b.signerName != null ? b.signerName : "SIGNER";
        this.handler = b.handler;
        this.tSAPolicyID = b.tSAPolicyID;
        this.tSADigestAlg = b.tSADigestAlg != null ? b.tSADigestAlg
                : Builder.getDefaultDigestAlgorithm();
        this.sectionsonly = b.sectionsonly;
        this.internalsf = b.internalsf;
    }

    /**
     * It signs the input JAR and writes the signed one into the output.
     *
     * <p><b>Not implemented in this library.</b> See the note of the class: PKCS#7 is missing and, with
     * a TSA, RFC 3161. It throws without writing anything into `os`.
     *
     * @param file the JAR to sign
     * @param os where to write the signed JAR
     * @throws JarSignerException always, in this library
     * @throws NullPointerException if either of the two is null
     */
    public void sign(ZipFile file, OutputStream os) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(os);
        throw new JarSignerException(
                "cannot sign " + file.getName() + ": no PKCS#7 signature generation in this library",
                new UnsupportedOperationException("PKCS#7 SignedData is not implemented"));
    }

    /** The digest algorithm it would sign with. */
    public String getDigestAlgorithm() {
        return this.digestalg[0];
    }

    /** The signature algorithm. */
    public String getSignatureAlgorithm() {
        return this.sigalg;
    }

    /** The TSA the time would be stamped with, or `null` if there is none. */
    public URI getTsa() {
        return this.tsaUrl;
    }

    /** The name of the signer: the one the files `META-INF/<name>.SF` and `.DSA` carry. */
    public String getSignerName() {
        return this.signerName;
    }

    /**
     * One of the additional properties.
     *
     * <p>The recognised ones are {@code tsaDigestAlg}, {@code tsaPolicyId}, {@code internalsf} and
     * {@code sectionsonly}. Any other key is an error and not a `null`: asking for a property that does
     * not exist is almost always a badly written name, and returning `null` would cover it up.
     *
     * @throws UnsupportedOperationException if the key is not one of the four
     * @throws NullPointerException if the key is null
     */
    public String getProperty(String key) {
        Objects.requireNonNull(key);
        if (key.equals("tsaDigestAlg")) {
            return this.tSADigestAlg;
        }
        if (key.equals("tsaPolicyId")) {
            return this.tSAPolicyID;
        }
        if (key.equals("internalsf")) {
            return Boolean.toString(this.internalsf);
        }
        if (key.equals("sectionsonly")) {
            return Boolean.toString(this.sectionsonly);
        }
        throw new UnsupportedOperationException("Unsupported key " + key);
    }

    /**
     * The builder of {@link JarSigner}.
     *
     * <p>Each method validates on the spot and returns the same builder, for chaining. Validating early
     * is the point: a badly written algorithm fails in the line that names it and not inside `sign`,
     * where the message would not say where it came from.
     */
    public static class Builder {

        final PrivateKey privateKey;
        final X509Certificate[] certChain;
        String[] digestalg;
        String sigalg;
        Provider digestProvider;
        Provider sigProvider;
        URI tsaUrl;
        String signerName;
        BiConsumer<String, String> handler;
        String tSAPolicyID;
        String tSADigestAlg;
        boolean sectionsonly = false;
        boolean internalsf = false;

        /**
         * A builder with the key and the chain of that store entry.
         *
         * @throws IllegalArgumentException if the chain is not of X.509 certificates
         * @throws NullPointerException if the entry is null
         */
        public Builder(KeyStore.PrivateKeyEntry entry) {
            Objects.requireNonNull(entry);
            this.privateKey = entry.getPrivateKey();
            Certificate[] chain = entry.getCertificateChain();
            this.certChain = toX509(chain);
        }

        /**
         * A builder with that private key and that certification chain.
         *
         * @throws IllegalArgumentException if the chain is not of X.509 certificates, or if it is empty
         * @throws NullPointerException if either of the two is null
         */
        public Builder(PrivateKey privateKey, CertPath certPath) {
            Objects.requireNonNull(privateKey);
            Objects.requireNonNull(certPath);
            List<? extends Certificate> list = certPath.getCertificates();
            if (list.isEmpty()) {
                throw new IllegalArgumentException("empty certPath");
            }
            Certificate[] chain = new Certificate[list.size()];
            for (int i = 0; i < chain.length; i++) {
                chain[i] = list.get(i);
            }
            this.privateKey = privateKey;
            this.certChain = toX509(chain);
        }

        /** The chain, checking that it is of X.509: a signed JAR admits nothing else. */
        private static X509Certificate[] toX509(Certificate[] chain) {
            Objects.requireNonNull(chain);
            X509Certificate[] out = new X509Certificate[chain.length];
            for (int i = 0; i < chain.length; i++) {
                if (!(chain[i] instanceof X509Certificate)) {
                    throw new IllegalArgumentException("Only X.509 certificates are supported");
                }
                out[i] = (X509Certificate) chain[i];
            }
            return out;
        }

        /**
         * The digest algorithm of the entries.
         *
         * @throws NoSuchAlgorithmException if no provider has it
         */
        public Builder digestAlgorithm(String algorithm) throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm);
            MessageDigest.getInstance(algorithm);
            this.digestalg = new String[] {algorithm};
            this.digestProvider = null;
            return this;
        }

        /**
         * The digest algorithm, of that provider.
         *
         * @throws NoSuchAlgorithmException if that provider does not have it
         */
        public Builder digestAlgorithm(String algorithm, Provider provider)
                throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm);
            Objects.requireNonNull(provider);
            MessageDigest.getInstance(algorithm, provider);
            this.digestalg = new String[] {algorithm};
            this.digestProvider = provider;
            return this;
        }

        /**
         * The signature algorithm.
         *
         * @throws NoSuchAlgorithmException if no provider has it
         */
        public Builder signatureAlgorithm(String algorithm) throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm);
            java.security.Signature.getInstance(algorithm);
            this.sigalg = algorithm;
            this.sigProvider = null;
            return this;
        }

        /**
         * The signature algorithm, of that provider.
         *
         * @throws NoSuchAlgorithmException if that provider does not have it
         */
        public Builder signatureAlgorithm(String algorithm, Provider provider)
                throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm);
            Objects.requireNonNull(provider);
            java.security.Signature.getInstance(algorithm, provider);
            this.sigalg = algorithm;
            this.sigProvider = provider;
            return this;
        }

        /** The time stamping authority, or `null` for not stamping. */
        public Builder tsa(URI uri) {
            this.tsaUrl = uri;
            return this;
        }

        /**
         * The name of the signer: the one `META-INF/<name>.SF` and the signature block carry.
         *
         * @throws IllegalArgumentException if it is empty, or if it has characters that cannot be in the
         *     name of a JAR entry
         */
        public Builder signerName(String name) {
            Objects.requireNonNull(name);
            if (name.isEmpty() || name.length() > 8) {
                throw new IllegalArgumentException("Name too long");
            }
            String mayus = name.toUpperCase(Locale.ENGLISH);
            for (int i = 0; i < mayus.length(); i++) {
                char c = mayus.charAt(i);
                boolean ok = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                        || c == '-' || c == '_';
                if (!ok) {
                    throw new IllegalArgumentException("Invalid characters in name");
                }
            }
            this.signerName = mayus;
            return this;
        }

        /** A receiver of the progress notices, in pairs (action, entry). */
        public Builder eventHandler(BiConsumer<String, String> handler) {
            Objects.requireNonNull(handler);
            this.handler = handler;
            return this;
        }

        /**
         * An additional property.
         *
         * <p>The keys --case-insensitive-- are {@code tsadigestalg}, {@code tsapolicyid},
         * {@code internalsf} and {@code sectionsonly}.
         *
         * @throws UnsupportedOperationException if the key is not one of the four
         * @throws IllegalArgumentException if the value does not serve for that key
         * @throws NoSuchAlgorithmException it is never declared: an unknown algorithm in
         *     {@code tsadigestalg} comes out as {@link IllegalArgumentException}, just as in the JDK
         */
        public Builder setProperty(String key, String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            String k = key.toLowerCase(Locale.ENGLISH);
            if (k.equals("tsadigestalg")) {
                try {
                    MessageDigest.getInstance(value);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException(e);
                }
                this.tSADigestAlg = value;
                return this;
            }
            if (k.equals("tsapolicyid")) {
                this.tSAPolicyID = value;
                return this;
            }
            if (k.equals("internalsf")) {
                this.internalsf = parseBoolean(key, value);
                return this;
            }
            if (k.equals("sectionsonly")) {
                this.sectionsonly = parseBoolean(key, value);
                return this;
            }
            throw new UnsupportedOperationException("Unsupported key " + key);
        }

        /** `"true"`/`"false"` and nothing else: an odd value is an error and not a `false`. */
        private static boolean parseBoolean(String key, String value) {
            if (value.equals("true")) {
                return true;
            }
            if (value.equals("false")) {
                return false;
            }
            throw new IllegalArgumentException("Invalid " + key + " value: " + value);
        }

        /** The digest algorithm that is used when no other is asked for. */
        public static String getDefaultDigestAlgorithm() {
            return "SHA-384";
        }

        /**
         * The signature algorithm that corresponds to that key, or `null` if it is not known.
         *
         * <p>`null` and not an exception: the JDK lets a provider with a kind of key nobody knows sign all
         * the same, naming the algorithm by hand.
         *
         * @throws NullPointerException if the key is null
         */
        public static String getDefaultSignatureAlgorithm(PrivateKey key) {
            Objects.requireNonNull(key);
            String alg = key.getAlgorithm();
            if (alg == null) {
                return null;
            }
            if (alg.equals("RSA")) {
                return "SHA384withRSA";
            }
            if (alg.equals("DSA")) {
                return "SHA384withDSA";
            }
            if (alg.equals("EC")) {
                return "SHA384withECDSA";
            }
            if (alg.equals("RSASSA-PSS")) {
                return "RSASSA-PSS";
            }
            if (alg.equals("Ed25519") || alg.equals("Ed448") || alg.equals("EdDSA")) {
                return alg;
            }
            return null;
        }

        /** The {@link JarSigner} with what has been configured so far. */
        public JarSigner build() {
            return new JarSigner(this);
        }
    }
}
