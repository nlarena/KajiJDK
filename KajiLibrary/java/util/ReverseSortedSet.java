package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Map;

// Las vistas invertidas genericas: la de un `SortedSet`, la de un `SortedMap` y la de un `Deque`.
// Son las que respaldan los `reversed()` que esas tres interfaces declaran como `default`, para
// cualquier implementacion -- incluida una que escriba alguien de afuera.
//
// **El problema, y por que no se resuelve copiando.** `reversed()` promete una *vista*: lo que se
// agregue de un lado tiene que verse del otro. Copiar los elementos a un arreglo y darlo vuelta es
// tres lineas y produce una foto; la primera modificacion la deja obsoleta sin avisar. Asi que estas
// clases no guardan elementos: guardan el conjunto de atras y una forma de recorrerlo al reves.
//
// **Como se recorre al reves un SortedSet cualquiera.** No hay iterador hacia atras en la interfaz,
// pero si hay `last()` y `headSet(e)`: el anterior a `e` es `headSet(e).last()`. Encadenando eso se
// recorre entero, en O(log n) por paso sobre un arbol. Es lo mismo que hace el JDK, y es la razon de
// que estas vistas sean utiles y no un adorno: no materializan nada.
//
// La comparacion tambien se invierte, con `Collections.reverseOrder`, para que `first`/`last`,
// `headSet`/`tailSet` y el orden de iteracion queden todos coherentes entre si.

/** Un `SortedSet` visto al reves. */
final class ReverseSortedSet<E> extends AbstractSet<E> implements SortedSet<E> {

    private final SortedSet<E> base;

    ReverseSortedSet(SortedSet<E> base) {
        this.base = base;
    }

    public Comparator<? super E> comparator() {
        Comparator<? super E> c = this.base.comparator();
        if (c == null) {
            // Orden natural del de atras: invertido es el `reverseOrder` sin comparador.
            return (Comparator<? super E>) Collections.reverseOrder();
        }
        return Collections.reverseOrder(c);
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public boolean contains(Object o) {
        return this.base.contains(o);
    }

    public boolean add(E e) {
        return this.base.add(e);
    }

    public boolean remove(Object o) {
        return this.base.remove(o);
    }

    public void clear() {
        this.base.clear();
    }

    public Iterator<E> iterator() {
        return new ReverseSortedItr<E>(this.base);
    }

    /** Invertir lo invertido es el de atras, no un tercer envoltorio. */
    public SortedSet<E> reversed() {
        return this.base;
    }

    public E first() {
        return this.base.last();
    }

    public E last() {
        return this.base.first();
    }

    // Los tres cortes se dan vuelta junto con el orden: lo que en la vista es "hasta `to`" es en el
    // de atras "desde `to`", y **exclusivo/inclusivo se intercambian** -- de ahi que `headSet` de la
    // vista use `tailSet` del de atras y despues saque el propio `to`.
    public SortedSet<E> headSet(E to) {
        SortedSet<E> cola = this.base.tailSet(to);
        return new ReverseSortedSet<E>(new SinPrimero<E>(cola, to));
    }

    public SortedSet<E> tailSet(E from) {
        return new ReverseSortedSet<E>(this.base.headSet(from));
    }

    public SortedSet<E> subSet(E from, E to) {
        SortedSet<E> tramo = this.base.subSet(to, from);
        return new ReverseSortedSet<E>(new SinPrimero<E>(tramo, to));
    }
}

// El recorrido hacia atras: arranca en `last()` y va tomando `headSet(actual).last()`.
final class ReverseSortedItr<E> implements Iterator<E> {

    private final SortedSet<E> base;
    private E proximo;
    private boolean hay;
    private boolean arrancado = false;

    ReverseSortedItr(SortedSet<E> base) {
        this.base = base;
    }

    private void arrancar() {
        if (!this.arrancado) {
            this.arrancado = true;
            this.hay = !this.base.isEmpty();
            if (this.hay) {
                this.proximo = this.base.last();
            }
        }
    }

    public boolean hasNext() {
        this.arrancar();
        return this.hay;
    }

    public E next() {
        this.arrancar();
        if (!this.hay) {
            throw new NoSuchElementException();
        }
        E actual = this.proximo;
        SortedSet<E> antes = this.base.headSet(actual);
        if (antes.isEmpty()) {
            this.hay = false;
        } else {
            this.proximo = antes.last();
        }
        return actual;
    }
}

// Un `SortedSet` sin su primer elemento. Existe para un solo detalle de los cortes de arriba: al dar
// vuelta el orden, un limite que era exclusivo pasa a ser inclusivo, y esta clase saca justamente el
// que sobra. Sin esto, `reversed().headSet(x)` incluiria `x`, que es el error clasico de invertir
// rangos y el mas dificil de ver en una prueba que solo mira los tamaños.
final class SinPrimero<E> extends AbstractSet<E> implements SortedSet<E> {

    private final SortedSet<E> base;
    private final E excluido;

    SinPrimero(SortedSet<E> base, E excluido) {
        this.base = base;
        this.excluido = excluido;
    }

    public Comparator<? super E> comparator() {
        return this.base.comparator();
    }

    public int size() {
        int n = this.base.size();
        return this.base.contains(this.excluido) ? n - 1 : n;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public boolean contains(Object o) {
        if (o != null && o.equals(this.excluido)) {
            return false;
        }
        return this.base.contains(o);
    }

    public Iterator<E> iterator() {
        Iterator<E> it = this.base.iterator();
        // El excluido, si esta, es el primero: `base` es la cola desde el.
        if (this.base.contains(this.excluido) && it.hasNext()) {
            it.next();
        }
        return it;
    }

    public E first() {
        Iterator<E> it = this.iterator();
        if (!it.hasNext()) {
            throw new NoSuchElementException();
        }
        return it.next();
    }

    public E last() {
        E u = this.base.last();
        if (u != null && u.equals(this.excluido)) {
            throw new NoSuchElementException();
        }
        return u;
    }

    public SortedSet<E> headSet(E to) {
        return new SinPrimero<E>(this.base.headSet(to), this.excluido);
    }

    public SortedSet<E> tailSet(E from) {
        return new SinPrimero<E>(this.base.tailSet(from), this.excluido);
    }

    public SortedSet<E> subSet(E from, E to) {
        return new SinPrimero<E>(this.base.subSet(from, to), this.excluido);
    }
}

/** Un `SortedMap` visto al reves. */
final class ReverseSortedMap<K, V> extends AbstractMap<K, V> implements SortedMap<K, V> {

    private final SortedMap<K, V> base;

    ReverseSortedMap(SortedMap<K, V> base) {
        this.base = base;
    }

    public Comparator<? super K> comparator() {
        Comparator<? super K> c = this.base.comparator();
        if (c == null) {
            return (Comparator<? super K>) Collections.reverseOrder();
        }
        return Collections.reverseOrder(c);
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.base.containsKey(key);
    }

    public V get(Object key) {
        return this.base.get(key);
    }

    public V put(K key, V value) {
        return this.base.put(key, value);
    }

    public V remove(Object key) {
        return this.base.remove(key);
    }

    public void clear() {
        this.base.clear();
    }

    public SortedMap<K, V> reversed() {
        return this.base;
    }

    public K firstKey() {
        return this.base.lastKey();
    }

    public K lastKey() {
        return this.base.firstKey();
    }

    public SortedMap<K, V> headMap(K to) {
        return new ReverseSortedMap<K, V>(this.base.tailMap(to));
    }

    public SortedMap<K, V> tailMap(K from) {
        return new ReverseSortedMap<K, V>(this.base.headMap(from));
    }

    public SortedMap<K, V> subMap(K from, K to) {
        return new ReverseSortedMap<K, V>(this.base.subMap(to, from));
    }
}

/** Un `Deque` visto al reves: las dos puntas intercambiadas. */
final class ReverseDeque<E> extends AbstractCollection<E> implements Deque<E> {

    private final Deque<E> base;

    ReverseDeque(Deque<E> base) {
        this.base = base;
    }

    public int size() {
        return this.base.size();
    }

    public boolean isEmpty() {
        return this.base.isEmpty();
    }

    public void clear() {
        this.base.clear();
    }

    public boolean contains(Object o) {
        return this.base.contains(o);
    }

    public Iterator<E> iterator() {
        return this.base.descendingIterator();
    }

    public Iterator<E> descendingIterator() {
        return this.base.iterator();
    }

    public Deque<E> reversed() {
        return this.base;
    }

    // Todo el resto es el mismo metodo con la punta cambiada.
    public void addFirst(E e) {
        this.base.addLast(e);
    }

    public void addLast(E e) {
        this.base.addFirst(e);
    }

    public boolean offerFirst(E e) {
        return this.base.offerLast(e);
    }

    public boolean offerLast(E e) {
        return this.base.offerFirst(e);
    }

    public E removeFirst() {
        return this.base.removeLast();
    }

    public E removeLast() {
        return this.base.removeFirst();
    }

    public E pollFirst() {
        return this.base.pollLast();
    }

    public E pollLast() {
        return this.base.pollFirst();
    }

    public E getFirst() {
        return this.base.getLast();
    }

    public E getLast() {
        return this.base.getFirst();
    }

    public E peekFirst() {
        return this.base.peekLast();
    }

    public E peekLast() {
        return this.base.peekFirst();
    }

    public boolean removeFirstOccurrence(Object o) {
        return this.base.removeLastOccurrence(o);
    }

    public boolean removeLastOccurrence(Object o) {
        return this.base.removeFirstOccurrence(o);
    }

    // Los de `Queue` y los de pila, que en un Deque son sinonimos de los de arriba. Se escriben en
    // terminos de **esta** vista, no del de atras: `push` empuja al principio de la vista.
    public boolean add(E e) {
        this.addLast(e);
        return true;
    }

    public boolean offer(E e) {
        return this.offerLast(e);
    }

    public E remove() {
        return this.removeFirst();
    }

    public E poll() {
        return this.pollFirst();
    }

    public E element() {
        return this.getFirst();
    }

    public E peek() {
        return this.peekFirst();
    }

    public void push(E e) {
        this.addFirst(e);
    }

    public E pop() {
        return this.removeFirst();
    }

    public boolean remove(Object o) {
        return this.removeFirstOccurrence(o);
    }
}
