package javax.tools;

// KajiLibrary's javax.tools.JavaFileObject — a FileObject that a Java tool understands,
// i.e. one that knows whether it holds source, bytecode, documentation or something else.
// Everything a compiler reads or writes travels through this interface.
//
// Two members used to be missing here, for types that did not exist in KajiLibrary:
// `NestingKind getNestingKind()` and `Modifier getAccessLevel()`, both from
// javax.lang.model.element. Bringing them back with the type changed to Object would have been a
// false signature, so they stayed out until the types arrived. They are both here now.
//
// javax.tools.SimpleJavaFileObject, this interface's canonical implementation, was left out of the
// package for the same reason and it is back too. It was not an oversight: its two fields are
// `protected final URI uri` and `protected final Kind kind`, and its ONLY constructor is
// `protected SimpleJavaFileObject(URI, Kind)`. With `java.net.URI` missing, that constructor fell
// away and javac would have synthesized a `public SimpleJavaFileObject()` — a public member the real
// API does NOT have, which is a false declaration and not an absence. What would have been left was
// a class unrelated to JavaFileObject, without its state, without its constructor and with an
// invented one; the name would have been the only correct thing about it.
public interface JavaFileObject extends FileObject {

    // The "what kind of file is this", with the canonical extension that goes with it.
    public enum Kind {
        SOURCE(".java"),
        CLASS(".class"),
        HTML(".html"),
        OTHER("");

        public final String extension;

        private Kind(String extension) {
            this.extension = extension;
        }
    }

    Kind getKind();

    /**
     * The nesting of this object's main class, or `null` if it is not known.
     *
     * <p>`null` is the right answer and the commonest one: finding it out means **reading** the file,
     * and this method exists for generated sources, where whoever generated them already knows.
     * Returning an invented value would be worse than saying "I do not know".
     */
    javax.lang.model.element.NestingKind getNestingKind();

    /**
     * The main class's access level, or `null` if it is not known.
     *
     * <p>Only four values make sense --`public`, `protected`, `private` and `null` for
     * package-private-- and the same note as above applies: `null` is not a gap.
     */
    javax.lang.model.element.Modifier getAccessLevel();

    boolean isNameCompatible(String simpleName, Kind kind);
}
