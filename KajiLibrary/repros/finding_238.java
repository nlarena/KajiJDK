// Repro de #238 - un campo de interfaz SIN `public static final` explicito produce un
// class file invalido. JLS 9.3: esos modificadores son IMPLICITOS.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_238.java
//   javap -v -p -cp KajiLibrary\repros finding_238
//
// Tres violaciones a la vez:
//   long NOPOS;
//     flags: (0x0000)                  <-- ni public, ni static, ni final; sin ConstantValue
//   public default finding_238();      <-- un <init>()V DENTRO de una interfaz (ilegal)
//        7: putstatic ... NOPOS:J      <-- putstatic sobre un campo no estatico
//
// Escribirlo `public static final long NOPOS = -1L;` sale correcto (0x0019 + ConstantValue).
public interface finding_238 {
    long NOPOS = -1L;
}
