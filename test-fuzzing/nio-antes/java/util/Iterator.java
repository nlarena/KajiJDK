package java.util;

// KajiLibrary's java.util.Iterator — a one-way cursor over a sequence: `hasNext()` asks
// whether another element remains, `next()` returns it and advances.
public interface Iterator<E> {

    boolean hasNext();

    E next();

    /**
     * Removes the element the last {@link #next()} returned.
     *
     * <p>The default refuses, which is why it is a default: an iterator over something immutable
     * has nothing to implement, and before Java 8 every one of them had to write this method to
     * say so.
     *
     * @throws UnsupportedOperationException unless the implementation overrides it
     */
    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * Feeds every remaining element to {@code action}.
     *
     * @param action what to do with each
     */
    default void forEachRemaining(java.util.function.Consumer<? super E> action) {
        while (this.hasNext()) {
            action.accept(this.next());
        }
    }

}
