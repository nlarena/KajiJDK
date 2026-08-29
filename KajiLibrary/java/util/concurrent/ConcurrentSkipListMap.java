package java.util.concurrent;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SequencedMap;
import java.util.SequencedSet;
import java.util.Set;
import java.util.SortedSet;


/**
 * A sorted map, safe for concurrent use, built on a SKIP LIST.
 *
 * <p>A skip list is a sorted linked list with express lanes. The bottom lane holds every entry in
 * key order; each lane above holds a random subset of the one below, so a search starts at the top,
 * runs right while the next key is still smaller than the target, then drops a lane and repeats.
 * Halving the population per lane makes that a binary search over a linked structure — O(log n)
 * expected — without any of the rotations a balanced tree needs to stay in shape.
 *
 * <p>The randomness is the point, not a shortcut. A tree earns its balance by rewriting itself on
 * insert; a skip list buys the same shape in expectation by flipping a coin per node, which is why
 * it is the structure of choice when several threads touch the map at once: there is no rebalancing
 * step for them to contend on.
 *
 * @implNote The JDK's version is LOCK-FREE — every link is moved with a compare-and-set, and no
 *           thread ever blocks another. This one takes the coarser road and marks its methods
 *           {@code synchronized}, which is the idiom the rest of this package already uses
 *           ({@code ConcurrentHashMap} does the same). The observable contract is the same: every
 *           operation is atomic, and iterators are weakly consistent. What differs is throughput
 *           under contention, not the answers.
 *
 *           <p>Synchronized <em>methods</em> and not synchronized <em>blocks</em>, deliberately: a
 *           block with an early {@code return} does not emit its {@code monitorexit} and leaks the
 *           monitor (finding #105), while a method's monitor is released by the VM on every exit
 *           path.
 *
 *           <p><strong>And, today, that atomicity is a claim about the SOURCE and not about what
 *           runs.</strong> Our javac drops {@code ACC_SYNCHRONIZED} from a method's flags (finding
 *           #255), so every method below currently takes no monitor at all. Nothing here is wrong
 *           as Java — the class is correct against the JDK, where it validates at 341,506
 *           comparisons — but on our own VM it is a single-threaded map until #255 lands. The
 *           newer {@link SubmissionPublisher} works around it with a {@code sync} object and
 *           single-exit blocks; this class is left faithful, because rewriting fifty methods to
 *           dodge a compiler bug buys nothing until something actually runs two threads at it.
 *
 * @implNote A KajiLibrary subset. The JDK also exposes the spliterators, {@code clone}, and the
 *           bulk {@code compute}/{@code merge} family; those are omitted rather than declared and
 *           left throwing, so a caller that needs them gets a compile error instead of a surprise.
 */
public class ConcurrentSkipListMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentNavigableMap<K, V> {

    // The comparator, or null to compare keys by their own natural ordering.
    private final Comparator<? super K> keyComparator;

    // Lane 0 is the full list; lane i>0 is the express lane above it. The head is a sentinel that
    // holds no entry — its forward pointers are the entry points of every lane.
    private final SkipNode<K, V> head;

    private int count;

    // State of the coin. A xorshift is enough here: level promotion only needs the *proportion* of
    // heads to be right, never unpredictability, so seeding it from a constant keeps runs
    // reproducible — which matters when a failure has to be re-created.
    private int randomState;

    /** The tallest lane the list may grow. 32 lanes index more entries than an int can count. */
    private static int maxLevel() {
        return 32;
    }

    /** Creates an empty map ordered by the natural ordering of its keys. */
    public ConcurrentSkipListMap() {
        this.keyComparator = null;
        this.head = new SkipNode<K, V>(null, null, ConcurrentSkipListMap.maxLevel());
        this.count = 0;
        this.randomState = 0x2545F491;
    }

    /**
     * Creates an empty map ordered by the given comparator.
     *
     * @param comparator the ordering, or {@code null} for the keys' natural ordering
     */
    public ConcurrentSkipListMap(Comparator<? super K> comparator) {
        this.keyComparator = comparator;
        this.head = new SkipNode<K, V>(null, null, ConcurrentSkipListMap.maxLevel());
        this.count = 0;
        this.randomState = 0x2545F491;
    }

    // ---- ordering ----

    // Compares by the comparator when there is one, and by the key's own ordering when there is
    // not. A null key has no place in a sorted map: there is nothing to compare it against.
    @SuppressWarnings("unchecked")
    private int compareKeys(Object a, Object b) {
        if (a == null || b == null) {
            throw new NullPointerException();
        }
        if (this.keyComparator != null) {
            return this.keyComparator.compare((K) a, (K) b);
        }
        Comparable<Object> left = (Comparable<Object>) a;
        return left.compareTo(b);
    }

    // A coin whose bias is 1/2, taken one bit at a time: level 1 always, level 2 half the time,
    // and so on. The loop is capped so a run of heads cannot outgrow the head node.
    private int randomLevel() {
        int x = this.randomState;
        x = x ^ (x << 13);
        x = x ^ (x >>> 17);
        x = x ^ (x << 5);
        this.randomState = x;
        int level = 1;
        int bits = x;
        while ((bits & 1) == 1 && level < ConcurrentSkipListMap.maxLevel()) {
            level = level + 1;
            bits = bits >>> 1;
        }
        return level;
    }

    // ---- the search, which every operation is built on ----

    // The node at or before `key` on lane 0. Walking down from the top lane and right while the
    // next key is still smaller is the whole algorithm; everything else reads the result.
    private SkipNode<K, V> findPredecessor(Object key) {
        SkipNode<K, V> at = this.head;
        int lane = ConcurrentSkipListMap.maxLevel() - 1;
        while (lane >= 0) {
            SkipNode<K, V> ahead = at.forward(lane);
            while (ahead != null && this.compareKeys(ahead.key(), key) < 0) {
                at = ahead;
                ahead = at.forward(lane);
            }
            lane = lane - 1;
        }
        return at;
    }

    // The node holding exactly `key`, or null.
    private SkipNode<K, V> findNode(Object key) {
        SkipNode<K, V> before = this.findPredecessor(key);
        SkipNode<K, V> node = before.forward(0);
        if (node != null && this.compareKeys(node.key(), key) == 0) {
            return node;
        }
        return null;
    }

    /**
     * The first node at or after `key` on lane 0, or null when the key is past the end. The bound
     * checks of every view are phrased in terms of this.
     */
    final SkipNode<K, V> ceilingNode(Object key) {
        SkipNode<K, V> before = this.findPredecessor(key);
        return before.forward(0);
    }

    /** The first node of lane 0, or null when the map is empty. */
    final SkipNode<K, V> firstNode() {
        return this.head.forward(0);
    }

    /** The node after `node` on lane 0, or null at the end. */
    final SkipNode<K, V> nextNode(SkipNode<K, V> node) {
        return node.forward(0);
    }

    /** The last node of lane 0, or null when the map is empty. Walks the express lanes down. */
    final SkipNode<K, V> lastNode() {
        SkipNode<K, V> at = this.head;
        int lane = ConcurrentSkipListMap.maxLevel() - 1;
        while (lane >= 0) {
            SkipNode<K, V> ahead = at.forward(lane);
            while (ahead != null) {
                at = ahead;
                ahead = at.forward(lane);
            }
            lane = lane - 1;
        }
        if (at == this.head) {
            return null;
        }
        return at;
    }

    /** The last node strictly before `key`, or null. */
    final SkipNode<K, V> lowerNode(Object key) {
        SkipNode<K, V> before = this.findPredecessor(key);
        if (before == this.head) {
            return null;
        }
        return before;
    }

    /** Exposed to the views, which must compare against the same ordering the map uses. */
    final int compareForView(Object a, Object b) {
        return this.compareKeys(a, b);
    }

    // ---- reads ----

    @Override
    public synchronized int size() {
        return this.count;
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.count == 0;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return this.findNode(key) != null;
    }

    @Override
    public synchronized V get(Object key) {
        SkipNode<K, V> node = this.findNode(key);
        if (node == null) {
            return null;
        }
        return node.value();
    }

    /**
     * The value for `key`, or `fallback` when there is none.
     *
     * @param key the key to look up
     * @param fallback what to return when the key is absent
     * @return the mapped value, or {@code fallback}
     */
    public synchronized V getOrDefault(Object key, V fallback) {
        SkipNode<K, V> node = this.findNode(key);
        if (node == null) {
            return fallback;
        }
        return node.value();
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        SkipNode<K, V> at = this.head.forward(0);
        while (at != null) {
            V held = at.value();
            if (value == null) {
                if (held == null) {
                    return true;
                }
            } else if (value.equals(held)) {
                return true;
            }
            at = at.forward(0);
        }
        return false;
    }

    // ---- writes ----

    @Override
    public synchronized V put(K key, V value) {
        return this.insert(key, value, false);
    }

    @Override
    public synchronized V putIfAbsent(K key, V value) {
        return this.insert(key, value, true);
    }

    // The one insertion path. `onlyIfAbsent` is what separates put from putIfAbsent, and keeping
    // them one method is what makes putIfAbsent atomic rather than a racy get-then-put.
    private V insert(K key, V value, boolean onlyIfAbsent) {
        Object[] behind = new Object[ConcurrentSkipListMap.maxLevel()];
        SkipNode<K, V> at = this.head;
        int lane = ConcurrentSkipListMap.maxLevel() - 1;
        while (lane >= 0) {
            SkipNode<K, V> ahead = at.forward(lane);
            while (ahead != null && this.compareKeys(ahead.key(), key) < 0) {
                at = ahead;
                ahead = at.forward(lane);
            }
            behind[lane] = at;
            lane = lane - 1;
        }
        SkipNode<K, V> found = at.forward(0);
        if (found != null && this.compareKeys(found.key(), key) == 0) {
            V previous = found.value();
            if (!onlyIfAbsent || previous == null) {
                found.setValue(value);
            }
            return previous;
        }
        int level = this.randomLevel();
        SkipNode<K, V> fresh = new SkipNode<K, V>(key, value, level);
        int i = 0;
        while (i < level) {
            SkipNode<K, V> previous = SkipNode.cast(behind[i]);
            fresh.setForward(i, previous.forward(i));
            previous.setForward(i, fresh);
            i = i + 1;
        }
        this.count = this.count + 1;
        return null;
    }

    @Override
    public synchronized V remove(Object key) {
        SkipNode<K, V> gone = this.unlink(key, null, false);
        if (gone == null) {
            return null;
        }
        return gone.value();
    }

    /**
     * Removes the entry for `key` only when it currently maps to `value`.
     *
     * @param key the key to remove
     * @param value the value it must currently hold
     * @return {@code true} if an entry was removed
     */
    @Override
    public synchronized boolean remove(Object key, Object value) {
        return this.unlink(key, value, true) != null;
    }

    // The one removal path. `matchValue` makes the two-argument remove atomic for the same reason
    // insert carries `onlyIfAbsent`.
    private SkipNode<K, V> unlink(Object key, Object value, boolean matchValue) {
        Object[] behind = new Object[ConcurrentSkipListMap.maxLevel()];
        SkipNode<K, V> at = this.head;
        int lane = ConcurrentSkipListMap.maxLevel() - 1;
        while (lane >= 0) {
            SkipNode<K, V> ahead = at.forward(lane);
            while (ahead != null && this.compareKeys(ahead.key(), key) < 0) {
                at = ahead;
                ahead = at.forward(lane);
            }
            behind[lane] = at;
            lane = lane - 1;
        }
        SkipNode<K, V> target = at.forward(0);
        if (target == null || this.compareKeys(target.key(), key) != 0) {
            return null;
        }
        if (matchValue) {
            V held = target.value();
            if (value == null) {
                if (held != null) {
                    return null;
                }
            } else if (!value.equals(held)) {
                return null;
            }
        }
        int i = 0;
        while (i < ConcurrentSkipListMap.maxLevel()) {
            SkipNode<K, V> previous = SkipNode.cast(behind[i]);
            if (previous.forward(i) == target) {
                previous.setForward(i, target.forward(i));
            }
            i = i + 1;
        }
        this.count = this.count - 1;
        return target;
    }

    /**
     * Replaces the value for `key` only when it currently holds `oldValue`.
     *
     * @param key the key whose value to replace
     * @param oldValue the value it must currently hold
     * @param newValue the value to store
     * @return {@code true} if the value was replaced
     */
    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        SkipNode<K, V> node = this.findNode(key);
        if (node == null) {
            return false;
        }
        V held = node.value();
        // `wanted` and not `oldValue` directly: a call whose receiver is typed as a TYPE VARIABLE
        // is dropped in silence and the `!` becomes an int branch over the leftover reference
        // (finding #252), which produces a method the interpreter cannot run. Holding the same
        // reference in an Object local is enough, and costs nothing -- V erases to Object anyway.
        Object wanted = oldValue;
        if (wanted == null) {
            if (held != null) {
                return false;
            }
        } else if (!wanted.equals(held)) {
            return false;
        }
        node.setValue(newValue);
        return true;
    }

    /**
     * Replaces the value for `key`, but only if the key is already present.
     *
     * @param key the key whose value to replace
     * @param value the value to store
     * @return the previous value, or {@code null} if the key was absent
     */
    @Override
    public synchronized V replace(K key, V value) {
        SkipNode<K, V> node = this.findNode(key);
        if (node == null) {
            return null;
        }
        V previous = node.value();
        node.setValue(value);
        return previous;
    }

    @Override
    public synchronized void clear() {
        int i = 0;
        while (i < ConcurrentSkipListMap.maxLevel()) {
            this.head.setForward(i, null);
            i = i + 1;
        }
        this.count = 0;
    }

    // ---- sorted-map queries ----

    @Override
    public Comparator<? super K> comparator() {
        return this.keyComparator;
    }

    @Override
    public synchronized K firstKey() {
        SkipNode<K, V> node = this.firstNode();
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.key();
    }

    @Override
    public synchronized K lastKey() {
        SkipNode<K, V> node = this.lastNode();
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.key();
    }

    @Override
    public synchronized Map.Entry<K, V> firstEntry() {
        return SkipEntry.snapshot(this.firstNode());
    }

    @Override
    public synchronized Map.Entry<K, V> lastEntry() {
        return SkipEntry.snapshot(this.lastNode());
    }

    @Override
    public synchronized Map.Entry<K, V> pollFirstEntry() {
        SkipNode<K, V> node = this.firstNode();
        if (node == null) {
            return null;
        }
        Map.Entry<K, V> taken = SkipEntry.snapshot(node);
        this.unlink(node.key(), null, false);
        return taken;
    }

    @Override
    public synchronized Map.Entry<K, V> pollLastEntry() {
        SkipNode<K, V> node = this.lastNode();
        if (node == null) {
            return null;
        }
        Map.Entry<K, V> taken = SkipEntry.snapshot(node);
        this.unlink(node.key(), null, false);
        return taken;
    }

    @Override
    public synchronized Map.Entry<K, V> lowerEntry(K key) {
        return SkipEntry.snapshot(this.lowerNode(key));
    }

    @Override
    public synchronized K lowerKey(K key) {
        return SkipNode.keyOrNull(this.lowerNode(key));
    }

    @Override
    public synchronized Map.Entry<K, V> floorEntry(K key) {
        return SkipEntry.snapshot(this.floorNode(key));
    }

    @Override
    public synchronized K floorKey(K key) {
        return SkipNode.keyOrNull(this.floorNode(key));
    }

    @Override
    public synchronized Map.Entry<K, V> ceilingEntry(K key) {
        return SkipEntry.snapshot(this.ceilingNode(key));
    }

    @Override
    public synchronized K ceilingKey(K key) {
        return SkipNode.keyOrNull(this.ceilingNode(key));
    }

    @Override
    public synchronized Map.Entry<K, V> higherEntry(K key) {
        return SkipEntry.snapshot(this.higherNode(key));
    }

    @Override
    public synchronized K higherKey(K key) {
        return SkipNode.keyOrNull(this.higherNode(key));
    }

    // floor = the key itself when present, otherwise the one below it.
    private SkipNode<K, V> floorNode(Object key) {
        SkipNode<K, V> exact = this.findNode(key);
        if (exact != null) {
            return exact;
        }
        return this.lowerNode(key);
    }

    // higher = strictly above, so an exact hit is stepped past.
    private SkipNode<K, V> higherNode(Object key) {
        SkipNode<K, V> at = this.ceilingNode(key);
        if (at != null && this.compareKeys(at.key(), key) == 0) {
            return at.forward(0);
        }
        return at;
    }

    // ---- views ----

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new SkipEntrySet<K, V>(this.wholeView(false));
    }

    @Override
    public NavigableSet<K> keySet() {
        return new SkipKeySet<K, V>(this.wholeView(false));
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new SkipKeySet<K, V>(this.wholeView(false));
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return new SkipKeySet<K, V>(this.wholeView(true));
    }

    /**
     * A collection view of the values, in key order.
     *
     * @return the values
     */
    // An unbounded view of the whole map, so the map own views and a submap views share one
    // code path. Two paths would mean two chances to get the bounds wrong, and one of them
    // already was.
    private SkipSubMap<K, V> wholeView(boolean descending) {
        return new SkipSubMap<K, V>(this, null, false, null, false, descending);
    }

    public Collection<V> values() {
        return new SkipValues<K, V>(this.wholeView(false));
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        return new SkipSubMap<K, V>(this, null, false, null, false, true);
    }

    @Override
    public SequencedMap<K, V> reversed() {
        return this.descendingMap();
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K from, boolean fromInclusive,
                                               K to, boolean toInclusive) {
        return new SkipSubMap<K, V>(this, from, fromInclusive, to, toInclusive, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K to, boolean inclusive) {
        return new SkipSubMap<K, V>(this, null, false, to, inclusive, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K from, boolean inclusive) {
        return new SkipSubMap<K, V>(this, from, inclusive, null, false, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K from, K to) {
        // Construido directo y no delegando a subMap(K,boolean,K,boolean): esa llamada resuelve
        // a la declaracion de NavigableMap, cuyo retorno es NavigableMap y no el nuestro.
        return new SkipSubMap<K, V>(this, from, true, to, false, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K to) {
        return new SkipSubMap<K, V>(this, null, false, to, false, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K from) {
        return new SkipSubMap<K, V>(this, from, true, null, false, false);
    }
}

/**
 * One entry, plus its forward pointer per lane.
 *
 * <p>The pointers are an {@code Object[]} and not a {@code SkipNode[]}: a generic array cannot be
 * created in Java, and the alternative — a raw array plus an unchecked cast — buys nothing here,
 * because every read already goes through {@link #forward} and is checked in one place.
 */
final class SkipNode<K, V> {

    private final K entryKey;
    private volatile V entryValue;
    private final Object[] lanes;

    SkipNode(K key, V value, int level) {
        this.entryKey = key;
        this.entryValue = value;
        this.lanes = new Object[level];
    }

    K key() {
        return this.entryKey;
    }

    V value() {
        return this.entryValue;
    }

    void setValue(V value) {
        this.entryValue = value;
    }

    /** The next node on `lane`, or null — including when this node is shorter than the lane. */
    @SuppressWarnings("unchecked")
    SkipNode<K, V> forward(int lane) {
        if (lane >= this.lanes.length) {
            return null;
        }
        return (SkipNode<K, V>) this.lanes[lane];
    }

    void setForward(int lane, SkipNode<K, V> node) {
        if (lane < this.lanes.length) {
            this.lanes[lane] = node;
        }
    }

    @SuppressWarnings("unchecked")
    static <K, V> SkipNode<K, V> cast(Object o) {
        return (SkipNode<K, V>) o;
    }

    static <K, V> K keyOrNull(SkipNode<K, V> node) {
        if (node == null) {
            return null;
        }
        return node.key();
    }
}

/**
 * An immutable snapshot of one entry.
 *
 * <p>A snapshot and not a live view, which is what the JDK returns too: an entry handed out by a
 * concurrent map cannot keep pointing at a node another thread may already have unlinked. That is
 * also why {@code setValue} refuses — writing through a detached entry would silently do nothing.
 */
final class SkipEntry<K, V> implements Map.Entry<K, V> {

    private final K entryKey;
    private final V entryValue;

    private SkipEntry(K key, V value) {
        this.entryKey = key;
        this.entryValue = value;
    }

    static <K, V> Map.Entry<K, V> snapshot(SkipNode<K, V> node) {
        if (node == null) {
            return null;
        }
        return new SkipEntry<K, V>(node.key(), node.value());
    }

    public K getKey() {
        return this.entryKey;
    }

    public V getValue() {
        return this.entryValue;
    }

    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
}

/**
 * Walks a view NODES, handing out snapshots.
 *
 * <p>It iterates an {@code Object[]} taken from the view rather than following lane 0 directly, for
 * two reasons: lane 0 runs one way only, so a descending view could not be walked at all; and a
 * bounded view has to stop at its bounds, which the lane knows nothing about. The array is read
 * once per iterator, which is what "weakly consistent" means here -- it will not throw if the map
 * changes underneath, and it may or may not show that change.
 */
final class SkipEntryIterator<K, V> implements Iterator<Map.Entry<K, V>> {

    private final Object[] nodes;
    private int cursor;

    SkipEntryIterator(SkipSubMap<K, V> view) {
        this.nodes = view.nodesInView();
        this.cursor = 0;
    }

    @Override
    public boolean hasNext() {
        return this.cursor < this.nodes.length;
    }

    @Override
    public Map.Entry<K, V> next() {
        if (this.cursor >= this.nodes.length) {
            throw new NoSuchElementException();
        }
        SkipNode<K, V> node = SkipNode.cast(this.nodes[this.cursor]);
        this.cursor = this.cursor + 1;
        return SkipEntry.snapshot(node);
    }
}

/** Walks a view NODES, handing out keys. */
final class SkipKeyIterator<K, V> implements Iterator<K> {

    private final Object[] nodes;
    private int cursor;

    SkipKeyIterator(SkipSubMap<K, V> view) {
        this.nodes = view.nodesInView();
        this.cursor = 0;
    }

    @Override
    public boolean hasNext() {
        return this.cursor < this.nodes.length;
    }

    @Override
    public K next() {
        if (this.cursor >= this.nodes.length) {
            throw new NoSuchElementException();
        }
        SkipNode<K, V> node = SkipNode.cast(this.nodes[this.cursor]);
        this.cursor = this.cursor + 1;
        return node.key();
    }
}

/** Walks a view NODES, handing out values. */
final class SkipValueIterator<K, V> implements Iterator<V> {

    private final Object[] nodes;
    private int cursor;

    SkipValueIterator(SkipSubMap<K, V> view) {
        this.nodes = view.nodesInView();
        this.cursor = 0;
    }

    @Override
    public boolean hasNext() {
        return this.cursor < this.nodes.length;
    }

    @Override
    public V next() {
        if (this.cursor >= this.nodes.length) {
            throw new NoSuchElementException();
        }
        SkipNode<K, V> node = SkipNode.cast(this.nodes[this.cursor]);
        this.cursor = this.cursor + 1;
        return node.value();
    }
}

final class SkipSubMap<K, V> implements ConcurrentNavigableMap<K, V> {

    private final ConcurrentSkipListMap<K, V> base;
    private final K low;
    private final boolean lowInclusive;
    private final K high;
    private final boolean highInclusive;
    private final boolean descending;

    SkipSubMap(ConcurrentSkipListMap<K, V> base, K low, boolean lowInclusive,
               K high, boolean highInclusive, boolean descending) {
        this.base = base;
        this.low = low;
        this.lowInclusive = lowInclusive;
        this.high = high;
        this.highInclusive = highInclusive;
        this.descending = descending;
    }

    // Whether `key` falls inside the two bounds. A null bound means open on that side.
    private boolean inRange(Object key) {
        if (this.low != null) {
            int c = this.base.compareForView(key, this.low);
            if (c < 0 || (c == 0 && !this.lowInclusive)) {
                return false;
            }
        }
        if (this.high != null) {
            int c = this.base.compareForView(key, this.high);
            if (c > 0 || (c == 0 && !this.highInclusive)) {
                return false;
            }
        }
        return true;
    }

    // The first in-range node in the BASE map's order, whatever `descending` says.
    private SkipNode<K, V> lowestNode() {
        SkipNode<K, V> at;
        if (this.low == null) {
            at = this.base.firstNode();
        } else {
            at = this.base.ceilingNode(this.low);
            if (at != null && !this.lowInclusive
                    && this.base.compareForView(at.key(), this.low) == 0) {
                at = this.base.nextNode(at);
            }
        }
        if (at == null || !this.inRange(at.key())) {
            return null;
        }
        return at;
    }

    // The last in-range node in the BASE map's order.
    private SkipNode<K, V> highestNode() {
        SkipNode<K, V> best = null;
        SkipNode<K, V> at = this.lowestNode();
        while (at != null && this.inRange(at.key())) {
            best = at;
            at = this.base.nextNode(at);
        }
        return best;
    }

    // "First" as this view sees it, which is the highest node when reversed.
    private SkipNode<K, V> firstInView() {
        if (this.descending) {
            return this.highestNode();
        }
        return this.lowestNode();
    }

    private SkipNode<K, V> lastInView() {
        if (this.descending) {
            return this.lowestNode();
        }
        return this.highestNode();
    }


    /**
     * The in-range nodes, in this view's order. Re-read on every call, which is what keeps a view
     * live: a snapshot taken once would stop reflecting the base map the moment it changed.
     */
    Object[] nodesInView() {
        int n = this.size();
        Object[] out = new Object[n];
        SkipNode<K, V> at = this.lowestNode();
        int i = 0;
        while (at != null && this.inRange(at.key()) && i < n) {
            if (this.descending) {
                out[n - 1 - i] = at;
            } else {
                out[i] = at;
            }
            at = this.base.nextNode(at);
            i = i + 1;
        }
        return out;
    }


    /** The same bounds, walked the other way. */
    SkipSubMap<K, V> flipped() {
        return new SkipSubMap<K, V>(this.base, this.low, this.lowInclusive,
                this.high, this.highInclusive, !this.descending);
    }

    /** A narrower range inside this one, keeping the direction. */
    SkipSubMap<K, V> narrow(K from, boolean fromInclusive, K to, boolean toInclusive) {
        return new SkipSubMap<K, V>(this.base, from, fromInclusive, to, toInclusive,
                this.descending);
    }

    /** This range, cut off above at `to`. */
    SkipSubMap<K, V> narrowHead(K to, boolean inclusive) {
        return new SkipSubMap<K, V>(this.base, this.low, this.lowInclusive, to, inclusive,
                this.descending);
    }

    /** This range, cut off below at `from`. */
    SkipSubMap<K, V> narrowTail(K from, boolean inclusive) {
        return new SkipSubMap<K, V>(this.base, from, inclusive, this.high, this.highInclusive,
                this.descending);
    }

    /** The map these bounds are a view OF. */
    ConcurrentSkipListMap<K, V> baseMap() {
        return this.base;
    }

    // ---- Map ----

    @Override
    public int size() {
        int n = 0;
        SkipNode<K, V> at = this.lowestNode();
        while (at != null && this.inRange(at.key())) {
            n = n + 1;
            at = this.base.nextNode(at);
        }
        return n;
    }

    @Override
    public boolean isEmpty() {
        return this.lowestNode() == null;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.inRange(key) && this.base.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        SkipNode<K, V> at = this.lowestNode();
        while (at != null && this.inRange(at.key())) {
            V held = at.value();
            if (value == null) {
                if (held == null) {
                    return true;
                }
            } else if (value.equals(held)) {
                return true;
            }
            at = this.base.nextNode(at);
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (!this.inRange(key)) {
            return null;
        }
        return this.base.get(key);
    }

    // `keySet()` ya lo tiene esta vista mas abajo, con retorno covariante `NavigableSet<K>`, que
    // satisface el `Set<K>` de `Map` — no hace falta agregar nada para el finding #205.

    // Writing outside the bounds is refused rather than silently redirected: a view that accepted a
    // key it will never show back would be lying about what it contains.
    @Override
    public V put(K key, V value) {
        if (!this.inRange(key)) {
            throw new IllegalArgumentException("key out of range");
        }
        return this.base.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if (!this.inRange(key)) {
            return null;
        }
        return this.base.remove(key);
    }

    /**
     * Copies every mapping of {@code m} in, refusing any key outside this view.
     *
     * <p>Refusing rather than silently dropping: a caller who puts a key into a submap and finds
     * it missing afterwards has no way to tell that from a bug.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // Walked by key and not by entrySet, which this library Map does not have.
        Map<K, V> other = (Map<K, V>) m;
        Set<K> keys = other.keySet();
        Iterator<K> it = keys.iterator();
        while (it.hasNext()) {
            K key = it.next();
            this.put(key, other.get(key));
        }
    }

    @Override
    public void clear() {
        SkipNode<K, V> at = this.lowestNode();
        while (at != null && this.inRange(at.key())) {
            K key = at.key();
            at = this.base.nextNode(at);
            this.base.remove(key);
        }
    }

    // ---- ConcurrentMap ----

    @Override
    public V putIfAbsent(K key, V value) {
        if (!this.inRange(key)) {
            throw new IllegalArgumentException("key out of range");
        }
        return this.base.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return this.inRange(key) && this.base.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return this.inRange(key) && this.base.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        if (!this.inRange(key)) {
            return null;
        }
        return this.base.replace(key, value);
    }

    // ---- SortedMap ----

    @Override
    public Comparator<? super K> comparator() {
        return this.base.comparator();
    }

    @Override
    public K firstKey() {
        SkipNode<K, V> node = this.firstInView();
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.key();
    }

    @Override
    public K lastKey() {
        SkipNode<K, V> node = this.lastInView();
        if (node == null) {
            throw new NoSuchElementException();
        }
        return node.key();
    }

    // ---- SequencedMap ----

    @Override
    public Map.Entry<K, V> firstEntry() {
        return SkipEntry.snapshot(this.firstInView());
    }

    @Override
    public Map.Entry<K, V> lastEntry() {
        return SkipEntry.snapshot(this.lastInView());
    }

    @Override
    public Map.Entry<K, V> pollFirstEntry() {
        SkipNode<K, V> node = this.firstInView();
        if (node == null) {
            return null;
        }
        Map.Entry<K, V> taken = SkipEntry.snapshot(node);
        this.base.remove(node.key());
        return taken;
    }

    @Override
    public Map.Entry<K, V> pollLastEntry() {
        SkipNode<K, V> node = this.lastInView();
        if (node == null) {
            return null;
        }
        Map.Entry<K, V> taken = SkipEntry.snapshot(node);
        this.base.remove(node.key());
        return taken;
    }

    @Override
    public SequencedMap<K, V> reversed() {
        return this.descendingMap();
    }

    // ---- NavigableMap ----
    //
    // Reversed, the view's "lower" is the base map's "higher". The caller asks in the view's order,
    // so the QUERY is flipped rather than the answer.

    @Override
    public Map.Entry<K, V> lowerEntry(K key) {
        return SkipEntry.snapshot(this.neighbour(key, false, false));
    }

    @Override
    public K lowerKey(K key) {
        return SkipNode.keyOrNull(this.neighbour(key, false, false));
    }

    @Override
    public Map.Entry<K, V> floorEntry(K key) {
        return SkipEntry.snapshot(this.neighbour(key, false, true));
    }

    @Override
    public K floorKey(K key) {
        return SkipNode.keyOrNull(this.neighbour(key, false, true));
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K key) {
        return SkipEntry.snapshot(this.neighbour(key, true, true));
    }

    @Override
    public K ceilingKey(K key) {
        return SkipNode.keyOrNull(this.neighbour(key, true, true));
    }

    @Override
    public Map.Entry<K, V> higherEntry(K key) {
        return SkipEntry.snapshot(this.neighbour(key, true, false));
    }

    @Override
    public K higherKey(K key) {
        return SkipNode.keyOrNull(this.neighbour(key, true, false));
    }

    // One scan answers all eight: `after` is the direction IN THIS VIEW, `orEqual` whether an exact
    // hit counts. Scanning forward, the first match wins; scanning backward, the last one does.
    private SkipNode<K, V> neighbour(K key, boolean after, boolean orEqual) {
        boolean forward = after != this.descending;
        SkipNode<K, V> best = null;
        SkipNode<K, V> at = this.lowestNode();
        while (at != null && this.inRange(at.key())) {
            int c = this.base.compareForView(at.key(), key);
            boolean ok;
            if (c == 0) {
                ok = orEqual;
            } else if (forward) {
                ok = c > 0;
            } else {
                ok = c < 0;
            }
            if (ok) {
                if (forward) {
                    return at;
                }
                best = at;
            }
            at = this.base.nextNode(at);
        }
        return best;
    }

    // ---- views of the view ----

    @Override
    public NavigableSet<K> keySet() {
        return new SkipKeySet<K, V>(this);
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return new SkipKeySet<K, V>(this);
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        SkipSubMap<K, V> flipped = new SkipSubMap<K, V>(this.base, this.low, this.lowInclusive,
                this.high, this.highInclusive, !this.descending);
        return new SkipKeySet<K, V>(flipped);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new SkipEntrySet<K, V>(this);
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        return new SkipSubMap<K, V>(this.base, this.low, this.lowInclusive,
                this.high, this.highInclusive, !this.descending);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K from, boolean fromInclusive,
                                               K to, boolean toInclusive) {
        return new SkipSubMap<K, V>(this.base, from, fromInclusive, to, toInclusive,
                this.descending);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K to, boolean inclusive) {
        return new SkipSubMap<K, V>(this.base, this.low, this.lowInclusive, to, inclusive,
                this.descending);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K from, boolean inclusive) {
        return new SkipSubMap<K, V>(this.base, from, inclusive, this.high, this.highInclusive,
                this.descending);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K from, K to) {
        return new SkipSubMap<K, V>(this.base, from, true, to, false, this.descending);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K to) {
        return new SkipSubMap<K, V>(this.base, this.low, this.lowInclusive, to, false,
                this.descending);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K from) {
        return new SkipSubMap<K, V>(this.base, from, true, this.high, this.highInclusive,
                this.descending);
    }
}

/**
 * A live {@link NavigableSet} view of a {@link ConcurrentSkipListMap}'s keys.
 *
 * <p>Removing a key here removes the mapping from the map, which is the point of a key SET rather
 * than a key list: it is the same keys, seen through a different interface. Adding is refused —
 * there is no value to pair a new key with, and inventing {@code null} would put an entry in the
 * map that the caller never asked for.
 */
/**
 * A live {@link NavigableSet} view of the keys of a {@link SkipSubMap}.
 *
 * <p>It wraps the VIEW and not the base map, which is the whole correctness question here: the key
 * set of a submap has to stop at the submap bounds. Wrapping the base map instead produces a set
 * that silently reports every key in the map -- a real bug this class was rewritten to fix, caught
 * by comparing 2000 random bounded ranges against the JDK.
 *
 * <p>Removing a key removes the mapping from the underlying map: it is the same keys seen through a
 * different interface. Adding is refused -- there is no value to pair a new key with, and inventing
 * {@code null} would put an entry in the map that the caller never asked for.
 */
final class SkipKeySet<K, V> implements NavigableSet<K> {

    private final SkipSubMap<K, V> view;

    SkipKeySet(SkipSubMap<K, V> view) {
        this.view = view;
    }

    @Override
    public int size() {
        return this.view.size();
    }

    @Override
    public boolean isEmpty() {
        return this.view.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.view.containsKey(o);
    }

    @Override
    public boolean add(K e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        if (!this.view.containsKey(o)) {
            return false;
        }
        this.view.remove(o);
        return true;
    }

    @Override
    public void clear() {
        this.view.clear();
    }

    @Override
    public Iterator<K> iterator() {
        return new SkipKeyIterator<K, V>(this.view);
    }

    @Override
    public Iterator<K> descendingIterator() {
        return new SkipKeyIterator<K, V>(this.view.flipped());
    }

    @Override
    public NavigableSet<K> descendingSet() {
        return new SkipKeySet<K, V>(this.view.flipped());
    }

    @Override
    public SequencedSet<K> reversed() {
        return new SkipKeySet<K, V>(this.view.flipped());
    }

    @Override
    public Comparator<? super K> comparator() {
        return this.view.comparator();
    }

    @Override
    public K first() {
        return this.view.firstKey();
    }

    @Override
    public K last() {
        return this.view.lastKey();
    }

    // The view already answers these in its own order, so there is nothing to flip here.
    @Override
    public K lower(K e) {
        return this.view.lowerKey(e);
    }

    @Override
    public K floor(K e) {
        return this.view.floorKey(e);
    }

    @Override
    public K ceiling(K e) {
        return this.view.ceilingKey(e);
    }

    @Override
    public K higher(K e) {
        return this.view.higherKey(e);
    }

    @Override
    public K pollFirst() {
        Map.Entry<K, V> taken = this.view.pollFirstEntry();
        if (taken == null) {
            return null;
        }
        return taken.getKey();
    }

    @Override
    public K pollLast() {
        Map.Entry<K, V> taken = this.view.pollLastEntry();
        if (taken == null) {
            return null;
        }
        return taken.getKey();
    }

    @Override
    public NavigableSet<K> subSet(K from, boolean fromInclusive, K to, boolean toInclusive) {
        SkipSubMap<K, V> range = this.view.narrow(from, fromInclusive, to, toInclusive);
        return new SkipKeySet<K, V>(range);
    }

    @Override
    public NavigableSet<K> headSet(K to, boolean inclusive) {
        SkipSubMap<K, V> range = this.view.narrowHead(to, inclusive);
        return new SkipKeySet<K, V>(range);
    }

    @Override
    public NavigableSet<K> tailSet(K from, boolean inclusive) {
        SkipSubMap<K, V> range = this.view.narrowTail(from, inclusive);
        return new SkipKeySet<K, V>(range);
    }

    @Override
    public SortedSet<K> subSet(K from, K to) {
        SkipSubMap<K, V> range = this.view.narrow(from, true, to, false);
        return new SkipKeySet<K, V>(range);
    }

    @Override
    public SortedSet<K> headSet(K to) {
        SkipSubMap<K, V> range = this.view.narrowHead(to, false);
        return new SkipKeySet<K, V>(range);
    }

    @Override
    public SortedSet<K> tailSet(K from) {
        SkipSubMap<K, V> range = this.view.narrowTail(from, true);
        return new SkipKeySet<K, V>(range);
    }
}

/**
 * A live {@link Set} view of the entries of a {@link SkipSubMap}, in the view order.
 *
 * <p>The entries it hands out are immutable snapshots, so {@code contains} and {@code remove} are
 * answered by looking the key up rather than by comparing against a live node -- the node may
 * already be gone.
 */
final class SkipEntrySet<K, V> implements Set<Map.Entry<K, V>> {

    private final SkipSubMap<K, V> view;

    SkipEntrySet(SkipSubMap<K, V> view) {
        this.view = view;
    }

    @Override
    public int size() {
        return this.view.size();
    }

    @Override
    public boolean isEmpty() {
        return this.view.isEmpty();
    }

    // An entry belongs here only if the map holds that key AND that value: a matching key with a
    // different value is a different entry.
    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Map.Entry)) {
            return false;
        }
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        Object key = entry.getKey();
        if (!this.view.containsKey(key)) {
            return false;
        }
        V held = this.view.get(key);
        Object wanted = entry.getValue();
        if (wanted == null) {
            return held == null;
        }
        return wanted.equals(held);
    }

    @Override
    public boolean add(Map.Entry<K, V> e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        if (!this.contains(o)) {
            return false;
        }
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        this.view.remove(entry.getKey());
        return true;
    }

    @Override
    public void clear() {
        this.view.clear();
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new SkipEntryIterator<K, V>(this.view);
    }
}

/**
 * A live {@link Collection} view of the values of a {@link SkipSubMap}, in key order.
 *
 * <p>A collection and not a set, because two keys may map to the same value and both occurrences
 * belong here. {@code remove} therefore drops the FIRST key holding that value, in the view order,
 * which is what "remove one occurrence" has to mean when the values carry no identity of their own.
 */
final class SkipValues<K, V> implements Collection<V> {

    private final SkipSubMap<K, V> view;

    SkipValues(SkipSubMap<K, V> view) {
        this.view = view;
    }

    @Override
    public int size() {
        return this.view.size();
    }

    @Override
    public boolean isEmpty() {
        return this.view.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.view.containsValue(o);
    }

    @Override
    public boolean add(V e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        Object[] nodes = this.view.nodesInView();
        int i = 0;
        while (i < nodes.length) {
            SkipNode<K, V> node = SkipNode.cast(nodes[i]);
            V held = node.value();
            boolean hit;
            if (o == null) {
                hit = held == null;
            } else {
                hit = o.equals(held);
            }
            if (hit) {
                this.view.remove(node.key());
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    @Override
    public void clear() {
        this.view.clear();
    }

    @Override
    public Iterator<V> iterator() {
        return new SkipValueIterator<K, V>(this.view);
    }
}
