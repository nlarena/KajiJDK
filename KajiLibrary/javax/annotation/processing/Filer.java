package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import java.io.IOException;

// Where a processor **creates** the files it generates (JSR 269 §Filer). It is not a general-purpose
// `JavaFileManager`: the tool keeps whatever is created here so as to feed it back in (a generated
// source triggers another round) and so as not to let two processors trample the same type.
//
// The `originatingElements` are varargs and may be empty: they are the elements that "caused" the
// file, and they serve incremental invalidation. The contract declares them optional explicitly, so
// passing none is legal and does not mean an error.
//
// This project's implementor is `KajiFiler`; only `createSourceFile` is really supported (see its
// header).
public interface Filer {

    /**
     * Creates a new `.java` source for the type `name` (its full, dotted name).
     *
     * @throws FilerException if that type was already created or the name is not valid
     */
    JavaFileObject createSourceFile(CharSequence name, Element... originatingElements)
            throws IOException;

    /**
     * Creates a new `.class` for the type `name`. Generating bytecode directly is legal but unusual:
     * the normal thing is to generate source and let the compiler compile it.
     */
    JavaFileObject createClassFile(CharSequence name, Element... originatingElements)
            throws IOException;

    /**
     * Creates an auxiliary resource (a `.properties`, a `META-INF/services/...`) at `location`.
     *
     * @param moduleAndPkg the package (or `module/package`) containing it; empty for the root
     * @param relativeName the file's name, relative to that package
     */
    FileObject createResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName, Element... originatingElements) throws IOException;

    /**
     * Opens an **existing** resource to read it. It creates nothing, and it takes no
     * `originatingElements` precisely because reading generates nothing.
     */
    FileObject getResource(JavaFileManager.Location location, CharSequence moduleAndPkg,
            CharSequence relativeName) throws IOException;
}
