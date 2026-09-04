package java.security;

// Genera parametros de algoritmo: los primos de DSA, una curva a medida.
//
// A diferencia de `AlgorithmParameters`, que decodifica parametros que ya existen, este los produce.
// En la practica casi no se usa: los parametros de dominio serios vienen de estandares, y generar
// los propios es una forma cara de terminar con parametros peores que los publicados.
//
// A KajiLibrary subset: **ningun proveedor registra este servicio**, asi que las tres sobrecargas
// de `getInstance` tiran siempre `NoSuchAlgorithmException`. La clase esta entera igual, porque su
// forma es la que tiene que cumplir cualquier proveedor que se agregue despues.
//
// Los cuatro `init` vienen en pares: uno con fuente de azar explicita y otro sin ella. El que no la
// recibe **no genera sin azar** -- usa el generador por omision, que es el del sistema operativo.
// Ver `SecureRandom`.
public class AlgorithmParameterGenerator {

    private final AlgorithmParameterGeneratorSpi paramGenSpi;
    private final Provider provider;
    private final String algorithm;

    protected AlgorithmParameterGenerator(AlgorithmParameterGeneratorSpi paramGenSpi,
                                          Provider provider, String algorithm) {
        this.paramGenSpi = paramGenSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("AlgorithmParameterGenerator", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(
            algorithm + " AlgorithmParameterGenerator not available");
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm, String provider)
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

    public static AlgorithmParameterGenerator getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("AlgorithmParameterGenerator", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static AlgorithmParameterGenerator armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof AlgorithmParameterGeneratorSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for AlgorithmParameterGenerator is not an "
                + "AlgorithmParameterGeneratorSpi: " + s.getClassName());
        }
        return new AlgorithmParameterGenerator(
            (AlgorithmParameterGeneratorSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    // Los parametros generados.
    /**
     * Inicializa por tamaño, con el generador de azar por omision.
     *
     * <p>"Por omision" no quiere decir "sin azar": es el del sistema operativo. Ver
     * {@link SecureRandom}.
     */
    public final void init(int size) {
        this.paramGenSpi.engineInit(size, new SecureRandom());
    }

    /** Idem, diciendo de donde sale el azar. */
    public final void init(int size, SecureRandom random) {
        this.paramGenSpi.engineInit(size, random);
    }

    /**
     * Inicializa con parametros concretos y el generador por omision.
     *
     * @throws InvalidAlgorithmParameterException si los parametros no le sirven a este generador
     */
    public final void init(java.security.spec.AlgorithmParameterSpec genParamSpec)
            throws InvalidAlgorithmParameterException {
        this.paramGenSpi.engineInit(genParamSpec, new SecureRandom());
    }

    /** Idem, diciendo de donde sale el azar. */
    public final void init(java.security.spec.AlgorithmParameterSpec genParamSpec,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        this.paramGenSpi.engineInit(genParamSpec, random);
    }

    public final AlgorithmParameters generateParameters() {
        return this.paramGenSpi.engineGenerateParameters();
    }
}
