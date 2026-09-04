package java.security;

import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

// La forma en que una clave viaja por serializacion: tipo, algoritmo, formato y bytes.
//
// Existe por un problema real: una `Key` la implementa el proveedor, y serializar el objeto tal
// cual ataria el flujo a **esa** implementacion. Del otro lado puede no estar. Entonces
// `writeReplace` de la clave devuelve un `KeyRep` —cuatro datos estandar, ninguno especifico de un
// proveedor— y al leer, `readResolve` reconstruye la clave con la `KeyFactory` que haya. Es lo que
// permite que una clave serializada en una VM se lea en otra con otro proveedor.
//
// En KajiLibrary la vuelta **no se puede completar**: no hay ningun proveedor de `KeyFactory`
// registrado, asi que `readResolve` no encuentra con que reconstruir y tira
// `NotSerializableException` con la causa adentro. Es la respuesta honesta, y ademas es lo que
// hace el JDK cuando el algoritmo no esta disponible. El dia que haya una `KeyFactory` esto anda
// sin cambiar nada.
public class KeyRep implements Serializable {

    private final Type type;
    private final String algorithm;
    private final String format;
    private final byte[] encoded;

    public KeyRep(Type type, String algorithm, String format, byte[] encoded) {
        if (type == null || algorithm == null || format == null || encoded == null) {
            throw new NullPointerException("invalid null input(s)");
        }
        this.type = type;
        this.algorithm = algorithm;
        this.format = format.toUpperCase();
        byte[] c = new byte[encoded.length];
        System.arraycopy(encoded, 0, c, 0, encoded.length);
        this.encoded = c;
    }

    // Reconstruye la clave a partir de los cuatro datos.
    //
    // El formato decide que spec usar, y el tipo decide a que fabrica pedirsela. Un tipo `SECRET`
    // se rechaza directamente: la fabrica que lo resolveria es
    // `javax.crypto.SecretKeyFactory`, que no existe en esta biblioteca.
    protected Object readResolve() throws ObjectStreamException {
        try {
            if (this.type == Type.PUBLIC && this.format.equals("X.509")) {
                KeyFactory f = KeyFactory.getInstance(this.algorithm);
                return f.generatePublic(new X509EncodedKeySpec(this.encoded));
            }
            if (this.type == Type.PRIVATE && this.format.equals("PKCS#8")) {
                KeyFactory f = KeyFactory.getInstance(this.algorithm);
                return f.generatePrivate(new PKCS8EncodedKeySpec(this.encoded));
            }
            if (this.type == Type.SECRET) {
                throw new NotSerializableException(
                    "javax.crypto.SecretKeyFactory is not available in this library");
            }
            throw new NotSerializableException(
                "unrecognized key type " + this.type + " with format " + this.format);
        } catch (NotSerializableException e) {
            throw e;
        } catch (Exception e) {
            NotSerializableException nse = new NotSerializableException(
                "java.security.Key: [" + this.type + "] [" + this.algorithm + "] ["
                + this.format + "]");
            nse.initCause(e);
            throw nse;
        }
    }

    // De que clase de clave se trata. Es lo que decide a que fabrica pedirle la reconstruccion, y
    // por eso tiene que viajar junto con los bytes: los mismos bytes significan cosas distintas
    // segun si son de una privada o de una publica.
    public enum Type {

        SECRET,

        PUBLIC,

        PRIVATE
    }
}
