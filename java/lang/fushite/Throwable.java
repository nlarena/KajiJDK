package java.lang.fushite;

// Our own root of the exception hierarchy, in our own package. It extends the real
// java.lang.Throwable *only* so javac accepts it in throw/catch — that real root is
// never loaded in our JVM (its <init> no-ops, like Object's). Everything we throw
// and catch hangs off THIS class, which is fully loadable and walkable by is_subtype.
//
// Minimal for now: a numeric `code` instead of a String message (a String would need
// ldc + java.lang.String, which the interpreter doesn't model yet).
public class Throwable extends java.lang.Throwable {
    public int code;

    public Throwable() {
        this.code = 0;
    }
}
