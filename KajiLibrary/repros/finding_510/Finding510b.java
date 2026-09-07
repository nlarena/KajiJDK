// #510, segunda forma -- la coma del inicializador de un `for` falla de dos maneras distintas.
//
//   bin/javac.exe --emit Finding510b.java
//     error: se esperaba Semi, se encontro Comma
//
// La forma con declaracion (`int i = 0, c = n`) pasa el analizador y pierde el alcance; esta, con
// asignacion, ni siquiera parsea. Y la coma en el AVANCE del mismo `for` compila, asi que la coma
// no es el problema: lo es el inicializador.
public class Finding510b {

    // FALLA: dos asignaciones en el inicializador.
    static int falla(int n) {
        int i;
        int c;
        int s = 0;
        for (i = 0, c = n; i < c; i++) {
            s = s + i;
        }
        return s;
    }

    // Control: la misma coma, en el avance.
    static int control1(int n) {
        int s = 0;
        for (int i = 0; i < n; i++, s++) {
            // nada
        }
        return s;
    }

    // Control: lo mismo sin coma.
    static int control2(int n) {
        int i = 0;
        int c = n;
        int s = 0;
        for (; i < c; i++) {
            s = s + i;
        }
        return s;
    }
}
