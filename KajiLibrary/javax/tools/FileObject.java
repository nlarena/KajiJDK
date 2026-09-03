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
// OMITIDO (salida (a), omitir el miembro): `java.net.URI toUri()`. El paquete java.net no
// existe en KajiLibrary (cero clases), y declarar `Object toUri()` sería una firma falsa
// que el gate daria por buena. Ausencia antes que mentira.
public interface FileObject {

    /**
     * El URI que **identifica** este objeto.
     *
     * <p>Es la identidad, no el nombre: dos `FileObject` con el mismo `getName()` --`Foo.java` en dos
     * directorios-- tienen URIs distintos, y esa es justamente la pregunta que un compilador necesita
     * responder para no compilar dos veces la misma fuente ni confundir dos fuentes homonimas.
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
