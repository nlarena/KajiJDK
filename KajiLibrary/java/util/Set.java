package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Collection;

// KajiLibrary's java.util.Set<E> — a Collection with no duplicate elements. It adds no new
// operations over Collection; the difference is the uniqueness contract (add returns false
// when the element is already present). Concrete: HashSet.
public interface Set<E> extends Collection<E> {
    /**
     * A spliterator over these elements.
     *
     *  <p>DISTINCT, que es lo que hace a un conjunto un conjunto. Un consumidor que deduplica puede
     * no hacer nada al ver esta caracteristica, y ese ahorro es todo el punto.
     *
     */
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT);
    }

}
