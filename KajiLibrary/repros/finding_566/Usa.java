// Repro del finding #566 -- ABIERTO. Paso 2 de 2.
//
// Como se corre, y los dos pasos son el punto: si `Op` se compila en la MISMA unidad que esto, anda,
// porque ahi el compilador tiene el AST y no el `.class`.
//
//   javac --emit -cp KajiLibrary Op.java
//   javac --emit -cp "KajiLibrary;<este directorio>" Usa.java
//
// Nuestro javac, en el paso 2:
//
//   Usa.java:26: error: excepcion chequeada `Throwable` sin capturar ni declarar en `throws`
//
// El javac del JDK 25 compila los dos pasos sin quejarse.
//
// La causa: al leer el `throws` de un metodo que viene de un `.class` se mira el `Exceptions`
// --- que lleva el BORRADO, `Throwable` --- y no el `^TX;` del `Signature`. Asi `throws X` se
// convierte en `throws Throwable`, y entonces este metodo, que declara `throws X`, no lo cubre.
//
// Lo tapaba un `.class` INVALIDO que el arbol tenia versionado: el `Exceptions` de
// `ScopedValue$CallableOp.call` nombraba un `Class` llamado `X`, que no es una clase. Al regenerar
// los `.class` con el javac congelado del 2026-10-06 ese `.class` se corrigio y el bug quedo a la
// vista. Es lo que hace que la biblioteca compile 4821/4822: `jdk/internal/vm/ScopedValueContainer`
// hace exactamente esta llamada.
public class Usa {
    static <V, X extends Throwable> V run(Op<V, X> op) throws X {
        return op.call();
    }

    public static void main(String[] a) {
        System.out.println(0);
    }
}
