package javax.tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;

// A convenient base for writing a JavaFileObject: it keeps the URI and the Kind, and gives a
// default body to everything else. Almost every method throws UnsupportedOperationException on
// purpose — it is the subclass that decides which ones it can answer. That is what the JDK does, not
// a simplification of ours.
//
// This class was left out on the package's first pass: without `java.net.URI` its two fields and its
// ONLY constructor fell away, and javac would have synthesized a no-argument `SimpleJavaFileObject()`
// that the real API does not have. Absence was preferred to an invented signature. Now `java.net.URI`
// exists and the class goes in complete.
//
// A NOTE ON WRITING IT: the nested type is named `JavaFileObject$Kind`, by its BINARY name. Written
// `JavaFileObject.Kind` it does not resolve (#101) and with an `import` it silently degrades to
// `Object` (#239). The binary name emits the exact descriptor, and it is what the JDK's own `javac`
// does when the class comes from the classpath. Drop the `$` once #101 is fixed.
// FORCED LIMITATION (#104): the five I/O methods and `getCharContent` go WITHOUT
// `throws IOException`, unlike the JDK. It is not a decision: the `throws` of a method read from a
// `.class` on the classpath is ignored, and then the legal override is rejected ("declares that it
// throws IOException, wider than what FileObject allows"). The emitted descriptor is identical -- the
// only thing missing is the `Exceptions` attribute. It is the same detour `JavaFileManager.flush`
// and `close` already used. Restore it once #104 is fixed.
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

    // The URI's path; for an opaque URI (mailto:...) there is no path and its scheme-specific part
    // stands in, which is what the JDK does.
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

    // 0 means "unknown", not "epoch": it is the JDK's contract.
    public long getLastModified() {
        return 0L;
    }

    public boolean delete() {
        return false;
    }

    public JavaFileObject$Kind getKind() {
        return this.kind;
    }

    // "is this `simpleName`'s file, with the extension `kind` calls for". The JDK compares against
    // the name's last segment; here it is done the same way, by hand, because our String has neither
    // endsWith nor lastIndexOf.
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

    // null = "not known", which is what the JDK's base returns.
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

    // A file object whose contents are already in memory: the "compile me this String" case. The JDK
    // uses an anonymous class; here it is a named class in the same file, which is an internal detail
    // and therefore free.
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

    // What follows the last '/' — our String has no lastIndexOf.
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
