// #461 -- una constante `case` que viene de OTRA unidad de compilacion se rechaza.
//
// Este archivo compila con el JDK 25 y `run()` devuelve -1. Con nuestro javac el archivo entero
// falla en `ajena()`:
//
//     error: el generador de bytecode todavia no soporta un `case` que no es una constante entera
//         case Integer.MAX_VALUE:
//              ^
//
// Los tres controles estan para acotar donde esta el corte, y ninguno de ellos falla:
//   - `propia()`  -- constante de la misma clase, por nombre simple;
//   - `anidada()` -- constante de una clase ANIDADA, calificada;
//   - `vecina()`  -- constante de otra clase de PRIMER NIVEL del mismo archivo, calificada.
//
// O sea: no es el nombre calificado ni el `static final` lo que molesta, es que el tipo que lleva
// la constante venga de otro archivo. `fuera()` prueba que la misma constante ajena se lee sin
// problemas fuera de un `case`, asi que tampoco es la resolucion del simbolo.
class Vecina461 {
    static final int V = 3;
}

public class finding_461 {

    static final int P = 1;

    static class Anidada {
        static final int N = 2;
    }

    /** El que falla: constante de otra unidad de compilacion en la etiqueta. */
    public static int ajena() {
        int x = Integer.MAX_VALUE;
        switch (x) {
            case Integer.MAX_VALUE:
                return -1;
            default:
                return 0;
        }
    }

    /** Control: constante de la misma clase, nombre simple. Anda. */
    public static int propia() {
        switch (1) {
            case P:
                return -1;
            default:
                return 0;
        }
    }

    /** Control: constante de una clase anidada, calificada. Anda. */
    public static int anidada() {
        switch (2) {
            case Anidada.N:
                return -1;
            default:
                return 0;
        }
    }

    /** Control: otra clase de primer nivel del mismo archivo, calificada. Anda. */
    public static int vecina() {
        switch (3) {
            case Vecina461.V:
                return -1;
            default:
                return 0;
        }
    }

    /** Control: la misma constante ajena, pero fuera de un `case`. Anda. */
    public static int fuera() {
        int lim = Integer.MAX_VALUE;
        return lim == 2147483647 ? -1 : 0;
    }

    public static int run() {
        if (propia() != -1) {
            return 1;
        }
        if (anidada() != -1) {
            return 2;
        }
        if (vecina() != -1) {
            return 3;
        }
        if (fuera() != -1) {
            return 4;
        }
        if (ajena() != -1) {
            return 5;
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
