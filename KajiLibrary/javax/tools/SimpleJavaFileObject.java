package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

// Base conveniente para escribir un JavaFileObject: guarda el URI y el Kind, y da un cuerpo
// por defecto a todo lo demas. Casi todos los metodos tiran UnsupportedOperationException a
// proposito — es la subclase la que decide cual sabe contestar. Eso es lo que hace el JDK, no
// una simplificacion nuestra.
//
// Esta clase quedo afuera en la primera pasada del paquete: sin `java.net.URI` se caian sus
// dos campos y su UNICO constructor, y javac habria sintetizado un `SimpleJavaFileObject()`
// sin argumentos que la API real no tiene. Se prefirio la ausencia a la firma inventada.
// Ahora `java.net.URI` existe y la clase entra completa.
//
// NOTA de escritura: el tipo anidado se nombra `JavaFileObject$Kind`, por su nombre BINARIO.
// Escrito `JavaFileObject.Kind` no resuelve (#101) y con `import` degrada en silencio a
// `Object` (#239). El nombre binario emite el descriptor exacto, y es lo que hace el propio
// `javac` del JDK cuando la clase viene del classpath. Sacar el `$` cuando se arregle #101.
// LIMITACION FORZADA (#104): los cinco metodos de I/O y `getCharContent` van SIN
// `throws IOException`, a diferencia del JDK. No es una decision: el `throws` de un metodo
// leido de un `.class` del classpath se ignora, y entonces el override legal se rechaza
// ("declara lanzar IOException, mas ancho que lo que permite FileObject"). El descriptor
// emitido es identico -- lo unico que falta es el atributo `Exceptions`. Es el mismo rodeo
// que ya usaron `JavaFileManager.flush`/`close`. Restaurar cuando se arregle #104.
public class SimpleJavaFileObject implements JavaFileObject {

    protected final URI uri;
    protected final JavaFileObject$Kind kind;

    protected SimpleJavaFileObject(URI uri, JavaFileObject$Kind kind) {
        this.uri = uri;
        this.kind = kind;
    }

    public URI toUri() {
        return this.uri;
    }

    // El camino del URI; para un URI opaco (mailto:...) no hay camino y vale su parte
    // especifica, que es lo que hace el JDK.
    public String getName() {
        String p = this.uri.getPath();
        if (p == null) {
            return this.uri.getSchemeSpecificPart();
        }
        return p;
    }

    public InputStream openInputStream() {
        throw new UnsupportedOperationException();
    }

    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    public Reader openReader(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    public Writer openWriter() {
        throw new UnsupportedOperationException();
    }

    // 0 significa "desconocido", no "epoch": es el contrato del JDK.
    public long getLastModified() {
        return 0L;
    }

    public boolean delete() {
        return false;
    }

    public JavaFileObject$Kind getKind() {
        return this.kind;
    }

    // "es este el archivo de `simpleName`, con la extension que le toca a `kind`". El JDK
    // compara contra el ultimo segmento del nombre; aca se hace igual, a mano, porque nuestro
    // String no tiene endsWith ni lastIndexOf.
    public boolean isNameCompatible(String simpleName, JavaFileObject$Kind kind) {
        if (kind != this.kind) {
            return false;
        }
        String name = getName();
        String tail = lastSegment(name);
        StringBuilder expected = new StringBuilder();
        expected.append(simpleName);
        expected.append(kind.extension);
        return tail.equals(expected.toString());
    }

    // null = "no se sabe", que es lo que devuelve la base del JDK.
    public NestingKind getNestingKind() {
        return null;
    }

    public Modifier getAccessLevel() {
        return null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("[");
        sb.append(this.uri.toString());
        sb.append("]");
        return sb.toString();
    }

    // Un objeto de archivo cuyo contenido ya esta en memoria: el caso de "compilame este
    // String". El JDK usa una anonima; aca es una clase nombrada del mismo archivo, que es
    // detalle interno y por lo tanto libre.
    public static JavaFileObject forSource(URI uri, String content) {
        return new SourceFromString(uri, content);
    }

    private static final class SourceFromString extends SimpleJavaFileObject {
        private final String content;

        SourceFromString(URI uri, String content) {
            super(uri, JavaFileObject$Kind.SOURCE);
            this.content = content;
        }

        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return this.content;
        }
    }

    // Lo que sigue al ultimo '/' — nuestro String no tiene lastIndexOf.
    private static String lastSegment(String s) {
        int len = s.length();
        int cut = -1;
        int i = 0;
        while (i < len) {
            if (s.charAt(i) == '/') {
                cut = i;
            }
            i = i + 1;
        }
        return s.substring(cut + 1, len);
    }
}
