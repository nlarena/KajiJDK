package java.security.spec;

import java.security.InvalidParameterException;
import java.util.Optional;

// Los parametros de una firma EdDSA: si es la variante "prehash" y que contexto usar.
//
// Las dos opciones cambian **que** se firma, no como, y por eso son parte de la firma y no una
// preferencia local: Ed25519, Ed25519ph y Ed25519ctx producen firmas que no se verifican entre si
// aunque la clave sea la misma. Eso es deliberado, es la separacion de dominios de RFC 8032: sirve
// para que una firma emitida para un proposito no pueda reusarse en otro.
//
// El contexto esta limitado a 255 bytes porque su largo se codifica en un solo byte dentro del
// mensaje que se hashea. El limite es del formato, no una politica de esta clase.
//
// Vale notar la rareza: pasarse de largo tira `InvalidParameterException`, que hereda de
// `IllegalArgumentException` pero vive en `java.security`. Es lo que hace el JDK y se replica.
public class EdDSAParameterSpec implements AlgorithmParameterSpec {

    private static final int LARGO_MAXIMO_CONTEXTO = 255;

    private final boolean prehash;
    private final byte[] context;

    // Sin contexto: Ed25519 puro (o Ed25519ph si prehash).
    public EdDSAParameterSpec(boolean prehash) {
        this.prehash = prehash;
        this.context = null;
    }

    public EdDSAParameterSpec(boolean prehash, byte[] context) {
        if (context == null) {
            throw new NullPointerException("context may not be null");
        }
        if (context.length > LARGO_MAXIMO_CONTEXTO) {
            throw new InvalidParameterException("context length cannot be greater than 255");
        }
        this.prehash = prehash;
        byte[] c = new byte[context.length];
        System.arraycopy(context, 0, c, 0, context.length);
        this.context = c;
    }

    // Si se firma el hash del mensaje en vez del mensaje entero. Sirve cuando el mensaje no entra en
    // memoria o llega en streaming; a cambio, la seguridad pasa a depender del hash.
    public boolean isPrehash() {
        return this.prehash;
    }

    // Copia del contexto, vacio si no hay. Es `Optional` y no null porque ausente y vacio son cosas
    // distintas aca: un contexto de cero bytes es un contexto.
    public Optional<byte[]> getContext() {
        if (this.context == null) {
            return Optional.empty();
        }
        byte[] c = new byte[this.context.length];
        System.arraycopy(this.context, 0, c, 0, this.context.length);
        return Optional.of(c);
    }
}
