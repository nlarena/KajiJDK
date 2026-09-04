package java.security;

// Marca lo que se puede escribir como DER, y por lo tanto lo que `PEMEncoder` sabe encodear.
//
// Es una interfaz vacia introducida con el soporte de PEM (JDK 25). No declara `getEncoded()` a
// proposito: los tipos que la implementan ya lo tienen con firmas incompatibles entre si —`Key`
// lo devuelve sin excepcion, `Certificate` lo tira— y unificarlas hubiera roto a los dos. La
// interfaz solo dice "esto tiene una forma DER"; quien encodea sabe como sacarla de cada tipo.
public interface DEREncodable {
}
