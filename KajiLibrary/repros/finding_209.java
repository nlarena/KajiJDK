// Repro de #209 - el literal de clase de un primitivo no parsea.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_209.java
//
// `ok()` compila. Descomentar `bad()` da:
//   error: se esperaba una expresion, se encontro Int
// El javac real acepta las dos.
//
// Consecuencia concreta: no hay NINGUNA expresion Java cuyo valor sea el mirror de un
// primitivo, asi que MethodType.unwrap() no se puede implementar - y el escape clasico,
// Integer.TYPE, esta declarado en el JDK justamente como `= int.class`.
public class finding_209 {
    public static Class<?> ok()  { return Integer.class; }
    // public static Class<?> bad() { return int.class; }   // <-- descomentar: no compila
}
