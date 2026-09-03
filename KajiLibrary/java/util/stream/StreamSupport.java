package java.util.stream;

import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Supplier;

/**
 * De un `Spliterator` a un flujo: por aca es por donde una coleccion propia se vuelve un `Stream`.
 *
 * <p>Es la contracara de `BaseStream.spliterator()`. Toda coleccion de esta biblioteca que ofrece
 * `stream()` puede escribirlo como `StreamSupport.stream(this.spliterator(), false)`.
 *
 * <p><b>Divergencia con el JDK, la de siempre en este paquete: aca el recorrido es ANSIOSO.</b> El
 * JDK envuelve el `Spliterator` y lo recorre recien cuando corre una operacion terminal; nosotros
 * lo vaciamos entero en un arreglo antes de devolver el flujo. Para toda fuente finita el
 * resultado es el mismo. Para una fuente <b>infinita</b> --un `Spliterator` que nunca deja de
 * ceder elementos-- esto no termina, mientras que el JDK la aceptaria y dejaria que un `limit()`
 * la corte.
 *
 * <p>Esa diferencia no se puede convertir en una negativa como la de `Stream.generate`, y vale la
 * pena decir por que: `generate` es infinito <em>siempre</em>, y por eso negarse es la respuesta
 * correcta en el 100 % de los casos; un `Spliterator` es finito casi siempre, y no hay forma de
 * preguntarle cual es --`estimateSize()` puede devolver `Long.MAX_VALUE` tanto por infinito como
 * por "no se". Negarse aca romperia todos los usos legitimos para atajar uno raro. Queda
 * documentado y no escondido.
 *
 * <p>El parametro `parallel` se acepta y se ignora, igual que en `BaseStream.parallel()`: no hay
 * substrato de fork/join, y el contrato del JDK solo dice que el flujo <em>puede</em> ser
 * paralelo. Las sobrecargas con `characteristics` tambien lo aceptan y lo ignoran: las
 * caracteristicas solo habilitan optimizaciones de la maquinaria perezosa que este paquete no
 * tiene, y ninguna de ellas cambia que elementos salen ni en que orden.
 */
public final class StreamSupport {

    private StreamSupport() {
    }

    /**
     * Un flujo con los elementos que ceda `spliterator`.
     *
     * @param spliterator la fuente, que queda consumida
     * @param parallel se acepta y se ignora (ver el encabezado)
     * @param <T> el tipo de los elementos
     * @return el flujo
     * @throws NullPointerException si `spliterator` es null
     */
    public static <T> Stream<T> stream(Spliterator<T> spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        ObjSink<T> sink = new ObjSink<T>();
        // `tryAdvance` en bucle y no `forEachRemaining`: en los `Spliterator.Of*` primitivos
        // `forEachRemaining` esta sobrecargado (la forma primitiva y la que embolsa), y elegir
        // entre las dos es justo lo que este javac hace mal. Con `tryAdvance` la sobrecarga que
        // corresponde queda fijada por el tipo estatico del sumidero.
        while (spliterator.tryAdvance(sink)) {
            // el trabajo lo hace tryAdvance
        }
        return sink.toStream();
    }

    /**
     * Idem, con la fuente diferida: `supplier.get()` se llama una sola vez.
     *
     * <p>En el JDK el diferimiento importa --el `Spliterator` se pide recien al arrancar la
     * operacion terminal, para que una coleccion que se sigue modificando hasta ese momento no
     * dispare `ConcurrentModificationException`. Aca el flujo se materializa al construirse, asi
     * que el `get()` pasa antes: la ventana entre pedir el flujo y recorrerlo simplemente no
     * existe.
     *
     * @param supplier de donde sacar la fuente
     * @param characteristics se acepta y se ignora (ver el encabezado)
     * @param parallel se acepta y se ignora
     * @param <T> el tipo de los elementos
     * @return el flujo
     * @throws NullPointerException si `supplier` es null
     */
    public static <T> Stream<T> stream(Supplier<? extends Spliterator<T>> supplier, int characteristics,
                                       boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator<T> spliterator = supplier.get();
        return StreamSupport.<T>stream(spliterator, parallel);
    }

    /**
     * Un `IntStream` con los elementos que ceda `spliterator`.
     *
     * @param spliterator la fuente, que queda consumida
     * @param parallel se acepta y se ignora
     * @return el flujo
     * @throws NullPointerException si `spliterator` es null
     */
    public static IntStream intStream(Spliterator.OfInt spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        IntSink sink = new IntSink();
        while (spliterator.tryAdvance(sink)) {
            // el trabajo lo hace tryAdvance
        }
        return sink.toStream();
    }

    /**
     * Idem, con la fuente diferida.
     *
     * @param supplier de donde sacar la fuente
     * @param characteristics se acepta y se ignora
     * @param parallel se acepta y se ignora
     * @return el flujo
     * @throws NullPointerException si `supplier` es null
     */
    public static IntStream intStream(Supplier<? extends Spliterator.OfInt> supplier, int characteristics,
                                      boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator.OfInt spliterator = supplier.get();
        return StreamSupport.intStream(spliterator, parallel);
    }

    /**
     * Un `LongStream` con los elementos que ceda `spliterator`.
     *
     * @param spliterator la fuente, que queda consumida
     * @param parallel se acepta y se ignora
     * @return el flujo
     * @throws NullPointerException si `spliterator` es null
     */
    public static LongStream longStream(Spliterator.OfLong spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        LongSink sink = new LongSink();
        while (spliterator.tryAdvance(sink)) {
            // el trabajo lo hace tryAdvance
        }
        return sink.toStream();
    }

    /**
     * Idem, con la fuente diferida.
     *
     * @param supplier de donde sacar la fuente
     * @param characteristics se acepta y se ignora
     * @param parallel se acepta y se ignora
     * @return el flujo
     * @throws NullPointerException si `supplier` es null
     */
    public static LongStream longStream(Supplier<? extends Spliterator.OfLong> supplier, int characteristics,
                                        boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator.OfLong spliterator = supplier.get();
        return StreamSupport.longStream(spliterator, parallel);
    }

    /**
     * Un `DoubleStream` con los elementos que ceda `spliterator`.
     *
     * @param spliterator la fuente, que queda consumida
     * @param parallel se acepta y se ignora
     * @return el flujo
     * @throws NullPointerException si `spliterator` es null
     */
    public static DoubleStream doubleStream(Spliterator.OfDouble spliterator, boolean parallel) {
        Objects.requireNonNull(spliterator);
        DoubleSink sink = new DoubleSink();
        while (spliterator.tryAdvance(sink)) {
            // el trabajo lo hace tryAdvance
        }
        return sink.toStream();
    }

    /**
     * Idem, con la fuente diferida.
     *
     * @param supplier de donde sacar la fuente
     * @param characteristics se acepta y se ignora
     * @param parallel se acepta y se ignora
     * @return el flujo
     * @throws NullPointerException si `supplier` es null
     */
    public static DoubleStream doubleStream(Supplier<? extends Spliterator.OfDouble> supplier, int characteristics,
                                            boolean parallel) {
        Objects.requireNonNull(supplier);
        Spliterator.OfDouble spliterator = supplier.get();
        return StreamSupport.doubleStream(spliterator, parallel);
    }
}
