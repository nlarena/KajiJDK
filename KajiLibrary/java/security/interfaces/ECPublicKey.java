package java.security.interfaces;

import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

// Una clave publica de curva eliptica: el punto W = d*G.
public interface ECPublicKey extends PublicKey, ECKey {

    long serialVersionUID = -3314988629879632826L;

    // El punto publico.
    ECPoint getW();

    // Resuelve el choque entre `ECKey.getParams()` (abstracto, devuelve `ECParameterSpec`) y
    // `AsymmetricKey.getParams()` (default, devuelve `AlgorithmParameterSpec`), que llegan por
    // ramas distintas. Devuelve null por lo mismo que el de `AsymmetricKey`: quiere decir "esta
    // clave no dice cuales son".
    @Override
    default ECParameterSpec getParams() {
        return null;
    }
}
