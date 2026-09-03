package java.security.interfaces;

import java.security.PrivateKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Optional;

// Una clave privada de curva de Montgomery: el escalar, si la clave lo deja salir.
public interface XECPrivateKey extends XECKey, PrivateKey {

    // El escalar privado, o vacio si no sale de donde esta.
    Optional<byte[]> getScalar();

    // Ver `XECPublicKey.getParams()`.
    @Override
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
