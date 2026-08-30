package java.util;

// Same-package imports work around... no longer needed for finding #4, but the classes are
// referenced by simple name so they must be resolvable; compiled with `-cp KajiLibrary` so
// List/Iterator bind to KajiLibrary's own (subset) types rather than the JDK's.
import java.util.List;
import java.util.Iterator;

// KajiLibrary's java.util.ArrayList<E> — a growable-array List: elements live in a backing
// Object[] that doubles when full, with O(1) indexed access and amortised O(1) append.
// Insertion/removal in the middle shift the tail via a copy; `iterator()` walks by index
// through an anonymous Iterator that captures the list.
public class ArrayList<E> extends AbstractList<E> implements List<E> {

    private Object[] elementData;
    private int size;

    public ArrayList() {
        this.elementData = new Object[10];
        this.size = 0;
    }

    /**
     * Con capacidad inicial.
     *
     * <p>No cambia lo que la lista **hace**, solo cuanto trabaja: una lista que va a recibir mil
     * elementos y arranca en diez se recopia unas siete veces por el camino. Es la unica razon para
     * usarlo, y por eso el argumento es una estimacion y no un limite.
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        }
        this.elementData = new Object[initialCapacity == 0 ? 1 : initialCapacity];
        this.size = 0;
    }

    // Copia los elementos de otra coleccion, en el orden en que los da su iterador.
    public ArrayList(Collection<? extends E> c) {
        Object[] a = c.toArray();
        this.elementData = a.length == 0 ? new Object[1] : a;
        this.size = a.length;
    }

    /**
     * Achica el arreglo de atras al tamano actual.
     *
     * <p>Sirve despues de una carga grande seguida de muchos borrados: la capacidad no baja sola
     * nunca, asi que una lista que llego a tener un millon de elementos sigue ocupando un millon de
     * referencias aunque le queden diez.
     */
    public void trimToSize() {
        if (this.size < this.elementData.length) {
            Object[] mas = new Object[this.size == 0 ? 1 : this.size];
            System.arraycopy(this.elementData, 0, mas, 0, this.size);
            this.elementData = mas;
        }
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public E get(int index) {
        return (E) this.elementData[index];
    }

    public E set(int index, E element) {
        E old = (E) this.elementData[index];
        this.elementData[index] = element;
        return old;
    }

    public boolean add(E e) {
        this.ensureCapacity(this.size + 1);
        this.elementData[this.size] = e;
        this.size = this.size + 1;
        return true;
    }

    public void add(int index, E element) {
        this.ensureCapacity(this.size + 1);
        for (int i = this.size; i > index; i--) {
            this.elementData[i] = this.elementData[i - 1];
        }
        this.elementData[index] = element;
        this.size = this.size + 1;
    }

    public E remove(int index) {
        E old = (E) this.elementData[index];
        for (int i = index; i < this.size - 1; i++) {
            this.elementData[i] = this.elementData[i + 1];
        }
        this.size = this.size - 1;
        this.elementData[this.size] = null;
        return old;
    }

    public boolean remove(Object o) {
        int i = this.indexOf(o);
        if (i < 0) {
            return false;
        }
        this.remove(i);
        return true;
    }

    public int indexOf(Object o) {
        for (int i = 0; i < this.size; i++) {
            Object e = this.elementData[i];
            if (o == null) {
                if (e == null) {
                    return i;
                }
            } else {
                if (o.equals(e)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean contains(Object o) {
        return this.indexOf(o) >= 0;
    }

    public void clear() {
        for (int i = 0; i < this.size; i++) {
            this.elementData[i] = null;
        }
        this.size = 0;
    }

    public Iterator<E> iterator() {
        return new ArrayListItr<E>(this);
    }

    // Grow the backing array to hold at least `min` elements (doubling, then copying).
    private void ensureCapacity(int min) {
        if (min > this.elementData.length) {
            int newCap = this.elementData.length * 2;
            if (newCap < min) {
                newCap = min;
            }
            Object[] grown = new Object[newCap];
            System.arraycopy(this.elementData, 0, grown, 0, this.size);
            this.elementData = grown;
        }
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED);
    }
}

// ArrayList's iterator, as a same-file top-level class holding the list explicitly. It does NOT
// rely on compiler-generated enclosing capture, which the frozen javac gets wrong for a class
// declared inside a *generic* class (finding #13) — so no inner/anonymous class here.
final class ArrayListItr<E> implements Iterator<E> {

    private final ArrayList<E> list;
    private int cursor = 0;

    ArrayListItr(ArrayList<E> list) {
        this.list = list;
    }

    public boolean hasNext() {
        return this.cursor < this.list.size();
    }

    public E next() {
        E element = this.list.get(this.cursor);
        this.cursor = this.cursor + 1;
        return element;
    }

}
