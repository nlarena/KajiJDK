package java.security.interfaces;

import java.security.PublicKey;
import java.security.spec.EdECPoint;
import java.security.spec.NamedParameterSpec;

// Una clave publica Edwards: el punto en forma comprimida.
public interface EdECPublicKey extends EdECKey, PublicKey {

    // El punto publico: la coordenada y mas el bit de signo de x.
    EdECPoint getPoint();

    // Ver `ECPublicKey.getParams()`: desempata `EdECKey` contra `AsymmetricKey`.
    @Override
    default NamedParameterSpec getParams() {
        return null;
    }
}
