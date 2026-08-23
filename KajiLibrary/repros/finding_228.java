// Repro de #228 - un literal char/String con escape de SUSTITUTO se rechaza.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_228.java
//
// `ok()` compila; descomentar `bad()` da "error: literal char invalido". El javac real
// acepta las dos: es Java valido, y el propio JDK lo usa (Character.MIN_HIGH_SURROGATE
// se declara con ese escape).
//
// Rodeo mientras tanto: comparar contra 0xd800 numerico.
public class finding_228 {
    public static char ok()  { return '\u00ff'; }
    // public static char bad() { return '\ud800'; }   // <-- descomentar: no compila
}
