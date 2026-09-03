package java.security.spec;

// Marca la representacion **transparente** de una clave.
//
// La distincion con `java.security.Key` es la que ordena todo `KeyFactory`: una `Key` es opaca —el
// proveedor decide que hay adentro y puede tenerla en hardware— mientras que un `KeySpec` es
// material que el programa puede mirar y construir. Convertir de uno al otro es exactamente lo que
// hace `KeyFactory`, y por eso el par de tipos tiene que existir aunque ninguno declare nada.
public interface KeySpec {
}
