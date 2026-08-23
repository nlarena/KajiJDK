// Repro de #200 - el javac no emite ACC_VARARGS (0x0080, JVMS 4.6).
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_200.java
//   javap -p -cp KajiLibrary\repros finding_200
//
// El javac REAL imprime `f(java.lang.String, java.lang.Object...)`; el nuestro
// `f(java.lang.String, java.lang.Object[])`. El descriptor es correcto en los dos
// casos: lo unico que falta es el flag, asi que nadie puede llamarlo en forma varargs.
//
// Aislado con matriz cruzada: nuestro javap sobre bytecode del javac real SI muestra
// `...`, o sea que el defecto esta en el EMISOR, no en el desensamblador.
public class finding_200 {
    public static String f(String s, Object... a) { return s; }
}
