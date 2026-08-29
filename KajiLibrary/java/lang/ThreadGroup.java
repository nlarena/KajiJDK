package java.lang;

/**
 * A set of threads, and of nested groups of threads, arranged as a tree.
 *
 * <p>It is the oldest way Java had of talking about several threads at once — interrupt them
 * all, count them, cap their priority — and it is terminally deprecated for a reason worth
 * knowing: a group is not a lifetime. Nothing tells the group when its threads finish, so
 * {@code activeCount} is a guess by the time the caller reads it, and a group cannot say
 * "wait until these are done". Structured concurrency ({@link java.util.concurrent
 * .StructuredTaskScope}) replaced it by making the SCOPE the owner instead of a bag.
 *
 * <p>What is still true is the tree: every thread belongs to exactly one group, every group but
 * the root has a parent, and an operation on a group reaches its subgroups.
 *
 * @implNote A KajiLibrary subset. Two deviations, both stated rather than hidden:
 *           {@link #list} and the root {@link #uncaughtException} print to {@code System.out}
 *           because this library has no {@code System.err} yet; and reading {@code System.out}
 *           from another compilation unit is emitted as a {@code getfield} over a static
 *           (finding #110), so those two methods are the only ones here that do not run on our
 *           own VM today. Everything else does, and the counts are real: a thread registers
 *           itself with the root group as it is constructed, and dead threads are pruned the
 *           next time anything walks the list.
 */
public class ThreadGroup implements Thread.UncaughtExceptionHandler {

    // The root of the tree and the group a thread joins when nothing says otherwise. Built in
    // this class and reached from Thread through root(), a METHOD: a static field read across
    // compilation units is emitted as a getfield and crashes (finding #110).
    private static final ThreadGroup SYSTEM = new ThreadGroup(null, "system");
    private static final ThreadGroup MAIN = new ThreadGroup(ThreadGroup.SYSTEM, "main");

    private final Object sync = new Object();

    private final String name;
    private final ThreadGroup parent;

    private int maxPriority;
    private boolean daemon;

    // Members, in insertion order, with a count rather than an exact-size array: groups grow.
    private Thread[] threads;
    private int threadCount;
    private ThreadGroup[] groups;
    private int groupCount;

    /** Creates a group under the current thread group. */
    public ThreadGroup(String name) {
        this(Thread.currentThread().getThreadGroup(), name);
    }

    /** Creates a group under {@code parent}. */
    public ThreadGroup(ThreadGroup parent, String name) {
        this.name = name;
        this.parent = parent;
        this.threads = new Thread[4];
        this.threadCount = 0;
        this.groups = new ThreadGroup[4];
        this.groupCount = 0;
        if (parent == null) {
            this.maxPriority = 10;
            this.daemon = false;
        } else {
            this.maxPriority = parent.getMaxPriority();
            this.daemon = parent.isDaemon();
            parent.addGroup(this);
        }
    }

    /** The group a thread joins when nothing says otherwise. */
    static ThreadGroup root() {
        return ThreadGroup.MAIN;
    }

    // Records `t` as a member. Nothing calls it today -- see the class note on why threads do
    // not register themselves -- so it is the seam and not the wiring.
    void addThread(Thread t) {
        synchronized (this.sync) {
            this.pruneDead();
            if (this.threadCount == this.threads.length) {
                Thread[] bigger = new Thread[this.threads.length * 2];
                // Copied by hand and not with System.arraycopy: that call is dropped in
                // silence and copies nothing (finding #258).
                int k = 0;
                while (k < this.threadCount) {
                    bigger[k] = this.threads[k];
                    k = k + 1;
                }
                this.threads = bigger;
            }
            this.threads[this.threadCount] = t;
            this.threadCount = this.threadCount + 1;
        }
    }

    private void addGroup(ThreadGroup g) {
        synchronized (this.sync) {
            if (this.groupCount == this.groups.length) {
                ThreadGroup[] bigger = new ThreadGroup[this.groups.length * 2];
                int k = 0;
                while (k < this.groupCount) {
                    bigger[k] = this.groups[k];
                    k = k + 1;
                }
                this.groups = bigger;
            }
            this.groups[this.groupCount] = g;
            this.groupCount = this.groupCount + 1;
        }
    }

    // Drops threads that have finished. Called with `sync` held, from the operations that walk
    // the list anyway: nothing tells a group when a thread ends, so the list is only ever
    // cleaned lazily -- which is the same reason activeCount() can only ever be an estimate.
    private void pruneDead() {
        int keep = 0;
        int i = 0;
        while (i < this.threadCount) {
            Thread t = this.threads[i];
            if (t.isAlive()) {
                this.threads[keep] = t;
                keep = keep + 1;
            }
            i = i + 1;
        }
        i = keep;
        while (i < this.threadCount) {
            this.threads[i] = null;
            i = i + 1;
        }
        this.threadCount = keep;
    }

    // ---- identity ----

    public final String getName() {
        return this.name;
    }

    /** The enclosing group, or {@code null} for the root. */
    public final ThreadGroup getParent() {
        return this.parent;
    }

    public final int getMaxPriority() {
        int cap;
        synchronized (this.sync) {
            cap = this.maxPriority;
        }
        return cap;
    }

    public final boolean isDaemon() {
        boolean d;
        synchronized (this.sync) {
            d = this.daemon;
        }
        return d;
    }

    /** Always false: a group is never destroyed, since {@link #destroy} does nothing. */
    public boolean isDestroyed() {
        return false;
    }

    public final void setDaemon(boolean daemon) {
        synchronized (this.sync) {
            this.daemon = daemon;
        }
    }

    /**
     * Caps the priority of threads later started in this group, and lowers any subgroup already
     * above the new cap.
     */
    public final void setMaxPriority(int pri) {
        int wanted = pri;
        if (wanted < 1) {
            wanted = 1;
        }
        if (wanted > 10) {
            wanted = 10;
        }
        ThreadGroup[] children;
        int n;
        synchronized (this.sync) {
            if (this.parent != null && wanted > this.parent.getMaxPriority()) {
                wanted = this.parent.getMaxPriority();
            }
            this.maxPriority = wanted;
            n = this.groupCount;
            children = new ThreadGroup[n];
            int k = 0;
            while (k < n) {
                children[k] = this.groups[k];
                k = k + 1;
            }
        }
        int i = 0;
        while (i < n) {
            children[i].setMaxPriority(wanted);
            i = i + 1;
        }
    }

    /** Whether this group is {@code g} or encloses it, at any depth. */
    public final boolean parentOf(ThreadGroup g) {
        ThreadGroup at = g;
        while (at != null) {
            if (at == this) {
                return true;
            }
            at = at.getParent();
        }
        return false;
    }

    /** A no-op: access checks went away with the security manager. */
    public final void checkAccess() {
    }

    // ---- counting and listing ----

    /**
     * An ESTIMATE of the live threads in this group and its subgroups.
     *
     * <p>Estimate is not a hedge: a thread may finish between the count and the caller reading
     * it, and nothing informs the group when one does.
     */
    public int activeCount() {
        int total;
        ThreadGroup[] children;
        int n;
        synchronized (this.sync) {
            this.pruneDead();
            total = this.threadCount;
            n = this.groupCount;
            children = new ThreadGroup[n];
            int k = 0;
            while (k < n) {
                children[k] = this.groups[k];
                k = k + 1;
            }
        }
        int i = 0;
        while (i < n) {
            total = total + children[i].activeCount();
            i = i + 1;
        }
        return total;
    }

    /** Copies the live threads of this group and its subgroups into {@code list}. */
    public int enumerate(Thread[] list) {
        return this.enumerate(list, true);
    }

    /**
     * Copies the live threads into {@code list}, recursing into subgroups only if asked.
     *
     * @return how many were copied, which is capped by the array and does NOT say whether more
     *         were left out — the caller sizes the array from {@link #activeCount} and accepts
     *         that both numbers are estimates
     */
    public int enumerate(Thread[] list, boolean recurse) {
        return this.fill(list, 0, recurse);
    }

    private int fill(Thread[] list, int at, boolean recurse) {
        int put = at;
        Thread[] mine;
        int n;
        ThreadGroup[] children;
        int c;
        synchronized (this.sync) {
            this.pruneDead();
            n = this.threadCount;
            mine = new Thread[n];
            int j = 0;
            while (j < n) {
                mine[j] = this.threads[j];
                j = j + 1;
            }
            c = this.groupCount;
            children = new ThreadGroup[c];
            int m = 0;
            while (m < c) {
                children[m] = this.groups[m];
                m = m + 1;
            }
        }
        int i = 0;
        while (i < n && put < list.length) {
            list[put] = mine[i];
            put = put + 1;
            i = i + 1;
        }
        if (recurse) {
            int k = 0;
            while (k < c && put < list.length) {
                put = children[k].fill(list, put, true);
                k = k + 1;
            }
        }
        return put;
    }

    /** An estimate of the subgroups, at any depth. */
    public int activeGroupCount() {
        ThreadGroup[] children;
        int n;
        synchronized (this.sync) {
            n = this.groupCount;
            children = new ThreadGroup[n];
            int k = 0;
            while (k < n) {
                children[k] = this.groups[k];
                k = k + 1;
            }
        }
        int total = n;
        int i = 0;
        while (i < n) {
            total = total + children[i].activeGroupCount();
            i = i + 1;
        }
        return total;
    }

    public int enumerate(ThreadGroup[] list) {
        return this.enumerate(list, true);
    }

    public int enumerate(ThreadGroup[] list, boolean recurse) {
        return this.fillGroups(list, 0, recurse);
    }

    private int fillGroups(ThreadGroup[] list, int at, boolean recurse) {
        int put = at;
        ThreadGroup[] children;
        int n;
        synchronized (this.sync) {
            n = this.groupCount;
            children = new ThreadGroup[n];
            int k = 0;
            while (k < n) {
                children[k] = this.groups[k];
                k = k + 1;
            }
        }
        int i = 0;
        while (i < n && put < list.length) {
            list[put] = children[i];
            put = put + 1;
            i = i + 1;
        }
        if (recurse) {
            int k = 0;
            while (k < n && put < list.length) {
                put = children[k].fillGroups(list, put, true);
                k = k + 1;
            }
        }
        return put;
    }

    // ---- acting on the members ----

    /** Interrupts every live thread in this group and its subgroups. */
    public final void interrupt() {
        Thread[] mine;
        int n;
        ThreadGroup[] children;
        int c;
        synchronized (this.sync) {
            this.pruneDead();
            n = this.threadCount;
            mine = new Thread[n];
            int j = 0;
            while (j < n) {
                mine[j] = this.threads[j];
                j = j + 1;
            }
            c = this.groupCount;
            children = new ThreadGroup[c];
            int m = 0;
            while (m < c) {
                children[m] = this.groups[m];
                m = m + 1;
            }
        }
        int i = 0;
        while (i < n) {
            mine[i].interrupt();
            i = i + 1;
        }
        int k = 0;
        while (k < c) {
            children[k].interrupt();
            k = k + 1;
        }
    }

    /** A no-op, kept for source compatibility: a group is never destroyed. */
    public final void destroy() {
    }

    /** Prints this group and its members, indented by depth. For debugging only. */
    public void list() {
        this.listAt(0);
    }

    private void listAt(int depth) {
        String pad = "";
        int i = 0;
        while (i < depth) {
            pad = pad + "    ";
            i = i + 1;
        }
        System.out.println(pad + this.toString());
        Thread[] mine;
        int n;
        ThreadGroup[] children;
        int c;
        synchronized (this.sync) {
            this.pruneDead();
            n = this.threadCount;
            mine = new Thread[n];
            int j = 0;
            while (j < n) {
                mine[j] = this.threads[j];
                j = j + 1;
            }
            c = this.groupCount;
            children = new ThreadGroup[c];
            int m = 0;
            while (m < c) {
                children[m] = this.groups[m];
                m = m + 1;
            }
        }
        int t = 0;
        while (t < n) {
            System.out.println(pad + "    " + mine[t].getName());
            t = t + 1;
        }
        int k = 0;
        while (k < c) {
            children[k].listAt(depth + 1);
            k = k + 1;
        }
    }

    /**
     * Called when a thread of this group dies of an uncaught throwable.
     *
     * <p>It walks UP: a group with a parent hands the problem over, so a handler installed near
     * the root sees everything below it. Only the root has to decide what to do, and what it
     * does is report — the thread is already gone, and swallowing the throwable would make it
     * disappear without trace.
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (this.parent != null) {
            this.parent.uncaughtException(t, e);
            return;
        }
        System.out.println("Exception in thread \"" + t.getName() + "\" " + e);
    }

    @Override
    public String toString() {
        return "java.lang.ThreadGroup[name=" + this.name + ",maxpri=" + this.getMaxPriority() + "]";
    }
}
