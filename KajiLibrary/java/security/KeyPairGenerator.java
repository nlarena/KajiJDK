package java.security;

// Genera pares de claves publica/privada.
//
// Igual que `Signature`, extiende a su propio SPI en vez de contenerlo: es una rareza historica del
// API que permitia que un proveedor escribiera una subclase directa.
//
// A KajiLibrary subset: **ningun proveedor registra este servicio**, asi que las tres sobrecargas
// de `getInstance` tiran siempre `NoSuchAlgorithmException`. La clase esta entera igual, porque su
// forma es la que tiene que cumplir cualquier proveedor que se agregue despues.
//
// Los cuatro `initialize` vienen en pares: uno con fuente de azar explicita y otro sin ella. El que
// no la recibe usa el generador por omision --el del sistema operativo-- y no una fuente inventada;
// ver `KeyPairGeneratorSpi` para por que esa distincion es la que mas importa de toda la clase.
public abstract class KeyPairGenerator extends KeyPairGeneratorSpi {

    private final String algorithm;

    Provider provider;

    protected KeyPairGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return this.algorithm;
    }

    public static KeyPairGenerator getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("KeyPairGenerator", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyPairGenerator not available");
    }

    public static KeyPairGenerator getInstance(String algorithm, String provider)
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

    public static KeyPairGenerator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("KeyPairGenerator", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static KeyPairGenerator armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof KeyPairGeneratorSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for KeyPairGenerator is not a KeyPairGeneratorSpi: "
                + s.getClassName());
        }
        KeyPairGeneradorDelegado d =
            new KeyPairGeneradorDelegado((KeyPairGeneratorSpi) o, algorithm);
        d.provider = s.getProvider();
        return d;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // El alias historico de `generateKeyPair()`. Existen los dos porque uno se agrego en 1.1 y el
    // otro en 1.2, y ninguno se pudo sacar.
    public final KeyPair genKeyPair() {
        return this.generateKeyPair();
    }

    /**
     * Configura el tamaño de clave, con el generador de azar por omision.
     *
     * <p>"Por omision" no quiere decir "sin azar": es el del sistema operativo. Ver
     * {@link SecureRandom}.
     */
    public void initialize(int keysize) {
        this.initialize(keysize, new SecureRandom());
    }

    /**
     * Idem, diciendo de donde sale el azar.
     *
     * <p>La implementacion base no hace nada, igual que en el JDK: quien llega aca es un proveedor
     * que escribio una subclase de {@code KeyPairGenerator} y decidio no aceptar configuracion, y
     * en ese caso genera con sus valores por omision.
     */
    @Override
    public void initialize(int keysize, SecureRandom random) {
    }

    /**
     * Configura con parametros concretos -- una curva, un grupo -- y el generador por omision.
     *
     * @throws InvalidAlgorithmParameterException si el proveedor no los entiende
     */
    public void initialize(java.security.spec.AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        this.initialize(params, new SecureRandom());
    }

    /** Idem, diciendo de donde sale el azar. */
    @Override
    public void initialize(java.security.spec.AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        super.initialize(params, random);
    }

    // Genera el par. Sin haber configurado nada, el proveedor usa sus valores por default.
    //
    // La implementacion base devuelve **null**, que es lo que hace el JDK y hay que replicar aunque
    // se vea mal. La razon es que aca nunca se llega: o el proveedor escribio una subclase de
    // `KeyPairGenerator` que lo sobreescribe, o escribio un `KeyPairGeneratorSpi` y entonces el que
    // corre es el reenvio de `KeyPairGeneradorDelegado`. Este cuerpo existe solo para que la clase
    // no tenga que declararse abstracta en el metodo.
    @Override
    public KeyPair generateKeyPair() {
        return null;
    }
}
