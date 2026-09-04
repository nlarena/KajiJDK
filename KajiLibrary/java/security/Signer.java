package java.security;

// Una identidad que ademas tiene clave **privada**: puede firmar, no solo ser verificada.
//
// La diferencia con `Identity` es toda la que importa en un sistema de claves. Una `Identity` es
// publica y se puede repartir; un `Signer` guarda el secreto. Que sean tipos distintos es lo que
// hace que una API que solo necesita verificar no pueda recibir por accidente un objeto con la
// clave privada adentro.
//
// `setKeyPair` es la unica forma de darle la clave privada, y toma el **par entero** a proposito:
// setear la privada sin la publica dejaria un firmante cuya firma nadie puede verificar. Ademas
// pasa por `setPublicKey`, que borra los certificados viejos — la misma invariante que en
// `Identity`.
//
// Obsoleto desde 1.2, junto con toda esta API.
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

    // El par completo, o nada: un par a medias no sirve para firmar de forma verificable.
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
