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

    /**
     * It takes out the head, or **throws** if the queue is empty.
     *
     * <p>It is `poll()`'s partner, and the difference is the whole reason both exist: `poll` returns
     * null because empty is an expected result --it is used in a loop that consumes until exhausted--
     * and `remove` throws because empty there is an error --it is used when the caller already knows
     * there is something. Choosing the wrong one turns a bug into a `null` that travels.
     */
    default E remove() {
        E e = this.poll();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    // `peek()`'s partner, with the same distinction.
    default E element() {
        E e = this.peek();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }
}
