// Repro de #217 - falta la ampliacion implicita int -> long / double (JLS 5.1.2), y el
// metodo emite `ireturn` en un descriptor ()J: bytecode ESTRUCTURALMENTE INVALIDO.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_217.java
//   javap -c -p -cp KajiLibrary\repros finding_217
//
//   static long asLong();
//     descriptor: ()J
//        0: getstatic  #.. // Field size:I
//        3: ireturn                        <-- ilegal: ireturn no aplica a long
//
//   bin\run-headless.exe KajiLibrary\repros\finding_217.class run
//        -> panic: compare: expected a long, found Int(3)
//
// Falta en CINCO posiciones: return, init de local, asignacion a campo, paso de
// argumento y array store. Y tampoco emite i2d. Si funcionan la promocion binaria
// (`n + 1L`) y el cast explicito.
//
// Corolario (#217b): nuestro VERIFICADOR no rechaza este class file.
public class finding_217 {
    static int size = 3;
    static long asLong() { return finding_217.size; }
    public static int run() { if (finding_217.asLong() != 3L) { return 1; } return 0; }
}
