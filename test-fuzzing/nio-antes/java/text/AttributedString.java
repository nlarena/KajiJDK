package java.text;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Text with attributes attached to character ranges, and the factory of the iterators that walk it.
 *
 * <p>A {@code String} says what characters there are; this says as well what rules over each run:
 * the language, the font, or --inside this package-- which field of a formatting's result each piece
 * is. It is the writable counterpart of {@link AttributedCharacterIterator}: here it is built, there
 * it is read.
 *
 * <p><b>The order of the calls matters.</b> Two {@code addAttribute}s with the same key over ranges
 * that overlap are neither merged nor rejected: the last one wins. That allows the ordinary pattern
 * of "paint everything A and then the middle piece B" without having to compute the difference of
 * ranges.
 *
 * <p><b>A deliberate difference from the JDK.</b> The JDK stores physical runs and splits a new one
 * at every boundary added, without merging them back; {@code getRunLimit()} with no argument then
 * returns that physical boundary, which can fall short even though the attributes on both sides are
 * identical. Here the runs are computed when reading, by comparing the attribute maps, so the limit
 * is the one the contract states ("as far as no attribute changes") and not a residue of how the
 * object was built. For a correct caller the difference is invisible; for one that counts runs, ours
 * is what the javadoc promises.
 *
 * <p>Non-public JDK members that are absent: the {@code text}/{@code runCount}/... fields and the
 * concatenating constructor are package-access and describe the JDK's internal representation, which
 * here is another. They are not part of the API.
 */
public class AttributedString {

    // Package access: AttributedStringIterator reads them directly. The alternative --accessors--
    // would only add noise, because the two classes are one piece split across two files.
    final String text;
    AttributedCharacterIterator.Attribute[] keys;
    Object[] values;
    int[] from;
    int[] to;
    int count;

    public AttributedString(String text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
        this.keys = new AttributedCharacterIterator.Attribute[4];
        this.values = new Object[4];
        this.from = new int[4];
        this.to = new int[4];
        this.count = 0;
    }

    public AttributedString(String text, Map<? extends AttributedCharacterIterator.Attribute, ?> attributes) {
        this(text);
        if (attributes == null) {
            throw new NullPointerException();
        }
        if (text.length() == 0) {
            // An empty text with attributes is a contradiction: there is no character for them to
            // rule over. The JDK rejects it and so do we, because accepting it would leave an object
            // whose attributes no iterator could ever return.
            if (!attributes.isEmpty()) {
                throw new IllegalArgumentException("Can't add attribute to 0-length text");
            }
            return;
        }
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> e : attributes.entrySet()) {
            this.add(e.getKey(), e.getValue(), 0, text.length());
        }
    }

    public AttributedString(AttributedCharacterIterator text) {
        this(text, beginOfRun(text), endOfRun(text), null);
    }

    public AttributedString(AttributedCharacterIterator text, int beginIndex, int endIndex) {
        this(text, beginIndex, endIndex, null);
    }

    /**
     * It copies a range of the iterator keeping only the attributes listed.
     *
     * @param attributes the keys to keep; {@code null} keeps them all. An EMPTY array is not the same
     *                   as {@code null}: it discards every attribute and leaves the text bare.
     */
    public AttributedString(AttributedCharacterIterator text, int beginIndex, int endIndex,
                            AttributedCharacterIterator.Attribute[] attributes) {
        if (text == null) {
            throw new NullPointerException();
        }
        if (beginIndex < text.getBeginIndex() || endIndex > text.getEndIndex() || beginIndex > endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++) {
            text.setIndex(i);
            sb.append(text.current());
        }
        this.text = sb.toString();
        this.keys = new AttributedCharacterIterator.Attribute[4];
        this.values = new Object[4];
        this.from = new int[4];
        this.to = new int[4];
        this.count = 0;

        Set<AttributedCharacterIterator.Attribute> filter = null;
        if (attributes != null) {
            filter = new HashSet<AttributedCharacterIterator.Attribute>();
            for (int i = 0; i < attributes.length; i++) {
                filter.add(attributes[i]);
            }
        }

        // It copies run by run and not character by character because the iterator already knows
        // where the attributes change: asking it avoids rereading the same map once per position.
        int i = beginIndex;
        while (i < endIndex) {
            text.setIndex(i);
            int end = text.getRunLimit();
            if (end > endIndex) {
                end = endIndex;
            }
            if (end <= i) {
                end = i + 1;
            }
            Map<AttributedCharacterIterator.Attribute, Object> map = text.getAttributes();
            if (map != null) {
                for (Map.Entry<AttributedCharacterIterator.Attribute, Object> e : map.entrySet()) {
                    if (filter == null || filter.contains(e.getKey())) {
                        this.add(e.getKey(), e.getValue(), i - beginIndex, end - beginIndex);
                    }
                }
            }
            i = end;
        }
    }

    private static int beginOfRun(AttributedCharacterIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        return it.getBeginIndex();
    }

    private static int endOfRun(AttributedCharacterIterator it) {
        if (it == null) {
            throw new NullPointerException();
        }
        return it.getEndIndex();
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object value) {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (this.text.length() == 0) {
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }
        this.add(attribute, value, 0, this.text.length());
    }

    public void addAttribute(AttributedCharacterIterator.Attribute attribute, Object value,
                             int beginIndex, int endIndex) {
        if (attribute == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || endIndex > this.text.length() || beginIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        this.add(attribute, value, beginIndex, endIndex);
    }

    public void addAttributes(Map<? extends AttributedCharacterIterator.Attribute, ?> attributes,
                              int beginIndex, int endIndex) {
        if (attributes == null) {
            throw new NullPointerException();
        }
        if (beginIndex < 0 || endIndex > this.text.length() || beginIndex > endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (beginIndex == endIndex) {
            // An empty range: the JDK accepts it and does nothing. It is not the same as
            // addAttribute's case, where the empty range comes from an empty text and is an error.
            if (attributes.isEmpty()) {
                return;
            }
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }
        for (Map.Entry<? extends AttributedCharacterIterator.Attribute, ?> e : attributes.entrySet()) {
            this.add(e.getKey(), e.getValue(), beginIndex, endIndex);
        }
    }

    public AttributedCharacterIterator getIterator() {
        return this.getIterator(null, 0, this.text.length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributes) {
        return this.getIterator(attributes, 0, this.text.length());
    }

    public AttributedCharacterIterator getIterator(AttributedCharacterIterator.Attribute[] attributes,
                                                   int beginIndex, int endIndex) {
        return new AttributedStringIterator(this, attributes, beginIndex, endIndex);
    }

    // Adding is a pure append, without merging or trimming what came before: the "the last one
    // wins" resolution is done when reading, walking the list in order. Merging here would force old
    // runs to be split on every call and would change no observable result.
    private void add(AttributedCharacterIterator.Attribute key, Object value, int d, int h) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (this.count == this.keys.length) {
            int raised = this.keys.length * 2;
            AttributedCharacterIterator.Attribute[] k = new AttributedCharacterIterator.Attribute[raised];
            Object[] v = new Object[raised];
            int[] a = new int[raised];
            int[] b = new int[raised];
            for (int i = 0; i < this.count; i++) {
                k[i] = this.keys[i];
                v[i] = this.values[i];
                a[i] = this.from[i];
                b[i] = this.to[i];
            }
            this.keys = k;
            this.values = v;
            this.from = a;
            this.to = b;
        }
        this.keys[this.count] = key;
        this.values[this.count] = value;
        this.from[this.count] = d;
        this.to[this.count] = h;
        this.count = this.count + 1;
    }
}
