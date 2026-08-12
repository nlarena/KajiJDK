package java.lang;

// KajiLibrary's java.lang.Void — an uninstantiable placeholder for the `void` type, used as
// a type argument when a generic slot must be filled but no value is ever produced
// (e.g. Future<Void>). Non-instantiable, like the JDK's. (The JDK also exposes the
// `Class<Void> TYPE` mirror; deferred — it needs VM support for the primitive void class.)
public final class Void {

    private Void() {}
}
