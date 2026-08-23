package javax.tools;

import java.util.Locale;

// KajiLibrary's javax.tools.Diagnostic<S> — one thing a tool has to say about one place in
// one source object. S is the type of that source object (for a compiler, JavaFileObject),
// which is why the interface is generic: the diagnostic carries the source, it does not
// merely describe it.
public interface Diagnostic<S> {

    // Se usa para posicion/linea/columna cuando la informacion no esta disponible.
    //
    // `public static final` va EXPLICITO a proposito. El javac congelado no aplica los
    // modificadores implicitos de campo de interfaz (JLS 9.3): escrito como `long NOPOS = -1L;`
    // emite el campo con flags 0x0000, sin ConstantValue, y mete el inicializador en un
    // `public <init>()V` sintetizado DENTRO de la interfaz (que ademas hace putstatic sobre un
    // campo no estatico). Con los modificadores escritos sale correcto. Ver el informe.
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
