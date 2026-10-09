package java.text;

/**
 * Wraps a text attribute value to mark it as <em>not</em> splittable.
 *
 * <p>The whole class is a one-bit signal, and the bit is about identity of ranges. Attributed text
 * carries values over ranges of characters, and most values survive being cut: if the range
 * {@code [0,10)} is bold and you ask for {@code [3,7)}, that sub-range is still bold, and two
 * adjacent bold runs can be merged into one. A few values do not work that way. The reading of a
 * Japanese word, or the source segment an input method produced, belongs to <em>that</em> run of
 * characters and to no part of it; splitting the run leaves two halves neither of which has the
 * annotation, and joining two runs that happen to carry equal values would claim they were one
 * annotation when they were two.
 *
 * <p>Wrapping the value in an {@code Annotation} tells the attributed-text machinery exactly that:
 * the run boundaries are part of the value, so never split it and never merge across it. Note that
 * {@link #equals} is <em>not</em> overridden -- identity comparison is the point, because two
 * annotations with equal contents are still two different annotations.
 *
 * @implNote Complete: the JDK class is these three members and nothing else.
 */
public class Annotation {

    private Object value;

    /**
     * Wraps the given value.
     *
     * @param value the attribute value, which may be {@code null}
     */
    public Annotation(Object value) {
        this.value = value;
    }

    /**
     * Returns the wrapped value.
     *
     * @return the value passed to the constructor
     */
    public Object getValue() {
        return this.value;
    }

    /**
     * Returns a debugging string naming the wrapped value.
     *
     * @return a string of the form {@code java.text.Annotation[value=...]}
     */
    public String toString() {
        return "java.text.Annotation[value=" + String.valueOf(this.value) + "]";
    }
}
