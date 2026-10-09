// Repro de #462: una constante heredada de una **interfaz implementada** no se resuelve por su
// nombre simple. La misma constante calificada anda, y la heredada de una **superclase** tambien.
//
// El JDK 25 compila el archivo entero y `run()` devuelve -1.
// Nuestro javac rechaza `deInterfaz()` y `enCampo`, y compila las otras tres.

interface Marcas {
    int DOS = 2;
}

class Base {
    static final int TRES = 3;
}

public class finding_462 extends Base implements Marcas {

    // FALLA: la constante de la interfaz implementada, por nombre simple, en un inicializador.
    private int enCampo = DOS;

    // FALLA: la misma, por nombre simple, en un cuerpo de metodo. Descarta que sea el
    // inicializador: es la herencia por interfaz lo que no llega al alcance.
    static int deInterfaz() {
        return DOS;
    }

    // ANDA: la misma constante, calificada. Descarta que el simbolo no exista o no se pueda leer.
    static int calificada() {
        return Marcas.DOS;
    }

    // ANDA: heredada de una **superclase**, por nombre simple. Es el control que parte el problema
    // en dos -- la herencia de miembros estaticos funciona; lo que no entra al alcance es la de la
    // clausula `implements`.
    static int deSuperclase() {
        return TRES;
    }

    public static int run() {
        if (deInterfaz() != 2) {
            return 0;
        }
        if (calificada() != 2) {
            return 1;
        }
        if (deSuperclase() != 3) {
            return 2;
        }
        if (new finding_462().enCampo != 2) {
            return 3;
        }
        return -1;
    }
}
