package java.util.concurrent;

import java.util.Deque;
import java.util.Iterator;

// A deque whose ends both block: takers wait for an element to appear, putters wait for
// room to appear. It is the intersection of {@link BlockingQueue} and {@link Deque}, and
// every single-ended operation it inherits from BlockingQueue is defined as the
// corresponding *First* operation — `take()` is `takeFirst()`, `put(e)` is `putLast(e)` —
// so a BlockingDeque drops into any place that wanted a FIFO blocking queue.
//
// The redeclarations of the inherited BlockingQueue and Deque methods are what the JDK
// declares too: they are there to carry the deque-specific documentation, and they change
// no descriptor.
//
// No `throws InterruptedException` anywhere, though the blocking methods do interrupt:
// re-stating a superinterface's throws clause against an already-compiled BlockingQueue is
// rejected (finding #104). The erased descriptors are identical either way.
public interface BlockingDeque<E> extends BlockingQueue<E>, Deque<E> {

    // --- head-specific ---

    // Insert at the head, throwing IllegalStateException if the deque is full.
    void addFirst(E e);

    // Insert at the head if there is room right now.
    boolean offerFirst(E e);

    // Insert at the head, waiting for room.
    void putFirst(E e);

    // Insert at the head, waiting up to the timeout for room.
    boolean offerFirst(E e, long timeout, TimeUnit unit);

    // Remove and return the head, waiting for an element.
    E takeFirst();

    // Remove and return the head, waiting up to the timeout.
    E pollFirst(long timeout, TimeUnit unit);

    // --- tail-specific ---

    void addLast(E e);

    boolean offerLast(E e);

    void putLast(E e);

    boolean offerLast(E e, long timeout, TimeUnit unit);

    E takeLast();

    E pollLast(long timeout, TimeUnit unit);

    // --- occurrence removal, from either end ---

    boolean removeFirstOccurrence(Object o);

    boolean removeLastOccurrence(Object o);

    // --- BlockingQueue view: everything below acts on the head, insertions on the tail ---

    boolean add(E e);

    boolean offer(E e);

    void put(E e);

    boolean offer(E e, long timeout, TimeUnit unit);

    E remove();

    E poll();

    E take();

    E poll(long timeout, TimeUnit unit);

    E element();

    E peek();

    boolean remove(Object o);

    boolean contains(Object o);

    int size();

    Iterator<E> iterator();

    // --- Deque-as-stack view: pushes onto the head ---

    void push(E e);
}
