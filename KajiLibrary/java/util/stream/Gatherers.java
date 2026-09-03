package java.util.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Los `Gatherer` de fabrica, como `Collectors` lo es de `Collector`.
 *
 * <p>Igual que en Collectors.java, <b>no hay ni una lambda en este archivo</b>: cada pieza de cada
 * `Gatherer` es una clase con nombre. La razon es la misma y esta anotada alla -- una lambda
 * alcanzada a traves de un campo de otro objeto no se ejecuta bien en nuestra VM, y las piezas de
 * un `Gatherer` viven exactamente ahi, en campos de `GathererImpl`.
 *
 * <p>Los contenedores de estado son `Object[]`, tambien por la regla de la casa: evita el acceso a
 * campos de una clase auxiliar desde otra clase del mismo archivo.
 *
 * <p>Los tres `Gatherer` con finalizador se arman con `new GathererImpl<...>(...)` y no con
 * `Gatherer.ofSequential(init, paso, fin)`, que seria lo natural: este javac no resuelve una
 * llamada cuyo parametro mete una variable de tipo dentro de un argumento de tipo invariante
 * (`BiConsumer&lt;A, Downstream&lt;R&gt;&gt;`). Esta explicado en el encabezado de Gatherer.java, y
 * el repro con las cinco variantes es java/WcLib3.java. El constructor si resuelve porque los
 * argumentos de tipo se escriben, no se infieren.
 *
 * <p><b>Divergencia con el JDK, y es una sola: `mapConcurrent` no es concurrente.</b> Esta
 * documentada en su propio javadoc; el resultado es identico, lo que no hay es paralelismo. Es la
 * misma decision --y el mismo precedente-- que `BaseStream.parallel()`, que devuelve un flujo
 * secuencial.
 */
public final class Gatherers {

    private Gatherers() {
    }

    /**
     * Ventanas consecutivas y sin solapamiento de `windowSize` elementos.
     *
     * <p>La ultima ventana puede salir mas corta: se emite al terminar la entrada con lo que
     * haya juntado. Cada ventana es una `List` que se niega a ser modificada.
     *
     * @param windowSize cuantos elementos por ventana
     * @param <TR> el tipo de los elementos
     * @return el `Gatherer`
     * @throws IllegalArgumentException si `windowSize` es menor que 1
     */
    public static <TR> Gatherer<TR, ?, List<TR>> windowFixed(int windowSize) {
        if (windowSize < 1) {
            // Mensaje constante: la concatenacion de String en tiempo de ejecucion no esta
            // disponible en nuestra VM (#226).
            throw new IllegalArgumentException("windowSize must be greater than zero");
        }
        Supplier<Object[]> init = new WindowSupplier();
        Gatherer.Integrator<Object[], TR, List<TR>> paso = new FixedWindowIntegrator<TR>(windowSize);
        BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> fin = new FixedWindowFinisher<TR>();
        return new GathererImpl<TR, Object[], List<TR>>(init, paso, new NoCombiner<Object[]>(), fin);
    }

    /**
     * Ventanas de `windowSize` elementos que avanzan de a uno.
     *
     * <p>Si la entrada entera tiene menos de `windowSize` elementos, sale <b>una</b> ventana con
     * todos ellos, en vez de ninguna. Es lo que hace el JDK y no es un caso de borde caprichoso:
     * la alternativa --no emitir nada-- pierde la entrada sin decirlo.
     *
     * @param windowSize cuantos elementos por ventana
     * @param <TR> el tipo de los elementos
     * @return el `Gatherer`
     * @throws IllegalArgumentException si `windowSize` es menor que 1
     */
    public static <TR> Gatherer<TR, ?, List<TR>> windowSliding(int windowSize) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize must be greater than zero");
        }
        Supplier<Object[]> init = new WindowSupplier();
        Gatherer.Integrator<Object[], TR, List<TR>> paso = new SlidingWindowIntegrator<TR>(windowSize);
        BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> fin = new SlidingWindowFinisher<TR>();
        return new GathererImpl<TR, Object[], List<TR>>(init, paso, new NoCombiner<Object[]>(), fin);
    }

    /**
     * Pliega toda la entrada en un solo valor y lo emite al final.
     *
     * <p>La diferencia con `Stream.reduce` es de forma, no de calculo: esto es una operacion
     * <b>intermedia</b> que deja un flujo de un elemento, y por eso se puede seguir encadenando.
     *
     * @param initial de donde sale el valor inicial
     * @param folder como combinar el acumulado con el siguiente elemento
     * @param <T> el tipo de entrada
     * @param <R> el tipo del acumulado
     * @return el `Gatherer`
     */
    public static <T, R> Gatherer<T, ?, R> fold(Supplier<R> initial,
                                                BiFunction<? super R, ? super T, ? extends R> folder) {
        Supplier<Object[]> init = new FoldSupplier<R>(initial);
        Gatherer.Integrator<Object[], T, R> paso = new FoldIntegrator<T, R>(folder);
        BiConsumer<Object[], Gatherer.Downstream<? super R>> fin = new FoldFinisher<R>();
        return new GathererImpl<T, Object[], R>(init, paso, new NoCombiner<Object[]>(), fin);
    }

    /**
     * Emite el acumulado <b>despues de cada elemento</b>: la suma corrida.
     *
     * <p>Salen tantos elementos como entraron, a diferencia de `fold`, que emite uno solo.
     *
     * @param initial de donde sale el valor inicial
     * @param scanner como combinar el acumulado con el siguiente elemento
     * @param <T> el tipo de entrada
     * @param <R> el tipo del acumulado
     * @return el `Gatherer`
     */
    public static <T, R> Gatherer<T, ?, R> scan(Supplier<R> initial,
                                                BiFunction<? super R, ? super T, ? extends R> scanner) {
        Supplier<Object[]> init = new FoldSupplier<R>(initial);
        Gatherer.Integrator<Object[], T, R> paso = new ScanIntegrator<T, R>(scanner);
        return Gatherer.ofSequential(init, paso);
    }

    /**
     * Aplica `mapper` a cada elemento, conservando el orden de encuentro.
     *
     * <p><b>Divergencia deliberada con el JDK: aca no hay concurrencia.</b> El JDK lanza hasta
     * `maxConcurrency` hilos virtuales y va emitiendo en orden a medida que terminan; esta
     * implementacion aplica `mapper` a un elemento por vez, en el mismo hilo.
     *
     * <p>El <b>resultado</b> es identico --los mismos elementos, en el mismo orden--, porque la
     * concurrencia de este metodo es una propiedad de rendimiento y no de significado. Lo que
     * cambia es la latencia cuando `mapper` bloquea, que es justamente para lo que uno lo usaria.
     * Se declara igual, por el mismo criterio con el que `BaseStream.parallel()` devuelve un flujo
     * secuencial: el codigo escrito contra la API real sigue compilando y dando lo correcto.
     *
     * <p>`maxConcurrency` se <b>valida</b> aunque despues no se use: un programa que pasa 0 esta
     * mal escrito contra la API real, y enterarse aca es mejor que enterarse al portarlo.
     *
     * @param maxConcurrency cuantas aplicaciones simultaneas permitiria el JDK
     * @param mapper la funcion a aplicar
     * @param <T> el tipo de entrada
     * @param <R> el tipo de salida
     * @return el `Gatherer`
     * @throws IllegalArgumentException si `maxConcurrency` es menor que 1
     */
    public static <T, R> Gatherer<T, ?, R> mapConcurrent(int maxConcurrency,
                                                         Function<? super T, ? extends R> mapper) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be greater than zero");
        }
        Gatherer.Integrator<Void, T, R> paso = new MapIntegrator<T, R>(mapper);
        return Gatherer.ofSequential(paso);
    }
}

// ---- ventanas -----------------------------------------------------------------------------------

// El estado de las dos ventanas es el mismo Object[2]: {ArrayList<?> buffer, Boolean primeraAun}.
// `primeraAun` solo lo mira la deslizante, pero compartir el proveedor ahorra una clase.
final class WindowSupplier implements Supplier<Object[]> {
    public Object[] get() {
        Object[] estado = new Object[2];
        estado[0] = new ArrayList<Object>();
        estado[1] = Boolean.TRUE;
        return estado;
    }
}

final class FixedWindowIntegrator<TR> implements Gatherer.Integrator<Object[], TR, List<TR>> {

    private final int windowSize;

    FixedWindowIntegrator(int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean integrate(Object[] estado, TR element, Gatherer.Downstream<? super List<TR>> downstream) {
        ArrayList<TR> buffer = (ArrayList<TR>) estado[0];
        buffer.add(element);
        if (buffer.size() < this.windowSize) {
            return true;
        }
        // La ventana emitida es una copia: el buffer se sigue usando para la siguiente.
        List<TR> ventana = new FrozenList<TR>(new ArrayList<TR>(buffer));
        buffer.clear();
        estado[1] = Boolean.FALSE;
        return downstream.push(ventana);
    }
}

final class FixedWindowFinisher<TR> implements BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> {
    public void accept(Object[] estado, Gatherer.Downstream<? super List<TR>> downstream) {
        ArrayList<TR> buffer = (ArrayList<TR>) estado[0];
        if (buffer.isEmpty()) {
            return;
        }
        List<TR> ventana = new FrozenList<TR>(new ArrayList<TR>(buffer));
        buffer.clear();
        downstream.push(ventana);
    }
}

final class SlidingWindowIntegrator<TR> implements Gatherer.Integrator<Object[], TR, List<TR>> {

    private final int windowSize;

    SlidingWindowIntegrator(int windowSize) {
        this.windowSize = windowSize;
    }

    public boolean integrate(Object[] estado, TR element, Gatherer.Downstream<? super List<TR>> downstream) {
        ArrayList<TR> buffer = (ArrayList<TR>) estado[0];
        buffer.add(element);
        if (buffer.size() < this.windowSize) {
            return true;
        }
        List<TR> ventana = new FrozenList<TR>(new ArrayList<TR>(buffer));
        // Se descarta el mas viejo: la ventana avanza de a uno.
        buffer.remove(0);
        estado[1] = Boolean.FALSE;
        return downstream.push(ventana);
    }
}

final class SlidingWindowFinisher<TR> implements BiConsumer<Object[], Gatherer.Downstream<? super List<TR>>> {
    public void accept(Object[] estado, Gatherer.Downstream<? super List<TR>> downstream) {
        // Solo emite si NUNCA se completo una ventana: es el caso de una entrada mas corta que
        // la ventana. Si ya salio alguna, lo que queda en el buffer es la cola de la ultima y
        // volver a emitirla seria una ventana incompleta que el JDK no produce.
        Boolean primeraAun = (Boolean) estado[1];
        if (!primeraAun.booleanValue()) {
            return;
        }
        ArrayList<TR> buffer = (ArrayList<TR>) estado[0];
        if (buffer.isEmpty()) {
            return;
        }
        List<TR> ventana = new FrozenList<TR>(new ArrayList<TR>(buffer));
        buffer.clear();
        downstream.push(ventana);
    }
}

// ---- fold y scan --------------------------------------------------------------------------------

// El estado es un Object[1] con el acumulado. `get()` llama al proveedor del que llama una sola
// vez, cuando el `Gatherer` empieza a correr, no cuando se construye.
final class FoldSupplier<R> implements Supplier<Object[]> {

    private final Supplier<R> initial;

    FoldSupplier(Supplier<R> initial) {
        this.initial = initial;
    }

    public Object[] get() {
        Object[] estado = new Object[1];
        estado[0] = this.initial.get();
        return estado;
    }
}

final class FoldIntegrator<T, R> implements Gatherer.Integrator<Object[], T, R> {

    private final BiFunction<R, T, R> folder;

    FoldIntegrator(BiFunction<? super R, ? super T, ? extends R> folder) {
        // Los comodines se sacan de encima con una conversion exacta en el borrado: el cuerpo
        // solo llama a `apply`, y por ahi entra un R y un T y sale un R.
        Object f = folder;
        this.folder = (BiFunction<R, T, R>) f;
    }

    public boolean integrate(Object[] estado, T element, Gatherer.Downstream<? super R> downstream) {
        R acumulado = (R) estado[0];
        estado[0] = this.folder.apply(acumulado, element);
        return true;
    }
}

final class FoldFinisher<R> implements BiConsumer<Object[], Gatherer.Downstream<? super R>> {
    public void accept(Object[] estado, Gatherer.Downstream<? super R> downstream) {
        R acumulado = (R) estado[0];
        downstream.push(acumulado);
    }
}

final class ScanIntegrator<T, R> implements Gatherer.Integrator<Object[], T, R> {

    private final BiFunction<R, T, R> scanner;

    ScanIntegrator(BiFunction<? super R, ? super T, ? extends R> scanner) {
        Object f = scanner;
        this.scanner = (BiFunction<R, T, R>) f;
    }

    public boolean integrate(Object[] estado, T element, Gatherer.Downstream<? super R> downstream) {
        R acumulado = (R) estado[0];
        R siguiente = this.scanner.apply(acumulado, element);
        estado[0] = siguiente;
        return downstream.push(siguiente);
    }
}

// ---- mapConcurrent ------------------------------------------------------------------------------

// Sin estado: cada elemento se mapea y se empuja. Ver el javadoc de Gatherers.mapConcurrent para
// por que aca no hay ni hilos ni cola.
final class MapIntegrator<T, R> implements Gatherer.Integrator<Void, T, R> {

    private final Function<T, R> mapper;

    MapIntegrator(Function<? super T, ? extends R> mapper) {
        Object f = mapper;
        this.mapper = (Function<T, R>) f;
    }

    public boolean integrate(Void estado, T element, Gatherer.Downstream<? super R> downstream) {
        R mapeado = this.mapper.apply(element);
        return downstream.push(mapeado);
    }
}
