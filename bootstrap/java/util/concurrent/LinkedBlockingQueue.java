package java.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// Minimal java.util.concurrent.LinkedBlockingQueue — an optionally-bounded FIFO blocking queue over
// a linked list, with the classic **two-lock** design that sets it apart from ArrayBlockingQueue:
// a `putLock` guards the tail (enqueue) and a separate `takeLock` guards the head (dequeue), so a
// producer and a consumer proceed **in parallel** without contending. The shared size is an
// `AtomicInteger` (the cross-lock coordination point, and the happens-before between a linked node
// and the taker that reads it). A dummy head node means head and tail never alias except when empty.
// `notEmpty`/`notFull` block/wake takers/putters. Default capacity is effectively unbounded.
public class LinkedBlockingQueue<E> {
    private static final int UNBOUNDED = 2147483647; // Integer.MAX_VALUE (not defined in our Integer)

    private static final class Node {
        Object item;
        Node next;

        Node(Object item) {
            this.item = item;
        }
    }

    private final int capacity;
    private final AtomicInteger count = new AtomicInteger(0);

    private Node head; // dummy; head.next is the first real element
    private Node tail; // tail.next is null

    private final ReentrantLock takeLock = new ReentrantLock();
    private final Condition notEmpty = takeLock.newCondition();
    private final ReentrantLock putLock = new ReentrantLock();
    private final Condition notFull = putLock.newCondition();

    public LinkedBlockingQueue() {
        this(UNBOUNDED);
    }

    public LinkedBlockingQueue(int capacity) {
        this.capacity = capacity;
        Node dummy = new Node(null);
        this.head = dummy;
        this.tail = dummy;
    }

    private void signalNotEmpty() {
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    private void signalNotFull() {
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    public void put(E e) throws InterruptedException {
        int c;
        Node node = new Node(e);
        putLock.lock();
        try {
            while (count.get() == capacity) {
                notFull.await();
            }
            tail.next = node; // link at the tail
            tail = node;
            c = count.getAndIncrement();
            if (c + 1 < capacity) {
                notFull.signal(); // room remains → wake another waiting putter
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0) {
            signalNotEmpty(); // the queue was empty → a taker may be waiting
        }
    }

    @SuppressWarnings("unchecked")
    public E take() throws InterruptedException {
        E x;
        int c;
        takeLock.lock();
        try {
            while (count.get() == 0) {
                notEmpty.await();
            }
            Node first = head.next; // unlink the first real node; it becomes the new dummy
            head.next = head;       // self-link the old dummy to help GC
            head = first;
            x = (E) first.item;
            first.item = null;
            c = count.getAndDecrement();
            if (c > 1) {
                notEmpty.signal(); // more items remain → wake another waiting taker
            }
        } finally {
            takeLock.unlock();
        }
        if (c == capacity) {
            signalNotFull(); // the queue was full → a putter may be waiting
        }
        return x;
    }

    public int size() {
        return count.get();
    }
}
