package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.List;
import java.util.Comparator;
import java.util.random.RandomGenerator;

// java.util.Collections: the algorithms and the factories that work over the collection
// interfaces without belonging to any implementation. It is not instantiated.
//
// The class reads in four parts:
//
//   1. **Empty and single-element** -- emptyList, singleton, nCopies and company. Immutable
//      collections built in one go, far cheaper than a HashSet of one element.
//   2. **Wrappers** -- unmodifiableX, synchronizedX, checkedX. All three return a VIEW of the
//      collection behind, not a copy: whatever changes underneath is seen from above. The
//      machinery is in GuardedCollection and GuardedMap.
//   3. **Algorithms** -- sort, binarySearch, min/max, shuffle, rotate, frequency, disjoint.
//   4. **Bridges** -- enumeration/list between Enumeration and Iterator, asLifoQueue,
//      newSetFromMap.
//
// About the views there is a trap worth keeping in mind: `unmodifiable*` promises that NOBODY can
// modify **through the view**, not that the collection is immutable. Whoever keeps the original
// reference can still write, and the view reflects it. For inner immutability there are `List.of`,
// `Set.of` and `Map.of`, which copy.
public final class Collections {

    private Collections() {}

    // ---- empty and single-element ---------------------------------------------------------------
    //
    // The three constants are raw -- `List`, not `List<T>` -- because they predate generics and
    // typing them would break the code that uses them. The factories emptyList/emptySet/emptyMap are
    // the typed form of exactly the same thing, and they are the ones to use.

    public static final List EMPTY_LIST = new FixedList(new Object[0]);

    public static final Set EMPTY_SET = FixedSet.fromArray(new Object[0], 0);

    public static final Map EMPTY_MAP = FixedMap.fromPairs(new Object[0], 0);

    public static final <T> List<T> emptyList() {
        return new FixedList<T>(new Object[0]);
    }

    public static final <T> Set<T> emptySet() {
        return FixedSet.fromArray(new Object[0], 0);
    }

    public static final <K, V> Map<K, V> emptyMap() {
        return FixedMap.fromPairs(new Object[0], 0);
    }

    public static <E> SortedSet<E> emptySortedSet() {
        return new EmptySortedSet<E>();
    }

    public static <E> NavigableSet<E> emptyNavigableSet() {
        return new EmptySortedSet<E>();
    }

    public static final <K, V> SortedMap<K, V> emptySortedMap() {
        return new EmptySortedMap<K, V>();
    }

    public static final <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return new EmptySortedMap<K, V>();
    }

    public static <T> Iterator<T> emptyIterator() {
        return new FixedListItr<T>(new Object[0]);
    }

    public static <T> ListIterator<T> emptyListIterator() {
        return new AbstractListLitr<T>(new FixedList<T>(new Object[0]), 0);
    }

    public static <T> Enumeration<T> emptyEnumeration() {
        // Qualified on purpose: there is another package-private `EmptyEnumeration` in `java.lang`
        // (`ClassLoader`'s, which is an `Enumeration<URL>`), and by simple name that one won.
        // The compiler defect is fixed (finding #493: the own package now beats the implicit
        // `java.lang.*`), but `bin/javac.exe` is frozen and still has it, so the qualification has
        // to stay until that binary is refreshed. Drop it then, and regenerate this `.class`.
        return new java.util.EmptyEnumeration<T>();
    }

    // An immutable set of a single element. It exists because it comes out far cheaper than a
    // HashSet of one: no table, no hash, no load factor. The same holds for the two that follow.
    public static <T> Set<T> singleton(T o) {
        Object[] a = new Object[1];
        a[0] = o;
        return FixedSet.fromArray(a, 1);
    }

    public static <T> List<T> singletonList(T o) {
        Object[] a = new Object[1];
        a[0] = o;
        return new FixedList<T>(a);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        Object[] kv = new Object[2];
        kv[0] = key;
        kv[1] = value;
        return FixedMap.fromPairs(kv, 2);
    }

    // An immutable list with `o` repeated `n` times.
    //
    // **A deliberate divergence**: the JDK's keeps the element ONCE and fakes the length, so
    // `nCopies(1000000, x)` does not take up a million references. This one materialises the array.
    // It is correct and more expensive; the day somebody uses it with a large `n`, it has to
    // change.
    public static <T> List<T> nCopies(int n, T o) {
        if (n < 0) {
            throw new IllegalArgumentException("List length = " + n);
        }
        Object[] a = new Object[n];
        int i = 0;
        while (i < n) {
            a[i] = o;
            i = i + 1;
        }
        return new FixedList<T>(a);
    }

    // ---- read-only wrappers ---------------------------------------------------------------------
    //
    // The Guarded*'s three arguments are, in order: the collection behind, the class each element
    // coming in is required to be (null = it is not validated) and whether it is read-only.

    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
        return new GuardedCollection<T>((Collection<T>) c, null, true);
    }

    public static <T> SequencedCollection<T> unmodifiableSequencedCollection(
            SequencedCollection<? extends T> c) {
        return new GuardedSequencedCollection<T>((SequencedCollection<T>) c, null, true);
    }

    public static <T> List<T> unmodifiableList(List<? extends T> list) {
        return new GuardedList<T>((List<T>) list, null, true);
    }

    public static <T> Set<T> unmodifiableSet(Set<? extends T> s) {
        return new GuardedSet<T>((Set<T>) s, null, true);
    }

    public static <T> SequencedSet<T> unmodifiableSequencedSet(SequencedSet<? extends T> s) {
        return new GuardedSequencedSet<T>((SequencedSet<T>) s, null, true);
    }

    public static <T> SortedSet<T> unmodifiableSortedSet(SortedSet<T> s) {
        return new GuardedSortedSet<T>(s, null, true);
    }

    public static <T> NavigableSet<T> unmodifiableNavigableSet(NavigableSet<T> s) {
        return new GuardedNavigableSet<T>(s, null, true);
    }

    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> m) {
        return new GuardedMap<K, V>((Map<K, V>) m, null, null, true);
    }

    public static <K, V> SequencedMap<K, V> unmodifiableSequencedMap(
            SequencedMap<? extends K, ? extends V> m) {
        return new GuardedSequencedMap<K, V>((SequencedMap<K, V>) m, null, null, true);
    }

    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> m) {
        return new GuardedSortedMap<K, V>((SortedMap<K, V>) m, null, null, true);
    }

    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> m) {
        return new GuardedNavigableMap<K, V>((NavigableMap<K, V>) m, null, null, true);
    }

    // ---- synchronized wrappers -------------------------------------------------------------------
    //
    // Each single operation is protected, but **a sequence of operations is not**. The classic case
    // is the walk: between the `hasNext()` and the `next()` another thread can slip in. That is why
    // the JDK documents that the user has to take the returned collection's monitor themselves:
    //
    //     List<X> l = Collections.synchronizedList(new ArrayList<X>());
    //     synchronized (l) { for (X x : l) { ... } }
    //
    // That it be the returned collection's monitor, and not another, is part of the contract: the
    // wrappers here synchronise on themselves precisely so that line works.

    public static <T> Collection<T> synchronizedCollection(Collection<T> c) {
        return new GuardedCollection<T>(c, null, false);
    }

    public static <T> List<T> synchronizedList(List<T> list) {
        return new GuardedList<T>(list, null, false);
    }

    public static <T> Set<T> synchronizedSet(Set<T> s) {
        return new GuardedSet<T>(s, null, false);
    }

    public static <T> SortedSet<T> synchronizedSortedSet(SortedSet<T> s) {
        return new GuardedSortedSet<T>(s, null, false);
    }

    public static <T> NavigableSet<T> synchronizedNavigableSet(NavigableSet<T> s) {
        return new GuardedNavigableSet<T>(s, null, false);
    }

    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> m) {
        return new GuardedMap<K, V>(m, null, null, false);
    }

    public static <K, V> SortedMap<K, V> synchronizedSortedMap(SortedMap<K, V> m) {
        return new GuardedSortedMap<K, V>(m, null, null, false);
    }

    public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(NavigableMap<K, V> m) {
        return new GuardedNavigableMap<K, V>(m, null, null, false);
    }

    // ---- type-checking wrappers ------------------------------------------------------------------
    //
    // They plug the hole erased generics leave: a `List<String>` passed as a raw `List` accepts an
    // Integer without complaint, and the ClassCastException jumps out much later, at the `get`, far
    // from whoever caused it. `checkedList` brings it forward to the `add`.
    //
    // It is a diagnostic tool: one wraps while hunting for whoever is dirtying the collection, and
    // afterwards takes it off.

    public static <E> Collection<E> checkedCollection(Collection<E> c, Class<E> type) {
        return new GuardedCollection<E>(c, type, false);
    }

    public static <E> List<E> checkedList(List<E> list, Class<E> type) {
        return new GuardedList<E>(list, type, false);
    }

    public static <E> Set<E> checkedSet(Set<E> s, Class<E> type) {
        return new GuardedSet<E>(s, type, false);
    }

    public static <E> SortedSet<E> checkedSortedSet(SortedSet<E> s, Class<E> type) {
        return new GuardedSortedSet<E>(s, type, false);
    }

    public static <E> NavigableSet<E> checkedNavigableSet(NavigableSet<E> s, Class<E> type) {
        return new GuardedNavigableSet<E>(s, type, false);
    }

    public static <E> Queue<E> checkedQueue(Queue<E> queue, Class<E> type) {
        return new GuardedQueue<E>(queue, type, false);
    }

    public static <K, V> Map<K, V> checkedMap(Map<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new GuardedMap<K, V>(m, keyType, valueType, false);
    }

    public static <K, V> SortedMap<K, V> checkedSortedMap(SortedMap<K, V> m, Class<K> keyType,
            Class<V> valueType) {
        return new GuardedSortedMap<K, V>(m, keyType, valueType, false);
    }

    public static <K, V> NavigableMap<K, V> checkedNavigableMap(NavigableMap<K, V> m,
            Class<K> keyType, Class<V> valueType) {
        return new GuardedNavigableMap<K, V>(m, keyType, valueType, false);
    }

    // ---- algorithms -------------------------------------------------------------------------------
    //
    // The methods that take a `List<?>` almost all start with the same line: a typed `List<Object>`
    // view of the same list. The thing is that `List<?>` lets NOTHING be written -- not even what
    // has just been taken out of it, because the compiler cannot prove they are the same captured
    // type. The conversion is safe: only elements that were already inside are rearranged.

    // Swap the elements at positions `i` and `j`.
    public static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // Reverse the order of the elements in place.
    public static <T> void reverse(List<T> list) {
        int size = list.size();
        for (int i = 0; i < size / 2; i++) {
            Collections.swap(list, i, size - 1 - i);
        }
    }

    // Replace every element with `obj`.
    public static <T> void fill(List<T> list, T obj) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            list.set(i, obj);
        }
    }

    // Sort ascending by a Comparator (stable-ish insertion sort over indexed access).
    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && c.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j = j - 1;
            }
            list.set(j + 1, key);
        }
    }

    // Sort ascending by natural order (elements must be Comparable to each other).
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).compareTo(key) > 0) {
                list.set(j + 1, list.get(j));
                j = j - 1;
            }
            list.set(j + 1, key);
        }
    }

    // The index of `key` in an **already sorted** list, or `-(where_it_would_go) - 1` if it is not
    // there.
    //
    // That the list has to come in sorted is not a detail of the documentation: over an unsorted one
    // it does not give an error, it gives a meaningless result. It is the price of dropping from
    // O(n) to O(log n) -- half can be discarded at each step precisely because the order is trusted.
    //
    // The negative encodes the insertion point instead of being a plain -1, and that detail is what
    // makes the method useful for keeping a list sorted: if it is not there, where it goes is
    // already known.
    public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
        return search((List<Object>) list, key, null);
    }

    public static <T> int binarySearch(List<? extends T> list, T key, Comparator<? super T> c) {
        return search((List<Object>) list, key, (Comparator<Object>) c);
    }

    private static int search(List<Object> list, Object key, Comparator<Object> c) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            // `>>> 1` and not `/ 2`: with large lists `low + high` can overflow, and the unsigned
            // shift returns the correct average all the same.
            int mid = (low + high) >>> 1;
            Object at = list.get(mid);
            int cmp;
            if (c != null) {
                cmp = c.compare(at, key);
            } else {
                cmp = ((Comparable<Object>) at).compareTo(key);
            }
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    // It shuffles the list: it walks from the back forwards swapping each position with a random one
    // among those left (Fisher-Yates). It is the only shuffle that gives the n! permutations with the
    // same probability; the naive one -- swapping each position with any one of the whole list --
    // skews the result and it does not show by looking at it.
    public static void shuffle(List<?> list) {
        shuffle(list, new Random());
    }

    public static void shuffle(List<?> list, Random rnd) {
        shuffle(list, (RandomGenerator) rnd);
    }

    public static void shuffle(List<?> list, RandomGenerator rnd) {
        List<Object> l = (List<Object>) list;
        int i = l.size() - 1;
        while (i > 0) {
            swap(l, i, rnd.nextInt(i + 1));
            i = i - 1;
        }
    }

    // It copies `src` over the start of `dest`, which has to have room enough. It does not grow the
    // destination: `copy` overwrites, it does not insert.
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        int n = src.size();
        if (dest.size() < n) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }
        List<Object> d = (List<Object>) dest;
        int i = 0;
        while (i < n) {
            d.set(i, src.get(i));
            i = i + 1;
        }
    }

    public static <T extends Comparable<? super T>> T min(Collection<? extends T> coll) {
        return (T) extreme(coll, null, true);
    }

    public static <T> T min(Collection<? extends T> coll, Comparator<? super T> comp) {
        return (T) extreme(coll, (Comparator<Object>) comp, true);
    }

    public static <T extends Comparable<? super T>> T max(Collection<? extends T> coll) {
        return (T) extreme(coll, null, false);
    }

    public static <T> T max(Collection<? extends T> coll, Comparator<? super T> comp) {
        return (T) extreme(coll, (Comparator<Object>) comp, false);
    }

    // What min and max have in common. It walks with an iterator and not by index because a
    // Collection promises no indexed access -- a HashSet has no "element 3".
    //
    // The strict `>`/`<` is kept on purpose: on a tie the first one that turned up wins, which is
    // what the JDK documents.
    private static Object extreme(Collection<?> coll, Comparator<Object> comp, boolean wantMin) {
        Iterator<?> it = coll.iterator();
        // Over an empty collection this throws NoSuchElementException, which is what is specified.
        Object best = it.next();
        while (it.hasNext()) {
            Object next = it.next();
            int cmp;
            if (comp != null) {
                cmp = comp.compare(next, best);
            } else {
                cmp = ((Comparable<Object>) next).compareTo(best);
            }
            if (wantMin) {
                if (cmp < 0) {
                    best = next;
                }
            } else {
                if (cmp > 0) {
                    best = next;
                }
            }
        }
        return best;
    }

    // It moves the elements `distance` places towards the end, wrapping around.
    //
    // It is done with three reversals -- the whole list, then each of the two parts -- which is the
    // trick that settles the rotation in O(n) and with no extra memory. Rotating one place at a time
    // `distance` times would be O(n * distance).
    public static void rotate(List<?> list, int distance) {
        int n = list.size();
        if (n == 0) {
            return;
        }
        // floorMod and not `%`: in Java the remainder keeps the dividend's sign, so `-1 % 5` is -1
        // and not 4, and a negative distance would give indices out of range.
        int d = Math.floorMod(distance, n);
        if (d == 0) {
            return;
        }
        List<Object> l = (List<Object>) list;
        reverse(l);
        reverse(l.subList(0, d));
        reverse(l.subList(d, n));
    }

    public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
        boolean changed = false;
        int n = list.size();
        int i = 0;
        while (i < n) {
            if (Objects.equals(list.get(i), oldVal)) {
                list.set(i, newVal);
                changed = true;
            }
            i = i + 1;
        }
        return changed;
    }

    // The first index where `target` appears whole inside `source`, or -1.
    //
    // It is the naive search, O(n * m): every position is tried. It is enough for what this is used
    // for -- short lists -- and it avoids a Knuth-Morris-Pratt's preprocessing, which over generic
    // lists would on top of that force more equals comparisons than it saves.
    public static int indexOfSubList(List<?> source, List<?> target) {
        int n = source.size();
        int m = target.size();
        int i = 0;
        while (i + m <= n) {
            if (matchesAt(source, target, i)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    public static int lastIndexOfSubList(List<?> source, List<?> target) {
        int n = source.size();
        int m = target.size();
        int i = n - m;
        while (i >= 0) {
            if (matchesAt(source, target, i)) {
                return i;
            }
            i = i - 1;
        }
        return -1;
    }

    private static boolean matchesAt(List<?> source, List<?> target, int at) {
        int m = target.size();
        int k = 0;
        while (k < m) {
            if (!Objects.equals(source.get(at + k), target.get(k))) {
                return false;
            }
            k = k + 1;
        }
        return true;
    }

    // How many times `o` turns up. It counts by equals, unless `o` is null, and then it counts the
    // nulls.
    public static int frequency(Collection<?> c, Object o) {
        int n = 0;
        Iterator<?> it = c.iterator();
        while (it.hasNext()) {
            if (Objects.equals(it.next(), o)) {
                n = n + 1;
            }
        }
        return n;
    }

    // Whether the two collections share no element.
    //
    // `c1` is walked asking `c2`, which is the opposite of what looks natural: it suits that the one
    // asked be the one with the fast `contains` (a Set) and the one walked the other. Whoever calls
    // with two large ArrayLists will pay O(n * m), and there is no way of avoiding it without
    // copying one into a set -- which the JDK does not do either.
    public static boolean disjoint(Collection<?> c1, Collection<?> c2) {
        Iterator<?> it = c1.iterator();
        while (it.hasNext()) {
            if (c2.contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    // It adds all the loose elements to the collection. It is the convenient version of an `addAll`
    // with an intermediate list, and it is not equivalent to `c.addAll(...)`: if one of the `add`s
    // fails half way, the earlier ones stay.
    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        boolean changed = false;
        int i = 0;
        while (i < elements.length) {
            if (c.add(elements[i])) {
                changed = true;
            }
            i = i + 1;
        }
        return changed;
    }

    // The comparator that reverses the natural order.
    public static <T> Comparator<T> reverseOrder() {
        return new ReverseComparator<T>(null);
    }

    // The one that reverses another comparator. With `null` it falls back to the reversed natural
    // order, which is what makes this form useful: it allows passing "the usual order, backwards"
    // with no special case.
    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
        return new ReverseComparator<T>(cmp);
    }

    // ---- bridges between the old APIs and the new  ------------------------------------------------

    // An Enumeration over the collection. The bridge towards what predates `Iterator` and still asks
    // for an Enumeration: Properties, ZipFile, ServletRequest.
    public static <T> Enumeration<T> enumeration(Collection<T> c) {
        return new ArrayEnumeration<T>(c.toArray());
    }

    // The bridge in the other direction: it empties an Enumeration into a list.
    public static <T> ArrayList<T> list(Enumeration<T> e) {
        ArrayList<T> out = new ArrayList<T>();
        while (e.hasMoreElements()) {
            out.add(e.nextElement());
        }
        return out;
    }

    // A Set backed by the given Map, which has to arrive empty.
    //
    // It exists for a concrete case: there is no IdentityHashSet nor ConcurrentHashSet in the JDK,
    // and this is the way to build them -- `newSetFromMap(new IdentityHashMap())` gives a set that
    // compares by identity instead of by equals.
    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        return new SetFromMap<E>(map);
    }

    public static <E> SequencedSet<E> newSequencedSetFromMap(SequencedMap<E, Boolean> map) {
        return new SequencedSetFromMap<E>(map);
    }

    // A Queue view of the deque that takes out where it puts in: a stack with a queue's face.
    public static <T> Queue<T> asLifoQueue(Deque<T> deque) {
        return new LifoQueue<T>(deque);
    }
}
