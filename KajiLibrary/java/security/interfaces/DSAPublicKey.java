package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

// Una clave publica DSA: y = g^x mod p, donde x es la privada.
//
// El `getParams()` default resuelve un choque real de herencia. `DSAKey.getParams()` devuelve
// `DSAParams` y `AsymmetricKey.getParams()` —que llega por `PublicKey`— devuelve
// `AlgorithmParameterSpec`; como `DSAParams` extiende `AlgorithmParameterSpec`, el metodo de
// `DSAKey` es un override covariante valido del otro, pero Java no lo elige solo cuando los dos
// llegan por ramas distintas. El default lo desempata declarando explicitamente cual gana.
public interface DSAPublicKey extends DSAKey, PublicKey {

    long serialVersionUID = 1234526332779022332L;

    // El valor publico y.
    BigInteger getY();

    // Devuelve null por default, igual que `AsymmetricKey.getParams()`: significa "esta clave no
    // dice cuales son sus parametros". Que sea default y no abstracto mantiene compilando a las
    // implementaciones anteriores a que `AsymmetricKey` existiera.
    @Override
    default DSAParams getParams() {
        return null;
    }
}
