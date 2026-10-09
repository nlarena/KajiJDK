package java.lang;

// KajiLibrary's java.lang.ClassCircularityError — a class hierarchy closes on itself (A extends
// B extends A). Detected while loading, because the loader must resolve a supertype before it
// can finish the subtype, and here that recursion never bottoms out.
public class ClassCircularityError extends LinkageError {

    public ClassCircularityError() {
    }

    public ClassCircularityError(String message) {
        super(message);
    }
}
