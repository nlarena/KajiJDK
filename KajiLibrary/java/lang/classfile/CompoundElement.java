package java.lang.classfile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

// An element that is also a sequence of smaller elements: a class, a field, a method or a method
// body. `forEach` is the primitive operation --what a model knows how to do is *walk itself*-- and
// everything else comes out of that.
public interface CompoundElement<E extends ClassFileElement> extends ClassFileElement, Iterable<E> {

    /** It hands each piece to `consumer`, in file order. */
    void forEach(Consumer<? super E> consumer);

    /** An iterator over the pieces. */
    default Iterator<E> iterator() {
        return elementList().iterator();
    }

    /** The pieces as a stream. */
    default Stream<E> elementStream() {
        return elementList().stream();
    }

    /** The pieces as a list. */
    default List<E> elementList() {
        Colector<E> c = new Colector<E>();
        forEach(c);
        return c.list;
    }

    /** A textual view of the pieces, one per line. It is for debugging; the format is not stable. */
    default String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.toString()).append('\n');
        List<E> pieces = elementList();
        for (int i = 0; i < pieces.size(); i++) {
            sb.append("  ").append(String.valueOf(pieces.get(i))).append('\n');
        }
        return sb.toString();
    }
}

// `elementList`'s accumulator. It is a named class and not an anonymous one because an interface's
// `default` is the last place where depending on variable capture is a good idea.
final class Colector<E> implements Consumer<E> {

    final List<E> list = new ArrayList<E>();

    public void accept(E e) {
        this.list.add(e);
    }
}
