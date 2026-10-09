package java.security.interfaces;

// What every DSA key has, public or private: its domain parameters.
//
// This interface does **not** extend `Key`, on purpose: a DSA key in an HSM may want to expose its
// parameters without committing to `getEncoded()`. The ones that are keys say so by also inheriting
// from `PublicKey` or `PrivateKey`.
public interface DSAKey {

    // This key's parameters.
    DSAParams getParams();
}
