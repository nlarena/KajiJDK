package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * KajiLibrary's java.util.Spliterators -- the factories, and the three shapes every spliterator
 * in the library turns out to be.
 *
 * <p>There are only three, and knowing that is most of understanding the type:
 *
 * <ul>
 *   <li><strong>Over an array.</strong> Splitting is arithmetic -- halve an index range -- so it
 *       is exact, cheap, and reports {@code SIZED | SUBSIZED}: the two halves know their sizes
 *       before anyone walks them.
 *   <li><strong>Over an iterator.</strong> Splitting cannot be arithmetic, because an iterator
 *       cannot be asked where its middle is. So a split PULLS a batch of elements into an array
 *       and hands that over, which means the first split is work and the result is an array
 *       spliterator. The batch grows on each split, so a long traversal does not pay for a
 *       thousand tiny splits.
 *   <li><strong>Empty.</strong> Which is not a degenerate case worth skipping: it is what makes
 *       every consumer able to assume it has a spliterator instead of testing for null.
 * </ul>
 */
public final class Spliterators {

    private Spliterators() {
    }

    // Cuantos elementos toma el primer split de un iterador, y cuanto crece despues. Los numeros
    // son los del JDK. El crecimiento es lo que importa: sin el, partir un millon de elementos de
    // a mil serian mil splits, y cada uno cuesta una copia.
    private static final int BATCH_UNIT = 1 << 10;

    private static final int MAX_BATCH = 1 << 25;

    // SIZED y SUBSIZED se agregan solos a un spliterator de tamano conocido... salvo cuando la
    // fuente es CONCURRENT. Ahi el tamano puede cambiar debajo del recorrido, y prometer SIZED
    // seria prometer algo que nadie sostiene: el que reparte trabajo dimensionaria buffers con
    // un numero que ya vencio.
    private static int sizedUnlessConcurrent(int characteristics) {
        if ((characteristics & Spliterator.CONCURRENT) == 0) {
            return characteristics | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
        return characteristics;
    }

    // ---- the empty ones ----

    /** A spliterator over nothing. */
    public static <T> Spliterator<T> emptySpliterator() {
        return new EmptySpliterator<T>();
    }

    /** A spliterator over no {@code int} values. */
    public static Spliterator.OfInt emptyIntSpliterator() {
        return new EmptyOfInt();
    }

    /** A spliterator over no {@code long} values. */
    public static Spliterator.OfLong emptyLongSpliterator() {
        return new EmptyOfLong();
    }

    /** A spliterator over no {@code double} values. */
    public static Spliterator.OfDouble emptyDoubleSpliterator() {
        return new EmptyOfDouble();
    }

    // ---- over an array ----
    //
    // SIZED y SUBSIZED se agregan siempre y no se le preguntan al llamador: son verdad de un
    // array, y dejar que alguien las omitiera solo serviria para que un consumidor haga de mas.

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     * @param additionalCharacteristics what else is true of it
     */
    public static <T> Spliterator<T> spliterator(Object[] array, int additionalCharacteristics) {
        return Spliterators.spliterator(array, 0, array.length, additionalCharacteristics);
    }

    /**
     * A spliterator over {@code [fromIndex, toIndex)} of {@code array}.
     *
     * @param array what to traverse
     * @param fromIndex where to start
     * @param toIndex where to stop, exclusive
     * @param additionalCharacteristics what else is true of it
     * @throws ArrayIndexOutOfBoundsException if the range is not valid
     */
    public static <T> Spliterator<T> spliterator(Object[] array, int fromIndex, int toIndex,
            int additionalCharacteristics) {
        Spliterators.checkRange(array.length, fromIndex, toIndex);
        return new ArraySpliterator<T>(array, fromIndex, toIndex, additionalCharacteristics);
    }

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     * @param additionalCharacteristics what else is true of it
     */
    public static Spliterator.OfInt spliterator(int[] array, int additionalCharacteristics) {
        return Spliterators.spliterator(array, 0, array.length, additionalCharacteristics);
    }

    /**
     * A spliterator over {@code [fromIndex, toIndex)} of {@code array}.
     *
     * @param array what to traverse
     * @param fromIndex where to start
     * @param toIndex where to stop, exclusive
     * @param additionalCharacteristics what else is true of it
     */
    public static Spliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex,
            int additionalCharacteristics) {
        Spliterators.checkRange(array.length, fromIndex, toIndex);
        return new IntArraySpliterator(array, fromIndex, toIndex, additionalCharacteristics);
    }

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     * @param additionalCharacteristics what else is true of it
     */
    public static Spliterator.OfLong spliterator(long[] array, int additionalCharacteristics) {
        return Spliterators.spliterator(array, 0, array.length, additionalCharacteristics);
    }

    /**
     * A spliterator over {@code [fromIndex, toIndex)} of {@code array}.
     *
     * @param array what to traverse
     * @param fromIndex where to start
     * @param toIndex where to stop, exclusive
     * @param additionalCharacteristics what else is true of it
     */
    public static Spliterator.OfLong spliterator(long[] array, int fromIndex, int toIndex,
            int additionalCharacteristics) {
        Spliterators.checkRange(array.length, fromIndex, toIndex);
        return new LongArraySpliterator(array, fromIndex, toIndex, additionalCharacteristics);
    }

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     * @param additionalCharacteristics what else is true of it
     */
    public static Spliterator.OfDouble spliterator(double[] array, int additionalCharacteristics) {
        return Spliterators.spliterator(array, 0, array.length, additionalCharacteristics);
    }

    /**
     * A spliterator over {@code [fromIndex, toIndex)} of {@code array}.
     *
     * @param array what to traverse
     * @param fromIndex where to start
     * @param toIndex where to stop, exclusive
     * @param additionalCharacteristics what else is true of it
     */
    public static Spliterator.OfDouble spliterator(double[] array, int fromIndex, int toIndex,
            int additionalCharacteristics) {
        Spliterators.checkRange(array.length, fromIndex, toIndex);
        return new DoubleArraySpliterator(array, fromIndex, toIndex, additionalCharacteristics);
    }

    private static void checkRange(int length, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > length || fromIndex > toIndex) {
            throw new ArrayIndexOutOfBoundsException(
                    "from " + fromIndex + ", to " + toIndex + ", length " + length);
        }
    }

    // ---- over a collection or an iterator ----

    /**
     * A spliterator over {@code c}, taken through its iterator.
     *
     * <p>{@code SIZED} and {@code SUBSIZED} come for free: a collection knows its size, and the
     * batches a split pulls out know theirs.
     *
     * @param c what to traverse
     * @param characteristics what is true of it
     */
    public static <T> Spliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        return new IteratorSpliterator<T>(c, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, which is claimed to have {@code size} elements.
     *
     * <p>The size is TAKEN ON TRUST -- nothing verifies it, and a wrong one produces a traversal
     * that ends early or asks for elements that are not there. That is why the caller has to say
     * it: an iterator cannot be asked.
     *
     * @param iterator what to traverse
     * @param size how many elements it will yield
     * @param characteristics what is true of it
     */
    public static <T> Spliterator<T> spliterator(Iterator<? extends T> iterator, long size,
            int characteristics) {
        return new IteratorSpliterator<T>(iterator, size, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, of unknown length.
     *
     * @param iterator what to traverse
     * @param characteristics what is true of it
     */
    public static <T> Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator,
            int characteristics) {
        return new IteratorSpliterator<T>(iterator, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, which is claimed to have {@code size} elements.
     *
     * @param iterator what to traverse
     * @param size how many elements it will yield
     * @param characteristics what is true of it
     */
    public static Spliterator.OfInt spliterator(PrimitiveIterator.OfInt iterator, long size,
            int characteristics) {
        return new IntIteratorSpliterator(iterator, size, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, of unknown length.
     *
     * @param iterator what to traverse
     * @param characteristics what is true of it
     */
    public static Spliterator.OfInt spliteratorUnknownSize(PrimitiveIterator.OfInt iterator,
            int characteristics) {
        return new IntIteratorSpliterator(iterator, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, which is claimed to have {@code size} elements.
     *
     * @param iterator what to traverse
     * @param size how many elements it will yield
     * @param characteristics what is true of it
     */
    public static Spliterator.OfLong spliterator(PrimitiveIterator.OfLong iterator, long size,
            int characteristics) {
        return new LongIteratorSpliterator(iterator, size, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, of unknown length.
     *
     * @param iterator what to traverse
     * @param characteristics what is true of it
     */
    public static Spliterator.OfLong spliteratorUnknownSize(PrimitiveIterator.OfLong iterator,
            int characteristics) {
        return new LongIteratorSpliterator(iterator, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, which is claimed to have {@code size} elements.
     *
     * @param iterator what to traverse
     * @param size how many elements it will yield
     * @param characteristics what is true of it
     */
    public static Spliterator.OfDouble spliterator(PrimitiveIterator.OfDouble iterator, long size,
            int characteristics) {
        return new DoubleIteratorSpliterator(iterator, size, characteristics);
    }

    /**
     * A spliterator over {@code iterator}, of unknown length.
     *
     * @param iterator what to traverse
     * @param characteristics what is true of it
     */
    public static Spliterator.OfDouble spliteratorUnknownSize(PrimitiveIterator.OfDouble iterator,
            int characteristics) {
        return new DoubleIteratorSpliterator(iterator, characteristics);
    }

    // ---- back the other way ----
    //
    // Un spliterator sabe hacer de iterador y no al reves: `tryAdvance` hace la prueba y la
    // busqueda de una, y para partirlo en `hasNext` + `next` hay que guardarse el elemento.

    /**
     * An iterator over what {@code spliterator} would yield.
     *
     * @param spliterator what to walk
     */
    public static <T> Iterator<T> iterator(Spliterator<? extends T> spliterator) {
        return new SpliteratorIterator<T>(spliterator);
    }

    /**
     * An iterator over what {@code spliterator} would yield.
     *
     * @param spliterator what to walk
     */
    public static PrimitiveIterator.OfInt iterator(Spliterator.OfInt spliterator) {
        return new IntSpliteratorIterator(spliterator);
    }

    /**
     * An iterator over what {@code spliterator} would yield.
     *
     * @param spliterator what to walk
     */
    public static PrimitiveIterator.OfLong iterator(Spliterator.OfLong spliterator) {
        return new LongSpliteratorIterator(spliterator);
    }

    /**
     * An iterator over what {@code spliterator} would yield.
     *
     * @param spliterator what to walk
     */
    public static PrimitiveIterator.OfDouble iterator(Spliterator.OfDouble spliterator) {
        return new DoubleSpliteratorIterator(spliterator);
    }

    // ---- forma 1: sobre un array ----
    //
    // Partir es aritmetica: la mitad del rango de indices. Exacto, barato, y por eso reporta
    // SIZED y SUBSIZED -- las dos mitades saben su tamano antes de que nadie las recorra.

    static final class ArraySpliterator<T> implements Spliterator<T> {

        private final Object[] array;

        private int index;

        private final int fence;

        private final int flags;

        // Si es >= 0, una estimacion heredada: este spliterator salio de partir uno cuyo tamano
        // NO se conocia, y contar los elementos del pedazo seria mentir sobre el recorrido que
        // el pedazo representa. Si es -1, el tamano es exactamente `fence - index`.
        private long estimatedSize;

        ArraySpliterator(Object[] array, int origin, int fence, int additional) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = additional | Spliterator.SIZED | Spliterator.SUBSIZED;
            this.estimatedSize = -1L;
        }

        // El pedazo de un reparto sin tamano: SIZED y SUBSIZED se caen, porque una
        // parte de algo que nadie midio tampoco esta medida.
        ArraySpliterator(Object[] array, int origin, int fence, int characteristics,
                long estimatedSize) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
            this.estimatedSize = estimatedSize;
        }

        public Spliterator<T> trySplit() {
            int lo = this.index;
            int mid = (lo + this.fence) >>> 1;
            if (lo >= mid) {
                return null;
            }
            this.index = mid;
            if (this.estimatedSize == -1L) {
                return new ArraySpliterator<T>(this.array, lo, mid, this.flags);
            }
            long prefix = this.estimatedSize >>> 1;
            this.estimatedSize = this.estimatedSize - prefix;
            return new ArraySpliterator<T>(this.array, lo, mid, this.flags, prefix);
        }

        public boolean tryAdvance(Consumer<? super T> action) {
            if (this.index >= this.fence) {
                return false;
            }
            T element = (T) this.array[this.index];
            this.index = this.index + 1;
            action.accept(element);
            return true;
        }

        public void forEachRemaining(Consumer<? super T> action) {
            int i = this.index;
            int hi = this.fence;
            this.index = hi;
            while (i < hi) {
                action.accept((T) this.array[i]);
                i = i + 1;
            }
        }

        public long estimateSize() {
            if (this.estimatedSize >= 0L) {
                return this.estimatedSize;
            }
            return (long) (this.fence - this.index);
        }

        public int characteristics() {
            return this.flags;
        }

        public Comparator<? super T> getComparator() {
            if (this.hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            throw new IllegalStateException("this spliterator is not SORTED");
        }
    }

    static final class IntArraySpliterator implements Spliterator.OfInt {

        private final int[] array;

        private int index;

        private final int fence;

        private final int flags;

        // Si es >= 0, una estimacion heredada: este spliterator salio de partir uno cuyo tamano
        // NO se conocia, y contar los elementos del pedazo seria mentir sobre el recorrido que
        // el pedazo representa. Si es -1, el tamano es exactamente `fence - index`.
        private long estimatedSize;

        IntArraySpliterator(int[] array, int origin, int fence, int additional) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = additional | Spliterator.SIZED | Spliterator.SUBSIZED;
            this.estimatedSize = -1L;
        }

        // El pedazo de un reparto sin tamano: SIZED y SUBSIZED se caen, porque una
        // parte de algo que nadie midio tampoco esta medida.
        IntArraySpliterator(int[] array, int origin, int fence, int characteristics,
                long estimatedSize) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
            this.estimatedSize = estimatedSize;
        }

        public Spliterator.OfInt trySplit() {
            int lo = this.index;
            int mid = (lo + this.fence) >>> 1;
            if (lo >= mid) {
                return null;
            }
            this.index = mid;
            if (this.estimatedSize == -1L) {
                return new IntArraySpliterator(this.array, lo, mid, this.flags);
            }
            long prefix = this.estimatedSize >>> 1;
            this.estimatedSize = this.estimatedSize - prefix;
            return new IntArraySpliterator(this.array, lo, mid, this.flags, prefix);
        }

        public boolean tryAdvance(IntConsumer action) {
            if (this.index >= this.fence) {
                return false;
            }
            int element = this.array[this.index];
            this.index = this.index + 1;
            action.accept(element);
            return true;
        }

        public void forEachRemaining(IntConsumer action) {
            int i = this.index;
            int hi = this.fence;
            this.index = hi;
            while (i < hi) {
                action.accept(this.array[i]);
                i = i + 1;
            }
        }

        public long estimateSize() {
            if (this.estimatedSize >= 0L) {
                return this.estimatedSize;
            }
            return (long) (this.fence - this.index);
        }

        public int characteristics() {
            return this.flags;
        }

        public Comparator<? super Integer> getComparator() {
            if (this.hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            throw new IllegalStateException("this spliterator is not SORTED");
        }
    }

    static final class LongArraySpliterator implements Spliterator.OfLong {

        private final long[] array;

        private int index;

        private final int fence;

        private final int flags;

        // Si es >= 0, una estimacion heredada: este spliterator salio de partir uno cuyo tamano
        // NO se conocia, y contar los elementos del pedazo seria mentir sobre el recorrido que
        // el pedazo representa. Si es -1, el tamano es exactamente `fence - index`.
        private long estimatedSize;

        LongArraySpliterator(long[] array, int origin, int fence, int additional) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = additional | Spliterator.SIZED | Spliterator.SUBSIZED;
            this.estimatedSize = -1L;
        }

        // El pedazo de un reparto sin tamano: SIZED y SUBSIZED se caen, porque una
        // parte de algo que nadie midio tampoco esta medida.
        LongArraySpliterator(long[] array, int origin, int fence, int characteristics,
                long estimatedSize) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
            this.estimatedSize = estimatedSize;
        }

        public Spliterator.OfLong trySplit() {
            int lo = this.index;
            int mid = (lo + this.fence) >>> 1;
            if (lo >= mid) {
                return null;
            }
            this.index = mid;
            if (this.estimatedSize == -1L) {
                return new LongArraySpliterator(this.array, lo, mid, this.flags);
            }
            long prefix = this.estimatedSize >>> 1;
            this.estimatedSize = this.estimatedSize - prefix;
            return new LongArraySpliterator(this.array, lo, mid, this.flags, prefix);
        }

        public boolean tryAdvance(LongConsumer action) {
            if (this.index >= this.fence) {
                return false;
            }
            long element = this.array[this.index];
            this.index = this.index + 1;
            action.accept(element);
            return true;
        }

        public void forEachRemaining(LongConsumer action) {
            int i = this.index;
            int hi = this.fence;
            this.index = hi;
            while (i < hi) {
                action.accept(this.array[i]);
                i = i + 1;
            }
        }

        public long estimateSize() {
            if (this.estimatedSize >= 0L) {
                return this.estimatedSize;
            }
            return (long) (this.fence - this.index);
        }

        public int characteristics() {
            return this.flags;
        }

        public Comparator<? super Long> getComparator() {
            if (this.hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            throw new IllegalStateException("this spliterator is not SORTED");
        }
    }

    static final class DoubleArraySpliterator implements Spliterator.OfDouble {

        private final double[] array;

        private int index;

        private final int fence;

        private final int flags;

        // Si es >= 0, una estimacion heredada: este spliterator salio de partir uno cuyo tamano
        // NO se conocia, y contar los elementos del pedazo seria mentir sobre el recorrido que
        // el pedazo representa. Si es -1, el tamano es exactamente `fence - index`.
        private long estimatedSize;

        DoubleArraySpliterator(double[] array, int origin, int fence, int additional) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = additional | Spliterator.SIZED | Spliterator.SUBSIZED;
            this.estimatedSize = -1L;
        }

        // El pedazo de un reparto sin tamano: SIZED y SUBSIZED se caen, porque una
        // parte de algo que nadie midio tampoco esta medida.
        DoubleArraySpliterator(double[] array, int origin, int fence, int characteristics,
                long estimatedSize) {
            this.array = array;
            this.index = origin;
            this.fence = fence;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
            this.estimatedSize = estimatedSize;
        }

        public Spliterator.OfDouble trySplit() {
            int lo = this.index;
            int mid = (lo + this.fence) >>> 1;
            if (lo >= mid) {
                return null;
            }
            this.index = mid;
            if (this.estimatedSize == -1L) {
                return new DoubleArraySpliterator(this.array, lo, mid, this.flags);
            }
            long prefix = this.estimatedSize >>> 1;
            this.estimatedSize = this.estimatedSize - prefix;
            return new DoubleArraySpliterator(this.array, lo, mid, this.flags, prefix);
        }

        public boolean tryAdvance(DoubleConsumer action) {
            if (this.index >= this.fence) {
                return false;
            }
            double element = this.array[this.index];
            this.index = this.index + 1;
            action.accept(element);
            return true;
        }

        public void forEachRemaining(DoubleConsumer action) {
            int i = this.index;
            int hi = this.fence;
            this.index = hi;
            while (i < hi) {
                action.accept(this.array[i]);
                i = i + 1;
            }
        }

        public long estimateSize() {
            if (this.estimatedSize >= 0L) {
                return this.estimatedSize;
            }
            return (long) (this.fence - this.index);
        }

        public int characteristics() {
            return this.flags;
        }

        public Comparator<? super Double> getComparator() {
            if (this.hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            throw new IllegalStateException("this spliterator is not SORTED");
        }
    }

    // ---- forma 2: sobre un iterador ----
    //
    // Un iterador no se puede preguntar donde esta su mitad, asi que partir no es aritmetica:
    // es SACAR un lote de elementos a un array y entregar ese array. El primer split cuesta una
    // copia, y el resultado es un spliterator de array -- o sea que la forma 1 es adonde todo
    // termina yendo.
    //
    // El lote crece en cada split (1024, 2048, ...), y ese crecimiento es lo que evita que
    // partir un millon de elementos sean mil splits de mil.

    static final class IteratorSpliterator<T> implements Spliterator<T> {

        private Collection<? extends T> collection;

        private Iterator<? extends T> iterator;

        private final int flags;

        private long estimate;

        private int batch;

        IteratorSpliterator(Collection<? extends T> collection, int characteristics) {
            this.collection = collection;
            this.iterator = null;
            this.flags = Spliterators.sizedUnlessConcurrent(characteristics);
            this.estimate = 0L;
        }

        IteratorSpliterator(Iterator<? extends T> iterator, long size, int characteristics) {
            this.collection = null;
            this.iterator = iterator;
            this.estimate = size;
            this.flags = Spliterators.sizedUnlessConcurrent(characteristics);
        }

        IteratorSpliterator(Iterator<? extends T> iterator, int characteristics) {
            this.collection = null;
            this.iterator = iterator;
            this.estimate = Long.MAX_VALUE;
            // Sin tamano no se puede prometer SIZED, y prometerlo haria que un consumidor
            // dimensionara un buffer con Long.MAX_VALUE.
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        // El iterador, sacado de la coleccion la primera vez que hace falta. Tarde a proposito:
        // tomarlo en el constructor fijaria el momento en que la coleccion se congela, y la
        // especificacion dice que eso pasa en el primer recorrido.
        private Iterator<? extends T> iterator() {
            if (this.iterator == null) {
                this.iterator = this.collection.iterator();
                this.estimate = (long) this.collection.size();
            }
            return this.iterator;
        }

        public Spliterator<T> trySplit() {
            Iterator<? extends T> source = this.iterator();
            if (this.estimate <= 1L || !source.hasNext()) {
                return null;
            }
            int n = this.batch + Spliterators.BATCH_UNIT;
            if ((long) n > this.estimate) {
                n = (int) this.estimate;
            }
            if (n > Spliterators.MAX_BATCH) {
                n = Spliterators.MAX_BATCH;
            }
            Object[] taken = new Object[n];
            int taking = 0;
            while (taking < n && source.hasNext()) {
                taken[taking] = source.next();
                taking = taking + 1;
            }
            this.batch = taking;
            if (this.estimate != Long.MAX_VALUE) {
                this.estimate = this.estimate - (long) taking;
                return new ArraySpliterator<T>(taken, 0, taking, this.flags);
            }
            // Sin tamano conocido, el pedazo no dice cuantos elementos se llevo sino que
            // representa la mitad de lo que falta, que es todo lo que se puede afirmar.
            return new ArraySpliterator<T>(taken, 0, taking, this.flags, Long.MAX_VALUE / 2L);
        }

        public boolean tryAdvance(Consumer<? super T> action) {
            Iterator<? extends T> source = this.iterator();
            if (!source.hasNext()) {
                return false;
            }
            action.accept(source.next());
            return true;
        }

        public void forEachRemaining(Consumer<? super T> action) {
            Iterator<? extends T> source = this.iterator();
            while (source.hasNext()) {
                action.accept(source.next());
            }
        }

        public long estimateSize() {
            this.iterator();
            return this.estimate;
        }

        public int characteristics() {
            return this.flags;
        }

        public Comparator<? super T> getComparator() {
            if (this.hasCharacteristics(Spliterator.SORTED)) {
                return null;
            }
            throw new IllegalStateException("this spliterator is not SORTED");
        }
    }

    static final class IntIteratorSpliterator implements Spliterator.OfInt {

        private final PrimitiveIterator.OfInt iterator;

        private final int flags;

        private long estimate;

        private int batch;

        IntIteratorSpliterator(PrimitiveIterator.OfInt iterator, long size, int characteristics) {
            this.iterator = iterator;
            this.estimate = size;
            this.flags = Spliterators.sizedUnlessConcurrent(characteristics);
        }

        IntIteratorSpliterator(PrimitiveIterator.OfInt iterator, int characteristics) {
            this.iterator = iterator;
            this.estimate = Long.MAX_VALUE;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        public Spliterator.OfInt trySplit() {
            if (this.estimate <= 1L || !this.iterator.hasNext()) {
                return null;
            }
            int n = this.batch + Spliterators.BATCH_UNIT;
            if ((long) n > this.estimate) {
                n = (int) this.estimate;
            }
            if (n > Spliterators.MAX_BATCH) {
                n = Spliterators.MAX_BATCH;
            }
            int[] taken = new int[n];
            int taking = 0;
            while (taking < n && this.iterator.hasNext()) {
                taken[taking] = this.iterator.nextInt();
                taking = taking + 1;
            }
            this.batch = taking;
            if (this.estimate != Long.MAX_VALUE) {
                this.estimate = this.estimate - (long) taking;
                return new IntArraySpliterator(taken, 0, taking, this.flags);
            }
            // Sin tamano conocido, el pedazo no dice cuantos elementos se llevo sino que
            // representa la mitad de lo que falta, que es todo lo que se puede afirmar.
            return new IntArraySpliterator(taken, 0, taking, this.flags, Long.MAX_VALUE / 2L);
        }

        public boolean tryAdvance(IntConsumer action) {
            if (!this.iterator.hasNext()) {
                return false;
            }
            action.accept(this.iterator.nextInt());
            return true;
        }

        public void forEachRemaining(IntConsumer action) {
            while (this.iterator.hasNext()) {
                action.accept(this.iterator.nextInt());
            }
        }

        public long estimateSize() {
            return this.estimate;
        }

        public int characteristics() {
            return this.flags;
        }
    }

    static final class LongIteratorSpliterator implements Spliterator.OfLong {

        private final PrimitiveIterator.OfLong iterator;

        private final int flags;

        private long estimate;

        private int batch;

        LongIteratorSpliterator(PrimitiveIterator.OfLong iterator, long size,
                int characteristics) {
            this.iterator = iterator;
            this.estimate = size;
            this.flags = Spliterators.sizedUnlessConcurrent(characteristics);
        }

        LongIteratorSpliterator(PrimitiveIterator.OfLong iterator, int characteristics) {
            this.iterator = iterator;
            this.estimate = Long.MAX_VALUE;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        public Spliterator.OfLong trySplit() {
            if (this.estimate <= 1L || !this.iterator.hasNext()) {
                return null;
            }
            int n = this.batch + Spliterators.BATCH_UNIT;
            if ((long) n > this.estimate) {
                n = (int) this.estimate;
            }
            if (n > Spliterators.MAX_BATCH) {
                n = Spliterators.MAX_BATCH;
            }
            long[] taken = new long[n];
            int taking = 0;
            while (taking < n && this.iterator.hasNext()) {
                taken[taking] = this.iterator.nextLong();
                taking = taking + 1;
            }
            this.batch = taking;
            if (this.estimate != Long.MAX_VALUE) {
                this.estimate = this.estimate - (long) taking;
                return new LongArraySpliterator(taken, 0, taking, this.flags);
            }
            // Sin tamano conocido, el pedazo no dice cuantos elementos se llevo sino que
            // representa la mitad de lo que falta, que es todo lo que se puede afirmar.
            return new LongArraySpliterator(taken, 0, taking, this.flags, Long.MAX_VALUE / 2L);
        }

        public boolean tryAdvance(LongConsumer action) {
            if (!this.iterator.hasNext()) {
                return false;
            }
            action.accept(this.iterator.nextLong());
            return true;
        }

        public void forEachRemaining(LongConsumer action) {
            while (this.iterator.hasNext()) {
                action.accept(this.iterator.nextLong());
            }
        }

        public long estimateSize() {
            return this.estimate;
        }

        public int characteristics() {
            return this.flags;
        }
    }

    static final class DoubleIteratorSpliterator implements Spliterator.OfDouble {

        private final PrimitiveIterator.OfDouble iterator;

        private final int flags;

        private long estimate;

        private int batch;

        DoubleIteratorSpliterator(PrimitiveIterator.OfDouble iterator, long size,
                int characteristics) {
            this.iterator = iterator;
            this.estimate = size;
            this.flags = Spliterators.sizedUnlessConcurrent(characteristics);
        }

        DoubleIteratorSpliterator(PrimitiveIterator.OfDouble iterator, int characteristics) {
            this.iterator = iterator;
            this.estimate = Long.MAX_VALUE;
            this.flags = characteristics & ~(Spliterator.SIZED | Spliterator.SUBSIZED);
        }

        public Spliterator.OfDouble trySplit() {
            if (this.estimate <= 1L || !this.iterator.hasNext()) {
                return null;
            }
            int n = this.batch + Spliterators.BATCH_UNIT;
            if ((long) n > this.estimate) {
                n = (int) this.estimate;
            }
            if (n > Spliterators.MAX_BATCH) {
                n = Spliterators.MAX_BATCH;
            }
            double[] taken = new double[n];
            int taking = 0;
            while (taking < n && this.iterator.hasNext()) {
                taken[taking] = this.iterator.nextDouble();
                taking = taking + 1;
            }
            this.batch = taking;
            if (this.estimate != Long.MAX_VALUE) {
                this.estimate = this.estimate - (long) taking;
                return new DoubleArraySpliterator(taken, 0, taking, this.flags);
            }
            // Sin tamano conocido, el pedazo no dice cuantos elementos se llevo sino que
            // representa la mitad de lo que falta, que es todo lo que se puede afirmar.
            return new DoubleArraySpliterator(taken, 0, taking, this.flags, Long.MAX_VALUE / 2L);
        }

        public boolean tryAdvance(DoubleConsumer action) {
            if (!this.iterator.hasNext()) {
                return false;
            }
            action.accept(this.iterator.nextDouble());
            return true;
        }

        public void forEachRemaining(DoubleConsumer action) {
            while (this.iterator.hasNext()) {
                action.accept(this.iterator.nextDouble());
            }
        }

        public long estimateSize() {
            return this.estimate;
        }

        public int characteristics() {
            return this.flags;
        }
    }

    // ---- forma 3: vacio ----
    //
    // No es un caso degenerado que se pueda saltear: es lo que le permite a todo consumidor
    // asumir que TIENE un spliterator, en vez de probar contra null en cada uso.

    static final class EmptySpliterator<T> implements Spliterator<T> {

        public Spliterator<T> trySplit() {
            return null;
        }

        public boolean tryAdvance(Consumer<? super T> action) {
            return false;
        }

        public void forEachRemaining(Consumer<? super T> action) {
        }

        public long estimateSize() {
            return 0L;
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    static final class EmptyOfInt implements Spliterator.OfInt {

        public Spliterator.OfInt trySplit() {
            return null;
        }

        public boolean tryAdvance(IntConsumer action) {
            return false;
        }

        public void forEachRemaining(IntConsumer action) {
        }

        public long estimateSize() {
            return 0L;
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    static final class EmptyOfLong implements Spliterator.OfLong {

        public Spliterator.OfLong trySplit() {
            return null;
        }

        public boolean tryAdvance(LongConsumer action) {
            return false;
        }

        public void forEachRemaining(LongConsumer action) {
        }

        public long estimateSize() {
            return 0L;
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    static final class EmptyOfDouble implements Spliterator.OfDouble {

        public Spliterator.OfDouble trySplit() {
            return null;
        }

        public boolean tryAdvance(DoubleConsumer action) {
            return false;
        }

        public void forEachRemaining(DoubleConsumer action) {
        }

        public long estimateSize() {
            return 0L;
        }

        public int characteristics() {
            return Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    // ---- de vuelta a iterador ----
    //
    // `tryAdvance` hace la prueba y la busqueda de una sola vez, y un iterador las quiere
    // separadas -- asi que para partirlas hay que guardarse el elemento entre las dos llamadas.
    // Ese `holder` es toda la diferencia, y es por lo que la conversion va en esta direccion y
    // no al reves.

    static final class SpliteratorIterator<T> implements Iterator<T> {

        private final Spliterator<? extends T> source;

        private boolean holding;

        private T held;

        SpliteratorIterator(Spliterator<? extends T> source) {
            this.source = source;
        }

        public boolean hasNext() {
            if (this.holding) {
                return true;
            }
            Consumer<T> keep = new Consumer<T>() {
                public void accept(T value) {
                    SpliteratorIterator.this.held = value;
                }
            };
            this.holding = this.source.tryAdvance(keep);
            return this.holding;
        }

        public T next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.holding = false;
            return this.held;
        }
    }

    static final class IntSpliteratorIterator implements PrimitiveIterator.OfInt {

        private final Spliterator.OfInt source;

        private boolean holding;

        private int held;

        IntSpliteratorIterator(Spliterator.OfInt source) {
            this.source = source;
        }

        public boolean hasNext() {
            if (this.holding) {
                return true;
            }
            IntConsumer keep = new IntConsumer() {
                public void accept(int value) {
                    IntSpliteratorIterator.this.held = value;
                }
            };
            this.holding = this.source.tryAdvance(keep);
            return this.holding;
        }

        public int nextInt() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.holding = false;
            return this.held;
        }
    }

    static final class LongSpliteratorIterator implements PrimitiveIterator.OfLong {

        private final Spliterator.OfLong source;

        private boolean holding;

        private long held;

        LongSpliteratorIterator(Spliterator.OfLong source) {
            this.source = source;
        }

        public boolean hasNext() {
            if (this.holding) {
                return true;
            }
            LongConsumer keep = new LongConsumer() {
                public void accept(long value) {
                    LongSpliteratorIterator.this.held = value;
                }
            };
            this.holding = this.source.tryAdvance(keep);
            return this.holding;
        }

        public long nextLong() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.holding = false;
            return this.held;
        }
    }

    static final class DoubleSpliteratorIterator implements PrimitiveIterator.OfDouble {

        private final Spliterator.OfDouble source;

        private boolean holding;

        private double held;

        DoubleSpliteratorIterator(Spliterator.OfDouble source) {
            this.source = source;
        }

        public boolean hasNext() {
            if (this.holding) {
                return true;
            }
            DoubleConsumer keep = new DoubleConsumer() {
                public void accept(double value) {
                    DoubleSpliteratorIterator.this.held = value;
                }
            };
            this.holding = this.source.tryAdvance(keep);
            return this.holding;
        }

        public double nextDouble() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.holding = false;
            return this.held;
        }
    }

}
