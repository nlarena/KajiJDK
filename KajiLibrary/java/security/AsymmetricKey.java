package java.security;

import java.security.spec.AlgorithmParameterSpec;

// La mitad de un par asimetrico: lo que `PublicKey` y `PrivateKey` tienen en comun.
//
// Se agrego tarde (JDK 22) y no por prolijidad: lo que aporta es `getParams()`, y el punto es que
// en un algoritmo asimetrico los parametros del dominio —la curva, el grupo— son parte de la
// identidad de la clave y hasta entonces cada familia los exponia con un metodo propio en su
// interfaz especifica. Aca se pregunta una sola vez, sin saber de que familia es la clave.
public interface AsymmetricKey extends Key, DEREncodable {

    // Los parametros asociados, o null si la clave no tiene.
    //
    // Default y no abstracto porque hay implementaciones anteriores a este metodo que no lo
    // escriben: para ellas la respuesta correcta es "no se", y null es como se dice.
    default AlgorithmParameterSpec getParams() {
        return null;
    }
}
