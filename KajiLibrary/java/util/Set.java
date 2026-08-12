package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Collection;

// KajiLibrary's java.util.Set<E> — a Collection with no duplicate elements. It adds no new
// operations over Collection; the difference is the uniqueness contract (add returns false
// when the element is already present). Concrete: HashSet.
public interface Set<E> extends Collection<E> {
}
