package java.util;

// A double-ended queue: elements go in and come out at *both* ends. That one property is
// why it subsumes two classic structures — use only the tail and it is a queue (FIFO), use
// only the head and it is a stack (LIFO). {@link #push}/{@link #pop} are the stack names,
// kept as aliases for addFirst/removeFirst.
//
// Each operation comes in two flavours: one that **throws** when the deque is empty or full
// (addFirst / removeFirst / getFirst) and one that returns a **sentinel** — false or null —
// instead (offerFirst / pollFirst / peekFirst). Which to use is a real choice: the throwing
// form when an empty deque means a bug, the sentinel form when it is expected.
//
// Subset: the JDK's Deque also extends SequencedCollection (Java 21) and declares addAll /
// reversed; those are omitted.
public interface Deque<E> extends Queue<E>, SequencedCollection<E> {

    // --- head/tail insertion ---

    void addFirst(E e);

    void addLast(E e);

    boolean offerFirst(E e);

    boolean offerLast(E e);

    // --- head/tail removal ---

    E removeFirst();

    E removeLast();

    E pollFirst();

    E pollLast();

    // --- head/tail inspection ---

    E getFirst();

    E getLast();

    E peekFirst();

    E peekLast();

    // Remove the first (resp. last) element equal to `o`; reports whether one was found.
    boolean removeFirstOccurrence(Object o);

    boolean removeLastOccurrence(Object o);

    // --- stack view ---

    void push(E e);

    E pop();

    // --- queue view (redeclared, as the JDK does) ---

    boolean add(E e);

    boolean offer(E e);

    E remove();

    E poll();

    E element();

    E peek();

    boolean remove(Object o);

    boolean contains(Object o);

    int size();

    Iterator<E> iterator();

    // Walks the deque from tail to head.
    Iterator<E> descendingIterator();

    /**
     * Una **vista** de esta cola doble con las dos puntas intercambiadas.
     *
     * <p>Es la mas barata de las tres vistas invertidas de la biblioteca: un `Deque` ya sabe
     * recorrerse al reves (`descendingIterator`) y ya tiene las dos puntas, asi que invertir es
     * cruzar los nombres.
     */
    default Deque<E> reversed() {
        return new ReverseDeque<E>(this);
    }
}
