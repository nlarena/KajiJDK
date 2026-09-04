package java.security;

import java.io.Serializable;

// Las dos mitades de un par asimetrico juntas.
//
// Es un contenedor y nada mas —no verifica que las dos claves se correspondan, porque para eso
// habria que hacer criptografia y este tipo no la hace— pero es el contenedor correcto: un par
// generado se entrega de a dos o no se entrega, y separarlos en dos valores sueltos es como se
// termina firmando con la clave de otro par.
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
