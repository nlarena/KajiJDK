package java.security;

import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;

// Firma digital: producirla y verificarla.
//
// ===============================================================================================
// EL CONTRATO QUE HAY QUE LEER DOS VECES
// ===============================================================================================
//
// `verify()` **devuelve un boolean**, al reves que `Certificate.verify()`, que no devuelve nada y
// lanza si falla. La diferencia es deliberada y es donde se cometen los errores: aca, una firma
// invalida **no lanza ninguna excepcion**, devuelve `false`. El codigo que hace
//
//     s.initVerify(k); s.update(datos); s.verify(firma);
//
// sin mirar el resultado acepta cualquier firma. La excepcion se reserva para "el objeto no estaba
// inicializado" o "el proveedor se rompio", que son errores del programa, no del atacante.
//
// El otro punto es el orden: hay que inicializar **antes** de alimentar datos, y cada `sign()` o
// `verify()` deja el objeto listo para otra operacion con la misma clave. Alimentar datos sin
// inicializar tira `SignatureException`, que es lo correcto: firmar con estado indefinido daria una
// firma sin significado.
//
// ===============================================================================================
// POR QUE NO HAY NINGUN ALGORITMO, Y POR QUE NO HAY SecureRandom
// ===============================================================================================
//
// **No hay ningun proveedor de `Signature` registrado**, asi que las tres sobrecargas de
// `getInstance` tiran siempre `NoSuchAlgorithmException`. Es la decision central de todo este
// paquete: un `Signature.verify()` que devuelva true sin verificar no es deuda tecnica, es un
// agujero, y la unica forma de no tenerlo es no ofrecer el algoritmo. La estructura entera esta
// —`SignatureSpi` es la interfaz completa— asi que un proveedor que sepa RSA o ECDSA se enchufa y
// todo lo demas funciona.
//
// `initSign(PrivateKey, SecureRandom)` si esta, y la fuente que recibe importa mas de lo que parece:
// es de donde sale el nonce de DSA y ECDSA. Ver `SignatureSpi` para que pasa si ese nonce se repite.
public abstract class Signature extends SignatureSpi {

    // Todavia no se dijo si se firma o se verifica.
    protected static final int UNINITIALIZED = 0;

    // Listo para firmar.
    protected static final int SIGN = 2;

    // Listo para verificar.
    protected static final int VERIFY = 3;

    // En cual de los tres estados esta. Es `protected` y no privado porque las subclases directas
    // —las que escribe un proveedor sin pasar por un SPI aparte— lo miran.
    protected int state = UNINITIALIZED;

    private final String algorithm;

    // El proveedor solo lo tiene la instancia que sale de `getInstance`; una subclase escrita a mano
    // no tiene ninguno.
    Provider provider;

    protected Signature(String algorithm) {
        this.algorithm = algorithm;
    }

    public static Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("Signature", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " Signature not available");
    }

    public static Signature getInstance(String algorithm, String provider)
            throws NoSuchAlgorithmException, NoSuchProviderException {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("missing provider");
        }
        Provider p = Security.getProvider(provider);
        if (p == null) {
            throw new NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(algorithm, p);
    }

    public static Signature getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("Signature", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static Signature armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof SignatureSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for Signature is not a SignatureSpi: " + s.getClassName());
        }
        SignatureDelegada d = new SignatureDelegada((SignatureSpi) o, algorithm);
        d.provider = s.getProvider();
        return d;
    }

    // El proveedor que resolvio el algoritmo, o null si esta instancia no salio de `getInstance`.
    public final Provider getProvider() {
        return this.provider;
    }

    // Prepara para verificar con esta clave publica. Descarta cualquier dato que se hubiera
    // alimentado antes.
    public final void initVerify(PublicKey publicKey) throws InvalidKeyException {
        this.engineInitVerify(publicKey);
        this.state = VERIFY;
    }

    // Prepara para verificar con la clave que trae un certificado.
    //
    // No es solo un atajo: comprueba la extension KeyUsage antes de aceptar la clave. Si el
    // certificado dice que su clave no sirve para firmar —el bit 0, digitalSignature, apagado— se
    // rechaza. Sin eso, un certificado emitido para cifrar podria usarse para validar firmas, que es
    // exactamente lo que KeyUsage existe para impedir.
    //
    // El detalle que importa, y que es facil de leer al reves: **solo se mira si la extension viene
    // marcada como critica**. Es lo que hace el JDK y es coherente con el modelo de X.509 —una
    // extension no critica es una recomendacion que quien no la entienda puede ignorar— pero deja
    // pasar certificados con un KeyUsage no critico que dice que no. Quien necesite la regla
    // estricta tiene que mirar `getKeyUsage()` el mismo.
    public final void initVerify(java.security.cert.Certificate certificate)
            throws InvalidKeyException {
        if (certificate instanceof java.security.cert.X509Certificate) {
            java.security.cert.X509Certificate cert =
                (java.security.cert.X509Certificate) certificate;
            java.util.Set<String> criticas = cert.getCriticalExtensionOIDs();
            if (criticas != null && !criticas.isEmpty() && criticas.contains("2.5.29.15")) {
                boolean[] usos = cert.getKeyUsage();
                if (usos != null && !usos[0]) {
                    throw new InvalidKeyException("Wrong key usage");
                }
            }
        }
        PublicKey publicKey = certificate.getPublicKey();
        this.engineInitVerify(publicKey);
        this.state = VERIFY;
    }

    // Prepara para firmar con esta clave privada.
    public final void initSign(PrivateKey privateKey) throws InvalidKeyException {
        this.engineInitSign(privateKey);
        this.state = SIGN;
    }

    /**
     * Idem, diciendo de donde sale el azar.
     *
     * <p>La fuente es la del nonce por firma. Ver {@link SignatureSpi} para por que un nonce
     * repetido en ECDSA revela la clave privada.
     */
    public final void initSign(PrivateKey privateKey, SecureRandom random)
            throws InvalidKeyException {
        this.engineInitSign(privateKey, random);
        this.state = SIGN;
    }

    // Cierra la operacion y devuelve la firma. El objeto queda listo para firmar de nuevo con la
    // misma clave.
    public final byte[] sign() throws SignatureException {
        if (this.state == SIGN) {
            return this.engineSign();
        }
        throw new SignatureException("object not initialized for signing");
    }

    // Igual, dejando la firma en un buffer. Devuelve cuantos bytes ocupo.
    public final int sign(byte[] outbuf, int offset, int len) throws SignatureException {
        if (this.state != SIGN) {
            throw new SignatureException("object not initialized for signing");
        }
        if (outbuf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (offset < 0 || len < 0) {
            throw new IllegalArgumentException("offset or len is less than 0");
        }
        if (outbuf.length - offset < len) {
            throw new IllegalArgumentException(
                "Output buffer too small for specified offset and length");
        }
        return this.engineSign(outbuf, offset, len);
    }

    // Verifica. **Devuelve false si la firma no vale; no lanza.** Ver la nota de la clase.
    public final boolean verify(byte[] signature) throws SignatureException {
        if (this.state == VERIFY) {
            return this.engineVerify(signature);
        }
        throw new SignatureException("object not initialized for verification");
    }

    public final boolean verify(byte[] signature, int offset, int length)
            throws SignatureException {
        if (this.state != VERIFY) {
            throw new SignatureException("object not initialized for verification");
        }
        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("offset or length is less than 0");
        }
        if (signature.length - offset < length) {
            throw new IllegalArgumentException(
                "signature too small for specified offset and length");
        }
        return this.engineVerify(signature, offset, length);
    }

    public final void update(byte b) throws SignatureException {
        if (this.state == VERIFY || this.state == SIGN) {
            this.engineUpdate(b);
        } else {
            throw new SignatureException("object not initialized for signature or verification");
        }
    }

    public final void update(byte[] data) throws SignatureException {
        if (this.state == SIGN || this.state == VERIFY) {
            if (data == null) {
                throw new IllegalArgumentException("data is null");
            }
            this.engineUpdate(data, 0, data.length);
        } else {
            throw new SignatureException("object not initialized for signature or verification");
        }
    }

    public final void update(byte[] data, int off, int len) throws SignatureException {
        if (this.state == SIGN || this.state == VERIFY) {
            if (data == null) {
                throw new IllegalArgumentException("data is null");
            }
            if (off < 0 || len < 0) {
                throw new IllegalArgumentException("off or len is less than 0");
            }
            if (data.length - off < len) {
                throw new IllegalArgumentException(
                    "data too small for specified offset and length");
            }
            this.engineUpdate(data, off, len);
        } else {
            throw new SignatureException("object not initialized for signature or verification");
        }
    }

    // Alimenta desde un buffer. Lo consume entero: al volver, `position` quedo en `limit`.
    public final void update(ByteBuffer data) throws SignatureException {
        if (this.state != SIGN && this.state != VERIFY) {
            throw new SignatureException("object not initialized for signature or verification");
        }
        if (data == null) {
            throw new NullPointerException();
        }
        this.engineUpdate(data);
    }

    // El nombre del algoritmo: "SHA256withRSA".
    public final String getAlgorithm() {
        return this.algorithm;
    }

    @Override
    public String toString() {
        String estado = "";
        if (this.state == UNINITIALIZED) {
            estado = "<not initialized>";
        } else if (this.state == VERIFY) {
            estado = "<initialized for verifying>";
        } else if (this.state == SIGN) {
            estado = "<initialized for signing>";
        }
        return "Signature object: " + this.getAlgorithm() + estado;
    }

    // Parametros por nombre. Desaconsejado desde el JDK 1.2: los nombres nunca se estandarizaron,
    // asi que el mismo string significaba cosas distintas segun el proveedor.
    public final void setParameter(String param, Object value) throws InvalidParameterException {
        this.engineSetParameter(param, value);
    }

    // La forma buena de pasar parametros. Para RSASSA-PSS no es opcional: sin `PSSParameterSpec`,
    // el hash y el largo de sal quedan a criterio del proveedor y la firma no verifica del otro
    // lado.
    public final void setParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        this.engineSetParameter(params);
    }

    // Los parametros efectivos, incluidos los que el proveedor eligio solo. Sirve para averiguar
    // que se uso realmente y poder repetirlo del otro lado.
    public final AlgorithmParameters getParameters() {
        return this.engineGetParameters();
    }

    public final Object getParameter(String param) throws InvalidParameterException {
        return this.engineGetParameter(param);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
