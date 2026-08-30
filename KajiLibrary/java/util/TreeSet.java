package java.util;

// Un conjunto ordenado, apoyado en un {@link TreeMap} — exactamente como lo arma el JDK. Un
// conjunto es un mapa cuyos valores no dicen nada, asi que cada elemento se guarda como clave
// apuntando a un unico objeto centinela compartido; todo el trabajo (orden, balanceo, busqueda
// en O(log n)) es del arbol, y esta clase es la proyeccion delgada que esconde los valores.
//
// Ese es el diseno entero: `add` es `put`, `contains` es `containsKey`, `remove` es `remove`, y
// recorrer es recorrer el arbol en orden.
//
// Lo que se apoya no es un `TreeMap` sino un **`NavigableMap` cualquiera**, y eso es lo que hace
// que las vistas salgan gratis: `headSet(x)` es este mismo conjunto sobre `mapa.headMap(x)`, y
// `descendingSet()` sobre `mapa.descendingMap()`. Los quince metodos de navegacion se escriben una
// sola vez y funcionan igual sobre el conjunto entero o sobre un corte de un corte al reves.
//
// El mismo mecanismo da las vistas de clave de un mapa: `TreeMap.navigableKeySet()` devuelve un
// TreeSet sobre el mapa, con `noAdd` puesto. Es la unica diferencia entre un conjunto y la vista
// de claves de un mapa: la vista **no** puede agregar, porque no sabria que valor poner.
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E> {

    // El unico valor al que apunta toda clave. Su identidad no importa — solo cuenta que "hay una
    // entrada aca" — asi que una instancia alcanza para todos los elementos de todos los TreeSet.
    private static final Object PRESENT = new Object();

    private final NavigableMap<E, Object> map;

    // El mismo mapa visto como recorrible. Se guarda aparte porque `NavigableMap` no promete
    // saber caminarse nodo a nodo; los dos que llegan aca — TreeMap y TmView — si.
    private final TmWalk<E, Object> walk;

    // Puesto cuando este conjunto es la vista de claves de un mapa: entonces `add` se niega.
    private final boolean noAdd;

    public TreeSet() {
        this(new TreeMap<E, Object>(), false);
    }

    public TreeSet(Comparator<E> comparator) {
        this(new TreeMap<E, Object>(comparator), false);
    }

    // Copia los elementos de otra coleccion, ordenandolos por su orden natural.
    public TreeSet(Collection<? extends E> c) {
        this(new TreeMap<E, Object>(), false);
        this.addAll(c);
    }

    // Copia un conjunto que **ya viene ordenado**, y se queda con su comparador — igual que
    // `TreeMap(SortedMap)`, y por la misma razon: sin el comparador la copia se reordenaria.
    public TreeSet(SortedSet<E> s) {
        this(new TreeMap<E, Object>((Comparator<E>) s.comparator()), false);
        this.addAll(s);
    }

    TreeSet(NavigableMap<E, Object> map, boolean noAdd) {
        this.map = map;
        this.walk = (TmWalk<E, Object>) map;
        this.noAdd = noAdd;
    }

    // Envuelve una vista del mapa de atras conservando la restriccion de agregado: un corte de
    // una vista de claves sigue sin poder agregar.
    private TreeSet<E> over(NavigableMap<E, Object> view) {
        return new TreeSet<E>(view, this.noAdd);
    }

    public int size() {
        return this.map.size();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public boolean contains(Object o) {
        return this.map.containsKey(o);
    }

    // Un `add` de conjunto informa si el elemento era **nuevo**, que es exactamente si el `put` no
    // encontro nada antes.
    public boolean add(E e) {
        if (this.noAdd) {
            throw new UnsupportedOperationException();
        }
        return this.map.put(e, PRESENT) == null;
    }

    public boolean remove(Object o) {
        return this.map.remove(o) != null;
    }

    public void clear() {
        this.map.clear();
    }

    public Comparator<? super E> comparator() {
        return this.map.comparator();
    }

    public Iterator<E> iterator() {
        return new TmKeyItr<E, Object>(this.walk, this.map);
    }

    public Iterator<E> descendingIterator() {
        return this.descendingSet().iterator();
    }

    // --- SortedSet ---

    public E first() {
        return this.map.firstKey();
    }

    public E last() {
        return this.map.lastKey();
    }

    // --- SequencedCollection ---
    //
    // Los extremos se leen y se sacan, pero **no se ponen**: en un conjunto ordenado la posicion
    // la decide el orden, no quien inserta. Es la misma negativa que `TreeMap.putFirst`.

    public E getFirst() {
        return this.first();
    }

    public E getLast() {
        return this.last();
    }

    public E removeFirst() {
        E e = this.first();
        this.remove(e);
        return e;
    }

    public E removeLast() {
        E e = this.last();
        this.remove(e);
        return e;
    }

    public void addFirst(E e) {
        throw new UnsupportedOperationException();
    }

    public void addLast(E e) {
        throw new UnsupportedOperationException();
    }

    // Se estrecha a NavigableSet, que es lo que `NavigableSet.reversed()` promete desde que lleva
    // su propio default.
    public NavigableSet<E> reversed() {
        return this.descendingSet();
    }

    // --- NavigableSet: los vecinos ---
    //
    // Las cuatro son la navegacion del mapa mirando solo las claves. `lower` es el mayor elemento
    // estrictamente menor; `floor`, el mayor <=; y `ceiling`/`higher` los simetricos hacia
    // arriba. Devuelven null cuando no hay ninguno, que es la unica respuesta razonable — a
    // diferencia de `first`/`last`, que prometen un elemento y por eso lanzan.

    public E lower(E e) {
        return this.map.lowerKey(e);
    }

    public E floor(E e) {
        return this.map.floorKey(e);
    }

    public E ceiling(E e) {
        return this.map.ceilingKey(e);
    }

    public E higher(E e) {
        return this.map.higherKey(e);
    }

    public E pollFirst() {
        Map.Entry<E, Object> e = this.map.pollFirstEntry();
        if (e == null) {
            return null;
        }
        return e.getKey();
    }

    public E pollLast() {
        Map.Entry<E, Object> e = this.map.pollLastEntry();
        if (e == null) {
            return null;
        }
        return e.getKey();
    }

    // --- NavigableSet: las vistas ---

    public NavigableSet<E> descendingSet() {
        return this.over(this.map.descendingMap());
    }

    public NavigableSet<E> subSet(E from, boolean fromInclusive, E to, boolean toInclusive) {
        return this.over(this.map.subMap(from, fromInclusive, to, toInclusive));
    }

    public NavigableSet<E> headSet(E to, boolean inclusive) {
        return this.over(this.map.headMap(to, inclusive));
    }

    public NavigableSet<E> tailSet(E from, boolean inclusive) {
        return this.over(this.map.tailMap(from, inclusive));
    }

    // Las tres formas de SortedSet: el piso entra, el techo no.
    public SortedSet<E> subSet(E from, E to) {
        return this.subSet(from, true, to, false);
    }

    public SortedSet<E> headSet(E to) {
        return this.headSet(to, false);
    }

    public SortedSet<E> tailSet(E from) {
        return this.tailSet(from, true);
    }

    /**
     * A spliterator over these elements, in sort order.
     *
     * @see SortedSet#spliterator()
     */
    public Spliterator<E> spliterator() {
        final Comparator<? super E> order = this.comparator();
        return new Spliterators.IteratorSpliterator<E>(this,
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED
                        | Spliterator.SIZED) {
            public Comparator<? super E> getComparator() {
                return order;
            }
        };
    }
}
