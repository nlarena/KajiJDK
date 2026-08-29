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
// Implementa la interfaz `JavaFileObject` **completa** (que a su vez extiende `FileObject`): además
// de `getName`/`openWriter` —lo único que el Filer usa de verdad— cumple el resto del contrato con
// implementaciones mínimas pero honestas, con el comportamiento que documenta `SimpleJavaFileObject`
// del JDK (la referencia para un file object que no vive en disco). `JavaFileObject.Kind` es un tipo
// anidado de otra unidad de compilación, y nombrarlo era justo lo que el compilador no podía hacer
// (#239/#267): al desbloquearse, esta clase quedó completa.
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

    // Un archivo que crea el Filer es siempre fuente.
    public Kind getKind() {
        return Kind.SOURCE;
    }

    public boolean isNameCompatible(String simpleName, Kind kind) {
        return kind == Kind.SOURCE && this.name.equals(simpleName);
    }

    // Lo que el procesador lleva escrito: es como el ciclo de APT recupera el texto generado sin
    // volver a pasar por la VM. No declara `throws` (estrechar es válido, §8.4.8.3).
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.writer.toString();
    }

    public Reader openReader(boolean ignoreEncodingErrors) {
        return new StringReader(this.writer.toString());
    }

    // Los dos flujos de **bytes** NO se soportan, igual que en `SimpleJavaFileObject`: este objeto es
    // texto en memoria y no hay codificación elegida con la que convertirlo sin inventarla. La
    // `UnsupportedOperationException` es no-comprobada, así que no hace falta declararla.
    public InputStream openInputStream() {
        throw new UnsupportedOperationException("KajiSourceFile es texto en memoria, no bytes");
    }

    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException("KajiSourceFile es texto en memoria, no bytes");
    }

    // Cero: el contrato dice "0 si no se sabe", y de un buffer en memoria no se sabe.
    public long getLastModified() {
        return 0L;
    }

    // No hay nada que borrar.
    public boolean delete() {
        return false;
    }
}
