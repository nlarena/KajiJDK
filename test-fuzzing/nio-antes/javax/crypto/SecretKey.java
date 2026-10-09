package javax.crypto;

import java.security.Key;
import javax.security.auth.Destroyable;

/**
 * A symmetric key: the same one serves to encrypt and to decrypt.
 *
 * <p>It adds no method, and that is exactly what it says. A `SecretKey` is a {@link Key} --it has an
 * algorithm, a format and bytes-- that can also be destroyed. What it contributes as a type of its
 * own is the distinction: a method asking for a `SecretKey` does not accept a public key, and that
 * check is made by the compiler instead of failing at run time.
 *
 * <p>That it extends {@link Destroyable} is not decoration: the material of a symmetric key is a
 * secret worth wiping from memory once it is no longer used, and without that supertype there would
 * be no way to ask for it by contract.
 */
public interface SecretKey extends Key, Destroyable {

    /**
     * @deprecated A `serialVersionUID` in an interface does nothing: only the implementing class's
     *     counts. It is declared because the JDK declares it, and removing it would change the
     *     surface.
     */
    @Deprecated
    public static final long serialVersionUID = -4795878709595146952L;
}
