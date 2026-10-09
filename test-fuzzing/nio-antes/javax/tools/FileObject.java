package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

// KajiLibrary's javax.tools.FileObject — the file abstraction the tool APIs are built on.
// A FileObject is "a thing you can read bytes or characters out of, and maybe write into";
// it is deliberately not a java.io.File, so a compiler can be fed sources held in memory.
//
// `java.net.URI toUri()` used to be missing here: java.net did not exist in KajiLibrary, and
// declaring `Object toUri()` would have been a false signature the gate would have taken for
// good — absence before a lie. java.net exists now and the member is back.
public interface FileObject {

    /**
     * The URI that **identifies** this object.
     *
     * <p>It is the identity, not the name: two `FileObject`s with the same `getName()` --`Foo.java`
     * in two directories-- have different URIs, and that is precisely the question a compiler needs
     * answered so as not to compile the same source twice nor confuse two sources of the same name.
     */
    java.net.URI toUri();

    String getName();

    InputStream openInputStream() throws IOException;

    OutputStream openOutputStream() throws IOException;

    Reader openReader(boolean ignoreEncodingErrors) throws IOException;

    CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException;

    Writer openWriter() throws IOException;

    long getLastModified();

    boolean delete();
}
