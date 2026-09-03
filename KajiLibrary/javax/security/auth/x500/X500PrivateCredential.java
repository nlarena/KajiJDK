package javax.security.auth.x500;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.security.auth.Destroyable;

/**
 * KajiLibrary's javax.security.auth.x500.X500PrivateCredential -- una identidad completa: el
 * certificado, la clave privada que le corresponde, y de que entrada del almacen salieron.
 *
 * <p>Es un par, no dos objetos sueltos, y por una razon concreta: un certificado sin su clave sirve
 * para verificar, y una clave sin su certificado no sirve para nada -- nadie sabe a quien pertenece.
 * Lo que se necesita para <b>actuar</b> como alguien son los dos juntos, y esta clase es eso.
 *
 * <p>Los cuatro argumentos de los constructores son obligatorios y se rechazan con
 * {@code IllegalArgumentException}, no con {@code NullPointerException}. Es lo que hace el JDK y
 * conviene anotarlo porque no es lo habitual en el resto de la biblioteca.
 *
 * <h2>Sobre destroy()</h2>
 *
 * <p>{@code destroy()} <b>no borra la clave</b>: pone las tres referencias en null y suelta el
 * objeto. La clave privada en si sigue en memoria hasta que el recolector la levante, y si es un
 * objeto compartido sigue viva en quien la tenga. Es lo mismo que hace el JDK y hay que decirlo,
 * porque el nombre promete mas de lo que el metodo puede dar: quien quiera de verdad borrar el
 * material tiene que llamar al {@code destroy()} de la clave, si es que la clave lo implementa.
 *
 * <p>Un detalle que sorprende: con el constructor de dos argumentos el alias ya arranca en null,
 * pero {@code isDestroyed()} da false igual, porque exige que los <b>tres</b> campos lo esten.
 */
public final class X500PrivateCredential implements Destroyable {

    private X509Certificate cert;
    private PrivateKey key;
    private String alias;

    /**
     * @throws IllegalArgumentException si alguno es null
     */
    public X500PrivateCredential(X509Certificate cert, PrivateKey key) {
        if (cert == null || key == null) {
            throw new IllegalArgumentException();
        }
        this.cert = cert;
        this.key = key;
        this.alias = null;
    }

    /**
     * Idem, recordando ademas de que entrada del almacen salio el par.
     *
     * @throws IllegalArgumentException si alguno es null, el alias incluido
     */
    public X500PrivateCredential(X509Certificate cert, PrivateKey key, String alias) {
        if (cert == null || key == null || alias == null) {
            throw new IllegalArgumentException();
        }
        this.cert = cert;
        this.key = key;
        this.alias = alias;
    }

    /** El certificado, o null si ya se llamo a {@link #destroy}. */
    public X509Certificate getCertificate() {
        return this.cert;
    }

    /** La clave privada, o null si ya se llamo a {@link #destroy}. */
    public PrivateKey getPrivateKey() {
        return this.key;
    }

    /** El alias en el almacen, o null si no se dio o si ya se llamo a {@link #destroy}. */
    public String getAlias() {
        return this.alias;
    }

    /**
     * Suelta las tres referencias. Ver la nota de la clase sobre lo que esto no hace.
     *
     * <p>No declara {@code DestroyFailedException}: soltar una referencia no puede fallar. Llamarlo
     * dos veces tampoco.
     */
    public void destroy() {
        this.cert = null;
        this.key = null;
        this.alias = null;
    }

    /** Si las tres referencias estan sueltas. */
    public boolean isDestroyed() {
        return this.cert == null && this.key == null && this.alias == null;
    }
}
