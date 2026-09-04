package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

// La cara del proveedor para una `KeyFactory`.
//
// Los cuatro metodos son abstractos: convertir entre la forma opaca y la transparente de una clave
// solo lo puede hacer quien conozca el algoritmo. KajiLibrary no trae ninguna implementacion.
public abstract class KeyFactorySpi {

    public KeyFactorySpi() {
    }

    protected abstract PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException;

    protected abstract PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException;

    protected abstract <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec)
        throws InvalidKeySpecException;

    // Traduce una clave de otro proveedor a una de este.
    protected abstract Key engineTranslateKey(Key key) throws InvalidKeyException;
}
