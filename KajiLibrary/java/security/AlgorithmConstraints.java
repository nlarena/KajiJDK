package java.security;

import java.util.Set;

// Una regla sobre que algoritmos se pueden usar y para que.
//
// Es lo que permite decir "en este proceso, nada de MD5 para firmar" sin tocar el codigo que
// firma. Las tres sobrecargas no son redundantes: se puede prohibir un algoritmo por nombre, una
// clave concreta —por corta, por ejemplo, aunque el algoritmo este permitido— o la combinacion de
// los dos con parametros. Una politica realista necesita las tres, porque "RSA esta bien" y "RSA
// de 512 bits esta bien" son afirmaciones distintas.
//
// El `Set<CryptoPrimitive>` es el "para que": el mismo algoritmo puede estar permitido para cifrar
// y prohibido para firmar.
//
// KajiLibrary no trae ninguna implementacion, y no es una omision: una lista de algoritmos
// prohibidos es una decision de politica, no de biblioteca. Quien la tenga la escribe.
public interface AlgorithmConstraints {

    // Si el algoritmo esta permitido para esas primitivas, con esos parametros.
    boolean permits(Set<CryptoPrimitive> primitives, String algorithm,
                    AlgorithmParameters parameters);

    // Si esa clave esta permitida para esas primitivas.
    boolean permits(Set<CryptoPrimitive> primitives, Key key);

    boolean permits(Set<CryptoPrimitive> primitives, String algorithm, Key key,
                    AlgorithmParameters parameters);
}
