package java.lang;

// Our own minimal java.lang.Throwable — the real root of the exception hierarchy.
// Implicitly extends our java.lang.Object. Minimal for now (no message/cause/stack
// trace: those need String and native fillInStackTrace, which we don't model yet).
public class Throwable {
    public Throwable() {
    }
}
