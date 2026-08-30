package java.util.concurrent;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

// A list that never mutates its backing array: every write copies the array, swaps the copy
// in, and leaves readers walking the old one. Reads therefore take no lock at all and can
// never see a half-finished write — the trade is that each write is O(n), so this pays off
// exactly when reads vastly outnumber writes (a listener list, a config snapshot).
//
// An iterator holds the array it started on, so it reflects the list as of its creation and
// never throws ConcurrentModificationException.
//
// Single-exit style throughout (finding #105).
public class CopyOnWriteArrayList<E> implements List<E>, Serializable {

    private final Object sync = new Object();
    // Never mutated in place — replaced wholesale under `sync` by every write.
    private volatile Object[] elements = new Object[0];

    public CopyOnWriteArrayList() {
    }

    public int size() {
        return elements.length;
    }

    public boolean isEmpty() {
        return elements.length == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    // Null-safe equality. Written as a helper with an explicit if/else because a
    // **boolean-valued** ternary (`o == null ? e == null : o.equals(e)`) is rejected by our
    // javac with "operando no numérico" — finding #109. Int- and reference-valued ternaries
    // are fine, so only this shape needs the rewrite.
    private static boolean eq(Object a, Object b) {
        boolean same;
        if (a == null) {
            same = b == null;
        } else {
            same = a.equals(b);
        }
        return same;
    }

    public int indexOf(Object o) {
        Object[] snapshot = elements;
        int found = -1;
        for (int i = 0; i < snapshot.length; i++) {
            if (found < 0) {
                Object e = snapshot[i];
                if (eq(o, e)) {
                    found = i;
                }
            }
        }
        return found;
    }

    public E get(int index) {
        Object[] snapshot = elements;
        if (index < 0 || index >= snapshot.length) {
            throw new IndexOutOfBoundsException();
        }
        return (E) snapshot[index];
    }

    public boolean add(E e) {
        synchronized (sync) {
            Object[] old = elements;
            Object[] copy = new Object[old.length + 1];
            for (int i = 0; i < old.length; i++) {
                copy[i] = old[i];
            }
            copy[old.length] = e;
            elements = copy;
        }
        return true;
    }

    public void add(int index, E element) {
        synchronized (sync) {
            Object[] old = elements;
            if (index < 0 || index > old.length) {
                throw new IndexOutOfBoundsException();
            }
            Object[] copy = new Object[old.length + 1];
            for (int i = 0; i < index; i++) {
                copy[i] = old[i];
            }
            copy[index] = element;
            for (int i = index; i < old.length; i++) {
                copy[i + 1] = old[i];
            }
            elements = copy;
        }
    }

    public E set(int index, E element) {
        E prev;
        synchronized (sync) {
            Object[] old = elements;
            if (index < 0 || index >= old.length) {
                throw new IndexOutOfBoundsException();
            }
            prev = (E) old[index];
            Object[] copy = new Object[old.length];
            for (int i = 0; i < old.length; i++) {
                copy[i] = old[i];
            }
            copy[index] = element;
            elements = copy;
        }
        return prev;
    }

    public E remove(int index) {
        E prev;
        synchronized (sync) {
            Object[] old = elements;
            if (index < 0 || index >= old.length) {
                throw new IndexOutOfBoundsException();
            }
            prev = (E) old[index];
            Object[] copy = new Object[old.length - 1];
            for (int i = 0; i < index; i++) {
                copy[i] = old[i];
            }
            for (int i = index + 1; i < old.length; i++) {
                copy[i - 1] = old[i];
            }
            elements = copy;
        }
        return prev;
    }

    public boolean remove(Object o) {
        boolean removed;
        synchronized (sync) {
            int index = indexOf(o);
            if (index >= 0) {
                Object[] old = elements;
                Object[] copy = new Object[old.length - 1];
                for (int i = 0; i < index; i++) {
                    copy[i] = old[i];
                }
                for (int i = index + 1; i < old.length; i++) {
                    copy[i - 1] = old[i];
                }
                elements = copy;
                removed = true;
            } else {
                removed = false;
            }
        }
        return removed;
    }

    // Add only if absent, atomically — the reason this class exists for listener lists.
    public boolean addIfAbsent(E e) {
        boolean added;
        synchronized (sync) {
            if (indexOf(e) < 0) {
                add(e);
                added = true;
            } else {
                added = false;
            }
        }
        return added;
    }

    public void clear() {
        synchronized (sync) {
            elements = new Object[0];
        }
    }

    // Walks the array as it was when the iterator was created.
    public Iterator<E> iterator() {
        return new CowItr<E>(elements);
    }

    /**
     * A spliterator over these elements.
     */
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this,
                Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.IMMUTABLE);
    }

    // ---- las operaciones en bloque, escritas ------------------------------------------------
    //
    // Escritas y no heredadas porque en el JDK esta clase no extiende ningun esqueleto: es
    // `implements List` a secas. Los cuerpos son los mismos de `AbstractCollection`, sobre una
    // foto y no sobre el iterador vivo.
    //
    // El costo real aca es otro: cada `remove` de esta clase copia el arreglo entero, asi que un
    // `removeAll` cuesta una copia por elemento quitado. Es la contrapartida conocida del
    // copy-on-write, no un descuido — quien usa esta lista escribe poco y lee mucho.

    public boolean containsAll(java.util.Collection<?> c) {
        java.util.Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            if (!this.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(java.util.Collection<? extends E> c) {
        boolean cambio = false;
        java.util.Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            if (this.add(it.next())) {
                cambio = true;
            }
        }
        return cambio;
    }

    public boolean removeAll(java.util.Collection<?> c) {
        boolean cambio = false;
        Object[] foto = this.toArray();
        int i = 0;
        while (i < foto.length) {
            if (c.contains(foto[i])) {
                while (this.remove(foto[i])) {
                    cambio = true;
                }
            }
            i = i + 1;
        }
        return cambio;
    }

    public boolean retainAll(java.util.Collection<?> c) {
        boolean cambio = false;
        Object[] foto = this.toArray();
        int i = 0;
        while (i < foto.length) {
            if (!c.contains(foto[i])) {
                while (this.remove(foto[i])) {
                    cambio = true;
                }
            }
            i = i + 1;
        }
        return cambio;
    }

    public Object[] toArray() {
        Object[] out = new Object[this.size()];
        int i = 0;
        java.util.Iterator<E> it = this.iterator();
        while (it.hasNext() && i < out.length) {
            out[i] = it.next();
            i = i + 1;
        }
        return out;
    }

    public <T> T[] toArray(T[] a) {
        int n = this.size();
        Object[] dest = a;
        if (a.length < n) {
            dest = (Object[]) Array.newInstance(a.getClass().getComponentType(), n);
        }
        int i = 0;
        java.util.Iterator<E> it = this.iterator();
        while (it.hasNext() && i < n) {
            dest[i] = it.next();
            i = i + 1;
        }
        if (dest.length > n) {
            dest[n] = null;
        }
        return (T[]) dest;
    }

    // ---- lo que List agrega sobre Collection -------------------------------------------------
    //
    // Escritos y no heredados por lo mismo que las operaciones en bloque: en el JDK esta clase no
    // extiende ningun esqueleto. `AbstractListLitr` y `SubList`, que son los que usa
    // `AbstractList`, son package-private de `java.util` y desde aca no se ven.

    // El indice de la ULTIMA aparicion de `o`, o -1.
    public int lastIndexOf(Object o) {
        int i = this.size() - 1;
        while (i >= 0) {
            E e = this.get(i);
            if (o == null) {
                if (e == null) {
                    return i;
                }
            } else if (o.equals(e)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    // Un cursor bidireccional desde el principio.
    public java.util.ListIterator<E> listIterator() {
        return new CowLitr<E>(this, 0);
    }

    // Un cursor bidireccional desde `index`.
    public java.util.ListIterator<E> listIterator(int index) {
        return new CowLitr<E>(this, index);
    }

    // Una vista de [fromIndex, toIndex).
    public java.util.List<E> subList(int fromIndex, int toIndex) {
        return new CowSubList<E>(this, fromIndex, toIndex);
    }

    // Inserta todos los de `c` a partir de `index`, en el orden de su iterador.
    public boolean addAll(int index, java.util.Collection<? extends E> c) {
        if (index < 0 || index > this.size()) {
            throw new IndexOutOfBoundsException();
        }
        boolean cambio = false;
        int at = index;
        java.util.Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.add(at, it.next());
            at = at + 1;
            cambio = true;
        }
        return cambio;
    }
}

// The snapshot iterator: it holds the array the list had at creation, so later writes
// (which replace the array) are invisible to it and it can never see a torn update.
final class CowItr<E> implements Iterator<E> {

    private final Object[] snapshot;
    private int cursor;

    CowItr(Object[] snapshot) {
        this.snapshot = snapshot;
    }

    public boolean hasNext() {
        return cursor < snapshot.length;
    }

    public E next() {
        E e = (E) snapshot[cursor];
        cursor++;
        return e;
    }

}


// El ListIterator de CopyOnWriteArrayList. Gemelo de `java.util.AbstractListLitr`, que no se ve
// desde este paquete.
//
// El cursor va entre elementos; `ultimo` recuerda cual devolvio la ultima llamada, porque `set` y
// `remove` operan sobre ese y no sobre el hueco.
final class CowLitr<E> implements java.util.ListIterator<E> {

    private final java.util.List<E> list;
    private int cursor;
    private int ultimo;

    CowLitr(java.util.List<E> list, int index) {
        if (index < 0 || index > list.size()) {
            throw new IndexOutOfBoundsException();
        }
        this.list = list;
        this.cursor = index;
        this.ultimo = -1;
    }

    public boolean hasNext() {
        return this.cursor < this.list.size();
    }

    public E next() {
        if (this.cursor >= this.list.size()) {
            throw new java.util.NoSuchElementException();
        }
        E e = this.list.get(this.cursor);
        this.ultimo = this.cursor;
        this.cursor = this.cursor + 1;
        return e;
    }

    public boolean hasPrevious() {
        return this.cursor > 0;
    }

    public E previous() {
        if (this.cursor <= 0) {
            throw new java.util.NoSuchElementException();
        }
        this.cursor = this.cursor - 1;
        this.ultimo = this.cursor;
        return this.list.get(this.cursor);
    }

    public int nextIndex() {
        return this.cursor;
    }

    public int previousIndex() {
        return this.cursor - 1;
    }

    public void remove() {
        if (this.ultimo < 0) {
            throw new IllegalStateException();
        }
        this.list.remove(this.ultimo);
        if (this.ultimo < this.cursor) {
            this.cursor = this.cursor - 1;
        }
        this.ultimo = -1;
    }

    public void set(E e) {
        if (this.ultimo < 0) {
            throw new IllegalStateException();
        }
        this.list.set(this.ultimo, e);
    }

    public void add(E e) {
        this.list.add(this.cursor, e);
        this.cursor = this.cursor + 1;
        this.ultimo = -1;
    }
}

// La vista que devuelve CopyOnWriteArrayList.subList. Cuelga de `java.util.AbstractList`, que si
// es publica, y de ahi hereda iterator/listIterator/subList/lastIndexOf y las operaciones en
// bloque. Vista y no copia: escribir en ella escribe en la lista de atras.
final class CowSubList<E> extends java.util.AbstractList<E> {

    private final java.util.List<E> base;
    private final int offset;
    private int length;

    CowSubList(java.util.List<E> base, int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > base.size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        this.base = base;
        this.offset = fromIndex;
        this.length = toIndex - fromIndex;
    }

    public E get(int index) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException();
        }
        return this.base.get(this.offset + index);
    }

    public int size() {
        return this.length;
    }

    public E set(int index, E element) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException();
        }
        return this.base.set(this.offset + index, element);
    }

    public void add(int index, E element) {
        if (index < 0 || index > this.length) {
            throw new IndexOutOfBoundsException();
        }
        this.base.add(this.offset + index, element);
        this.length = this.length + 1;
    }

    public E remove(int index) {
        if (index < 0 || index >= this.length) {
            throw new IndexOutOfBoundsException();
        }
        E viejo = this.base.remove(this.offset + index);
        this.length = this.length - 1;
        return viejo;
    }

    public boolean add(E e) {
        this.add(this.length, e);
        return true;
    }
}
