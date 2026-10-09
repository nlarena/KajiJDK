package javax.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

// KajiLibrary's javax.tools.ForwardingFileObject<F> — a FileObject that is another
// FileObject, member for member. On its own it does nothing; its whole point is to be
// subclassed so you can override one method and let the rest pass through unchanged.
//
// `java.net.URI toUri()` used to be missing here, as it was in FileObject, because java.net did not
// exist in KajiLibrary. It exists now and the delegation is back, so this class covers the whole
// interface.
public class ForwardingFileObject<F extends FileObject> implements FileObject {

    protected final F fileObject;

    protected ForwardingFileObject(F fileObject) {
        this.fileObject = Objects.requireNonNull(fileObject);
    }

    public java.net.URI toUri() {
        return this.fileObject.toUri();
    }

    public String getName() {
        return this.fileObject.getName();
    }

    public InputStream openInputStream() throws IOException {
        return this.fileObject.openInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return this.fileObject.openOutputStream();
    }

    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return this.fileObject.openReader(ignoreEncodingErrors);
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return this.fileObject.getCharContent(ignoreEncodingErrors);
    }

    public Writer openWriter() throws IOException {
        return this.fileObject.openWriter();
    }

    public long getLastModified() {
        return this.fileObject.getLastModified();
    }

    public boolean delete() {
        return this.fileObject.delete();
    }
}
