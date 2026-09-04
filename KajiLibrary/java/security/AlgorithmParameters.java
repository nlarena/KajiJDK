package java.security;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

// Los parametros de un algoritmo, en forma **opaca**.
//
// El par con `AlgorithmParameterSpec` es la idea central: la spec es transparente y el programa la
// lee campo por campo; esto guarda los mismos parametros codificados en DER y sabe convertir entre
// las dos formas. Sirve para mover parametros de un lado a otro sin entenderlos —lo que hace falta
// para hablar con una contraparte que usa un algoritmo que uno no implementa.
//
// El objeto es de **un solo disparo**: se construye sin inicializar, se llama a `init` una vez, y
// desde ahi es de solo lectura. Llamar a `init` dos veces tira, y llamar a `getEncoded` antes de
// inicializar tambien. Eso es lo que impide que unos parametros cambien despues de que alguien
// los uso para tomar una decision.
//
// ===============================================================================================
// LA FABRICA NO TIENE PROVEEDORES, Y ESO ES A PROPOSITO
// ===============================================================================================
//
// `getInstance` recorre los proveedores registrados buscando un servicio de tipo
// "AlgorithmParameters", y no hay ninguno: `KajiProvider` solo ofrece digests. O sea que hoy las
// tres sobrecargas de `getInstance` tiran siempre `NoSuchAlgorithmException`.
//
// Es la respuesta correcta y no un agujero: codificar parametros es escribir DER especifico de
// cada algoritmo, y un `AlgorithmParameters` que devolviera bytes que no son los parametros seria
// exactamente el tipo de miembro que miente. La estructura queda escrita y funcionando —el ciclo
// de vida, la delegacion al spi, los errores de estado— para el dia que haya un proveedor que
// registrar.
public class AlgorithmParameters {

    private final AlgorithmParametersSpi paramSpi;
    private final Provider provider;
    private final String algorithm;

    // Si ya se llamo a `init`.
    private boolean initialized;

    protected AlgorithmParameters(AlgorithmParametersSpi paramSpi, Provider provider,
                                  String algorithm) {
        this.paramSpi = paramSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public static AlgorithmParameters getInstance(String algorithm)
            throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("AlgorithmParameters", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " AlgorithmParameters not available");
    }

    public static AlgorithmParameters getInstance(String algorithm, String provider)
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

    public static AlgorithmParameters getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("AlgorithmParameters", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static AlgorithmParameters armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof AlgorithmParametersSpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for AlgorithmParameters is not an AlgorithmParametersSpi: "
                + s.getClassName());
        }
        return new AlgorithmParameters((AlgorithmParametersSpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final void init(AlgorithmParameterSpec paramSpec)
            throws InvalidParameterSpecException {
        if (this.initialized) {
            throw new InvalidParameterSpecException("already initialized");
        }
        this.paramSpi.engineInit(paramSpec);
        this.initialized = true;
    }

    public final void init(byte[] params) throws IOException {
        if (this.initialized) {
            throw new IOException("already initialized");
        }
        this.paramSpi.engineInit(params);
        this.initialized = true;
    }

    public final void init(byte[] params, String format) throws IOException {
        if (this.initialized) {
            throw new IOException("already initialized");
        }
        this.paramSpi.engineInit(params, format);
        this.initialized = true;
    }

    // La version transparente de estos parametros, del tipo pedido.
    public final <T extends AlgorithmParameterSpec> T getParameterSpec(Class<T> paramSpec)
            throws InvalidParameterSpecException {
        if (!this.initialized) {
            throw new InvalidParameterSpecException("not initialized");
        }
        return this.paramSpi.engineGetParameterSpec(paramSpec);
    }

    public final byte[] getEncoded() throws IOException {
        if (!this.initialized) {
            throw new IOException("not initialized");
        }
        return this.paramSpi.engineGetEncoded();
    }

    public final byte[] getEncoded(String format) throws IOException {
        if (!this.initialized) {
            throw new IOException("not initialized");
        }
        return this.paramSpi.engineGetEncoded(format);
    }

    // Devuelve null si no esta inicializado, y no una cadena vacia ni "<uninitialized>". Es lo que
    // hace el JDK y hay codigo que lo comprueba.
    @Override
    public final String toString() {
        if (!this.initialized) {
            return null;
        }
        return this.paramSpi.engineToString();
    }
}
