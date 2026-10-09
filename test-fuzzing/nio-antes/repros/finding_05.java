// Finding #5 — identidad "fuente vs externo" de un tipo core en el chequeo de override.
// Al compilar `java.lang.String` en sí, el retorno `String` de `toString()` (heredado de
// Object) liga al `String` EXTERNO del classpath, distinto del `String` FUENTE que se está
// compilando, así que el chequeo de retorno covariante lo rechaza.
//
// Esperado (javac real): OK — el tipo fuente debe SOMBREAR al externo (semántica --patch-module).
// Síntoma del bug:       "String no es un subtipo de String".
// Familia: #5 / #7 / #9 (una misma raíz: shadowing del fuente + sustitución de variables de tipo).
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_05.java
package java.lang;

public class String implements CharSequence {
    public String toString() { return this; }
    public int length() { return 0; }
    public char charAt(int i) { return 'a'; }
    public CharSequence subSequence(int a, int b) { return this; }
}
