// REPRO (java.util.stream, 2026-09-01) — la resolucion de sobrecargas no elige la mas
// especifica sino la ULTIMA declarada. Aca `of(T...)` esta declarada ANTES que `of(T)`.
//
//   ../bin/javac.exe --emit -cp ../KajiLibrary java/OvLib3.java
//   ../bin/javac.exe --emit -cp "../KajiLibrary;java" java/OvUse3.java   -> ERROR
//
// El javac real liga `OvLib3.of(unArregloDeString)` a `of(T...)` con T = String; este liga a
// `of(T)` con T = String[]. Comparar con OvLib5.java, que es el mismo archivo con las dos
// declaraciones al reves y ahi si compila. Es por esto que java.util.stream.Stream declara
// `of(T)` antes que `of(T...)`.
import java.util.ArrayList;
public interface OvLib3<T> {
    static <T> ArrayList<T> of(T... vs) {
        ArrayList<T> a = new ArrayList<T>();
        for (int i = 0; i < vs.length; i++) { a.add(vs[i]); }
        return a;
    }
    static <T> ArrayList<T> of(T v) { ArrayList<T> a = new ArrayList<T>(); a.add(v); return a; }
}
