package java.security.interfaces;

import java.security.spec.NamedParameterSpec;

// What every Edwards key has: the curve, always by name.
//
// The return type is `NamedParameterSpec` and not `AlgorithmParameterSpec`, and that is not a
// detail: in Ed25519/Ed448 the parameters are fixed and there is no way to write them by hand. The
// narrower type is what makes it impossible to pass an invented curve.
public interface EdECKey {

    NamedParameterSpec getParams();
}
