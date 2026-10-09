package java.security;

// An identity that also has a **private** key: it can sign, not only be verified.
//
// The difference from `Identity` is all that matters in a system of keys. An `Identity` is public
// and can be handed round; a `Signer` keeps the secret. That they are different types is what keeps
// an API that only needs to verify from receiving by accident an object with the private key
// inside.
//
// `setKeyPair` is the only way of giving it the private key, and it takes the **whole pair** on
// purpose: setting the private one without the public one would leave a signer whose signature
// nobody can verify. It also goes through `setPublicKey`, which erases the old certificates — the
// same invariant as in `Identity`.
//
// Obsolete since 1.2, along with this whole API.
@Deprecated
public abstract class Signer extends Identity {

    private PrivateKey privateKey;

    protected Signer() {
        super();
    }

    public Signer(String name) {
        super(name);
    }

    public Signer(String name, IdentityScope scope) throws KeyManagementException {
        super(name, scope);
    }

    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    // The complete pair, or nothing: half a pair does not serve for signing in a verifiable way.
    public final void setKeyPair(KeyPair pair)
            throws InvalidParameterException, KeyException {
        PublicKey pub = pair.getPublic();
        PrivateKey priv = pair.getPrivate();
        if (pub == null || priv == null) {
            throw new InvalidParameterException();
        }
        this.setPublicKey(pub);
        this.privateKey = priv;
    }

    @Override
    String printKeys() {
        String pub = super.printKeys();
        if (this.privateKey != null) {
            return pub + "\tprivate key initialized";
        }
        return pub + "\tno private key";
    }

    @Override
    public String toString() {
        return "[Signer]" + super.toString();
    }
}
