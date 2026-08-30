package java.util;

// Las vistas de un TreeMap: subMap, headMap, tailMap y descendingMap, todas la misma clase.
//
// El JDK tiene tres (`NavigableSubMap` y sus dos subclases `Ascending`/`Descending`); aca hay una
// sola, con un piso, un techo y un booleano de sentido. Las cinco fabricas de `TreeMap` son cinco
// combinaciones de esos campos:
//
//   headMap(to)        sin piso, techo en `to`,   ascendente
//   tailMap(from)      piso en `from`, sin techo, ascendente
//   subMap(from, to)   los dos limites,           ascendente
//   descendingMap()    sin limites,               descendente
//   y cualquier corte de un descendente, que combina las dos cosas
//
// **Es una vista, no una copia**, y ahi esta todo el punto: `mapa.subMap(a, b).clear()` borra ese
// rango del mapa original, y `mapa.put(...)` dentro del rango se ve por la vista. Una copia haria
// que la primera linea no borrara nada, en silencio.
//
// Lo que una vista **no** hace es dejar escribir fuera de su rango: `put` de una clave que no cae
// entre los limites tira IllegalArgumentException. Sin eso, "la vista de [a, b)" seria mentira.
//
// El sentido descendente es lo que ahorra la mitad del codigo. Todo se calcula primero en orden
// **absoluto** -- el del mapa de atras -- con los seis metodos `abs*`, y recien al final se
// traduce: para una vista al reves, "el primero" es el mayor y `lower(k)` es el `absHigher(k)`.
// Escribir las dos direcciones por separado seria duplicar catorce metodos para cambiarles el
// signo.
//
// Costo a tener presente: `size()` **cuenta**, O(n). El JDK hace lo mismo con sus submapas, y por
// la misma razon: el arbol no lleva cuenta de cuantos nodos hay en un rango, y mantenerla saldria
// mas caro que la cuenta ocasional.
class TmView<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, TmWalk<K, V> {

    private final TreeMap<K, V> m;

    // Los limites, en orden **absoluto**: `lo` siempre es el chico aunque la vista sea
    // descendente. `fromStart`/`toEnd` dicen que ese lado no tiene limite -- y no se puede usar
    // null para eso, porque null puede ser una clave.
    private final boolean fromStart;
    private final Object lo;
    private final boolean loInc;
    private final boolean toEnd;
    private final Object hi;
    private final boolean hiInc;

    private final boolean desc;

    TmView(TreeMap<K, V> m, boolean fromStart, Object lo, boolean loInc,
            boolean toEnd, Object hi, boolean hiInc, boolean desc) {
        this.m = m;
        this.fromStart = fromStart;
        this.lo = lo;
        this.loInc = loInc;
        this.toEnd = toEnd;
        this.hi = hi;
        this.hiInc = hiInc;
        this.desc = desc;
    }

    // --- el rango ---

    private boolean tooLow(Object key) {
        if (this.fromStart) {
            return false;
        }
        int c = this.m.compare(key, this.lo);
        return c < 0 || (c == 0 && !this.loInc);
    }

    private boolean tooHigh(Object key) {
        if (this.toEnd) {
            return false;
        }
        int c = this.m.compare(key, this.hi);
        return c > 0 || (c == 0 && !this.hiInc);
    }

    private boolean inRange(Object key) {
        return !this.tooLow(key) && !this.tooHigh(key);
    }

    // Filtra un nodo por el rango. Todas las busquedas de abajo terminan pasando por aca, que es
    // lo que hace que una vista no pueda ver ni un nodo de mas.
    private TmNode<K, V> clip(TmNode<K, V> p) {
        if (p == null || !this.inRange(p.key)) {
            return null;
        }
        return p;
    }

    // --- las seis busquedas en orden absoluto ---

    private TmNode<K, V> absLowest() {
        TmNode<K, V> p;
        if (this.fromStart) {
            p = this.m.firstNode();
        } else if (this.loInc) {
            p = this.m.getCeilingNode(this.lo);
        } else {
            p = this.m.getHigherNode(this.lo);
        }
        return this.clip(p);
    }

    private TmNode<K, V> absHighest() {
        TmNode<K, V> p;
        if (this.toEnd) {
            p = this.m.lastNode();
        } else if (this.hiInc) {
            p = this.m.getFloorNode(this.hi);
        } else {
            p = this.m.getLowerNode(this.hi);
        }
        return this.clip(p);
    }

    // Ojo con estas cuatro: cuando la clave pedida cae **fuera** del rango, la respuesta no es
    // null sino el extremo del rango. Pedir el "ceiling" de algo que esta por debajo del piso
    // tiene que dar el primer elemento de la vista, no nada.
    private TmNode<K, V> absCeiling(Object key) {
        if (this.tooLow(key)) {
            return this.absLowest();
        }
        return this.clip(this.m.getCeilingNode(key));
    }

    private TmNode<K, V> absHigher(Object key) {
        if (this.tooLow(key)) {
            return this.absLowest();
        }
        return this.clip(this.m.getHigherNode(key));
    }

    private TmNode<K, V> absFloor(Object key) {
        if (this.tooHigh(key)) {
            return this.absHighest();
        }
        return this.clip(this.m.getFloorNode(key));
    }

    private TmNode<K, V> absLower(Object key) {
        if (this.tooHigh(key)) {
            return this.absHighest();
        }
        return this.clip(this.m.getLowerNode(key));
    }

    // --- la traduccion al sentido de la vista ---

    private TmNode<K, V> viewFirst() {
        return this.desc ? this.absHighest() : this.absLowest();
    }

    private TmNode<K, V> viewLast() {
        return this.desc ? this.absLowest() : this.absHighest();
    }

    private TmNode<K, V> viewLower(Object key) {
        return this.desc ? this.absHigher(key) : this.absLower(key);
    }

    private TmNode<K, V> viewFloor(Object key) {
        return this.desc ? this.absCeiling(key) : this.absFloor(key);
    }

    private TmNode<K, V> viewCeiling(Object key) {
        return this.desc ? this.absFloor(key) : this.absCeiling(key);
    }

    private TmNode<K, V> viewHigher(Object key) {
        return this.desc ? this.absLower(key) : this.absHigher(key);
    }

    // --- TmWalk: el recorrido, en el sentido de la vista y sin salirse del rango ---

    public TmNode<K, V> walkFirst() {
        return this.viewFirst();
    }

    public TmNode<K, V> walkNext(TmNode<K, V> n) {
        TmNode<K, V> p;
        if (this.desc) {
            p = this.m.predecessor(n);
        } else {
            p = this.m.successor(n);
        }
        return this.clip(p);
    }

    // --- Map ---

    // Cuenta, O(n). Es el precio de que el arbol no lleve cuenta por rango.
    public int size() {
        int n = 0;
        TmNode<K, V> p = this.walkFirst();
        while (p != null) {
            n = n + 1;
            p = this.walkNext(p);
        }
        return n;
    }

    public boolean isEmpty() {
        return this.walkFirst() == null;
    }

    public boolean containsKey(Object key) {
        return this.inRange(key) && this.m.containsKey(key);
    }

    public V get(Object key) {
        if (!this.inRange(key)) {
            return null;
        }
        return this.m.get(key);
    }

    // Escribir fuera del rango es un error, no un no-op silencioso: la vista prometio ser
    // exactamente ese rango.
    public V put(K key, V value) {
        if (!this.inRange(key)) {
            throw new IllegalArgumentException("key out of range");
        }
        return this.m.put(key, value);
    }

    public V remove(Object key) {
        if (!this.inRange(key)) {
            return null;
        }
        return this.m.remove(key);
    }

    public void clear() {
        // Se toman las claves primero y se borran despues: borrar mientras se camina el arbol
        // deja al nodo actual sin enlaces, y el recorrido se pierde.
        ArrayList<K> claves = new ArrayList<K>();
        TmNode<K, V> p = this.walkFirst();
        while (p != null) {
            claves.add(p.key);
            p = this.walkNext(p);
        }
        int i = 0;
        while (i < claves.size()) {
            this.m.remove(claves.get(i));
            i = i + 1;
        }
    }

    public boolean containsValue(Object value) {
        TmNode<K, V> p = this.walkFirst();
        while (p != null) {
            Object v = p.value;
            if (value == null) {
                if (v == null) {
                    return true;
                }
            } else if (value.equals(v)) {
                return true;
            }
            p = this.walkNext(p);
        }
        return false;
    }

    public Set<K> keySet() {
        return this.navigableKeySet();
    }

    public Collection<V> values() {
        ArrayList<V> out = new ArrayList<V>();
        TmNode<K, V> p = this.walkFirst();
        while (p != null) {
            out.add(p.value);
            p = this.walkNext(p);
        }
        return out;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        LinkedHashSet<Map.Entry<K, V>> out = new LinkedHashSet<Map.Entry<K, V>>();
        TmNode<K, V> p = this.walkFirst();
        while (p != null) {
            out.add(new FixedEntry<K, V>(p.key, p.value));
            p = this.walkNext(p);
        }
        return out;
    }

    // --- SortedMap ---

    // El comparador de una vista descendente es el del mapa dado vuelta, y tiene que serlo: quien
    // reciba este mapa y quiera ordenar algo igual que el, necesita el orden que la vista muestra,
    // no el del mapa de atras.
    public Comparator<? super K> comparator() {
        if (this.desc) {
            return Collections.reverseOrder(this.m.comparator());
        }
        return this.m.comparator();
    }

    public K firstKey() {
        TmNode<K, V> p = this.viewFirst();
        if (p == null) {
            throw new NoSuchElementException();
        }
        return p.key;
    }

    public K lastKey() {
        TmNode<K, V> p = this.viewLast();
        if (p == null) {
            throw new NoSuchElementException();
        }
        return p.key;
    }

    // --- SequencedMap ---

    public Map.Entry<K, V> firstEntry() {
        return TreeMap.entryOf(this.viewFirst());
    }

    public Map.Entry<K, V> lastEntry() {
        return TreeMap.entryOf(this.viewLast());
    }

    public Map.Entry<K, V> pollFirstEntry() {
        TmNode<K, V> p = this.viewFirst();
        Map.Entry<K, V> e = TreeMap.entryOf(p);
        if (p != null) {
            this.m.remove(p.key);
        }
        return e;
    }

    public Map.Entry<K, V> pollLastEntry() {
        TmNode<K, V> p = this.viewLast();
        Map.Entry<K, V> e = TreeMap.entryOf(p);
        if (p != null) {
            this.m.remove(p.key);
        }
        return e;
    }

    public V putFirst(K k, V v) {
        throw new UnsupportedOperationException();
    }

    public V putLast(K k, V v) {
        throw new UnsupportedOperationException();
    }

    // --- NavigableMap: los vecinos ---

    public Map.Entry<K, V> lowerEntry(K key) {
        return TreeMap.entryOf(this.viewLower(key));
    }

    public K lowerKey(K key) {
        return TreeMap.keyOf(this.viewLower(key));
    }

    public Map.Entry<K, V> floorEntry(K key) {
        return TreeMap.entryOf(this.viewFloor(key));
    }

    public K floorKey(K key) {
        return TreeMap.keyOf(this.viewFloor(key));
    }

    public Map.Entry<K, V> ceilingEntry(K key) {
        return TreeMap.entryOf(this.viewCeiling(key));
    }

    public K ceilingKey(K key) {
        return TreeMap.keyOf(this.viewCeiling(key));
    }

    public Map.Entry<K, V> higherEntry(K key) {
        return TreeMap.entryOf(this.viewHigher(key));
    }

    public K higherKey(K key) {
        return TreeMap.keyOf(this.viewHigher(key));
    }

    // --- NavigableMap: cortes de un corte ---
    //
    // Los limites nuevos se piden en el orden **de la vista**, y hay que guardarlos en orden
    // absoluto: sobre una vista descendente, el "desde" del que llama es el techo.

    public NavigableMap<K, V> subMap(K from, boolean fromInclusive, K to, boolean toInclusive) {
        if (!this.inRange(from) || !this.inRange(to)) {
            throw new IllegalArgumentException("key out of range");
        }
        if (this.desc) {
            return new TmView<K, V>(this.m, false, to, toInclusive, false, from, fromInclusive,
                    true);
        }
        return new TmView<K, V>(this.m, false, from, fromInclusive, false, to, toInclusive, false);
    }

    public NavigableMap<K, V> headMap(K to, boolean inclusive) {
        if (!this.inRange(to)) {
            throw new IllegalArgumentException("key out of range");
        }
        if (this.desc) {
            // "hasta `to` sin incluirlo", vista al reves, es "desde `to` para arriba" en absoluto.
            return new TmView<K, V>(this.m, false, to, inclusive, this.toEnd, this.hi, this.hiInc,
                    true);
        }
        return new TmView<K, V>(this.m, this.fromStart, this.lo, this.loInc, false, to, inclusive,
                false);
    }

    public NavigableMap<K, V> tailMap(K from, boolean inclusive) {
        if (!this.inRange(from)) {
            throw new IllegalArgumentException("key out of range");
        }
        if (this.desc) {
            return new TmView<K, V>(this.m, this.fromStart, this.lo, this.loInc, false, from,
                    inclusive, true);
        }
        return new TmView<K, V>(this.m, false, from, inclusive, this.toEnd, this.hi, this.hiInc,
                false);
    }

    public SortedMap<K, V> subMap(K from, K to) {
        return this.subMap(from, true, to, false);
    }

    public SortedMap<K, V> headMap(K to) {
        return this.headMap(to, false);
    }

    public SortedMap<K, V> tailMap(K from) {
        return this.tailMap(from, true);
    }

    // Dar vuelta una vista es la misma vista con el sentido cambiado: los limites no se tocan,
    // porque siempre estuvieron guardados en orden absoluto.
    public NavigableMap<K, V> descendingMap() {
        return new TmView<K, V>(this.m, this.fromStart, this.lo, this.loInc, this.toEnd, this.hi,
                this.hiInc, !this.desc);
    }

    public NavigableMap<K, V> reversed() {
        return this.descendingMap();
    }

    public NavigableSet<K> navigableKeySet() {
        return new TreeSet<K>((NavigableMap) this, true);
    }

    public NavigableSet<K> descendingKeySet() {
        return new TreeSet<K>((NavigableMap) this.descendingMap(), true);
    }
}

// Lo que un TreeMap y una de sus vistas tienen en comun para poder recorrerlos igual: el primer
// nodo, y el siguiente.
//
// Existe para que haya **un solo** iterador para los dos. Sin esto, `TreeSet` tendria que saber si
// esta apoyado en el mapa entero o en un corte -- y `TreeSet` esta escrito justamente para no
// tener que saberlo.
interface TmWalk<K, V> {

    TmNode<K, V> walkFirst();

    TmNode<K, V> walkNext(TmNode<K, V> n);
}

// El iterador de claves sobre cualquier TmWalk. Sin foto y sin memoria extra: va nodo a nodo por
// los enlaces del arbol.
final class TmKeyItr<K, V> implements Iterator<K> {

    private final TmWalk<K, V> walk;
    private TmNode<K, V> next;

    // El ultimo devuelto, para que `remove()` sepa sobre cual opera.
    private TmNode<K, V> last;
    private final Map<K, V> owner;

    TmKeyItr(TmWalk<K, V> walk, Map<K, V> owner) {
        this.walk = walk;
        this.owner = owner;
        this.next = walk.walkFirst();
    }

    public boolean hasNext() {
        return this.next != null;
    }

    public K next() {
        if (this.next == null) {
            throw new NoSuchElementException();
        }
        this.last = this.next;
        K key = this.next.key;
        // Se avanza **antes** de devolver, para que un `remove()` posterior no deje el cursor
        // apuntando a un nodo ya desenlazado.
        this.next = this.walk.walkNext(this.next);
        return key;
    }

    public void remove() {
        if (this.last == null) {
            throw new IllegalStateException();
        }
        this.owner.remove(this.last.key);
        this.last = null;
    }
}
