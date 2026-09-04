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
 * Firma archivos JAR.
 *
 * <p>Es inmutable y se arma con {@link Builder}. Que sea inmutable es lo que la hace reusable: un
 * mismo `JarSigner` firma muchos JAR, y firmar no cambia nada de el.
 *
 * <p>Firmar un JAR son tres archivos en `META-INF/`: el manifiesto con un resumen por entrada, un
 * `.SF` con un resumen del manifiesto, y un `.DSA`/`.RSA`/`.EC` con la firma PKCS#7 del `.SF` mas
 * la cadena de certificados. Verificar es rehacer los resumenes y comprobar la firma; por eso
 * agregar entradas a un JAR firmado lo invalida y quitar la carpeta `META-INF/` lo "desfirma"
 * sin dejar rastro.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #sign} no esta implementado. Producir la firma pide PKCS#7 --codificacion DER,
 * `SignerInfo`, `ContentInfo`, atributos autenticados-- y, si hay TSA, ademas el protocolo RFC 3161
 * por HTTP contra un tercero. Nada de eso esta en esta biblioteca.
 *
 * <p>Todo lo demas si: el {@link Builder} valida los algoritmos de verdad contra los proveedores
 * instalados, y los seis metodos de consulta devuelven lo que se configuro. Un `JarSigner` mal
 * armado falla aca, en la linea que lo arma, y no al firmar.
 *
 * <p>{@link #sign} lanza {@link JarSignerException} --que es lo que ya declara para cualquier
 * fallo-- con el motivo adentro. No escribe nada en la salida antes de lanzar.
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
     * Firma el JAB de entrada y escribe el firmado en la salida.
     *
     * <p><b>No implementado en esta biblioteca.</b> Ver la nota de la clase: falta PKCS#7 y, con
     * TSA, RFC 3161. Lanza sin escribir nada en `os`.
     *
     * @param file el JAR a firmar
     * @param os donde escribir el JAR firmado
     * @throws JarSignerException siempre, en esta biblioteca
     * @throws NullPointerException si alguno de los dos es nulo
     */
    public void sign(ZipFile file, OutputStream os) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(os);
        throw new JarSignerException(
                "cannot sign " + file.getName() + ": no PKCS#7 signature generation in this library",
                new UnsupportedOperationException("PKCS#7 SignedData is not implemented"));
    }

    /** El algoritmo de resumen con el que se firmaria. */
    public String getDigestAlgorithm() {
        return this.digestalg[0];
    }

    /** El algoritmo de firma. */
    public String getSignatureAlgorithm() {
        return this.sigalg;
    }

    /** La TSA con la que se sellaria el tiempo, o `null` si no hay. */
    public URI getTsa() {
        return this.tsaUrl;
    }

    /** El nombre del firmante: el que llevan los archivos `META-INF/<nombre>.SF` y `.DSA`. */
    public String getSignerName() {
        return this.signerName;
    }

    /**
     * Una de las propiedades adicionales.
     *
     * <p>Las reconocidas son {@code tsaDigestAlg}, {@code tsaPolicyId}, {@code internalsf} y
     * {@code sectionsonly}. Cualquier otra clave es un error y no un `null`: pedir una propiedad
     * que no existe casi siempre es un nombre mal escrito, y devolver `null` lo taparia.
     *
     * @throws UnsupportedOperationException si la clave no es una de las cuatro
     * @throws NullPointerException si la clave es nula
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
     * El armador de {@link JarSigner}.
     *
     * <p>Cada metodo valida en el acto y devuelve el mismo armador, para encadenar. Validar
     * temprano es el punto: un algoritmo mal escrito falla en la linea que lo nombra y no adentro
     * de `sign`, donde el mensaje no diria de donde salio.
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
         * Un armador con la clave y la cadena de esa entrada de almacen.
         *
         * @throws IllegalArgumentException si la cadena no es de certificados X.509
         * @throws NullPointerException si la entrada es nula
         */
        public Builder(KeyStore.PrivateKeyEntry entry) {
            Objects.requireNonNull(entry);
            this.privateKey = entry.getPrivateKey();
            Certificate[] cadena = entry.getCertificateChain();
            this.certChain = aX509(cadena);
        }

        /**
         * Un armador con esa clave privada y esa cadena de certificacion.
         *
         * @throws IllegalArgumentException si la cadena no es de certificados X.509, o si esta
         *     vacia
         * @throws NullPointerException si alguno de los dos es nulo
         */
        public Builder(PrivateKey privateKey, CertPath certPath) {
            Objects.requireNonNull(privateKey);
            Objects.requireNonNull(certPath);
            List<? extends Certificate> lista = certPath.getCertificates();
            if (lista.isEmpty()) {
                throw new IllegalArgumentException("empty certPath");
            }
            Certificate[] cadena = new Certificate[lista.size()];
            for (int i = 0; i < cadena.length; i++) {
                cadena[i] = lista.get(i);
            }
            this.privateKey = privateKey;
            this.certChain = aX509(cadena);
        }

        /** La cadena, comprobando que sea de X.509: un JAR firmado no admite otra cosa. */
        private static X509Certificate[] aX509(Certificate[] cadena) {
            Objects.requireNonNull(cadena);
            X509Certificate[] out = new X509Certificate[cadena.length];
            for (int i = 0; i < cadena.length; i++) {
                if (!(cadena[i] instanceof X509Certificate)) {
                    throw new IllegalArgumentException("Only X.509 certificates are supported");
                }
                out[i] = (X509Certificate) cadena[i];
            }
            return out;
        }

        /**
         * El algoritmo de resumen de las entradas.
         *
         * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
         */
        public Builder digestAlgorithm(String algorithm) throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm);
            MessageDigest.getInstance(algorithm);
            this.digestalg = new String[] {algorithm};
            this.digestProvider = null;
            return this;
        }

        /**
         * El algoritmo de resumen, de ese proveedor.
         *
         * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
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
         * El algoritmo de firma.
         *
         * @throws NoSuchAlgorithmException si ningun proveedor lo tiene
         */
        public Builder signatureAlgorithm(String algorithm) throws NoSuchAlgorithmException {
            Objects.requireNonNull(algorithm);
            java.security.Signature.getInstance(algorithm);
            this.sigalg = algorithm;
            this.sigProvider = null;
            return this;
        }

        /**
         * El algoritmo de firma, de ese proveedor.
         *
         * @throws NoSuchAlgorithmException si ese proveedor no lo tiene
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

        /** La autoridad de sellado de tiempo, o `null` para no sellar. */
        public Builder tsa(URI uri) {
            this.tsaUrl = uri;
            return this;
        }

        /**
         * El nombre del firmante: el que llevan `META-INF/<nombre>.SF` y el bloque de firma.
         *
         * @throws IllegalArgumentException si esta vacio, o si tiene caracteres que no pueden
         *     estar en un nombre de entrada de JAR
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

        /** Un receptor de los avisos de progreso, en pares (accion, entrada). */
        public Builder eventHandler(BiConsumer<String, String> handler) {
            Objects.requireNonNull(handler);
            this.handler = handler;
            return this;
        }

        /**
         * Una propiedad adicional.
         *
         * <p>Las claves --sin distinguir mayusculas-- son {@code tsadigestalg},
         * {@code tsapolicyid}, {@code internalsf} y {@code sectionsonly}.
         *
         * @throws UnsupportedOperationException si la clave no es una de las cuatro
         * @throws IllegalArgumentException si el valor no sirve para esa clave
         * @throws NoSuchAlgorithmException nunca se declara: un algoritmo desconocido en
         *     {@code tsadigestalg} sale como {@link IllegalArgumentException}, igual que en el JDK
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

        /** `"true"`/`"false"` y nada mas: un valor raro es un error y no un `false`. */
        private static boolean parseBoolean(String key, String value) {
            if (value.equals("true")) {
                return true;
            }
            if (value.equals("false")) {
                return false;
            }
            throw new IllegalArgumentException("Invalid " + key + " value: " + value);
        }

        /** El algoritmo de resumen que se usa cuando no se pide otro. */
        public static String getDefaultDigestAlgorithm() {
            return "SHA-384";
        }

        /**
         * El algoritmo de firma que corresponde a esa clave, o `null` si no se sabe.
         *
         * <p>`null` y no una excepcion: el JDK deja que un proveedor con un tipo de clave que
         * nadie conoce se firme igual, nombrando el algoritmo a mano.
         *
         * @throws NullPointerException si la clave es nula
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

        /** El {@link JarSigner} con lo configurado hasta aca. */
        public JarSigner build() {
            return new JarSigner(this);
        }
    }
}
