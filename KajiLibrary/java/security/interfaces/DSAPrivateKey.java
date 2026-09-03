package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;

// Una clave privada DSA: el exponente secreto x.
//
// Que `getX()` exista en el API es una herencia de cuando se daba por hecho que las claves vivian en
// memoria. Una clave en hardware no puede implementarlo sin dejar de estar en hardware, y por eso el
// codigo moderno firma pidiendole a la clave que firme en vez de sacarle el x.
public interface DSAPrivateKey extends DSAKey, PrivateKey {

    long serialVersionUID = 7776497482533790279L;

    // El exponente privado x.
    BigInteger getX();

    // Ver `DSAPublicKey.getParams()`: desempata la herencia entre `DSAKey` y `AsymmetricKey`, y
    // devuelve null por default por la misma razon.
    @Override
    default DSAParams getParams() {
        return null;
    }
}
