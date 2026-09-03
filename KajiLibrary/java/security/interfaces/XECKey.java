package java.security.interfaces;

import java.security.spec.AlgorithmParameterSpec;

// Lo que toda clave de curva de Montgomery (X25519, X448) tiene: sus parametros.
//
// A diferencia de `EdECKey`, aca el tipo es `AlgorithmParameterSpec` y no `NamedParameterSpec`. La
// asimetria es del API real y no de esta implementacion.
public interface XECKey {

    AlgorithmParameterSpec getParams();
}
