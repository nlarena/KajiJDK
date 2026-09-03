package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.spec.ECParameterSpec;

// Una clave privada de curva eliptica: el escalar s.
public interface ECPrivateKey extends PrivateKey, ECKey {

    long serialVersionUID = -7896394956925609184L;

    // El escalar privado.
    BigInteger getS();

    // Ver `ECPublicKey.getParams()`: mismo choque, misma resolucion.
    @Override
    default ECParameterSpec getParams() {
        return null;
    }
}
