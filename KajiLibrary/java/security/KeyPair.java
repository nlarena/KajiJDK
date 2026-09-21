package java.security;

import java.io.Serializable;

// The two halves of an asymmetric pair together.
//
// It is a container and nothing else —it does not check that the two keys correspond, because for
// that one would have to do cryptography and this type does not— but it is the right container: a
// generated pair is handed over as two or is not handed over, and separating them into two loose
// values is how one ends up signing with the key of another pair.
public final class KeyPair implements Serializable, DEREncodable {

    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public KeyPair(PublicKey publicKey, PrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public PublicKey getPublic() {
        return this.publicKey;
    }

    public PrivateKey getPrivate() {
        return this.privateKey;
    }
}
