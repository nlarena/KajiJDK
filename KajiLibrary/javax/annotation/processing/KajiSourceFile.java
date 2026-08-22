package javax.annotation.processing;

import javax.tools.JavaFileObject;
import java.io.Writer;
import java.io.StringWriter;

// KajiLibrary's JavaFileObject for a Filer-created source file (APT fase 4). It is a thin envelope
// over a name and a StringWriter: the writer is where the annotation processor writes the generated
// source, and openWriter() just hands it back. The VM (KajiFiler.nativeRegisterSourceFile) already
// holds the same writer, so the round loop can recover the text once the processor is done.
class KajiSourceFile implements JavaFileObject {

    private final String name;
    private final StringWriter writer;

    KajiSourceFile(String name, StringWriter writer) {
        this.name = name;
        this.writer = writer;
    }

    // The name the processor asked to create ("Foo" for a top-level class Foo).
    public String getName() {
        return this.name;
    }

    // The one sink the processor writes its generated source into. Widening StringWriter to the
    // declared Writer return type — no covariant-return bridge needed.
    public Writer openWriter() {
        return this.writer;
    }
}
