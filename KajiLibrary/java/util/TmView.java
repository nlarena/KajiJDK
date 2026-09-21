package java.util;

// A TreeMap's views: subMap, headMap, tailMap and descendingMap, all the same class.
//
// The JDK has three (`NavigableSubMap` and its two subclasses `Ascending`/`Descending`); here there
// is a single one, with a floor, a ceiling and a direction boolean. `TreeMap`'s five factories are
// five combinations of those fields:
//
//   headMap(to)        no floor, ceiling at `to`, ascending
//   tailMap(from)      floor at `from`, no ceiling, ascending
//   subMap(from, to)   both bounds,               ascending
//   descendingMap()    no bounds,                 descending
//   and any slice of a descending one, which combines the two
//
// **It is a view, not a copy**, and that is the whole point: `map.subMap(a, b).clear()` removes that
// range from the original map, and a `map.put(...)` inside the range is seen through the view. A
// copy would make the first line remove nothing, in silence.
//
// What a view does **not** do is let anyone write outside its range: a `put` of a key that does not
// fall between the bounds throws IllegalArgumentException. Without that, "the view of [a, b)" would
// be a lie.
//
// The descending direction is what saves half the code. Everything is computed first in **absolute**
// order -- the map behind's -- with the six `abs*` methods, and only at the end translated: for a
// reversed view, "the first" is the largest and `lower(k)` is `absHigher(k)`. Writing the two
// directions separately would mean duplicating fourteen methods to change their sign.
//
// A cost to keep in mind: `size()` **counts**, O(n). The JDK does the same with its submaps, and for
// the same reason: the tree keeps no count of how many nodes are in a range, and maintaining one
// would cost more than the occasional count.
class TmView<K, V> extends AbstractMap<K, V> implements NavigableMap<K, V>, TmWalk<K, V> {

    private final TreeMap<K, V> m;

    // The bounds, in **absolute** order: `lo` is always the small one even if the view is
    // descending. `fromStart`/`toEnd` say that side has no bound -- and null cannot be used for
    // that, because null can be a key.
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

    // --- the range ---

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

    // It filters a node by the range. Every search below ends up going through here, which is what
    // makes it impossible for a view to see even one node too many.
    private TmNode<K, V> clip(TmNode<K, V> p) {
        if (p == null || !this.inRange(p.key)) {
            return null;
        }
        return p;
    }

    // --- the six searches in absolute order ---

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

    // Mind these four: when the key asked for falls **outside** the range, the answer is not null
    // but the range's end. Asking for the "ceiling" of something below the floor has to give the
    // view's first element, not nothing.
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

    // --- the translation into the view's direction ---

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

    // --- TmWalk: the traversal, in the view's direction and without leaving the range ---

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

    // It counts, O(n). It is the price of the tree keeping no count per range.
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

    // Writing outside the range is an error, not a silent no-op: the view promised to be exactly
    // that range.
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
        // The keys are taken first and removed afterwards: removing while walking the tree leaves
        // the current node with no links, and the traversal is lost.
        ArrayList<K> keyList = new ArrayList<K>();
        TmNode<K, V> p = this.walkFirst();
        while (p != null) {
            keyList.add(p.key);
            p = this.walkNext(p);
        }
        int i = 0;
        while (i < keyList.size()) {
            this.m.remove(keyList.get(i));
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
            out.add(new ViewEntry<K, V>(p.key, p.value));
            p = this.walkNext(p);
        }
        return out;
    }

    // --- SortedMap ---

    // A descending view's comparator is the map's reversed, and it has to be: whoever receives this
    // map and wants to sort something the same way needs the order the view shows, not the map
    // behind's.
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

    // --- NavigableMap: the neighbours ---

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

    // --- NavigableMap: slices of a slice ---
    //
    // The new bounds are asked for in the **view's** order, and they have to be stored in absolute
    // order: over a descending view, the caller's "from" is the ceiling.

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
            // "up to `to` exclusive", reversed, is "from `to` upwards" in absolute terms.
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

    // Reversing a view is the same view with the direction changed: the bounds are not touched,
    // because they were always stored in absolute order.
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

// What a TreeMap and one of its views have in common so they can be walked the same way: the first
// node, and the next one.
//
// It exists so there is **one** iterator for both. Without it, `TreeSet` would have to know whether
// it is resting on the whole map or on a slice -- and `TreeSet` is written precisely so it does not
// have to know.
interface TmWalk<K, V> {

    TmNode<K, V> walkFirst();

    TmNode<K, V> walkNext(TmNode<K, V> n);
}

// The key iterator over any TmWalk. With no snapshot and no extra memory: it goes node by node
// along the tree's links.
final class TmKeyItr<K, V> implements Iterator<K> {

    private final TmWalk<K, V> walk;
    private TmNode<K, V> next;

    // The last one returned, so `remove()` knows which one it operates on.
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
        // It advances **before** returning, so that a later `remove()` does not leave the cursor
        // pointing at an already unlinked node.
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
