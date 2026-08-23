package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

// KajiLibrary's javax.tools.ForwardingFileObject<F> — a FileObject that is another
// FileObject, member for member. On its own it does nothing; its whole point is to be
// subclassed so you can override one method and let the rest pass through unchanged.
//
// OMITIDO (salida (a), omitir el miembro): `java.net.URI toUri()`, porque java.net no existe
// en KajiLibrary. Es el mismo miembro que ya falta en FileObject, asi que esta clase sigue
// cubriendo el 100% de la interfaz tal como la declaramos.
//
// SIN `throws IOException` en los cinco metodos de I/O, y no por gusto: el javac congelado lee
// las firmas heredadas desde el .class del classpath SIN su atributo Exceptions, asi que ve
// `FileObject.openInputStream()` como si no lanzara nada y rechaza el override por ensanchar
// el throws (§8.4.8.3) — aunque la interfaz lo declare igualito. El descriptor emitido es el
// correcto; lo unico que falta es el atributo Exceptions. Ver el informe.
public class ForwardingFileObject<F extends FileObject> implements FileObject {

    protected final F fileObject;

    protected ForwardingFileObject(F fileObject) {
        this.fileObject = Objects.requireNonNull(fileObject);
    }

    public String getName() {
        return this.fileObject.getName();
    }

    public InputStream openInputStream() {
        return this.fileObject.openInputStream();
    }

    public OutputStream openOutputStream() {
        return this.fileObject.openOutputStream();
    }

    public Reader openReader(boolean ignoreEncodingErrors) {
        return this.fileObject.openReader(ignoreEncodingErrors);
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return this.fileObject.getCharContent(ignoreEncodingErrors);
    }

    public Writer openWriter() {
        return this.fileObject.openWriter();
    }

    public long getLastModified() {
        return this.fileObject.getLastModified();
    }

    public boolean delete() {
        return this.fileObject.delete();
    }
}
