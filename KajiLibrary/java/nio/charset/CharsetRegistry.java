package java.nio.charset;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;

/**
 * The set of charsets this library provides, and the name lookup over it.
 *
 * <p>Package-private, and deliberately a plain array rather than the service-provider machinery
 * the JDK uses. That machinery exists so that a charset can arrive from a jar nobody knew about
 * at compile time; there is no such jar here, and pretending otherwise would add a loader,
 * a cache and a lock to answer a question about nine constants.
 *
 * <p>Lookup ignores case, over canonical names and aliases alike, because that is what the
 * charset registry specifies -- names are compared without regard to case, and only the
 * canonical spelling is ever handed back.
 */
final class CharsetRegistry {

    private CharsetRegistry() {
    }

    // Held here rather than read from StandardCharsets on every lookup so that the order is
    // fixed and sorted once. Initialising this class initialises StandardCharsets, never the
    // other way round, which is what keeps the two out of a cycle.
    private static final Charset[] ALL = CharsetRegistry.sorted();

    private static Charset[] sorted() {
        Charset[] out = new Charset[9];
        out[0] = StandardCharsets.US_ASCII;
        out[1] = StandardCharsets.ISO_8859_1;
        out[2] = StandardCharsets.UTF_8;
        out[3] = StandardCharsets.UTF_16BE;
        out[4] = StandardCharsets.UTF_16LE;
        out[5] = StandardCharsets.UTF_16;
        out[6] = StandardCharsets.UTF_32BE;
        out[7] = StandardCharsets.UTF_32LE;
        out[8] = StandardCharsets.UTF_32;
        // Insertion sort by canonical name, ignoring case. Nine elements: the algorithm is
        // beside the point, the fixed order is not.
        int i = 1;
        while (i < out.length) {
            Charset moving = out[i];
            int j = i - 1;
            while (j >= 0 && out[j].name().compareToIgnoreCase(moving.name()) > 0) {
                out[j + 1] = out[j];
                j = j - 1;
            }
            out[j + 1] = moving;
            i = i + 1;
        }
        return out;
    }

    /** The charset with this canonical name or alias, or null if there is none. */
    static Charset lookup(String name) {
        int i = 0;
        while (i < CharsetRegistry.ALL.length) {
            Charset candidate = CharsetRegistry.ALL[i];
            if (candidate.name().equalsIgnoreCase(name)) {
                return candidate;
            }
            i = i + 1;
        }
        i = 0;
        while (i < CharsetRegistry.ALL.length) {
            Charset candidate = CharsetRegistry.ALL[i];
            if (CharsetRegistry.containsIgnoreCase(candidate.aliasArray(), name)) {
                return candidate;
            }
            i = i + 1;
        }
        return null;
    }

    // Canonical names are checked before aliases so that a string that is both -- "UTF_16" is an
    // alias of UTF-16, and nothing else is affected -- resolves the way its owner intends.
    private static boolean containsIgnoreCase(String[] names, String wanted) {
        int i = 0;
        while (i < names.length) {
            String candidate = names[i];
            if (candidate.equalsIgnoreCase(wanted)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * The charset with this name, or null -- including when the name is not legally shaped.
     *
     * <p>The difference from {@link #lookup} is what happens to a malformed name: this one
     * answers null instead of throwing, which is what {@link Charset#forName(String, Charset)}
     * needs in order to return its fallback rather than an exception.
     */
    static Charset lookupUnchecked(String name) {
        int n = name.length();
        if (n == 0) {
            return null;
        }
        int i = 0;
        while (i < n) {
            char c = name.charAt(i);
            boolean alnum = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9');
            boolean punct = c == '-' || c == '+' || c == ':' || c == '_' || c == '.';
            if (!alnum && !(punct && i != 0)) {
                return null;
            }
            i = i + 1;
        }
        return CharsetRegistry.lookup(name);
    }

    /** Every charset, by canonical name, sorted without regard to case. */
    static SortedMap<String, Charset> available() {
        String[] names = new String[CharsetRegistry.ALL.length];
        Charset[] values = new Charset[CharsetRegistry.ALL.length];
        int i = 0;
        while (i < CharsetRegistry.ALL.length) {
            names[i] = CharsetRegistry.ALL[i].name();
            values[i] = CharsetRegistry.ALL[i];
            i = i + 1;
        }
        return new CharsetNameMap(names, values, false);
    }
}

/**
 * An immutable {@link SortedMap} from canonical name to {@link Charset}.
 *
 * <p>Written here rather than reusing {@code TreeMap} for one blunt reason: this library's
 * {@code TreeMap} implements {@code Map} and not {@code SortedMap}, and {@link
 * Charset#availableCharsets} is specified to return the latter. Nine entries in two parallel
 * arrays, already in order, is also simply the right shape for a map that never changes.
 *
 * <p>Every mutator throws, which is the unmodifiability the JDK promises for this map.
 */
final class CharsetNameMap implements SortedMap<String, Charset> {

    private final String[] names;
    private final Charset[] values;
    private final boolean descending;

    CharsetNameMap(String[] names, Charset[] values, boolean descending) {
        this.names = names;
        this.values = values;
        this.descending = descending;
    }

    // The order this map is in. Every range operation below is expressed through it, so
    // ascending and descending need no separate code paths.
    private int order(String left, String right) {
        int natural = left.compareToIgnoreCase(right);
        return this.descending ? -natural : natural;
    }

    private int indexOf(Object key) {
        if (!(key instanceof String)) {
            return -1;
        }
        String wanted = (String) key;
        int i = 0;
        while (i < this.names.length) {
            if (this.names[i].equalsIgnoreCase(wanted)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    // The entries between two bounds, keeping this map's order. Linear, over nine entries,
    // which is cheaper than the bisection it replaces once the comparisons are counted.
    private CharsetNameMap range(String from, boolean hasFrom, String to, boolean hasTo) {
        int count = 0;
        int i = 0;
        while (i < this.names.length) {
            if (this.inRange(this.names[i], from, hasFrom, to, hasTo)) {
                count = count + 1;
            }
            i = i + 1;
        }
        String[] keptNames = new String[count];
        Charset[] keptValues = new Charset[count];
        int put = 0;
        i = 0;
        while (i < this.names.length) {
            if (this.inRange(this.names[i], from, hasFrom, to, hasTo)) {
                keptNames[put] = this.names[i];
                keptValues[put] = this.values[i];
                put = put + 1;
            }
            i = i + 1;
        }
        return new CharsetNameMap(keptNames, keptValues, this.descending);
    }

    private boolean inRange(String key, String from, boolean hasFrom, String to, boolean hasTo) {
        if (hasFrom && this.order(key, from) < 0) {
            return false;
        }
        return !(hasTo && this.order(key, to) >= 0);
    }

    /** How many entries this map holds. */
    public int size() {
        return this.names.length;
    }

    /** Whether this map holds nothing. */
    public boolean isEmpty() {
        return this.names.length == 0;
    }

    /**
     * Whether a charset is registered under this name, ignoring case.
     *
     * @param key the name to look for
     */
    public boolean containsKey(Object key) {
        return this.indexOf(key) >= 0;
    }

    /**
     * Whether this charset appears in the map.
     *
     * @param value the charset to look for
     */
    public boolean containsValue(Object value) {
        int i = 0;
        while (i < this.values.length) {
            if (this.values[i].equals(value)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * The charset registered under this name, or null.
     *
     * @param key the name to look up
     */
    public Charset get(Object key) {
        int at = this.indexOf(key);
        return at < 0 ? null : this.values[at];
    }

    /**
     * Unsupported: this map is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    public Charset put(String key, Charset value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported: this map is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    public Charset remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported: this map is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported: this map is immutable.
     *
     * @throws UnsupportedOperationException always
     */
    public void putAll(Map<? extends String, ? extends Charset> m) {
        throw new UnsupportedOperationException();
    }

    /** The canonical names, in this map's order. */
    public Set<String> keySet() {
        LinkedHashSet<String> out = new LinkedHashSet<String>();
        int i = 0;
        while (i < this.names.length) {
            out.add(this.names[i]);
            i = i + 1;
        }
        return out;
    }

    /** The comparator this map is ordered by: canonical name, ignoring case. */
    public Comparator<? super String> comparator() {
        // Two unrelated implementation types in one conditional would ask the compiler to work
        // out their least upper bound; if/else asks it nothing.
        if (this.descending) {
            return new DescendingNames();
        }
        return String.CASE_INSENSITIVE_ORDER;
    }

    /**
     * The entries from {@code from} inclusive to {@code to} exclusive.
     *
     * @param from the low bound, included
     * @param to the high bound, excluded
     */
    public SortedMap<String, Charset> subMap(String from, String to) {
        return this.range(from, true, to, true);
    }

    /**
     * The entries strictly below {@code to}.
     *
     * @param to the high bound, excluded
     */
    public SortedMap<String, Charset> headMap(String to) {
        return this.range(null, false, to, true);
    }

    /**
     * The entries from {@code from} inclusive onwards.
     *
     * @param from the low bound, included
     */
    public SortedMap<String, Charset> tailMap(String from) {
        return this.range(from, true, null, false);
    }

    /**
     * The first name in this map's order.
     *
     * @throws java.util.NoSuchElementException if the map is empty
     */
    public String firstKey() {
        if (this.names.length == 0) {
            throw new java.util.NoSuchElementException();
        }
        return this.names[0];
    }

    /**
     * The last name in this map's order.
     *
     * @throws java.util.NoSuchElementException if the map is empty
     */
    public String lastKey() {
        if (this.names.length == 0) {
            throw new java.util.NoSuchElementException();
        }
        return this.names[this.names.length - 1];
    }

    /** The same entries in the opposite order. */
    public SequencedMap<String, Charset> reversed() {
        int n = this.names.length;
        String[] flippedNames = new String[n];
        Charset[] flippedValues = new Charset[n];
        int i = 0;
        while (i < n) {
            flippedNames[i] = this.names[n - 1 - i];
            flippedValues[i] = this.values[n - 1 - i];
            i = i + 1;
        }
        return new CharsetNameMap(flippedNames, flippedValues, !this.descending);
    }

    /**
     * Los valores de este mapa.
     *
     * <p>**Divergencia deliberada**, la misma que ya declara `keySet()`: la del JDK es una *vista*
     * respaldada por el mapa; esta es una copia sacada en el momento. Y a diferencia de `keySet()`
     * es una `Collection` y no un `Set`, porque los valores **si** pueden repetirse.
     */
    public java.util.Collection<Charset> values() {
        java.util.ArrayList<Charset> out = new java.util.ArrayList<Charset>();
        java.util.Iterator<String> it = this.keySet().iterator();
        while (it.hasNext()) {
            out.add(this.get(it.next()));
        }
        return out;
    }

    /**
     * Los pares de este mapa.
     *
     * <p>Misma divergencia que `values()`: copia, no vista. Los pares que devuelve son inmutables,
     * asi que `setValue` sobre uno de ellos lanza en vez de escribir en el mapa — que es lo
     * coherente con que sea una copia: escribir en un par que nadie mira seria peor que negarse.
     */
    public java.util.Set<java.util.Map.Entry<String, Charset>> entrySet() {
        java.util.HashSet<java.util.Map.Entry<String, Charset>> out =
            new java.util.HashSet<java.util.Map.Entry<String, Charset>>();
        java.util.Iterator<String> it = this.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            out.add(Map.entry(k, this.get(k)));
        }
        return out;
    }
}

/** Canonical names, ignoring case, largest first. */
final class DescendingNames implements Comparator<String> {

    /**
     * Orders two names in reverse.
     *
     * @param left the first name
     * @param right the second name
     */
    public int compare(String left, String right) {
        return right.compareToIgnoreCase(left);
    }
}
