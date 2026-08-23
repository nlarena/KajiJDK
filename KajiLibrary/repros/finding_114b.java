// Repro de #114 (reaparicion) - `"texto" + <referencia no-String>` no compila: falta
// StringBuilder.append(Object).
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_114b.java
//
// `conInt()` compila. Descomentar `conObjeto()` da:
//   error: no se encuentra el metodo: append
//
// #114 documenta este defecto con append(long) y lo da por arreglado del lado de la
// biblioteca: el arreglo fue PARCIAL. Nuestro java/lang/StringBuilder declara append de
// char, String, boolean y CharSequence - no de Object. El javac real pasa por
// String.valueOf.
//
// Ademas el diagnostico es inservible: sale en la LINEA 0, apunta al inicio del archivo
// y nombra `append`, no la concatenacion.
//
// La variante SILENCIOSA de #114 (emitir el invokespecial con la pila vacia, sin error)
// se observo en constructores con `super("..." + e + "...")`; en las formas simples de
// aca da error duro. Depende de la forma de la expresion.
public class finding_114b {
    public static String conInt(int i) { return "x" + i; }
    // public static String conObjeto(Object o) { return "x" + o; }   // <-- descomentar
}
