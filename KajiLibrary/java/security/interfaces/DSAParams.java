package java.security.interfaces;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

// Los parametros de dominio de DSA: el primo p, el subprimo q y el generador g.
//
// Los tres se comparten entre todas las claves de un mismo dominio y sin ellos una clave DSA no
// significa nada: y = g^x mod p depende de p y g tanto como de x.
//
// q es el orden del subgrupo generado por g y divide a p-1. Que exista un q chico —160 a 256 bits
// frente a 1024 o 3072 de p— es lo que hace a DSA practico: las firmas son del tamaño de q, no de p.
public interface DSAParams extends AlgorithmParameterSpec {

    // El primo grande.
    BigInteger getP();

    // El subprimo: divide a p-1 y fija el orden del subgrupo.
    BigInteger getQ();

    // El generador del subgrupo de orden q.
    BigInteger getG();
}
