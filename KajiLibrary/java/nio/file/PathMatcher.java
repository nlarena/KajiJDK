package java.nio.file;

// KajiLibrary's java.nio.file.PathMatcher -- a predicate over paths. A functional interface.
public interface PathMatcher {

    /** Whether {@code path} matches this matcher's pattern. */
    boolean matches(Path path);
}
