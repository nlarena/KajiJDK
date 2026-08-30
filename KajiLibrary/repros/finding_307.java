// Repro del finding #307 -- ABIERTO, no arreglado.
//
// El `U` de un metodo generico no se infiere del CUERPO de la lambda:
//
//   Optional.of("perro").map(x -> x.toUpperCase()).orElseThrow().length()
//
// da "no se encuentra el metodo: length / ubicacion: clase Object": el `U` sale `Object` en vez de
// `String`. `javac` real lo compila. Con destino explicito (`Optional<String> r = ...`) a veces
// anda, lo que confirma que lo que falta es la restriccion que aporta el cuerpo (JLS 18.2.1).
//
// En `flatMap` es peor: tampoco le da tipo al PARAMETRO, porque su firma anida un comodin adentro
// de otro (`Function<? super T, ? extends Optional<? extends U>>`).
//
// Esta clase NO compila con nuestro javac. Queda como repro del pendiente.
import java.util.Optional;

public class finding_307 {
    public static int run() {
        return Optional.of("perro").map(x -> x.toUpperCase()).orElseThrow().length();
    }
}
