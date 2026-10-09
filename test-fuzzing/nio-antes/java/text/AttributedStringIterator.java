package java.text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The read-only view {@link AttributedString#getIterator()} hands out.
 *
 * <p>It is not public because the JDK does not expose it either: the type the caller sees is the
 * {@link AttributedCharacterIterator} interface. It lives in its own file and not as a nested class
 * so the piece's two sides --the writable one and the readable one-- can be read separately.
 *
 * <p>The runs are computed here and not stored in the {@code AttributedString}: the attributes are
 * kept as a list of "this key, this value, this range", and a run's boundary is the place where the
 * effective map changes. Computing it when reading is what makes "the last one wins" come out free
 * and what keeps a boundary that changes nothing from showing up as a run.
 */
final class AttributedStringIterator implements AttributedCharacterIterator {

    private final AttributedString source;
    private final int begin;
    private final int end;
    private final Set<AttributedCharacterIterator.Attribute> filter;
    private int pos;

    AttributedStringIterator(AttributedString source,
                             AttributedCharacterIterator.Attribute[] attributes,
                             int begin, int end) {
        if (begin < 0 || end > source.text.length() || begin > end) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        this.source = source;
        this.begin = begin;
        this.end = end;
        this.pos = begin;
        if (attributes == null) {
            this.filter = null;
        } else {
            // An empty array is NOT the same as null: it explicitly asks for "no attributes".
            Set<AttributedCharacterIterator.Attribute> s =
                    new HashSet<AttributedCharacterIterator.Attribute>();
            for (int i = 0; i < attributes.length; i++) {
                s.add(attributes[i]);
            }
            this.filter = s;
        }
    }

    private AttributedStringIterator(AttributedStringIterator other) {
        this.source = other.source;
        this.begin = other.begin;
        this.end = other.end;
        this.filter = other.filter;
        this.pos = other.pos;
    }

    // ---- CharacterIterator ----

    public char first() {
        return this.setIndex(this.begin);
    }

    public char last() {
        if (this.end == this.begin) {
            this.pos = this.end;
            return CharacterIterator.DONE;
        }
        return this.setIndex(this.end - 1);
    }

    public char current() {
        if (this.pos < this.begin || this.pos >= this.end) {
            return CharacterIterator.DONE;
        }
        return this.source.text.charAt(this.pos);
    }

    public char next() {
        if (this.pos < this.end) {
            this.pos = this.pos + 1;
        }
        return this.current();
    }

    public char previous() {
        if (this.pos > this.begin) {
            this.pos = this.pos - 1;
            return this.current();
        }
        return CharacterIterator.DONE;
    }

    public char setIndex(int position) {
        if (position < this.begin || position > this.end) {
            throw new IllegalArgumentException("Invalid index");
        }
        this.pos = position;
        return this.current();
    }

    public int getBeginIndex() {
        return this.begin;
    }

    public int getEndIndex() {
        return this.end;
    }

    public int getIndex() {
        return this.pos;
    }

    // Copied by hand, like the rest of the house: a new iterator with the same window and the same
    // cursor is exactly the same thing, and does not depend on the native Object.clone().
    public Object clone() {
        return new AttributedStringIterator(this);
    }

    // ---- AttributedCharacterIterator ----

    public int getRunStart() {
        return this.runStartFrom(this.pos, null);
    }

    public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
        return this.runStartFrom(this.pos, unitary(attribute));
    }

    public int getRunStart(Set<? extends AttributedCharacterIterator.Attribute> attributes) {
        return this.runStartFrom(this.pos, copy(attributes));
    }

    public int getRunLimit() {
        return this.runLimitFrom(this.pos, null);
    }

    public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
        return this.runLimitFrom(this.pos, unitary(attribute));
    }

    public int getRunLimit(Set<? extends AttributedCharacterIterator.Attribute> attributes) {
        return this.runLimitFrom(this.pos, copy(attributes));
    }

    public Map<AttributedCharacterIterator.Attribute, Object> getAttributes() {
        return this.mapAt(this.pos);
    }

    public Object getAttribute(AttributedCharacterIterator.Attribute attribute) {
        return this.valueAt(this.pos, attribute);
    }

    public Set<AttributedCharacterIterator.Attribute> getAllAttributeKeys() {
        Set<AttributedCharacterIterator.Attribute> s =
                new HashSet<AttributedCharacterIterator.Attribute>();
        for (int i = 0; i < this.source.count; i++) {
            // A run that does not touch the window contributes no key: the iterator cannot return
            // its value at any position, so announcing it would be announcing something
            // unreachable.
            if (this.source.to[i] > this.begin && this.source.from[i] < this.end) {
                AttributedCharacterIterator.Attribute k = this.source.keys[i];
                if (this.filter == null || this.filter.contains(k)) {
                    s.add(k);
                }
            }
        }
        return s;
    }

    // ---- internal -------------------------------------------------------------------------------

    private static Set<AttributedCharacterIterator.Attribute> unitary(
            AttributedCharacterIterator.Attribute a) {
        Set<AttributedCharacterIterator.Attribute> s =
                new HashSet<AttributedCharacterIterator.Attribute>();
        s.add(a);
        return s;
    }

    private static Set<AttributedCharacterIterator.Attribute> copy(
            Set<? extends AttributedCharacterIterator.Attribute> in) {
        Set<AttributedCharacterIterator.Attribute> s =
                new HashSet<AttributedCharacterIterator.Attribute>();
        if (in != null) {
            for (AttributedCharacterIterator.Attribute a : in) {
                s.add(a);
            }
        }
        return s;
    }

    private Map<AttributedCharacterIterator.Attribute, Object> mapAt(int idx) {
        Map<AttributedCharacterIterator.Attribute, Object> m =
                new HashMap<AttributedCharacterIterator.Attribute, Object>();
        if (idx < this.begin || idx >= this.end) {
            return m;
        }
        // In insertion order: the last one covering the position overrides the earlier ones. That
        // is the whole conflict-resolution rule, and it is why there is no need to store split runs
        // in the AttributedString.
        for (int i = 0; i < this.source.count; i++) {
            if (this.source.from[i] <= idx && idx < this.source.to[i]) {
                AttributedCharacterIterator.Attribute k = this.source.keys[i];
                if (this.filter == null || this.filter.contains(k)) {
                    m.put(k, this.source.values[i]);
                }
            }
        }
        return m;
    }

    private Object valueAt(int idx, AttributedCharacterIterator.Attribute key) {
        if (idx < this.begin || idx >= this.end) {
            return null;
        }
        if (this.filter != null && !this.filter.contains(key)) {
            return null;
        }
        Object v = null;
        for (int i = 0; i < this.source.count; i++) {
            if (this.source.keys[i] == key
                    && this.source.from[i] <= idx && idx < this.source.to[i]) {
                v = this.source.values[i];
            }
        }
        return v;
    }

    // A run's possible boundaries are exactly the declared ranges' ends, plus the window's. Between
    // two consecutive boundaries nothing can change, so looking at the boundaries is enough instead
    // of walking character by character.
    private int[] boundaries() {
        int n = this.source.count * 2 + 2;
        int[] b = new int[n];
        int k = 0;
        b[k] = this.begin;
        k = k + 1;
        b[k] = this.end;
        k = k + 1;
        for (int i = 0; i < this.source.count; i++) {
            b[k] = this.trimTo(this.source.from[i]);
            k = k + 1;
            b[k] = this.trimTo(this.source.to[i]);
            k = k + 1;
        }
        // Insertion sort: n is small (two per attribute added) and the order has to be stable and
        // free of duplicates so the start/limit walks read each boundary once.
        for (int i = 1; i < k; i++) {
            int v = b[i];
            int j = i - 1;
            while (j >= 0 && b[j] > v) {
                b[j + 1] = b[j];
                j = j - 1;
            }
            b[j + 1] = v;
        }
        int m = 0;
        for (int i = 0; i < k; i++) {
            if (i == 0 || b[i] != b[i - 1]) {
                b[m] = b[i];
                m = m + 1;
            }
        }
        int[] out = new int[m];
        for (int i = 0; i < m; i++) {
            out[i] = b[i];
        }
        return out;
    }

    private int trimTo(int v) {
        if (v < this.begin) {
            return this.begin;
        }
        if (v > this.end) {
            return this.end;
        }
        return v;
    }

    private int runLimitFrom(int idx, Set<AttributedCharacterIterator.Attribute> considered) {
        if (idx >= this.end) {
            return this.end;
        }
        int[] b = this.boundaries();
        for (int i = 0; i < b.length; i++) {
            if (b[i] > idx && !this.sameAttributes(b[i], idx, considered)) {
                return b[i];
            }
        }
        return this.end;
    }

    private int runStartFrom(int idx, Set<AttributedCharacterIterator.Attribute> considered) {
        if (idx >= this.end) {
            return this.end;
        }
        int[] b = this.boundaries();
        int r = -1;
        for (int i = 0; i < b.length; i++) {
            if (b[i] > idx) {
                break;
            }
            if (this.sameAttributes(b[i], idx, considered)) {
                if (r < 0) {
                    r = b[i];
                }
            } else {
                r = -1;
            }
        }
        if (r < 0) {
            return this.begin;
        }
        return r;
    }

    private boolean sameAttributes(int a, int c, Set<AttributedCharacterIterator.Attribute> considered) {
        if (considered == null) {
            Map<AttributedCharacterIterator.Attribute, Object> ma = this.mapAt(a);
            Map<AttributedCharacterIterator.Attribute, Object> mc = this.mapAt(c);
            if (ma.size() != mc.size()) {
                return false;
            }
            for (Map.Entry<AttributedCharacterIterator.Attribute, Object> e : ma.entrySet()) {
                if (!mc.containsKey(e.getKey())) {
                    return false;
                }
                if (!bothMatch(e.getValue(), mc.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        for (AttributedCharacterIterator.Attribute k : considered) {
            if (!bothMatch(this.valueAt(a, k), this.valueAt(c, k))) {
                return false;
            }
        }
        return true;
    }

    private static boolean bothMatch(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
