package java.util;

// Las vistas y adaptadores chicos que devuelve Collections: un Set apoyado en un Map, una Queue
// apoyada en un Deque, las dos Enumeration y el comparador invertido. Package-private.

// Un Set respaldado por un Map<E, Boolean>.
//
// Parece un rodeo hasta que se ve para que existe: el JDK tiene IdentityHashMap y
// ConcurrentHashMap pero no tiene IdentityHashSet ni ConcurrentHashSet, y en vez de duplicar cada
// implementacion de mapa en version conjunto, expone esta. `newSetFromMap(new IdentityHashMap())`
// da un conjunto que compara por identidad; `newSetFromMap(new ConcurrentHashMap())`, uno seguro
// entre hilos. Un Set no es mas que un Map al que no le importan los valores.
//
// El mapa tiene que llegar **vacio** y no lo puede tocar nadie mas: si tiene claves de antes, el
// conjunto nace con elementos que nadie agrego; si alguien le escribe por afuera, el conjunto
// cambia sin que se lo pidan.
class SetFromMap<E> implements Set<E> {

    final Map<E, Boolean> m;

    // La vista de claves, que es donde vive casi todo el comportamiento del conjunto.
    private final Set<E> keys;

    SetFromMap(Map<E, Boolean> map) {
        if (!map.isEmpty()) {
            throw new IllegalArgumentException("Map is non-empty");
        }
        this.m = map;
        this.keys = map.keySet();
    }

    public int size() {
        return this.m.size();
    }

    public boolean isEmpty() {
        return this.m.isEmpty();
    }

    public boolean contains(Object o) {
        return this.m.containsKey(o);
    }

    // `put` devuelve el valor anterior, o null si la clave no estaba: eso es exactamente lo que
    // `add` tiene que informar.
    public boolean add(E e) {
        return this.m.put(e, Boolean.TRUE) == null;
    }

    public boolean remove(Object o) {
        return this.m.remove(o) != null;
    }

    public void clear() {
        this.m.clear();
    }

    public Iterator<E> iterator() {
        return this.keys.iterator();
    }

    public Object[] toArray() {
        return this.keys.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.keys.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return this.keys.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            if (this.add(it.next())) {
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        return this.keys.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return this.keys.retainAll(c);
    }

    public boolean equals(Object o) {
        return o == this || this.keys.equals(o);
    }

    public int hashCode() {
        return this.keys.hashCode();
    }

    public String toString() {
        return this.keys.toString();
    }
}

// La misma idea sobre un SequencedMap, que conserva el orden de encuentro y por lo tanto puede
// darse vuelta.
final class SequencedSetFromMap<E> extends SetFromMap<E> implements SequencedSet<E> {

    SequencedSetFromMap(SequencedMap<E, Boolean> map) {
        super(map);
    }

    public SequencedSet<E> reversed() {
        return new SequencedSetFromMap<E>(((SequencedMap<E, Boolean>) this.m).reversed());
    }

    public void addFirst(E e) {
        ((SequencedMap<E, Boolean>) this.m).putFirst(e, Boolean.TRUE);
    }

    public void addLast(E e) {
        ((SequencedMap<E, Boolean>) this.m).putLast(e, Boolean.TRUE);
    }

    public E getFirst() {
        return ((SequencedMap<E, Boolean>) this.m).firstEntry().getKey();
    }

    public E getLast() {
        return ((SequencedMap<E, Boolean>) this.m).lastEntry().getKey();
    }

    public E removeFirst() {
        return ((SequencedMap<E, Boolean>) this.m).pollFirstEntry().getKey();
    }

    public E removeLast() {
        return ((SequencedMap<E, Boolean>) this.m).pollLastEntry().getKey();
    }
}

// Una Queue que saca por donde mete: una pila con cara de cola.
//
// Sirve para pasarle una pila a codigo escrito contra Queue sin que ese codigo se entere. Lo
// unico que cambia respecto del Deque de atras es que las tres operaciones de cola apuntan al
// mismo extremo -- el frente -- en vez de meter atras y sacar adelante.
final class LifoQueue<E> implements Queue<E> {

    private final Deque<E> back;

    LifoQueue(Deque<E> back) {
        if (back == null) {
            throw new NullPointerException();
        }
        this.back = back;
    }

    public boolean add(E e) {
        this.back.addFirst(e);
        return true;
    }

    public boolean offer(E e) {
        return this.back.offerFirst(e);
    }

    public E poll() {
        return this.back.pollFirst();
    }

    public E peek() {
        return this.back.peekFirst();
    }

    public int size() {
        return this.back.size();
    }

    public boolean isEmpty() {
        return this.back.isEmpty();
    }

    public boolean contains(Object o) {
        return this.back.contains(o);
    }

    public boolean remove(Object o) {
        return this.back.remove(o);
    }

    public void clear() {
        this.back.clear();
    }

    public Iterator<E> iterator() {
        return this.back.iterator();
    }

    public Object[] toArray() {
        return this.back.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.back.toArray(a);
    }

    public boolean containsAll(Collection<?> c) {
        return this.back.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        Iterator<? extends E> it = c.iterator();
        while (it.hasNext()) {
            this.back.addFirst(it.next());
            changed = true;
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        return this.back.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return this.back.retainAll(c);
    }

    public String toString() {
        return this.back.toString();
    }
}

// La Enumeration que no tiene nada.
final class EmptyEnumeration<E> implements Enumeration<E> {

    public boolean hasMoreElements() {
        return false;
    }

    public E nextElement() {
        throw new NoSuchElementException();
    }
}

// Una Enumeration sobre una foto de la coleccion. Se toma la foto en el constructor a proposito:
// `Collections.enumeration` promete recorrer lo que habia, y una Enumeration no tiene forma de
// avisar que la coleccion cambio mientras tanto -- no existe el equivalente de
// ConcurrentModificationException para ella.
final class ArrayEnumeration<E> implements Enumeration<E> {

    private final Object[] items;
    private int cursor;

    ArrayEnumeration(Object[] items) {
        this.items = items;
        this.cursor = 0;
    }

    public boolean hasMoreElements() {
        return this.cursor < this.items.length;
    }

    public E nextElement() {
        if (this.cursor >= this.items.length) {
            throw new NoSuchElementException();
        }
        E e = (E) this.items[this.cursor];
        this.cursor = this.cursor + 1;
        return e;
    }
}

// El comparador que da vuelta a otro, o al orden natural si no se le pasa ninguno.
//
// Invertir es `compare(b, a)` y no `-compare(a, b)`: la negacion se rompe con Integer.MIN_VALUE,
// que es su propio negativo, y un comparador que devuelve MIN_VALUE es raro pero legal.
final class ReverseComparator<T> implements Comparator<T> {

    private final Comparator<T> inner;

    ReverseComparator(Comparator<T> inner) {
        this.inner = inner;
    }

    public int compare(T a, T b) {
        if (this.inner != null) {
            return this.inner.compare(b, a);
        }
        return ((Comparable<T>) b).compareTo(a);
    }
}
