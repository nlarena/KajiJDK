package java.security.interfaces;

import java.security.spec.ECParameterSpec;

// What every EC key has: its domain parameters.
//
// Without them the key cannot be interpreted —the same point is valid on infinitely many curves— so
// they are the only data truly common to the public and the private key.
public interface ECKey {

    ECParameterSpec getParams();
}
