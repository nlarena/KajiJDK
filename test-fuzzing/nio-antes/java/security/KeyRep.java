package java.security;

import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

// The way a key travels through serialisation: type, algorithm, format and bytes.
//
// It exists because of a real problem: a `Key` is implemented by the provider, and serialising the
// object as it is would tie the stream to **that** implementation. On the other side it may not be
// there. So the `writeReplace` of the key returns a `KeyRep` —four standard data, none specific to
// a provider— and on reading, `readResolve` rebuilds the key with whatever `KeyFactory` there is.
// It is what allows a key serialised in one VM to be read in another with another provider.
//
// In KajiLibrary the way back **cannot be completed**: there is no registered `KeyFactory`
// provider, so `readResolve` finds nothing to rebuild with and throws `NotSerializableException`
// with the cause inside. It is the honest answer, and it is also what the JDK does when the
// algorithm is not available. The day there is a `KeyFactory` this works without anything being
// changed.
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

    // It rebuilds the key from the four data.
    //
    // The format decides which spec to use, and the type decides which factory to ask for it. A
    // `SECRET` type is rejected outright: the factory that would resolve it is
    // `javax.crypto.SecretKeyFactory`, which does not exist in this library.
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

    // Which kind of key it is. It is what decides which factory to ask for the rebuilding, and that
    // is why it has to travel together with the bytes: the same bytes mean different things
    // depending on whether they are of a private or of a public one.
    public enum Type {

        SECRET,

        PUBLIC,

        PRIVATE
    }
}
