package java.util;

// KajiLibrary's java.util.Iterator — a one-way cursor over a sequence: `hasNext()` asks
// whether another element remains, `next()` returns it and advances.
public interface Iterator<E> {

    boolean hasNext();

    E next();
}
