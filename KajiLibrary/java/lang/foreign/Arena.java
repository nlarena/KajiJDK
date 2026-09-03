package java.lang.foreign;

/**
 * KajiLibrary's java.lang.foreign.Arena -- un asignador con **tiempo de vida**.
 *
 * <p>Es la idea central del manejo de memoria de este paquete, y vale entenderla aparte de la
 * implementacion: en vez de liberar cada bloque por su cuenta --que es donde nacen los errores de
 * "usar despues de liberar"-- se agrupan todos en una arena y se cierra la arena entera. Los
 * segmentos que entrego dejan de servir todos juntos, y el intento de usar uno **falla** con una
 * excepcion en vez de leer basura.
 *
 * <p>Por eso extiende `AutoCloseable`: la forma normal de usarla es un `try`-con-recursos, donde el
 * cierre no se puede olvidar.
 *
 * <h2>Lo que cambia en esta biblioteca</h2>
 *
 * <p><strong>Los segmentos que entrega son de heap, no nativos.</strong> Esta VM no reserva memoria
 * del sistema, asi que la arena respalda cada reserva con un arreglo de Java, elegido segun el
 * alineamiento pedido: un `byte[]` para 1, un `short[]` para 2, un `int[]` para 4, un `long[]` para
 * 8. Un alineamiento mayor que 8 **se rechaza** en vez de fingirse.
 *
 * <p>La consecuencia visible es que {@link MemorySegment#isNative()} da `false` donde el JDK da
 * `true`. Todo lo demas --el tamanio, el alineamiento efectivo, los cortes, la lectura, la escritura,
 * y sobre todo el cierre del ambito-- se comporta igual.
 *
 * <p>Y hay algo que gana: la memoria de una arena que nadie cerro no se pierde. La recoge el
 * recolector como cualquier arreglo, mientras que en el JDK una arena automatica es la unica que se
 * limpia sola.
 */
public interface Arena extends SegmentAllocator, AutoCloseable {

    /** El ambito de los segmentos que entrega. */
    MemorySegment.Scope scope();

    /**
     * Cierra la arena: todos sus segmentos dejan de poder usarse.
     *
     * @throws IllegalStateException si ya estaba cerrada
     * @throws UnsupportedOperationException si es la arena global, que no se cierra
     */
    void close();

    MemorySegment allocate(long byteSize, long byteAlignment);

    /**
     * La arena que **nunca** se cierra.
     *
     * <p>Sus segmentos viven todo el programa. Es la que se usa cuando el tiempo de vida es "para
     * siempre" y por lo tanto no hay nada que administrar.
     */
    static Arena global() {
        return ArenaHeap.laGlobal();
    }

    /**
     * Una arena que se limpia sola cuando nadie la mira.
     *
     * <p>En el JDK es la unica cuyos segmentos los libera el recolector. Aca **todas** son asi --la
     * memoria son arreglos de Java-- con lo cual esta y las otras se diferencian solo en si se pueden
     * cerrar a mano. Se sigue distinguiendo porque el codigo que elige una u otra esta diciendo algo
     * sobre su intencion.
     */
    static Arena ofAuto() {
        return ArenaHeap.nueva();
    }

    /**
     * Una arena cerrable, para un solo hilo.
     *
     * <p>En el JDK, "confinada" quiere decir que sus segmentos **solo** se pueden usar desde el hilo
     * que la creo, y eso es lo que le permite no sincronizar nada. Aca no se confina: los segmentos
     * son arreglos de Java y usarlos desde otro hilo no rompe nada, asi que
     * {@link MemorySegment#isAccessibleBy} da `true` siempre. Es una restriccion **menos**, no una
     * respuesta falsa.
     */
    static Arena ofConfined() {
        return ArenaHeap.nueva();
    }

    /** Una arena cerrable, compartida entre hilos. */
    static Arena ofShared() {
        return ArenaHeap.nueva();
    }
}
