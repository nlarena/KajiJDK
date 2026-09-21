package java.lang;

// Through an import and a simple name: qualifying the type at the use site does not resolve
// from java.lang (finding #210).
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import java.util.Iterator;

// KajiLibrary's java.lang.Iterable — anything that can hand out an Iterator over its
// elements. Implementing it is what makes a type usable in a for-each loop.
public interface Iterable<T> {

    Iterator<T> iterator();

    /**
     * It runs `action` over each element, in the order the iterator gives them.
     *
     * <p>It was missing, and it was the only public member of `Iterable` that was not there: without
     * it, `forEach` existed on NO collection in the library -- everything that is `Iterable` inherits
     * it, which is half of `java.util`. `LinkedBlockingDeque` and `LinkedTransferQueue` declared it
     * on their own, and were overriding nothing (#291).
     *
     * <p>It goes in as a `default` and not as abstract for the usual reason: declaring it abstract
     * would force writing it in every implementor, and the body would be this very one.
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
     * <p>With no size and no order: an `Iterable` promises neither, and this default exists so every
     * collection has a spliterator even when it knows nothing about itself.
     */
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(this.iterator(), 0);
    }

}
