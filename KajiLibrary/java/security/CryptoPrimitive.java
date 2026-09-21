package java.security;

// The cryptographic primitives an algorithm constraint can name.
//
// It is an enum and not a set of strings because sets are made of this —`AlgorithmConstraints`
// receives a `Set<CryptoPrimitive>`— and an algorithm usually serves for more than one: RSA serves
// for signing and for encrypting, and a policy that wants to forbid it only for signing has to be
// able to say so.
public enum CryptoPrimitive {

    // A hash with no key.
    MESSAGE_DIGEST,

    // Generation of secure pseudo-random numbers.
    SECURE_RANDOM,

    // Symmetric block cipher.
    BLOCK_CIPHER,

    // Symmetric stream cipher.
    STREAM_CIPHER,

    // Message authentication code.
    MAC,

    // Wrapping of one key with another.
    KEY_WRAP,

    // Public key encryption.
    PUBLIC_KEY_ENCRYPTION,

    // A digital signature.
    SIGNATURE,

    // Key encapsulation.
    KEY_ENCAPSULATION,

    // Key agreement.
    KEY_AGREEMENT
}
