package javax.annotation.processing;

import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import java.io.Writer;
import java.io.StringWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.OutputStream;

// KajiLibrary's JavaFileObject for a Filer-created source file (APT fase 4). It is a thin envelope
// over a name and a StringWriter: the writer is where the annotation processor writes the generated
// source, and openWriter() just hands it back. The VM (KajiFiler.nativeRegisterSourceFile) already
// holds the same writer, so the round loop can recover the text once the processor is done.
//
// It implements the **complete** `JavaFileObject` interface (which in turn extends `FileObject`):
// besides `getName`/`openWriter` —the only things the Filer really uses— it fulfils the rest of the
// contract with minimal but honest implementations, with the behaviour the JDK's
// `SimpleJavaFileObject` documents (the reference for a file object that does not live on disk).
// `JavaFileObject.Kind` is a type nested in another compilation unit, and naming it was exactly what
// the compiler could not do (#239/#267): once that was unblocked, this class became complete.
class KajiSourceFile implements JavaFileObject {

    private final String name;
    private final StringWriter writer;

    KajiSourceFile(String name, StringWriter writer) {
        this.name = name;
        this.writer = writer;
    }

    // The name the processor asked to create ("Foo" for a top-level class Foo).
    /**
     * The URI that identifies this generated source.
     *
     * <p>A `kaji:` scheme and not `file:`, on purpose: this source **is not on disk** -- it lives in
     * the `StringWriter` the `Filer` handed over. A `file:` would promise a file nobody can open.
     */
    public java.net.URI toUri() {
        return java.net.URI.create("kaji:///" + this.getName());
    }

    /**
     * The nesting, or `null`: finding it out means reading the generated text, and this class does not
     * parse it. `null` is the answer the contract defines for "not known".
     */
    public javax.lang.model.element.NestingKind getNestingKind() {
        return null;
    }

    /** The access level, or `null`. The same reason as above. */
    public javax.lang.model.element.Modifier getAccessLevel() {
        return null;
    }

    public String getName() {
        return this.name;
    }

    // The one sink the processor writes its generated source into. Widening StringWriter to the
    // declared Writer return type — no covariant-return bridge needed.
    public Writer openWriter() {
        return this.writer;
    }

    // A file the Filer creates is always source.
    public Kind getKind() {
        return Kind.SOURCE;
    }

    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind == Kind.SOURCE && this.name.equals(simpleName);
    }

    // What the processor has written so far: it is how APT's cycle recovers the generated text
    // without going back through the VM. It declares no `throws` (narrowing is valid, §8.4.8.3).
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.writer.toString();
    }

    public Reader openReader(boolean ignoreEncodingErrors) {
        return new StringReader(this.writer.toString());
    }

    // The two **byte** streams are NOT supported, just as in `SimpleJavaFileObject`: this object is
    // text in memory and there is no chosen encoding to convert it with that would not be invented.
    // `UnsupportedOperationException` is unchecked, so it does not have to be declared.
    public InputStream openInputStream() {
        throw new UnsupportedOperationException("KajiSourceFile is text in memory, not bytes");
    }

    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException("KajiSourceFile is text in memory, not bytes");
    }

    // Zero: the contract says "0 if not known", and of an in-memory buffer it is not known.
    public long getLastModified() {
        return 0L;
    }

    // There is nothing to delete.
    public boolean delete() {
        return false;
    }
}
