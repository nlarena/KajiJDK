// #513 -- en un `case`, un nombre SIMPLE que nombra una constante de la clase ENVOLVENTE no se
// pliega cuando la expresion esta en una clase anidada.
//
//   bin/javac.exe --emit Finding513.java
//     error: el generador de bytecode todavia no soporta un `case` que no es una constante entera
//
// javac del JDK 25 acepta las cuatro formas. Lo llamativo es el par 3/4: el mismo nombre, en la
// misma clase anidada, pliega o no segun si se lo califica. O sea que el camino que resuelve la
// ETIQUETA de un `case` no mira las clases envolventes, aunque el que resuelve una expresion
// comun si (lo muestra `control2`).
public class Finding513 {

    private static final char CH = '#';
    private static final int IN = 35;

    // 1) control: desde la clase envolvente, el nombre simple pliega.
    static int control1(char c) {
        switch (c) {
            case CH: return 1;
            default: return 0;
        }
    }

    static class Anidada {

        // 2) control: fuera de un `case`, el nombre simple resuelve bien desde la anidada.
        static boolean control2(char c) {
            return c == CH;
        }

        // 3) FALLA: nombre simple en un `case`, desde la anidada.
        static int falla(char c) {
            switch (c) {
                case CH: return 1;
                default: return 0;
            }
        }

        // 3b) FALLA igual con un int: no es cosa del tipo `char`.
        static int fallaInt(int c) {
            switch (c) {
                case IN: return 1;
                default: return 0;
            }
        }

        // 4) control: el MISMO caso, calificado, compila.
        static int control4(char c) {
            switch (c) {
                case Finding513.CH: return 1;
                default: return 0;
            }
        }
    }
}
