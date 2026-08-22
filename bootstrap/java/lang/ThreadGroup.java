package java.lang;

// A minimal java.lang.ThreadGroup: a named node in a tree of groups that holds the threads
// constructed into it. Real JDKs also use groups to route uncaught exceptions, interrupt whole
// sets of threads and carry security policy; we keep the part that is pure bookkeeping — name,
// parent, membership — and the one observable it buys: activeCount().
//
// The membership list is deliberately *append-only*: a thread joins when it is constructed and is
// never removed, so the group keeps its Thread objects alive for the life of the program (a real
// JDK unlinks a thread when it dies). activeCount() therefore walks the list and counts the ones
// still alive, which lands on the JDK's own contract — "an estimate" — from the other side. No VM
// bookkeeping was added for this: the whole registry is Java, driven from Thread's constructor.
public class ThreadGroup {
    // One membership link. Hand-rolled because java.util is not part of the bootstrap image and
    // java.lang must not depend on it.
    private static final class Node {
        final Thread thread;
        final Node next;

        Node(Thread thread, Node next) {
            this.thread = thread;
            this.next = next;
        }
    }

    // The root of the tree, built on first use (see root()). Not a static initializer: a
    // <clinit> here would run while Thread's constructor is halfway through, and this class is
    // reached from exactly there.
    private static ThreadGroup root;

    private final ThreadGroup parent;
    private final String name;
    private Node members;

    // A new group under the creating thread's own group — the usual way to make one.
    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    // A new group under an explicit parent. A null parent means "this is the root", which only
    // the fabricated "main" group uses.
    public ThreadGroup(ThreadGroup parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public final ThreadGroup getParent() {
        return parent;
    }

    // An *estimate* of the live threads in this group (the JDK says the same): the count is taken
    // by walking the membership list, so it can go stale the instant it is returned. Threads in
    // subgroups are not counted — subgroups are data here, not a traversal.
    public int activeCount() {
        int count = 0;
        for (Node node = members; node != null; node = node.next) {
            if (node.thread.isAlive()) {
                count++;
            }
        }
        return count;
    }

    // Record `t` as a member. Package-private: Thread's constructor is the only caller, so every
    // thread joins exactly one group exactly once.
    void add(Thread t) {
        members = new Node(t, members);
    }

    // The root ("main") group, created on demand. The main thread's Thread object is fabricated by
    // the VM without running a constructor, so nothing else would ever build it.
    static ThreadGroup root() {
        if (root == null) {
            root = new ThreadGroup(null, "main");
        }
        return root;
    }
}
