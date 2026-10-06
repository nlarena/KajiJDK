import java.util.Optional;

/**
 * Finding #307: la inferencia no recibia la restriccion que aporta el CUERPO de la lambda (JLS 18.2.1).
 *
 * <p>La `U` de `<U> Optional<U> map(Function<? super T, ? extends U>)` aparece SOLO en el retorno de la
 * funcion, asi que ningun otro argumento la toca. Y la lambda llegaba a la inferencia con su tipo sin
 * resolver, a proposito: `reattribute_poly_args` difiere toda lambda cuyo parametro nombra una variable
 * del propio metodo, para no tiparla contra una inferencia a medias (es lo que arreglo el #286). Entre
 * las dos cosas la `U` quedaba sin ninguna restriccion y `resolve` la cerraba en su cota: `Object`.
 *
 * <p>Encadenar algo despues es lo unico que lo destapaba --- `Optional<Object>` solo, pasa igual ---, y
 * daba "no se encuentra el metodo: length / ubicacion: clase Object".
 *
 * <p>El arreglo es una SEGUNDA RONDA de inferencia (18.5.2): despues de que `reattribute_deferred` tipa
 * los cuerpos, se juntan los pares `(lo que produce el argumento, retorno del SAM)` y se vuelve a
 * inferir con ellos como reducciones extra. El par sale de tres fuentes distintas, y las tres se miden
 * aca: el cuerpo-expresion, el cuerpo-bloque, y el retorno del metodo que una REFERENCIA nombra.
 *
 * <p>Devuelve <b>-1</b> si las siete formas dan lo que da el JDK 25, o el numero de la primera que no.
 * Con el javac anterior al arreglo no COMPILA: la forma 1 ya es "no se encuentra el metodo: length /
 * ubicacion: clase Object", y el javac corta ahi.
 */
public class finding_307 {

    public static int run() {
        // 1. El caso de la ficha: cuerpo-expresion, encadenado.
        if (Optional.of("perro").map(x -> x.toUpperCase()).orElseThrow().length() != 5) {
            return 1;
        }
        // 2. Una REFERENCIA A METODO: aca no hay cuerpo tipado, el tipo sale del metodo nombrado.
        if (Optional.of("perro").map(String::toUpperCase).orElseThrow().length() != 5) {
            return 2;
        }
        // 3. Un cuerpo-BLOQUE, que llega por otro camino: el tipo de sus `return`.
        if (Optional.of("perro").map(x -> { return x.toUpperCase(); }).orElseThrow().length() != 5) {
            return 3;
        }
        // 4. `flatMap`, que la ficha daba por "peor": su firma anida un comodin adentro de otro
        //    (`Function<? super T, ? extends Optional<? extends U>>`), asi que el par a reducir es
        //    `Optional<String>` contra `Optional<? extends U>`.
        if (Optional.of("perro").flatMap(x -> Optional.of(x.toUpperCase())).orElseThrow().length() != 5) {
            return 4;
        }
        // 5. A OTRO tipo: la `U` no tiene por que ser la `T`.
        if (Optional.of("perro").map(String::length).orElseThrow().intValue() != 5) {
            return 5;
        }
        // 6. CONTROL --- esta ya andaba, la guia el tipo objetivo de la asignacion. La segunda ronda no
        //    la tenia que tocar.
        Optional<String> o = Optional.of("perro").map(x -> x.toUpperCase());
        if (o.orElseThrow().length() != 5) {
            return 6;
        }
        // 7. CONTROL --- con argumento de tipo explicito no hay nada que inferir, y tampoco cambia.
        if (Optional.of("perro").<String>map(x -> x.toUpperCase()).orElseThrow().length() != 5) {
            return 7;
        }
        return -1;
    }

    public static void main(String[] a) {
        System.out.println(run());
    }
}
