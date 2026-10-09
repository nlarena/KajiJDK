package java.nio.channels;

/**
 * Factory of {@link FileChannel.MapMode} for the modes that live **outside** `java.nio.channels`.
 *
 * <p>Not a JDK class: it is scaffolding of ours, the same kind as `KajiFileChannel`. It exists
 * because `MapMode` is not an `enum` --the list of modes is left open on purpose-- but its
 * constructor is not public, so `jdk.nio.mapmode.ExtendedMapMode` has no way of building its two
 * constants from another package. In the real JDK that bridge is
 * `jdk.internal.access.SharedSecrets` with a `MethodHandle` to a private constructor; here a
 * package-private method is enough, which is the same bridge without the machinery.
 *
 * <p>That it sits in `java.nio.channels` is exactly the point: it is the only place from which
 * `MapMode`'s constructor is visible.
 */
public final class MapModes {

    private MapModes() {
    }

    /**
     * A new map mode with the given name.
     *
     * <p>The name is the only thing telling one `MapMode` from another: a `MapMode` carries no
     * behaviour, it is a label that `map()` interprets.
     *
     * @param name the mode's name, the one {@code toString()} returns
     * @return a new `MapMode`, different from every other
     * @throws NullPointerException if `name` is null
     */
    public static FileChannel.MapMode of(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        return new FileChannel.MapMode(name);
    }
}
