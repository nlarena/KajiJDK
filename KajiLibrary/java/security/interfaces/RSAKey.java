package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

// Lo que toda clave RSA tiene: el modulo.
//
// El modulo es publico en los dos lados del par —esta tanto en la clave publica como en la privada—
// y es lo unico que se puede pedir sin saber de cual de las dos se trata. Tambien es lo que fija el
// "tamaño" de la clave: `getModulus().bitLength()` es lo que la gente llama RSA-2048.
public interface RSAKey {

    BigInteger getModulus();

    // Los parametros del algoritmo, para RSASSA-PSS. Null por default: la mayoria de las claves RSA
    // no llevan ninguno, y las implementaciones anteriores a este metodo no lo escriben.
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
