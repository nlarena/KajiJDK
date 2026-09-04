package java.util;

// Compiled with `-cp KajiLibrary` so Set/Iterator bind to KajiLibrary's own (subset) types.
import java.util.Set;
import java.util.Iterator;

// KajiLibrary's java.util.HashSet<E> — a set backed by a hash table (open addressing with
// linear probing over one Object[]), doubling past a ~50% load factor. `add` returns false
// if the element is already present; `remove` re-inserts the trailing cluster to keep the
// probe invariant. `iterator()` walks the table (see HashSetItr below). (The JDK's HashSet
// delegates to a HashMap; ours holds its own table.)
public class HashSet<E> extends AbstractSet<E> implements Set<E> {

    // Package-private so HashSetItr can walk the table (still implementation, not API surface).
    Object[] table;
    private int size;

    /**
     * El elemento null vive aparte de la tabla.
     *
     * <p>Por lo mismo que en {@link HashMap}: la tabla es de direccionamiento abierto y usa null como
     * marca de slot vacio, asi que un null adentro seria a la vez "ocupado" y "libre". Y aceptarlo
     * hace falta: un `HashSet` permite <b>un</b> elemento null, y sin eso `keySet()` de un mapa con
     * clave null no podria devolverla.
     *
     * <p>Paquete-privado porque el iterador tiene que verlo para emitirlo.
     */
    boolean hasNull = false;

    public HashSet() {
        this.table = new Object[16];
        this.size = 0;
    }

    /**
     * Con capacidad inicial.
     *
     * <p>La tabla se dimensiona al **doble** de lo pedido, y eso no es un margen arbitrario: esta
     * implementacion es de direccionamiento abierto con sondeo lineal, y a partir de la mitad de
     * ocupacion los grupos de colisiones empiezan a fundirse entre si. Pedir capacidad para `n`
     * quiere decir "quiero meter `n` sin que se agrande", y para eso hacen falta `2n` casilleros.
     */
    public HashSet(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        int cap = initialCapacity * 2;
        if (cap < 16) {
            cap = 16;
        }
        this.table = new Object[cap];
        this.size = 0;
    }

    // El factor de carga se acepta y se **ignora**: esta tabla no lo usa (ver arriba). El JDK lo
    // toma para decidir cuando agrandar; aca ese umbral esta fijo en la mitad.
    public HashSet(int initialCapacity, float loadFactor) {
        this(initialCapacity);
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
    }

    /**
     * Un conjunto dimensionado para `numElements` sin que se agrande.
     *
     * <p>Existe porque `new HashSet<>(n)` **no** quiere decir eso: ese `n` es la capacidad de la
     * tabla, no la cantidad de elementos, y con el factor de carga del JDK un `new HashSet<>(100)`
     * se agranda a los 75. Es una de las trampas mas viejas de la API, y por eso Java 19 agrego
     * esta fabrica con un nombre que si dice lo que hace.
     */
    public static <T> HashSet<T> newHashSet(int numElements) {
        if (numElements < 0) {
            throw new IllegalArgumentException("Negative number of elements: " + numElements);
        }
        return new HashSet<T>(numElements);
    }

    // Copia los elementos de otra coleccion, descartando los repetidos.
    //
    // No es un lujo: es el idioma con el que se congela un argumento que el llamador podria seguir
    // modificando (`this.violaciones = new HashSet<>(violaciones)`), y no habia forma de escribirlo.
    // Hasta #293 se podia escribir igual y compilaba **mal** en silencio -- el argumento se evaluaba,
    // se llamaba al constructor sin argumentos y el conjunto nacia vacio.
    public HashSet(Collection<? extends E> c) {
        this.table = new Object[16];
        this.size = 0;
        this.addAll(c);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    // The slot holding `e`, or the first empty slot on its probe sequence if absent.
    private int slotFor(Object e) {
        int cap = this.table.length;
        int i = e.hashCode() & (cap - 1);
        while (this.table[i] != null) {
            if (this.table[i].equals(e)) {
                return i;
            }
            i = (i + 1) & (cap - 1);
        }
        return i;
    }

    public boolean contains(Object o) {
        if (o == null) {
            return this.hasNull;
        }
        return this.table[this.slotFor(o)] != null;
    }

    public boolean add(E e) {
        if (e == null) {
            if (this.hasNull) {
                return false;
            }
            this.hasNull = true;
            this.size = this.size + 1;
            return true;
        }
        if (this.size * 2 >= this.table.length) {
            this.resize();
        }
        int i = this.slotFor(e);
        if (this.table[i] != null) {
            return false;
        }
        this.table[i] = e;
        this.size = this.size + 1;
        return true;
    }

    public boolean remove(Object o) {
        if (o == null) {
            if (!this.hasNull) {
                return false;
            }
            this.hasNull = false;
            this.size = this.size - 1;
            return true;
        }
        int cap = this.table.length;
        int i = this.slotFor(o);
        if (this.table[i] == null) {
            return false;
        }
        this.table[i] = null;
        this.size = this.size - 1;
        int j = (i + 1) & (cap - 1);
        while (this.table[j] != null) {
            Object e = this.table[j];
            this.table[j] = null;
            this.size = this.size - 1;
            this.add((E) e);
            j = (j + 1) & (cap - 1);
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < this.table.length; i++) {
            this.table[i] = null;
        }
        this.size = 0;
        this.hasNull = false;
    }

    public Iterator<E> iterator() {
        return new HashSetItr<E>(this);
    }

    // Double the table and re-insert every element into the fresh, larger array.
    private void resize() {
        Object[] old = this.table;
        int newCap = old.length * 2;
        this.table = new Object[newCap];
        // El null no esta en la tabla: su +1 hay que conservarlo a mano.
        this.size = this.hasNull ? 1 : 0;
        for (int i = 0; i < old.length; i++) {
            if (old[i] != null) {
                this.add((E) old[i]);
            }
        }
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.SIZED);
    }
}

// HashSet's iterator, as a same-file top-level class (compiler-generated enclosing capture is
// broken for a class inside a generic one — finding #13 — so no inner/anonymous class). Walks
// the backing table, skipping empty slots.
final class HashSetItr<E> implements Iterator<E> {

    private final HashSet<E> set;
    private int index = 0;

    /** El null va primero, y una sola vez. Ver el campo homonimo de {@link HashSet}. */
    private boolean nullPending;

    HashSetItr(HashSet<E> set) {
        this.set = set;
        this.nullPending = set.hasNull;
        this.advance();
    }

    // Advance `index` to the next occupied slot (or past the end).
    private void advance() {
        while (this.index < this.set.table.length && this.set.table[this.index] == null) {
            this.index = this.index + 1;
        }
    }

    public boolean hasNext() {
        return this.nullPending || this.index < this.set.table.length;
    }

    public E next() {
        if (this.nullPending) {
            this.nullPending = false;
            return null;
        }
        E element = (E) this.set.table[this.index];
        this.index = this.index + 1;
        this.advance();
        return element;
    }

}
