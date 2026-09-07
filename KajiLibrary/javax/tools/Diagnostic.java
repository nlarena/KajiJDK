package javax.tools;

import java.util.Locale;

// KajiLibrary's javax.tools.Diagnostic<S> — one thing a tool has to say about one place in
// one source object. S is the type of that source object (for a compiler, JavaFileObject),
// which is why the interface is generic: the diagnostic carries the source, it does not
// merely describe it.
public interface Diagnostic<S> {

    // Used for position/line/column when the information is not available.
    //
    // `public static final` is written EXPLICITLY on purpose. The frozen javac does not apply the
    // implicit modifiers of an interface field (JLS 9.3): written as `long NOPOS = -1L;` it emits
    // the field with flags 0x0000, with no ConstantValue, and puts the initializer into a
    // synthesized `public <init>()V` INSIDE the interface (which also does a putstatic on a
    // non-static field). With the modifiers written out it comes out correct. See the report.
    public static final long NOPOS = -1L;

    Kind getKind();

    S getSource();

    long getPosition();

    long getStartPosition();

    long getEndPosition();

    long getLineNumber();

    long getColumnNumber();

    String getCode();

    String getMessage(Locale locale);

    public enum Kind { ERROR, WARNING, MANDATORY_WARNING, NOTE, OTHER; }
}
