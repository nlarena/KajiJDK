package java.util.stream;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/**
 * La receta de una operacion intermedia definida por el usuario: lo que `Stream.gather` ejecuta.
 *
 * <p>Un `Gatherer` tiene cuatro piezas, igual que un `Collector`: como crear el estado privado
 * (`initializer`), que hacer con cada elemento (`integrator`), como fusionar dos estados
 * parciales (`combiner`) y que emitir al terminar (`finisher`).
 *
 * <p><b>Por que este tipo si entra en una biblioteca de flujos ansiosos, y `generate` no.</b> La
 * diferencia esta en quien empuja. Un flujo perezoso <em>tira</em> de su fuente, y por eso una
 * fuente sin fin se puede representar sin materializarla; un `Gatherer` <em>empuja</em> hacia el
 * `Downstream` que le pasan. Empujar no necesita pereza: recorremos el arreglo de entrada una vez,
 * llamamos al integrador por elemento y juntamos lo que salga. El unico rasgo del modelo perezoso
 * que hay que emular a mano es el <b>corte</b>, y esta contemplado: si `integrate` devuelve
 * `false` el recorrido para ahi, sin visitar el resto (ver `Stream.gather`).
 *
 * <p><b>Cuatro fabricas estan declaradas pero HOY NO SE PUEDEN LLAMAR, y la culpa es del
 * compilador, no de la implementacion.</b> Son las cuatro que reciben un finalizador:
 * `of(Integrator, BiConsumer)`, `of(Supplier, Integrator, BinaryOperator, BiConsumer)`,
 * `ofSequential(Integrator, BiConsumer)` y `ofSequential(Supplier, Integrator, BiConsumer)`. Este
 * javac no da por aplicable ningun metodo cuyo <b>parametro</b> meta una variable de tipo dentro
 * de un argumento de tipo <b>invariante</b> --la forma `BiConsumer&lt;A, Downstream&lt;R&gt;&gt;`--,
 * aunque el argumento tenga exactamente ese tipo escrito a mano y aunque se le pase un `witness`
 * explicito. Lo que salva el caso es un comodin <em>en esa posicion</em>
 * (`Supplier&lt;? extends Spliterator&lt;T&gt;&gt;` si resuelve, y por eso `StreamSupport` se puede
 * llamar entero); el `? super R` de estas cuatro esta <em>adentro</em> de `Downstream`, no en la
 * posicion anidada, asi que no salva. Es la misma familia que ya tenia anotada `Stream.mapMulti`,
 * solo que aca sale como error duro ("no resolvio a ningun metodo") y no en silencio. Repro con las
 * cinco variantes: java/WcLib3.java + java/WcUse3.java.
 * Los cuerpos son correctos y el dia que el compilador resuelva la llamada quedan andando; mientras
 * tanto, `Gatherers` arma sus `GathererImpl` con el constructor, que si resuelve porque los
 * argumentos de tipo de la clase se escriben y no se infieren. Lo que SI se puede llamar hoy esta
 * cubierto por la sonda java/GfacProbe.java.
 *
 * <p><b>Divergencia con el JDK, y es la unica.</b> `combiner()` describe como fusionar dos estados
 * de dos mitades evaluadas <em>en paralelo</em>. Nuestro `gather` es secuencial y nunca parte la
 * entrada, asi que nunca lo llama. El combinador por omision de `defaultCombiner()` se niega al
 * ser invocado, exactamente como el del JDK: un combinador que no puede fusionar honestamente es
 * mejor que uno que devuelve cualquiera de los dos lados y finge.
 *
 * @param <T> el tipo de los elementos que entran
 * @param <A> el tipo del estado privado (`Void` si no hace falta ninguno)
 * @param <R> el tipo de los elementos que salen
 */
public interface Gatherer<T, A, R> {

    /**
     * Adonde un `Gatherer` empuja los elementos que produce.
     *
     * <p>`push` devuelve `false` cuando lo de abajo ya no quiere mas -- porque hubo un `limit`, o
     * porque un `Gatherer` compuesto mas abajo corto. Un integrador que respeta esa respuesta es
     * lo que hace que un corte se propague hacia arriba en vez de recorrer la entrada entera.
     *
     * @param <T> el tipo de lo que se empuja
     */
    interface Downstream<T> {

        /**
         * Empuja un elemento hacia abajo.
         *
         * @param element el elemento
         * @return `false` si de aca en adelante no se acepta nada mas
         */
        boolean push(T element);

        /**
         * Si ya se sabe que ningun `push` posterior va a ser aceptado.
         *
         * <p>Es una pregunta, no una promesa al reves: un `false` no garantiza que el proximo
         * `push` se acepte. Sirve para abandonar un calculo caro que igual se iba a descartar.
         *
         * @return `true` si esta rechazando
         */
        default boolean isRejecting() {
            return false;
        }
    }

    /**
     * Que hacer con cada elemento de entrada.
     *
     * <p>El orden de los parametros de tipo es `A, T, R` --estado, entrada, salida-- y no el
     * `T, A, R` del `Gatherer` que lo contiene. Es el orden del JDK y se respeta tal cual: es el
     * orden en que aparecen en `integrate`.
     *
     * @param <A> el estado privado
     * @param <T> el tipo de entrada
     * @param <R> el tipo de salida
     */
    interface Integrator<A, T, R> {

        /**
         * Procesa un elemento, empujando cero o mas elementos hacia `downstream`.
         *
         * @param state el estado privado
         * @param element el elemento de entrada
         * @param downstream adonde empujar lo producido
         * @return `false` para pedir que no se le mande ningun elemento mas
         */
        boolean integrate(A state, T element, Downstream<? super R> downstream);

        /**
         * Devuelve su argumento.
         *
         * <p>Existe solo para darle a una lambda un tipo objetivo donde escribirla; en el JDK
         * lleva `@ForceInline` porque el `invokestatic` desaparece en la compilacion JIT.
         *
         * @param integrator el integrador
         * @param <A> el estado
         * @param <T> el tipo de entrada
         * @param <R> el tipo de salida
         * @return `integrator`
         */
        static <A, T, R> Integrator<A, T, R> of(Integrator<A, T, R> integrator) {
            return integrator;
        }

        /**
         * Devuelve su argumento, tipado como `Greedy`.
         *
         * @param greedy el integrador voraz
         * @param <A> el estado
         * @param <T> el tipo de entrada
         * @param <R> el tipo de salida
         * @return `greedy`
         */
        static <A, T, R> Integrator.Greedy<A, T, R> ofGreedy(Integrator.Greedy<A, T, R> greedy) {
            return greedy;
        }

        /**
         * Un integrador que promete no cortar nunca: su `integrate` siempre devuelve `true`.
         *
         * <p>Es una interfaz marcadora --no agrega ningun miembro--, y esa promesa le sirve al
         * JDK para saltearse el chequeo de corte. Nuestro `gather` mira el valor devuelto de
         * todas formas, asi que aca el marcador no cambia el resultado, solo lo documenta.
         *
         * @param <A> el estado
         * @param <T> el tipo de entrada
         * @param <R> el tipo de salida
         */
        interface Greedy<A, T, R> extends Integrator<A, T, R> {
        }
    }

    /**
     * Como crear el estado privado. Por omision, uno que no existe (`null`).
     *
     * @return el proveedor del estado inicial
     */
    default Supplier<A> initializer() {
        return Gatherer.<A>defaultInitializer();
    }

    /**
     * Que hacer con cada elemento. Es la unica pieza obligatoria.
     *
     * @return el integrador
     */
    Integrator<A, T, R> integrator();

    /**
     * Como fusionar dos estados parciales. Por omision, no se puede: ver `defaultCombiner`.
     *
     * @return el combinador
     */
    default BinaryOperator<A> combiner() {
        return Gatherer.<A>defaultCombiner();
    }

    /**
     * Que emitir cuando se acabo la entrada. Por omision, nada.
     *
     * @return el finalizador
     */
    default BiConsumer<A, Downstream<? super R>> finisher() {
        return Gatherer.<A, R>defaultFinisher();
    }

    /**
     * Este `Gatherer` seguido de `that`: lo que este empuja es lo que aquel recibe.
     *
     * <p>El compuesto lleva los dos estados en un `Object[2]` y le da al primero un `Downstream`
     * intermedio que alimenta al segundo. El corte viaja en las dos direcciones: si el segundo
     * rechaza, el `push` del intermedio devuelve `false` y el primero se entera; si el primero
     * corta, el recorrido termina y solo quedan los dos finalizadores, en orden.
     *
     * @param that el `Gatherer` que va despues
     * @param <RR> lo que sale del compuesto
     * @return la composicion
     * @throws NullPointerException si `that` es null
     */
    default <RR> Gatherer<T, ?, RR> andThen(Gatherer<? super R, ?, ? extends RR> that) {
        Objects.requireNonNull(that);
        // Los dos lados se ven como `Gatherer<..., Object, ...>`: el estado de cada mitad es
        // opaco para el compuesto, que solo lo guarda y lo pasa. La conversion es no chequeada
        // en el sentido de las genericas y exacta en el del borrado.
        Object self = this;
        Object other = that;
        Gatherer<T, Object, R> primero = (Gatherer<T, Object, R>) self;
        Gatherer<R, Object, RR> segundo = (Gatherer<R, Object, RR>) other;
        return new CompositeGatherer<T, R, RR>(primero, segundo);
    }

    /**
     * El inicializador por omision: no hay estado, y `get()` devuelve `null`.
     *
     * @param <A> el estado nominal (en la practica `Void`)
     * @return un proveedor de `null`
     */
    static <A> Supplier<A> defaultInitializer() {
        return new NoStateInitializer<A>();
    }

    /**
     * El combinador por omision: se niega a fusionar.
     *
     * <p>Un `Gatherer` que no dice como fusionar dos mitades no se puede evaluar en paralelo, y
     * el JDK lo expresa con un combinador que tira `UnsupportedOperationException` en vez de con
     * uno que devuelve el lado izquierdo. Aca se copia esa decision, aunque nuestro `gather`
     * nunca lo llame: el dia que alguien lea `combiner()` y lo invoque, la respuesta honesta es
     * "no se puede", no un resultado silenciosamente incompleto.
     *
     * @param <A> el estado
     * @return un combinador que siempre falla
     */
    static <A> BinaryOperator<A> defaultCombiner() {
        return new NoCombiner<A>();
    }

    /**
     * El finalizador por omision: no emite nada al terminar.
     *
     * @param <A> el estado
     * @param <R> el tipo de salida
     * @return un finalizador que no hace nada
     */
    static <A, R> BiConsumer<A, Downstream<? super R>> defaultFinisher() {
        return new NoFinisher<A, R>();
    }

    /**
     * Un `Gatherer` sin estado ni finalizador, marcado como secuencial.
     *
     * @param integrator el integrador
     * @param <T> el tipo de entrada
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> ofSequential(Integrator<Void, T, R> integrator) {
        Objects.requireNonNull(integrator);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = Gatherer.<Void>defaultCombiner();
        BiConsumer<Void, Downstream<? super R>> fin = Gatherer.<Void, R>defaultFinisher();
        return new GathererImpl<T, Void, R>(init, integrator, comb, fin);
    }

    /**
     * Un `Gatherer` sin estado, con finalizador, marcado como secuencial.
     *
     * @param integrator el integrador
     * @param finisher el finalizador
     * @param <T> el tipo de entrada
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> ofSequential(Integrator<Void, T, R> integrator,
                                                    BiConsumer<Void, Downstream<? super R>> finisher) {
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(finisher);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = Gatherer.<Void>defaultCombiner();
        return new GathererImpl<T, Void, R>(init, integrator, comb, finisher);
    }

    /**
     * Un `Gatherer` con estado, sin finalizador, marcado como secuencial.
     *
     * @param initializer como crear el estado
     * @param integrator el integrador
     * @param <T> el tipo de entrada
     * @param <A> el estado
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     */
    static <T, A, R> Gatherer<T, A, R> ofSequential(Supplier<A> initializer, Integrator<A, T, R> integrator) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(integrator);
        BinaryOperator<A> comb = Gatherer.<A>defaultCombiner();
        BiConsumer<A, Downstream<? super R>> fin = Gatherer.<A, R>defaultFinisher();
        return new GathererImpl<T, A, R>(initializer, integrator, comb, fin);
    }

    /**
     * Un `Gatherer` con estado y finalizador, marcado como secuencial.
     *
     * @param initializer como crear el estado
     * @param integrator el integrador
     * @param finisher el finalizador
     * @param <T> el tipo de entrada
     * @param <A> el estado
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     */
    static <T, A, R> Gatherer<T, A, R> ofSequential(Supplier<A> initializer, Integrator<A, T, R> integrator,
                                                    BiConsumer<A, Downstream<? super R>> finisher) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(finisher);
        BinaryOperator<A> comb = Gatherer.<A>defaultCombiner();
        return new GathererImpl<T, A, R>(initializer, integrator, comb, finisher);
    }

    /**
     * Un `Gatherer` sin estado ni finalizador, apto para paralelo.
     *
     * <p>El combinador que le corresponde fusiona dos "sin estado", que es trivial: devuelve
     * `null`. Es el unico caso en que el combinador por omision <em>no</em> es el que se niega.
     *
     * @param integrator el integrador
     * @param <T> el tipo de entrada
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> of(Integrator<Void, T, R> integrator) {
        Objects.requireNonNull(integrator);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = new StatelessCombiner();
        BiConsumer<Void, Downstream<? super R>> fin = Gatherer.<Void, R>defaultFinisher();
        return new GathererImpl<T, Void, R>(init, integrator, comb, fin);
    }

    /**
     * Un `Gatherer` sin estado, con finalizador, apto para paralelo.
     *
     * @param integrator el integrador
     * @param finisher el finalizador
     * @param <T> el tipo de entrada
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     */
    static <T, R> Gatherer<T, Void, R> of(Integrator<Void, T, R> integrator,
                                          BiConsumer<Void, Downstream<? super R>> finisher) {
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(finisher);
        Supplier<Void> init = Gatherer.<Void>defaultInitializer();
        BinaryOperator<Void> comb = new StatelessCombiner();
        return new GathererImpl<T, Void, R>(init, integrator, comb, finisher);
    }

    /**
     * Un `Gatherer` con las cuatro piezas dadas.
     *
     * @param initializer como crear el estado
     * @param integrator el integrador
     * @param combiner el combinador
     * @param finisher el finalizador
     * @param <T> el tipo de entrada
     * @param <A> el estado
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     * @throws NullPointerException si alguna pieza es null
     */
    static <T, A, R> Gatherer<T, A, R> of(Supplier<A> initializer, Integrator<A, T, R> integrator,
                                          BinaryOperator<A> combiner,
                                          BiConsumer<A, Downstream<? super R>> finisher) {
        Objects.requireNonNull(initializer);
        Objects.requireNonNull(integrator);
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(finisher);
        return new GathererImpl<T, A, R>(initializer, integrator, combiner, finisher);
    }
}

// ---- las piezas por omision -------------------------------------------------------------------
//
// Clases con nombre y no lambdas, por la regla de la casa que ya rige en Collectors.java: una
// lambda alcanzada a traves de un *campo* de otro objeto no se ejecuta bien en nuestra VM. Estos
// objetos viven justamente ahi, en campos de GathererImpl.

final class NoStateInitializer<A> implements Supplier<A> {
    public A get() {
        return null;
    }
}

// El combinador que se niega. Ver Gatherer.defaultCombiner().
final class NoCombiner<A> implements BinaryOperator<A> {
    public A apply(A left, A right) {
        // Mensaje constante: la concatenacion de String en tiempo de ejecucion no esta
        // disponible en nuestra VM (#226).
        throw new UnsupportedOperationException("este combinador no se puede usar");
    }
}

// Fusionar dos estados que no existen: no hay nada que fusionar.
final class StatelessCombiner implements BinaryOperator<Void> {
    public Void apply(Void left, Void right) {
        return null;
    }
}

final class NoFinisher<A, R> implements BiConsumer<A, Gatherer.Downstream<? super R>> {
    public void accept(A state, Gatherer.Downstream<? super R> downstream) {
    }
}

// ---- la implementacion que devuelven las fabricas ---------------------------------------------

final class GathererImpl<T, A, R> implements Gatherer<T, A, R> {

    private final Supplier<A> initializer;
    private final Gatherer.Integrator<A, T, R> integrator;
    private final BinaryOperator<A> combiner;
    private final BiConsumer<A, Gatherer.Downstream<? super R>> finisher;

    GathererImpl(Supplier<A> initializer, Gatherer.Integrator<A, T, R> integrator, BinaryOperator<A> combiner,
                 BiConsumer<A, Gatherer.Downstream<? super R>> finisher) {
        this.initializer = initializer;
        this.integrator = integrator;
        this.combiner = combiner;
        this.finisher = finisher;
    }

    public Supplier<A> initializer() {
        return this.initializer;
    }

    public Gatherer.Integrator<A, T, R> integrator() {
        return this.integrator;
    }

    public BinaryOperator<A> combiner() {
        return this.combiner;
    }

    public BiConsumer<A, Gatherer.Downstream<? super R>> finisher() {
        return this.finisher;
    }
}

// ---- el buffer al que empuja `Stream.gather` --------------------------------------------------

// Junta lo que el Gatherer empuja. Nunca rechaza: es el final de la cadena y el flujo resultante
// se materializa entero, asi que no hay nada mas abajo que pueda pedir que se corte.
final class GatherBuffer<R> implements Gatherer.Downstream<R> {

    private Object[] data;
    private int size;

    GatherBuffer() {
        this.data = new Object[16];
        this.size = 0;
    }

    public boolean push(R element) {
        if (this.size == this.data.length) {
            Object[] bigger = new Object[this.data.length * 2];
            for (int i = 0; i < this.size; i++) {
                bigger[i] = this.data[i];
            }
            this.data = bigger;
        }
        this.data[this.size] = element;
        this.size = this.size + 1;
        return true;
    }

    public boolean isRejecting() {
        return false;
    }

    // Una copia exacta, del largo vivo.
    Object[] toArray() {
        Object[] out = new Object[this.size];
        for (int i = 0; i < this.size; i++) {
            out[i] = this.data[i];
        }
        return out;
    }
}

// ---- la composicion de dos Gatherers ----------------------------------------------------------

// El `Downstream` que el primero de los dos ve: cada `push` es un `integrate` del segundo.
final class MidDownstream<R, RR> implements Gatherer.Downstream<R> {

    private final Gatherer.Integrator<Object, R, RR> integrator;
    // El estado del segundo vive en `estado[1]` del Object[2] compartido, no en un campo propio:
    // el finalizador del compuesto necesita el mismo estado que vio el integrador.
    private final Object[] estado;
    private final Gatherer.Downstream<RR> abajo;
    private boolean rechazando;

    MidDownstream(Gatherer.Integrator<Object, R, RR> integrator, Object[] estado,
                  Gatherer.Downstream<RR> abajo) {
        this.integrator = integrator;
        this.estado = estado;
        this.abajo = abajo;
        this.rechazando = false;
    }

    public boolean push(R element) {
        if (this.rechazando) {
            return false;
        }
        boolean sigue = this.integrator.integrate(this.estado[1], element, this.abajo);
        if (!sigue) {
            this.rechazando = true;
        }
        return sigue;
    }

    public boolean isRejecting() {
        if (this.rechazando) {
            return true;
        }
        return this.abajo.isRejecting();
    }

    boolean rechazo() {
        return this.rechazando;
    }
}

final class CompositeIntegrator<T, R, RR> implements Gatherer.Integrator<Object[], T, RR> {

    private final Gatherer<T, Object, R> primero;
    private final Gatherer<R, Object, RR> segundo;

    CompositeIntegrator(Gatherer<T, Object, R> primero, Gatherer<R, Object, RR> segundo) {
        this.primero = primero;
        this.segundo = segundo;
    }

    public boolean integrate(Object[] estado, T element, Gatherer.Downstream<? super RR> downstream) {
        Gatherer.Downstream<RR> abajo = (Gatherer.Downstream<RR>) downstream;
        Gatherer.Integrator<Object, R, RR> i2 = this.segundo.integrator();
        MidDownstream<R, RR> medio = new MidDownstream<R, RR>(i2, estado, abajo);
        Gatherer.Integrator<Object, T, R> i1 = this.primero.integrator();
        boolean sigue = i1.integrate(estado[0], element, medio);
        if (!sigue) {
            return false;
        }
        return !medio.rechazo();
    }
}

final class CompositeInitializer<T, R, RR> implements Supplier<Object[]> {

    private final Gatherer<T, Object, R> primero;
    private final Gatherer<R, Object, RR> segundo;

    CompositeInitializer(Gatherer<T, Object, R> primero, Gatherer<R, Object, RR> segundo) {
        this.primero = primero;
        this.segundo = segundo;
    }

    public Object[] get() {
        Supplier<Object> s1 = this.primero.initializer();
        Supplier<Object> s2 = this.segundo.initializer();
        Object[] estado = new Object[2];
        estado[0] = s1.get();
        estado[1] = s2.get();
        return estado;
    }
}

// Los dos finalizadores, en orden: lo que emita el primero todavia tiene que pasar por el segundo.
final class CompositeFinisher<T, R, RR> implements BiConsumer<Object[], Gatherer.Downstream<? super RR>> {

    private final Gatherer<T, Object, R> primero;
    private final Gatherer<R, Object, RR> segundo;

    CompositeFinisher(Gatherer<T, Object, R> primero, Gatherer<R, Object, RR> segundo) {
        this.primero = primero;
        this.segundo = segundo;
    }

    public void accept(Object[] estado, Gatherer.Downstream<? super RR> downstream) {
        Gatherer.Downstream<RR> abajo = (Gatherer.Downstream<RR>) downstream;
        Gatherer.Integrator<Object, R, RR> i2 = this.segundo.integrator();
        MidDownstream<R, RR> medio = new MidDownstream<R, RR>(i2, estado, abajo);
        BiConsumer<Object, Gatherer.Downstream<? super R>> f1 = this.primero.finisher();
        Gatherer.Downstream<? super R> destinoMedio = medio;
        f1.accept(estado[0], destinoMedio);
        BiConsumer<Object, Gatherer.Downstream<? super RR>> f2 = this.segundo.finisher();
        f2.accept(estado[1], downstream);
    }
}

final class CompositeGatherer<T, R, RR> implements Gatherer<T, Object[], RR> {

    private final Gatherer<T, Object, R> primero;
    private final Gatherer<R, Object, RR> segundo;

    CompositeGatherer(Gatherer<T, Object, R> primero, Gatherer<R, Object, RR> segundo) {
        this.primero = primero;
        this.segundo = segundo;
    }

    public Supplier<Object[]> initializer() {
        return new CompositeInitializer<T, R, RR>(this.primero, this.segundo);
    }

    public Gatherer.Integrator<Object[], T, RR> integrator() {
        return new CompositeIntegrator<T, R, RR>(this.primero, this.segundo);
    }

    // Un compuesto no sabe fusionar: haria falta fusionar las dos mitades de cada lado, y el
    // combinador de cualquiera de los dos puede ser el que se niega. Se niega el compuesto.
    public BinaryOperator<Object[]> combiner() {
        return new NoCombiner<Object[]>();
    }

    public BiConsumer<Object[], Gatherer.Downstream<? super RR>> finisher() {
        return new CompositeFinisher<T, R, RR>(this.primero, this.segundo);
    }
}
