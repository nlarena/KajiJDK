package java.util;

// Same-package import works around the frozen javac's finder (finding #4).
import java.util.Collection;

// KajiLibrary's java.util.Queue<E> — a Collection ordered for processing, typically FIFO.
// `offer` enqueues (returning false if capacity-bounded and full), `poll` dequeues the
// head (null if empty), `peek` looks at the head without removing (null if empty). A
// KajiLibrary subset (the JDK also has the throwing variants add/remove/element).
public interface Queue<E> extends Collection<E> {

    boolean offer(E e);

    E poll();

    E peek();
}
