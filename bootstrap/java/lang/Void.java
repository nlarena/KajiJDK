package java.lang;

// java.lang.Void — the placeholder for the `void` primitive. `void.class` compiles to
// `getstatic Void.TYPE`, so a `MethodType` return of `void` (a constructor's) needs this.
public final class Void {
    public static final Class<Void> TYPE = (Class<Void>) Class.getPrimitiveClass("void");

    private Void() {
    }
}
