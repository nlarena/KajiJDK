package java.util;

// The original iteration protocol, from Java 1.0, kept alive by the legacy classes that still
// hand one out (Vector, Hashtable). {@link Iterator} replaced it: same job, shorter names, and
// the ability to remove.
public interface Enumeration<E> {

    boolean hasMoreElements();

    E nextElement();

    /**
     * This same enumeration seen as an {@link Iterator}.
     *
     * <p>It is the bridge that was missing on the old interface's side: `Collections.enumeration`
     * goes from Iterator to Enumeration, and this one comes back. Without it, any pre-1.2 API that
     * returns an Enumeration is left out of the for-each and of the streams.
     *
     * <p>The Iterator that comes out does **not** support `remove()`: an Enumeration has nothing to
     * do it with.
     */
    default Iterator<E> asIterator() {
        return new EnumerationItr<E>(this);
    }
}

// `asIterator`'s adapter. Package-private, and top-level rather than nested because of the
// miscompilation of a nested class inside a generic one (#13).
final class EnumerationItr<E> implements Iterator<E> {

    private final Enumeration<E> e;

    EnumerationItr(Enumeration<E> e) {
        this.e = e;
    }

    public boolean hasNext() {
        return this.e.hasMoreElements();
    }

    public E next() {
        return this.e.nextElement();
    }
}
