package java.util.stream;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

// KajiLibrary's java.util.stream.Collector<T,A,R> — the recipe a Stream's terminal `collect`
// follows to fold elements of type T into a result of type R, using a mutable accumulator of
// type A: create one (`supplier`), fold each element in (`accumulator`), merge partials
// (`combiner`), and finish (`finisher`). `characteristics` flags optimisations the pipeline
// may exploit.
//
// Ya no falta nada: la interfaz esta completa. Los dos motivos que la pasada anterior anoto para
// dejar afuera `characteristics()` y las dos `of(...)` estaban vivos entonces y ya no lo estan:
//
//   - la *identidad* de un tipo anidado sobrevive hoy a cruzar unidades de compilacion. Con el
//     `enum Characteristics` declarado aca y `CollectorImpl` (en Collectors.java) implementando
//     `Set<Collector.Characteristics> characteristics()`, el chequeo de sobreescritura pasa —
//     compilando los dos archivos juntos y compilando cada uno contra el `.class` del otro, que
//     es como los compila tools/apidiff/recompile.py;
//   - `ACC_VARARGS` se emite (#200), asi que el descriptor de `of(..., Characteristics...)` es el
//     mismo que el del JDK y el `javap` real imprime los puntos suspensivos.
//
// Lo que si se copia del JDK es la LECTURA de las caracteristicas: son PERMISOS, no promesas. Un
// conjunto vacio siempre es correcto — significa "no habilito ninguna optimizacion" — y por eso
// es lo que devuelven, por omision, los colectores de Collectors.java que no pueden justificar
// una. Declarar IDENTITY_FINISH sin serlo, en cambio, seria mentir: quien lo lea puede saltearse
// el finalizador y quedarse con el acumulador.
public interface Collector<T, A, R> {

    /**
     * Los permisos que un colector le da a quien lo ejecuta.
     *
     * <p>Cada constante habilita una optimizacion. Ninguna es obligatoria de honrar, y ninguna se
     * verifica: un colector que declara `IDENTITY_FINISH` sin que su finalizador sea la identidad
     * produce resultados equivocados y nada lo va a atrapar.
     *
     * <p>Nuestro `collect` es secuencial, ansioso y siempre llama al finalizador, asi que no lee
     * ninguna de las tres. Estan igual porque describen al colector, no al motor: un colector
     * nuestro pasado a codigo escrito contra el JDK real tiene que decir la verdad sobre si mismo.
     */
    enum Characteristics {

        /** El acumulador soporta que varios hilos lo alimenten a la vez. */
        CONCURRENT,

        /** El resultado no depende del orden en que lleguen los elementos. */
        UNORDERED,

        /**
         * El finalizador es la identidad: el acumulador ya <em>es</em> el resultado.
         *
         * <p>Habilita saltearse la llamada a `finisher()`, y por eso solo se puede declarar
         * cuando `A` y `R` son el mismo tipo.
         */
        IDENTITY_FINISH
    }

    /**
     * Como crear un acumulador vacio.
     *
     * @return el proveedor
     */
    Supplier<A> supplier();

    /**
     * Como meter un elemento en el acumulador.
     *
     * @return el acumulador
     */
    BiConsumer<A, T> accumulator();

    /**
     * Como fusionar dos acumuladores parciales.
     *
     * @return el combinador
     */
    BinaryOperator<A> combiner();

    /**
     * Como pasar del acumulador al resultado.
     *
     * @return el finalizador
     */
    Function<A, R> finisher();

    /**
     * Los permisos de este colector. Puede ser vacio, y vacio siempre es correcto.
     *
     * @return el conjunto, que no se puede modificar
     */
    Set<Characteristics> characteristics();

    /**
     * Un colector cuyo acumulador ya es el resultado.
     *
     * <p>Se le agrega `IDENTITY_FINISH` a lo que pida quien llama, porque el finalizador que se
     * arma aca es la identidad de verdad. Es lo mismo que hace el JDK.
     *
     * @param supplier como crear el acumulador
     * @param accumulator como meter un elemento
     * @param combiner como fusionar dos parciales
     * @param characteristics los permisos extra
     * @param <T> el tipo de los elementos
     * @param <R> el tipo del acumulador, que es tambien el del resultado
     * @return el colector
     * @throws NullPointerException si alguna pieza es null
     */
    static <T, R> Collector<T, R, R> of(Supplier<R> supplier, BiConsumer<R, T> accumulator,
                                        BinaryOperator<R> combiner, Characteristics... characteristics) {
        Function<R, R> finisher = new SelfFinisher<R>();
        Set<Characteristics> permisos = CharacteristicSet.of(characteristics, true);
        return new CollectorOf<T, R, R>(supplier, accumulator, combiner, finisher, permisos);
    }

    /**
     * Un colector con las cinco piezas dadas.
     *
     * @param supplier como crear el acumulador
     * @param accumulator como meter un elemento
     * @param combiner como fusionar dos parciales
     * @param finisher como pasar del acumulador al resultado
     * @param characteristics los permisos
     * @param <T> el tipo de los elementos
     * @param <A> el tipo del acumulador
     * @param <R> el tipo del resultado
     * @return el colector
     * @throws NullPointerException si alguna pieza es null
     */
    static <T, A, R> Collector<T, A, R> of(Supplier<A> supplier, BiConsumer<A, T> accumulator,
                                           BinaryOperator<A> combiner, Function<A, R> finisher,
                                           Characteristics... characteristics) {
        Set<Characteristics> permisos = CharacteristicSet.of(characteristics, false);
        return new CollectorOf<T, A, R>(supplier, accumulator, combiner, finisher, permisos);
    }
}

// ---- las piezas de las dos fabricas -----------------------------------------------------------
//
// Viven en ESTE archivo y no en Collectors.java, aunque alla ya haya un `CollectorImpl` casi
// igual, para no crear un ciclo entre las dos unidades de compilacion: hoy Collectors.java
// depende de Collector.java y no al reves, y tools/apidiff/recompile.py compila de a un archivo
// por vez. Cuatro campos duplicados cuestan menos que un ciclo.

// El conjunto de permisos: una copia inmodificable, para que nadie lo cambie despues.
final class CharacteristicSet {

    private CharacteristicSet() {
    }

    static Set<Collector.Characteristics> of(Collector.Characteristics[] pedidos, boolean identityFinish) {
        HashSet<Collector.Characteristics> s = new HashSet<Collector.Characteristics>();
        if (pedidos != null) {
            for (int i = 0; i < pedidos.length; i++) {
                s.add(pedidos[i]);
            }
        }
        if (identityFinish) {
            s.add(Collector.Characteristics.IDENTITY_FINISH);
        }
        return Collections.unmodifiableSet(s);
    }
}

// El finalizador identidad de `Collector.of(supplier, accumulator, combiner, ...)`.
final class SelfFinisher<R> implements Function<R, R> {
    public R apply(R acumulador) {
        return acumulador;
    }
}

final class CollectorOf<T, A, R> implements Collector<T, A, R> {

    private final Supplier<A> supplier;
    private final BiConsumer<A, T> accumulator;
    private final BinaryOperator<A> combiner;
    private final Function<A, R> finisher;
    private final Set<Collector.Characteristics> characteristics;

    CollectorOf(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner,
                Function<A, R> finisher, Set<Collector.Characteristics> characteristics) {
        if (supplier == null || accumulator == null || combiner == null || finisher == null) {
            // Mensaje constante: la concatenacion de String en tiempo de ejecucion no esta
            // disponible en nuestra VM (#226).
            throw new NullPointerException("una pieza del colector es null");
        }
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.combiner = combiner;
        this.finisher = finisher;
        this.characteristics = characteristics;
    }

    public Supplier<A> supplier() {
        return this.supplier;
    }

    public BiConsumer<A, T> accumulator() {
        return this.accumulator;
    }

    public BinaryOperator<A> combiner() {
        return this.combiner;
    }

    public Function<A, R> finisher() {
        return this.finisher;
    }

    public Set<Collector.Characteristics> characteristics() {
        return this.characteristics;
    }
}
