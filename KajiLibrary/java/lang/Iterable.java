package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve
// desde java.lang (finding #210).
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import java.util.Iterator;

// KajiLibrary's java.lang.Iterable — anything that can hand out an Iterator over its
// elements. Implementing it is what makes a type usable in a for-each loop.
public interface Iterable<T> {

    Iterator<T> iterator();

    /**
     * Perform {@code action} for each element, in iteration order, until all are done or it throws.
     */
    default void forEach(Consumer<? super T> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        for (T element : this) {
            action.accept(element);
        }
    }

    /**
     * A spliterator over these elements.
     *
     * <p>Sin tamano y sin orden: un `Iterable` no promete ninguna de las dos cosas, y este default
     * existe para que toda coleccion tenga un spliterator aunque no sepa nada de si misma.
     */
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), 0);
    }

}
