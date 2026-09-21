package java.security;

// The publishable half of an asymmetric pair.
//
// It does not add a single method over `AsymmetricKey`, and it does not need to: the only thing it
// contributes is **the type**. That `Signature.initVerify` asks for a `PublicKey` and
// `Signature.initSign` for a `PrivateKey` is what makes confusing them a compilation error and not
// a vulnerability.
public interface PublicKey extends AsymmetricKey {

    long serialVersionUID = 7187392471159151072L;
}
