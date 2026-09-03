package java.lang.foreign;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;

/**
 * KajiLibrary's java.lang.foreign.MemorySegment -- una **region de memoria** con un tamanio conocido,
 * sobre la que se lee y se escribe por offset.
 *
 * <h2>Que hay y que no</h2>
 *
 * <p><strong>Los segmentos de esta biblioteca viven sobre arreglos de Java.</strong>
 * {@link #ofArray(byte[])} y sus seis hermanos son reales y hacen exactamente lo que dicen; un
 * {@link Arena} tambien entrega segmentos, respaldados por un arreglo elegido segun el alineamiento
 * que se le pida. Lo que no hay es memoria **fuera** del heap: esta VM no reserva ni libera memoria
 * del sistema, asi que {@link #isNative()} es `false` para todo lo que se pueda usar.
 *
 * <p>La consecuencia visible, y conviene tenerla presente: un `Arena.ofConfined().allocate(16)` en el
 * JDK da un segmento nativo y aca da uno de heap. Todo lo demas --tamanio, cortes, lectura,
 * escritura, cierre del ambito-- se comporta igual.
 *
 * <h2>El alineamiento, que es de donde vienen las sorpresas</h2>
 *
 * <p>Un segmento sobre un `byte[]` tiene {@link #maxByteAlignment()} igual a **1**, porque la JVM no
 * promete donde cae un arreglo de bytes en memoria. Por eso
 * `segmento.get(ValueLayout.JAVA_INT, 0)` sobre un `byte[]` **falla**, y hay que usar
 * `JAVA_INT_UNALIGNED`. No es una limitacion de esta biblioteca: es lo que hace el JDK, y por eso
 * existen las constantes sin alinear.
 *
 * <p>Un segmento sobre un `long[]` tiene alineamiento 8 y admite `JAVA_LONG` sin mas.
 *
 * <h2>El ambito</h2>
 *
 * <p>Un segmento pertenece a un {@link Scope}, y cuando el ambito se cierra el segmento deja de
 * poder usarse. Eso es lo que convierte un error de memoria en una excepcion: sin ambito, usar un
 * segmento despues de liberarlo seria comportamiento indefinido, y con el es un
 * `IllegalStateException` con la linea exacta.
 */
public interface MemorySegment {

    /** Cuantos bytes cubre. */
    long byteSize();

    /**
     * La direccion de este segmento.
     *
     * <p>Para uno de heap es el **offset dentro del arreglo** que lo respalda, no una direccion de
     * memoria: un arreglo de Java se mueve cuando el recolector compacta, asi que no tiene una.
     */
    long address();

    /** El arreglo que lo respalda, si es de heap. */
    Optional<Object> heapBase();

    /**
     * Si vive fuera del heap de Java.
     *
     * <p>Siempre `false` salvo para {@link #NULL} y los de {@link #ofAddress(long)}, que no tienen
     * respaldo. Ver la nota de la interfaz.
     */
    boolean isNative();

    /** Si esta mapeado de un archivo. Siempre `false`: esta VM no mapea archivos. */
    boolean isMapped();

    /** Si rechaza las escrituras. */
    boolean isReadOnly();

    /** El alineamiento maximo que este segmento puede garantizar. */
    long maxByteAlignment();

    /** El ambito al que pertenece. */
    Scope scope();

    /**
     * Si ese hilo puede usarlo.
     *
     * <p>Siempre `true`: los ambitos de esta biblioteca no son confinados a un hilo. En el JDK un
     * `Arena.ofConfined()` solo deja usar sus segmentos desde el hilo que lo creo, y ahi esto puede
     * dar `false`.
     */
    boolean isAccessibleBy(Thread thread);

    /** El mismo segmento, sin permitir escrituras. */
    MemorySegment asReadOnly();

    /** Desde ese offset hasta el final. */
    MemorySegment asSlice(long offset);

    /** Desde ese offset, con ese largo. */
    MemorySegment asSlice(long offset, long newSize);

    /** Desde ese offset, con ese largo y ese alineamiento exigido. */
    MemorySegment asSlice(long offset, long newSize, long byteAlignment);

    /** El corte que describe ese layout, desde ese offset. */
    MemorySegment asSlice(long offset, MemoryLayout layout);

    /**
     * La parte de `other` que se superpone con este, si se superponen.
     *
     * <p>Solo tiene sentido entre dos segmentos con el **mismo respaldo**: dos arreglos distintos
     * nunca se superponen, aunque sus offsets coincidan.
     */
    Optional<MemorySegment> asOverlappingSlice(MemorySegment other);

    /**
     * El mismo segmento visto con otro tamanio.
     *
     * @throws UnsupportedOperationException en esta biblioteca cuando el segmento no tiene respaldo:
     *     agrandar un segmento sin memoria detras produciria uno que dice cubrir bytes que nadie
     *     puede leer, que es exactamente la clase de mentira que este proyecto no escribe.
     */
    MemorySegment reinterpret(long newSize);

    /** Ver {@link #reinterpret(long)}. */
    MemorySegment reinterpret(Arena arena, java.util.function.Consumer<MemorySegment> cleanup);

    /** Ver {@link #reinterpret(long)}. */
    MemorySegment reinterpret(long newSize, Arena arena,
            java.util.function.Consumer<MemorySegment> cleanup);

    /** Copia el contenido de `src` al principio de este. */
    MemorySegment copyFrom(MemorySegment src);

    /** Escribe ese byte en todo el segmento. */
    MemorySegment fill(byte value);

    /**
     * El offset del primer byte en que este y `other` difieren, o `-1` si son iguales.
     *
     * <p>Si uno es prefijo del otro, devuelve el largo del mas corto: ahi es donde "difieren", que es
     * la respuesta util para comparar.
     */
    long mismatch(MemorySegment other);


    /** Este segmento como {@link ByteBuffer}. */
    ByteBuffer asByteBuffer();

    /** Los elementos de ese layout, como flujo. */
    Stream<MemorySegment> elements(MemoryLayout elementLayout);

    /** Los elementos de ese layout, como spliterator. */
    Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout);

    /** Una cadena UTF-8 terminada en cero, desde ese offset. */
    String getString(long offset);

    /** Lo mismo, con otro charset. */
    String getString(long offset, java.nio.charset.Charset charset);

    /** Escribe una cadena UTF-8 terminada en cero en ese offset. */
    void setString(long offset, String str);

    /** Lo mismo, con otro charset. */
    void setString(long offset, String str, java.nio.charset.Charset charset);

    // ---- las cuatro operaciones de los archivos mapeados -------------------------------------------
    //
    // Los cuatro existen para un segmento mapeado de un archivo, que esta VM no hace. No se niegan
    // con una excepcion porque el contrato del JDK ya define que hacer para un segmento que **no**
    // esta mapeado, y es lo que hacen aca.

    /** Si esta cargado en memoria. `false`: no hay mapeo. */
    boolean isLoaded();

    /** Sugiere cargarlo. No hace nada: no hay mapeo. */
    void load();

    /** Sugiere descargarlo. No hace nada: no hay mapeo. */
    void unload();

    /** Fuerza la escritura a disco. No hace nada: no hay mapeo. */
    void force();

    /** Lee un `boolean` en ese offset. */
    boolean get(ValueLayout.OfBoolean layout, long offset);

    /** Escribe un `boolean` en ese offset. */
    void set(ValueLayout.OfBoolean layout, long offset, boolean value);

    /** Lee el `boolean` numero `index`: el offset es `index * layout.byteSize()`. */
    boolean getAtIndex(ValueLayout.OfBoolean layout, long index);

    /** Escribe el `boolean` numero `index`. */
    void setAtIndex(ValueLayout.OfBoolean layout, long index, boolean value);

    /** Lee un `byte` en ese offset. */
    byte get(ValueLayout.OfByte layout, long offset);

    /** Escribe un `byte` en ese offset. */
    void set(ValueLayout.OfByte layout, long offset, byte value);

    /** Lee el `byte` numero `index`: el offset es `index * layout.byteSize()`. */
    byte getAtIndex(ValueLayout.OfByte layout, long index);

    /** Escribe el `byte` numero `index`. */
    void setAtIndex(ValueLayout.OfByte layout, long index, byte value);

    /** Lee un `char` en ese offset. */
    char get(ValueLayout.OfChar layout, long offset);

    /** Escribe un `char` en ese offset. */
    void set(ValueLayout.OfChar layout, long offset, char value);

    /** Lee el `char` numero `index`: el offset es `index * layout.byteSize()`. */
    char getAtIndex(ValueLayout.OfChar layout, long index);

    /** Escribe el `char` numero `index`. */
    void setAtIndex(ValueLayout.OfChar layout, long index, char value);

    /** Lee un `short` en ese offset. */
    short get(ValueLayout.OfShort layout, long offset);

    /** Escribe un `short` en ese offset. */
    void set(ValueLayout.OfShort layout, long offset, short value);

    /** Lee el `short` numero `index`: el offset es `index * layout.byteSize()`. */
    short getAtIndex(ValueLayout.OfShort layout, long index);

    /** Escribe el `short` numero `index`. */
    void setAtIndex(ValueLayout.OfShort layout, long index, short value);

    /** Lee un `int` en ese offset. */
    int get(ValueLayout.OfInt layout, long offset);

    /** Escribe un `int` en ese offset. */
    void set(ValueLayout.OfInt layout, long offset, int value);

    /** Lee el `int` numero `index`: el offset es `index * layout.byteSize()`. */
    int getAtIndex(ValueLayout.OfInt layout, long index);

    /** Escribe el `int` numero `index`. */
    void setAtIndex(ValueLayout.OfInt layout, long index, int value);

    /** Lee un `long` en ese offset. */
    long get(ValueLayout.OfLong layout, long offset);

    /** Escribe un `long` en ese offset. */
    void set(ValueLayout.OfLong layout, long offset, long value);

    /** Lee el `long` numero `index`: el offset es `index * layout.byteSize()`. */
    long getAtIndex(ValueLayout.OfLong layout, long index);

    /** Escribe el `long` numero `index`. */
    void setAtIndex(ValueLayout.OfLong layout, long index, long value);

    /** Lee un `float` en ese offset. */
    float get(ValueLayout.OfFloat layout, long offset);

    /** Escribe un `float` en ese offset. */
    void set(ValueLayout.OfFloat layout, long offset, float value);

    /** Lee el `float` numero `index`: el offset es `index * layout.byteSize()`. */
    float getAtIndex(ValueLayout.OfFloat layout, long index);

    /** Escribe el `float` numero `index`. */
    void setAtIndex(ValueLayout.OfFloat layout, long index, float value);

    /** Lee un `double` en ese offset. */
    double get(ValueLayout.OfDouble layout, long offset);

    /** Escribe un `double` en ese offset. */
    void set(ValueLayout.OfDouble layout, long offset, double value);

    /** Lee el `double` numero `index`: el offset es `index * layout.byteSize()`. */
    double getAtIndex(ValueLayout.OfDouble layout, long index);

    /** Escribe el `double` numero `index`. */
    void setAtIndex(ValueLayout.OfDouble layout, long index, double value);

    /** Lee una direccion en ese offset, como segmento de largo cero. */
    MemorySegment get(AddressLayout layout, long offset);

    /** Escribe la direccion de `value` en ese offset. */
    void set(AddressLayout layout, long offset, MemorySegment value);

    /** Lee la direccion numero `index`. */
    MemorySegment getAtIndex(AddressLayout layout, long index);

    /** Escribe la direccion numero `index`. */
    void setAtIndex(AddressLayout layout, long index, MemorySegment value);

    /** El contenido como `byte[]`, leido con ese layout. */
    byte[] toArray(ValueLayout.OfByte elementLayout);

    /** El contenido como `char[]`, leido con ese layout. */
    char[] toArray(ValueLayout.OfChar elementLayout);

    /** El contenido como `short[]`, leido con ese layout. */
    short[] toArray(ValueLayout.OfShort elementLayout);

    /** El contenido como `int[]`, leido con ese layout. */
    int[] toArray(ValueLayout.OfInt elementLayout);

    /** El contenido como `long[]`, leido con ese layout. */
    long[] toArray(ValueLayout.OfLong elementLayout);

    /** El contenido como `float[]`, leido con ese layout. */
    float[] toArray(ValueLayout.OfFloat elementLayout);

    /** El contenido como `double[]`, leido con ese layout. */
    double[] toArray(ValueLayout.OfDouble elementLayout);

    /**
     * El segmento nulo: direccion cero, largo cero.
     *
     * <p>Largo cero y no "invalido": es lo que un `NULL` de C **es** -- una direccion que no se puede
     * leer. Cualquier acceso falla por limites, que es la respuesta correcta.
     */
    MemorySegment NULL = SegmentoHeap.nulo();

    /** Un segmento de largo cero en esa direccion. */
    static MemorySegment ofAddress(long address) {
        return SegmentoHeap.enDireccion(address);
    }

    /** Un segmento sobre un {@link ByteBuffer}. */
    static MemorySegment ofBuffer(java.nio.Buffer buffer) {
        return SegmentoHeap.deBuffer(buffer);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 1. */
    static MemorySegment ofArray(byte[] arr) {
        return SegmentoHeap.deArreglo(arr, 1);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 2. */
    static MemorySegment ofArray(char[] arr) {
        return SegmentoHeap.deArreglo(arr, 2);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 2. */
    static MemorySegment ofArray(short[] arr) {
        return SegmentoHeap.deArreglo(arr, 2);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 4. */
    static MemorySegment ofArray(int[] arr) {
        return SegmentoHeap.deArreglo(arr, 4);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 8. */
    static MemorySegment ofArray(long[] arr) {
        return SegmentoHeap.deArreglo(arr, 8);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 4. */
    static MemorySegment ofArray(float[] arr) {
        return SegmentoHeap.deArreglo(arr, 4);
    }

    /** Un segmento sobre ese arreglo. Alineamiento maximo: 8. */
    static MemorySegment ofArray(double[] arr) {
        return SegmentoHeap.deArreglo(arr, 8);
    }

    /**
     * El primer byte en que difieren esos dos rangos, relativo al arranque de cada uno.
     *
     * <p>Es **estatico** y no de instancia, a diferencia de la version de un argumento, y la razon se
     * lee en la firma: aca los dos segmentos entran en pie de igualdad, cada uno con su rango. Uno de
     * los dos no es "este".
     */
    static long mismatch(MemorySegment srcSegment, long srcFromOffset, long srcToOffset,
            MemorySegment dstSegment, long dstFromOffset, long dstToOffset) {
        return SegmentoHeap.diferenciaEntre(srcSegment, srcFromOffset, srcToOffset, dstSegment,
                dstFromOffset, dstToOffset);
    }

    /** Copia entre dos segmentos. */
    static void copy(MemorySegment srcSegment, long srcOffset, MemorySegment dstSegment,
            long dstOffset, long bytes) {
        SegmentoHeap.copiar(srcSegment, srcOffset, dstSegment, dstOffset, bytes);
    }

    /** Copia entre dos segmentos, elemento a elemento segun los layouts. */
    static void copy(MemorySegment srcSegment, ValueLayout srcElementLayout, long srcOffset,
            MemorySegment dstSegment, ValueLayout dstElementLayout, long dstOffset,
            long elementCount) {
        SegmentoHeap.copiarElementos(srcSegment, srcElementLayout, srcOffset, dstSegment,
                dstElementLayout, dstOffset, elementCount);
    }

    /** Copia de un arreglo de Java a un segmento. */
    static void copy(Object srcArray, int srcIndex, MemorySegment dstSegment,
            ValueLayout dstLayout, long dstOffset, int elementCount) {
        SegmentoHeap.copiarDesdeArreglo(srcArray, srcIndex, dstSegment, dstLayout, dstOffset,
                elementCount);
    }

    /** Copia de un segmento a un arreglo de Java. */
    static void copy(MemorySegment srcSegment, ValueLayout srcLayout, long srcOffset,
            Object dstArray, int dstIndex, int elementCount) {
        SegmentoHeap.copiarAArreglo(srcSegment, srcLayout, srcOffset, dstArray, dstIndex,
                elementCount);
    }

    /**
     * El ambito de vida de un segmento.
     *
     * <p>Es lo que convierte un error de memoria en una excepcion: un segmento cuyo ambito se cerro
     * no se puede usar, y el intento falla con la linea exacta en vez de leer basura.
     */
    interface Scope {

        /** Si sigue abierto. */
        boolean isAlive();
    }
}
