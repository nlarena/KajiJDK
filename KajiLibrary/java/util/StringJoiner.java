package java.util;

// Builds a delimited sequence — "a, b, c" or "[a, b, c]" — without the usual off-by-one dance
// of appending a separator and trimming the last one. It holds prefix, delimiter and suffix,
// and inserts the delimiter only *between* additions.
//
// `setEmptyValue` covers the case the naive loop always gets wrong: what to print when nothing
// was added at all, which is not necessarily prefix+suffix.
public final class StringJoiner {

    private final String prefix;
    private final String delimiter;
    private final String suffix;
    private StringBuilder value;
    // What toString() returns when nothing has been added; null means prefix+suffix.
    private String emptyValue;

    public StringJoiner(CharSequence delimiter) {
        this(delimiter, "", "");
    }

    public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        if (delimiter == null || prefix == null || suffix == null) {
            throw new NullPointerException();
        }
        this.delimiter = delimiter.toString();
        this.prefix = prefix.toString();
        this.suffix = suffix.toString();
    }

    public StringJoiner setEmptyValue(CharSequence emptyValue) {
        if (emptyValue == null) {
            throw new NullPointerException();
        }
        this.emptyValue = emptyValue.toString();
        return this;
    }

    public StringJoiner add(CharSequence newElement) {
        prepare().append(newElement == null ? "null" : newElement.toString());
        return this;
    }

    // Append another joiner's contents as a single element, without its prefix and suffix.
    public StringJoiner merge(StringJoiner other) {
        if (other == null) {
            throw new NullPointerException();
        }
        if (other.value != null) {
            prepare().append(other.value.toString());
        }
        return this;
    }

    // The builder, positioned so the next append lands after the delimiter.
    private StringBuilder prepare() {
        StringBuilder b;
        if (value == null) {
            value = new StringBuilder();
            value.append(prefix);
            b = value;
        } else {
            value.append(delimiter);
            b = value;
        }
        return b;
    }

    public String toString() {
        String s;
        if (value == null) {
            if (emptyValue == null) {
                s = prefix + suffix;
            } else {
                s = emptyValue;
            }
        } else {
            s = value.toString() + suffix;
        }
        return s;
    }

    public int length() {
        return toString().length();
    }
}
