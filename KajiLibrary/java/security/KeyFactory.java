package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

// Convierte entre las dos formas de una clave: la opaca (`Key`) y la transparente (`KeySpec`).
//
// Es la pieza que hace que las dos representaciones sirvan de algo. Una `Key` puede vivir en un
// token y no dejarse mirar; un `KeySpec` es material que el programa puede construir a mano o leer
// de un archivo. `KeyFactory` es el unico camino entre las dos, y por eso es lo que se usa para
// cargar una clave desde su codificacion —el caso mas comun de todos.
//
// `translateKey` merece una linea aparte: convierte una clave de **otro** proveedor a una de este.
// Es lo que permite tomar una clave que llego de afuera y usarla con un proveedor que solo sabe
// trabajar con las suyas, sin exportarla ni volver a construirla desde bytes.
//
// ===============================================================================================
// LA FABRICA NO TIENE PROVEEDORES
// ===============================================================================================
//
// Igual que `AlgorithmParameters`: `KajiProvider` solo ofrece digests, asi que las tres
// sobrecargas de `getInstance` tiran siempre `NoSuchAlgorithmException`. No se registra ninguna
// `KeyFactory` porque implementar una honestamente pide un parser de DER y la aritmetica del
// algoritmo, y ninguna de las dos cosas esta escrita. La estructura queda lista para el dia que la
// haya.
public class KeyFactory {

    private final KeyFactorySpi spi;
    private final Provider provider;
    private final String algorithm;

    protected KeyFactory(KeyFactorySpi keyFacSpi, Provider provider, String algorithm) {
        this.spi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public static KeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider[] provs = Security.getProviders();
        int i = 0;
        while (i < provs.length) {
            Provider.Service s = provs[i].getService("KeyFactory", algorithm);
            if (s != null) {
                return armar(s, algorithm);
            }
            i = i + 1;
        }
        throw new NoSuchAlgorithmException(algorithm + " KeyFactory not available");
    }

    public static KeyFactory getInstance(String algorithm, String provider)
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

    public static KeyFactory getInstance(String algorithm, Provider provider)
            throws NoSuchAlgorithmException {
        if (provider == null) {
            throw new IllegalArgumentException("missing provider");
        }
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        Provider.Service s = provider.getService("KeyFactory", algorithm);
        if (s == null) {
            throw new NoSuchAlgorithmException(
                "no such algorithm: " + algorithm + " for provider " + provider.getName());
        }
        return armar(s, algorithm);
    }

    private static KeyFactory armar(Provider.Service s, String algorithm)
            throws NoSuchAlgorithmException {
        Object o = s.newInstance(null);
        if (!(o instanceof KeyFactorySpi)) {
            throw new NoSuchAlgorithmException(
                "class configured for KeyFactory is not a KeyFactorySpi: " + s.getClassName());
        }
        return new KeyFactory((KeyFactorySpi) o, s.getProvider(), algorithm);
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    public final PublicKey generatePublic(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spi.engineGeneratePublic(keySpec);
    }

    public final PrivateKey generatePrivate(KeySpec keySpec) throws InvalidKeySpecException {
        return this.spi.engineGeneratePrivate(keySpec);
    }

    public final <T extends KeySpec> T getKeySpec(Key key, Class<T> keySpec)
            throws InvalidKeySpecException {
        return this.spi.engineGetKeySpec(key, keySpec);
    }

    public final Key translateKey(Key key) throws InvalidKeyException {
        return this.spi.engineTranslateKey(key);
    }
}
