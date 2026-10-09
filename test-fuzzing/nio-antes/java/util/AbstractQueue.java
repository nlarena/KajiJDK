package java.util;

// The skeleton for queues. Its whole job is to derive the *throwing* half of the queue API from
// the *sentinel* half: a subclass writes `offer`/`poll`/`peek`, and `add`/`remove`/`element`
// come out of them by turning a false or a null into an exception. That pairing is the one
// design idea in java.util.Queue.
public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {

    protected AbstractQueue() {
    }

    // Insert, or fail loudly where offer would merely say no.
    public boolean add(E e) {
        if (!offer(e)) {
            throw new IllegalStateException("Queue full");
        }
        return true;
    }

    // Remove the head, or fail loudly where poll would return null.
    public E remove() {
        E e = poll();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    // Inspect the head, or fail loudly where peek would return null.
    public E element() {
        E e = peek();
        if (e == null) {
            throw new NoSuchElementException();
        }
        return e;
    }

    public void clear() {
        while (poll() != null) {
        }
    }
}
