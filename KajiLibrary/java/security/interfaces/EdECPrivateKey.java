package java.security.interfaces;

import java.security.PrivateKey;
import java.security.spec.NamedParameterSpec;
import java.util.Optional;

// Una clave privada Edwards: la semilla de la que se derivan el escalar y el nonce.
public interface EdECPrivateKey extends EdECKey, PrivateKey {

    // La semilla, o vacio si la clave no la deja salir. `Optional` y no null porque aca "no puedo
    // darte esto" es una respuesta legitima y frecuente —una clave en hardware— y merece decirse sin
    // que el llamador tenga que acordarse de chequear.
    Optional<byte[]> getBytes();

    // Ver `ECPublicKey.getParams()`.
    @Override
    default NamedParameterSpec getParams() {
        return null;
    }
}
