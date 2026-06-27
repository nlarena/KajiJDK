package java.lang;

// Minimal java.lang.LinkageError — failures of linking (loading/resolving a class
// or member). NoClassDefFoundError and NoSuchMethodError extend it.
public class LinkageError extends Error {
    public LinkageError() {
    }
}
