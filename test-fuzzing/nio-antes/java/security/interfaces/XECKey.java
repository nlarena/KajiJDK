package java.security.interfaces;

import java.security.spec.AlgorithmParameterSpec;

// What every Montgomery-curve key (X25519, X448) has: its parameters.
//
// Unlike `EdECKey`, here the type is `AlgorithmParameterSpec` and not `NamedParameterSpec`. The
// asymmetry is the real API's and not this implementation's.
public interface XECKey {

    AlgorithmParameterSpec getParams();
}
