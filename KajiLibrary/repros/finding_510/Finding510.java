// #510 -- un `for` que declara dos variables en el inicializador no compila.
//
// JLS 14.14.1: el inicializador de un `for` puede ser una declaracion de variables locales con
// varios declaradores. Nosotros vemos solo el primero: el segundo nombre no entra en el alcance y
// el primero, aparentemente, tampoco queda visible en la condicion.
public class Finding510 {

    // --- falla ---------------------------------------------------------------------------------
    static int dosEnElFor() {
        int suma = 0;
        for (int i = 0, n = 5; i < n; i++) {   // error: no se encuentra el simbolo: i
            suma = suma + i;
        }
        return suma;
    }

    // --- anda: la misma cuenta con el limite afuera --------------------------------------------
    static int unaEnElFor() {
        int suma = 0;
        int n = 5;
        for (int i = 0; i < n; i++) {
            suma = suma + i;
        }
        return suma;
    }

    // --- anda: dos declaradores fuera de un `for` ----------------------------------------------
    static int dosEnUnaSentencia() {
        int a = 1, b = 2;
        return a + b;
    }

    public static int run() {
        System.out.println("//una " + unaEnElFor());
        System.out.println("//sentencia " + dosEnUnaSentencia());
        System.out.println("//dos " + dosEnElFor());
        return 0;
    }

    public static void main(String[] a) {
        run();
    }
}
