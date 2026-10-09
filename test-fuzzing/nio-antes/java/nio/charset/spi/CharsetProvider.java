package java.nio.charset.spi;

import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * KajiLibrary's java.nio.charset.spi.CharsetProvider -- charsets the platform does not ship.
 *
 * <p>Two methods, and they are two different questions worth not confusing: {@link #charsetForName}
 * resolves <b>one</b> by name, and {@link #charsets} enumerates the ones this provider offers.
 * Resolution is not implemented by walking the enumeration, and that is why there are two: a
 * provider can recognize names it does not enumerate.
 *
 * <p>That really happens and is not a detail. A charset has a canonical name and a list of
 * <b>aliases</b> --{@code UTF-8} is also called {@code unicode-1-1-utf-8}, and there are dozens of
 * historical names-- and {@code charsetForName} has to recognize them all even though it enumerates
 * a single one. The other way round would be absurd: enumerating each alias as if it were a
 * distinct charset would show whoever asks twenty entries that are the same.
 *
 * <p>Lookup is case-insensitive, and the enumeration cannot repeat the same charset.
 */
public abstract class CharsetProvider {

    /** For subclasses. */
    protected CharsetProvider() {
    }

    /**
     * The charsets of this provider.
     *
     * <p>One per charset, not one per alias; see the class note.
     */
    public abstract Iterator<Charset> charsets();

    /**
     * The charset with that name or alias.
     *
     * @param charsetName case-insensitive
     * @return null if this provider does not know it, which is not an error: the next one is asked
     */
    public abstract Charset charsetForName(String charsetName);
}
