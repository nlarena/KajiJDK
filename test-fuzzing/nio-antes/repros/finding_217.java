// Repro de #217 y #217b - la ampliacion primitiva implicita (JLS 5.1.2) y el agujero del
// verificador que la dejaba pasar.
//
// No es "falta un i2l": el class file salia ESTRUCTURALMENTE INVALIDO.
//
//   static long asLong();
//     descriptor: ()J
//        0: getstatic  #.. // Field size:I
//        3: ireturn                        <-- ilegal: ireturn no aplica a un metodo `long`
//
// El emisor elegia la variante de `Xreturn` por el tipo de la EXPRESION en vez de por el
// descriptor declarado. Faltaba en CINCO posiciones -- return, init de local, asignacion a
// local, asignacion a campo, paso de argumento y array store -- y tampoco emitia `i2d`. Si
// funcionaban la promocion binaria (`n + 1L`) y el cast explicito, que es por lo que el bug
// sobrevivio tanto.
//
// Corolario #217b, del runtime: nuestro VERIFICADOR tampoco lo rechazaba. La pila queda
// consistente (el getstatic empuja un Int y el ireturn lo saca), asi que solo se detecta
// cotejando el opcode contra el descriptor del metodo (JVMS 6.5). Ahora se hace, y esta cubierto
// por `jvm::verifier::tests::return_opcode_must_match_the_declared_return_type`.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_217.java
//   bin\run-headless.exe KajiLibrary\repros\finding_217.class run     -> 0
public class finding_217 {

    static int size = 3;
    static long campoL;
    static double campoD;

    static long asLong() {
        return finding_217.size;
    }

    static double asDouble() {
        return finding_217.size;
    }

    static long tomaLong(long v) {
        return v;
    }

    static double tomaDouble(double v) {
        return v;
    }

    public static int run() {
        /* 1. return */
        if (finding_217.asLong() != 3L) {
            return 1;
        }
        if (finding_217.asDouble() != 3.0) {
            return 2;
        }

        /* 2. inicializacion de local */
        long local = finding_217.size;
        if (local != 3L) {
            return 3;
        }
        double locald = finding_217.size;
        if (locald != 3.0) {
            return 4;
        }

        /* 3. asignacion a local */
        local = 7;
        if (local != 7L) {
            return 5;
        }

        /* 4. asignacion a campo */
        finding_217.campoL = finding_217.size;
        if (finding_217.campoL != 3L) {
            return 6;
        }
        finding_217.campoD = finding_217.size;
        if (finding_217.campoD != 3.0) {
            return 7;
        }

        /* 5. paso de argumento */
        if (finding_217.tomaLong(finding_217.size) != 3L) {
            return 8;
        }
        if (finding_217.tomaDouble(finding_217.size) != 3.0) {
            return 9;
        }

        /* 6. array store */
        long[] arr = new long[1];
        arr[0] = finding_217.size;
        if (arr[0] != 3L) {
            return 10;
        }
        double[] arrd = new double[1];
        arrd[0] = finding_217.size;
        if (arrd[0] != 3.0) {
            return 11;
        }
        return 0;
    }
}
