package java.security.interfaces;

import java.security.spec.NamedParameterSpec;

// Lo que toda clave Edwards tiene: la curva, siempre por nombre.
//
// El tipo del retorno es `NamedParameterSpec` y no `AlgorithmParameterSpec`, y eso no es un detalle:
// en Ed25519/Ed448 los parametros son fijos y no hay forma de escribirlos a mano. El tipo mas
// estrecho es lo que hace imposible pasar una curva inventada.
public interface EdECKey {

    NamedParameterSpec getParams();
}
