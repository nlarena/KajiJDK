package javax.annotation.processing;

import javax.tools.JavaFileObject;
import java.io.Writer;
import java.io.StringWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

// KajiLibrary's JavaFileObject for a Filer-created source file (APT fase 4). It is a thin envelope
// over a name and a StringWriter: the writer is where the annotation processor writes the generated
// source, and openWriter() just hands it back. The VM (KajiFiler.nativeRegisterSourceFile) already
// holds the same writer, so the round loop can recover the text once the processor is done.
//
// Implementa la interfaz `JavaFileObject` **completa** (que a su vez extiende `FileObject`): además
// de `getName`/`openWriter` —lo único que el Filer usa de verdad— cumple el resto del contrato con
// implementaciones mínimas pero honestas (el texto generado se lee por `openReader`/`getCharContent`,
// y no hay flujo de bytes porque el respaldo es un `StringWriter`).
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

    // El texto generado hasta ahora, para releerlo (el round loop recompila esta fuente). No declara
    // `throws` (estrechar es válido, §8.4.8.3): `StringReader` no lanza excepción comprobada.
    public Reader openReader(boolean ignoreEncodingErrors) {
        return new StringReader(this.writer.toString());
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.writer.toString();
    }

    // No hay flujo de **bytes**: el respaldo es un `StringWriter` de caracteres. La
    // `UnsupportedOperationException` es no-comprobada, así que no hace falta declararla.
    public InputStream openInputStream() {
        throw new UnsupportedOperationException("KajiSourceFile es de caracteres, no de bytes");
    }

    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException("KajiSourceFile es de caracteres, no de bytes");
    }

    public long getLastModified() {
        return 0L;
    }

    public boolean delete() {
        return false;
    }

    // Una fuente `.java` generada.
    public JavaFileObject.Kind getKind() {
        return JavaFileObject.Kind.SOURCE;
    }

    public boolean isNameCompatible(String simpleName, JavaFileObject.Kind kind) {
        return kind == JavaFileObject.Kind.SOURCE && this.name.equals(simpleName);
    }
}
