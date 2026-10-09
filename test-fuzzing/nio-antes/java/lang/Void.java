package java.lang;

/**
 * KajiLibrary's java.lang.Void -- an uninstantiable placeholder for the {@code void} type.
 *
 * <p>It exists to fill a generic slot where no value is ever produced: {@code Future<Void>} is a
 * future that completes without a result. No instance of it is ever made, which is why the
 * constructor is private and why {@code null} is the only value a {@code Void} variable can
 * hold.
 */
public final class Void {

    /**
     * The mirror of the primitive type {@code void}.
     *
     * <p>Note what it is NOT: {@code Void.TYPE} is not {@code Void.class}. The first names the
     * primitive {@code void}, which has no values; the second names this class, which has
     * exactly one (null). They are different mirrors and comparing them is false -- the same
     * distinction as {@code Integer.TYPE} against {@code Integer.class}.
     */
    public static final Class<Void> TYPE = Class.getPrimitiveClass("void");

    // Non-instantiable: a pure placeholder. Without this, javac would synthesize a *public*
    // default constructor and `new Void()` would compile.
    private Void() {
    }
}
