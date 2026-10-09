// La otra mitad de #293: lo que ahora tiene que ser RECHAZADO.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_293b.java
//
// **Este archivo NO compila a proposito**, igual que finding_208. Es lo que prueba el arreglo: los
// tres `new` de abajo compilaban antes, en silencio, emitiendo `invokespecial <init>:()V` con los
// argumentos empujados.
//
// El compilador corta en el primer error, asi que hay que comentar los de arriba para ver los de
// abajo. Los tres mensajes esperados:
//
//   ctorDeFuente()     error: no se encontro un constructor `Solo(int)` aplicable
//                        metodo Solo.Solo() no es aplicable
//                          (las listas de argumentos difieren en longitud)
//
//   ctorDeClasspath()  error: el generador de bytecode todavia no soporta un `new` con argumentos
//                             que no resolvio a ningun constructor
//
//   sinNoArg()         idem -- y este es el que muestra que no se trata de "cayo al constructor
//                      por defecto": StringBuilder no tiene ninguno de dos argumentos, y salia
//                      `()V` lo mismo
//
// Los dos ultimos van por el codegen y no por la atribucion porque la clase es del **classpath**,
// donde la resolucion es indulgente a proposito: un no-match puede ser una limitacion nuestra al
// modelar las firmas del JDK. Ahi el que se planta es el emisor, que no tiene nada correcto que
// emitir de todos modos.
import java.util.List;

public class finding_293b {

    // (1) clase del fuente, aridad equivocada
    public static int ctorDeFuente() {
        return new Solo(7).n;
    }

    // (2) clase del classpath, constructor inexistente
    public static int ctorDeClasspath() {
        return new StringBuilder("x", "y").length();
    }

    // (3) el mismo, para dejar dicho que tampoco hace falta que exista un `()`
    public static int sinNoArg() {
        List<String> l = null;
        return new java.util.ArrayList<String>(l, 7).size();
    }
}

class Solo {

    int n;

    Solo() {
        this.n = 42;
    }
}
