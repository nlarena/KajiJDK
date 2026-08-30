// Repro del finding #311: la referencia de una lambda quedaba **stale** si el GC corria durante su
// constructor.
//
// `invokedynamic` instanciaba la clase sintetica de la lambda asi:
//
//     let object = allocate(...);            // un offset crudo dentro del heap
//     self.call_java(ctor, ...);             // codigo real: puede asignar, puede colectar
//     self.top().push(Value::Reference(object));   // <- la direccion VIEJA
//
// El recolector mueve los objetos vivos y actualiza las raices -- la pila de operandos entre ellas --
// pero no puede saber nada de una variable local de Rust. Si la colecta caia durante el `<init>`, lo
// que se empujaba era una referencia a memoria ya reciclada, y el que llamaba veia un
// NullPointerException que no tenia nada que ver con su codigo.
//
// **Como se lo acorralo, que es la parte que vale.** El sintoma dependia de la presion de asignacion:
//
//   - con OCHO call sites en el metodo andaba, con NUEVE fallaba, con DIEZ volvia a andar
//     (con diez, el noveno es un Consumer que un Optional vacio nunca invoca);
//   - con `JVM_GC_EDEN_SIZE` agrandado -- o sea, sin colectas -- andaba siempre;
//   - y el `.class` que emite NUESTRO javac corre bien en `java` real.
//
// Lo ultimo es lo que lo ubica: el class file esta bien, el defecto es de la VM.
//
// El arreglo es empujar la referencia ANTES de correr el constructor, con lo que pasa a ser una raiz
// y el recolector la reubica junto con el objeto. Es exactamente para lo que el `new`/`dup`/
// `invokespecial` de javac tiene el `dup`.
//
// Da 11 (una rama corrio una vez, la otra ninguna), igual que `java` real.
import java.util.Optional;

public class finding_311 {

    public static int run() {
        Optional<String> hay = Optional.of("p");
        Optional<String> no = Optional.empty();
        // Cinco lambdas antes, para que el noveno call site caiga donde caia la colecta.
        no.or(() -> Optional.of("x0"));
        no.or(() -> Optional.of("x1"));
        no.or(() -> Optional.of("x2"));
        no.or(() -> Optional.of("x3"));
        no.or(() -> Optional.of("x4"));
        int[] c = new int[2];
        hay.ifPresentOrElse(x -> c[0]++, () -> c[1]++);
        no.ifPresentOrElse(x -> c[0]++, () -> c[1]++);
        return c[0] * 10 + c[1];
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
