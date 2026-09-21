package java.nio.file;

import java.net.URI;

// The two path factories that existed before `Path.of`.
//
// **They are deprecated in the JDK and here too**, and it is worth saying why they exist all the
// same: since Java 11 `Path.of(...)` does exactly the same, and having the factory on the interface
// it returns saves importing two types. `Paths` is left for the older code, and that is why both
// delegate without adding anything -- that there be **one** implementation is what guarantees the
// two forms always give the same.
public final class Paths {

    // Factories only: there is nothing to instantiate.
    private Paths() {
    }

    /** The same as `Path.of(first, more)`. */
    public static Path get(String first, String... more) {
        return Path.of(first, more);
    }

    /** The same as `Path.of(uri)`. */
    public static Path get(URI uri) {
        return Path.of(uri);
    }
}
