package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Map;

// Las vistas secuenciadas de LinkedHashMap: el mapa al reves, y sus tres colecciones (claves,
// valores, entradas) en cualquiera de los dos sentidos.
//
// **Vistas, no copias.** Es la decision que gobierna todo el archivo. `m.reversed()` devuelve algo
// que comparte los mismos objetos entrada que `m`: poner un par de un lado se ve del otro, y
// recorrer una mientras se modifica la otra es tan valido (o tan invalido) como hacerlo sobre el
// mapa directo. Una copia seria varias veces mas corta de escribir y estaria mal -- el contrato de
// `reversed()` dice "vista", y un metodo que devuelve una foto cuando promete un espejo miente en el
// unico punto en el que a alguien le importa.
//
// Que todo esto sea barato lo permite una sola cosa: la lista de orden de LinkedHashMap ya es
// **doblemente enlazada**. Recorrerla al reves cuesta lo mismo que al derecho, asi que invertir es
// elegir por cual de los dos punteros se avanza. De ahi el booleano `rev` que atraviesa el archivo:
// no hay dos implementaciones, hay una con un sentido de marcha.

/** El mapa visto al reves. */
final class LhmReversed<K, V> extends AbstractMap<K, V> implements SequencedMap<K, V> {

    private final LinkedHashMap<K, V> base;

    LhmReversed(LinkedHashMap<K, V> base) {
        this.base = base;
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

    // `put` sobre la vista invertida agrega al **principio del orden de la vista**, que es el final
    // del orden del mapa de atras. Es lo que hace que la vista sea consistente consigo misma: lo
    // ultimo agregado se itera primero, igual que en el mapa directo.
    public V put(K key, V value) {
        return this.base.put(key, value);
    }

    public V remove(Object key) {
        return this.base.remove(key);
    }

    public void clear() {
        this.base.clear();
    }

    /** Invertir lo invertido es el mapa de atras, no un tercer envoltorio. */
    public SequencedMap<K, V> reversed() {
        return this.base;
    }

    public V putFirst(K key, V value) {
        return this.base.putLast(key, value);
    }

    public V putLast(K key, V value) {
        return this.base.putFirst(key, value);
    }

    public Map.Entry<K, V> firstEntry() {
        return this.base.ultimaEntrada();
    }

    public Map.Entry<K, V> lastEntry() {
        return this.base.primeraEntrada();
    }

    public Map.Entry<K, V> pollFirstEntry() {
        Map.Entry<K, V> e = this.firstEntry();
        if (e != null) {
            this.base.remove(e.getKey());
        }
        return e;
    }

    public Map.Entry<K, V> pollLastEntry() {
        Map.Entry<K, V> e = this.lastEntry();
        if (e != null) {
            this.base.remove(e.getKey());
        }
        return e;
    }

    public SequencedSet<K> sequencedKeySet() {
        return new LhmKeySet<K, V>(this.base, true);
    }

    public SequencedCollection<V> sequencedValues() {
        return new LhmValues<K, V>(this.base, true);
    }

    public SequencedSet<Map.Entry<K, V>> sequencedEntrySet() {
        return new LhmEntrySet<K, V>(this.base, true);
    }
}

// El recorrido de la lista de orden en un sentido u otro. Es el unico lugar del archivo que sabe
// que la lista existe; todo lo demas se apoya en esto.
final class LhmWalk<K, V> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmWalk(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    LhmEntry<K, V> primera() {
        return this.rev ? this.base.ultimaEntrada() : this.base.primeraEntrada();
    }

    LhmEntry<K, V> siguiente(LhmEntry<K, V> e) {
        return this.rev ? this.base.beforeEntry(e) : this.base.afterEntry(e);
    }
}

// El iterador sobre las entradas, en el sentido que diga `rev`. Los tres iteradores de abajo lo
// envuelven y proyectan lo que cada uno necesita.
final class LhmEntryItr<K, V> implements Iterator<Map.Entry<K, V>> {

    private final LhmWalk<K, V> walk;
    private LhmEntry<K, V> proxima;
    private boolean arrancado = false;

    LhmEntryItr(LinkedHashMap<K, V> base, boolean rev) {
        this.walk = new LhmWalk<K, V>(base, rev);
    }

    private void arrancar() {
        if (!this.arrancado) {
            this.proxima = this.walk.primera();
            this.arrancado = true;
        }
    }

    public boolean hasNext() {
        this.arrancar();
        return this.proxima != null;
    }

    public Map.Entry<K, V> next() {
        this.arrancar();
        if (this.proxima == null) {
            throw new NoSuchElementException();
        }
        LhmEntry<K, V> e = this.proxima;
        this.proxima = this.walk.siguiente(e);
        return e;
    }
}

final class LhmKeyItr<K, V> implements Iterator<K> {

    private final LhmEntryItr<K, V> it;

    LhmKeyItr(LinkedHashMap<K, V> base, boolean rev) {
        this.it = new LhmEntryItr<K, V>(base, rev);
    }

    public boolean hasNext() {
        return this.it.hasNext();
    }

    public K next() {
        return this.it.next().getKey();
    }
}

final class LhmValueItr<K, V> implements Iterator<V> {

    private final LhmEntryItr<K, V> it;

    LhmValueItr(LinkedHashMap<K, V> base, boolean rev) {
        this.it = new LhmEntryItr<K, V>(base, rev);
    }

    public boolean hasNext() {
        return this.it.hasNext();
    }

    public V next() {
        return this.it.next().getValue();
    }
}

/** Las claves, en el orden del mapa o al reves. */
final class LhmKeySet<K, V> extends AbstractSet<K> implements SequencedSet<K> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmKeySet(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    public Iterator<K> iterator() {
        return new LhmKeyItr<K, V>(this.base, this.rev);
    }

    public int size() {
        return this.base.size();
    }

    public boolean contains(Object o) {
        return this.base.containsKey(o);
    }

    // Sacar una clave de la vista la saca del mapa: es lo que "vista" quiere decir.
    public boolean remove(Object o) {
        boolean estaba = this.base.containsKey(o);
        this.base.remove(o);
        return estaba;
    }

    public void clear() {
        this.base.clear();
    }

    public SequencedSet<K> reversed() {
        return new LhmKeySet<K, V>(this.base, !this.rev);
    }
}

/** Los valores, en el orden del mapa o al reves. */
final class LhmValues<K, V> extends AbstractCollection<V> implements SequencedCollection<V> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmValues(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    public Iterator<V> iterator() {
        return new LhmValueItr<K, V>(this.base, this.rev);
    }

    public int size() {
        return this.base.size();
    }

    public void clear() {
        this.base.clear();
    }

    public SequencedCollection<V> reversed() {
        return new LhmValues<K, V>(this.base, !this.rev);
    }
}

/** Las entradas, en el orden del mapa o al reves. */
final class LhmEntrySet<K, V> extends AbstractSet<Map.Entry<K, V>>
        implements SequencedSet<Map.Entry<K, V>> {

    private final LinkedHashMap<K, V> base;
    private final boolean rev;

    LhmEntrySet(LinkedHashMap<K, V> base, boolean rev) {
        this.base = base;
        this.rev = rev;
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return new LhmEntryItr<K, V>(this.base, this.rev);
    }

    public int size() {
        return this.base.size();
    }

    public void clear() {
        this.base.clear();
    }

    public SequencedSet<Map.Entry<K, V>> reversed() {
        return new LhmEntrySet<K, V>(this.base, !this.rev);
    }
}
