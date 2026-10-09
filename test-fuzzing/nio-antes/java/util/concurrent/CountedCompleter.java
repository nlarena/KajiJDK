package java.util.concurrent;

// A fork/join task that completes when its *children* do, rather than when its own compute()
// returns. That inversion is the whole idea. A RecursiveTask forks subtasks and then blocks
// in `join` waiting for them, which costs a thread per level of the recursion; a
// CountedCompleter forks subtasks and returns, and the last child to finish is the one that
// completes the parent. Nobody blocks, so the tree can be arbitrarily deep.
//
// The bookkeeping is one integer. Before forking n children a task sets its pending count to
// n; every child, on finishing, calls `tryComplete` on its completer, which decrements the
// count and stops — unless the count was already zero, in which case this child was the last
// one, `onCompletion` runs, and the same step repeats one level up. The count is therefore
// "children still outstanding", and reaching zero is the signal to propagate.
//
// The subtlety worth naming: `tryComplete` decrements *unless* the count is zero, and it is
// the zero case that completes. A task with no children starts at zero and completes on its
// first tryComplete, which is why leaves need no special handling.
//
// `firstComplete`/`nextComplete` are the same protocol turned inside out, for callers that
// want to walk up the tree themselves rather than let it unwind — they return the task to
// continue at, or null when another child is still outstanding.
public abstract class CountedCompleter<T> extends ForkJoinTask<T> {

    // The parent to notify. Final: the shape of the completion tree is fixed when a task is
    // created, and a task that could change completers could be counted twice.
    final CountedCompleter<?> completer;
    private final Object countLock = new Object();
    private int pending;

    protected CountedCompleter(CountedCompleter<?> completer, int initialPendingCount) {
        this.completer = completer;
        this.pending = initialPendingCount;
    }

    protected CountedCompleter(CountedCompleter<?> completer) {
        this.completer = completer;
    }

    protected CountedCompleter() {
        this.completer = null;
    }

    // The work. Typically: set a pending count, fork children, and return — without joining.
    public abstract void compute();

    // Runs once, on the task that is being completed, when the last child arrives. The hook
    // where a parent folds its children's results into its own.
    public void onCompletion(CountedCompleter<?> caller) {
    }

    // Runs on the way up when a task ended abnormally. Returning true lets the failure keep
    // propagating to the completer; returning false stops it there.
    public boolean onExceptionalCompletion(Throwable ex, CountedCompleter<?> caller) {
        return true;
    }

    public final CountedCompleter<?> getCompleter() {
        return completer;
    }

    // ---------------------------------------------------------------- the count

    public final int getPendingCount() {
        int n;
        synchronized (countLock) {
            n = pending;
        }
        return n;
    }

    public final void setPendingCount(int count) {
        synchronized (countLock) {
            pending = count;
        }
    }

    public final void addToPendingCount(int delta) {
        synchronized (countLock) {
            pending = pending + delta;
        }
    }

    public final boolean compareAndSetPendingCount(int expected, int count) {
        boolean swapped = false;
        synchronized (countLock) {
            if (pending == expected) {
                pending = count;
                swapped = true;
            }
        }
        return swapped;
    }

    // Decrement, but never below zero — and say what the count was. Zero means "you are the
    // last one", which is the answer every caller of this actually wants.
    public final int decrementPendingCountUnlessZero() {
        int previous;
        synchronized (countLock) {
            previous = pending;
            if (previous != 0) {
                pending = previous - 1;
            }
        }
        return previous;
    }

    // The task at the top of this completion tree.
    public final CountedCompleter<?> getRoot() {
        CountedCompleter<?> node = this;
        CountedCompleter<?> parent = node.getCompleter();
        while (parent != null) {
            node = parent;
            parent = node.getCompleter();
        }
        return node;
    }

    // ---------------------------------------------------------------- propagation

    // One child has finished. If others are still outstanding, just count it; if this was
    // the last, run onCompletion, complete this task, and repeat at the completer. Written
    // as a loop rather than a recursion, so a deep tree cannot overflow the stack.
    public final void tryComplete() {
        CountedCompleter<?> node = this;
        CountedCompleter<?> caller = this;
        boolean climbing = true;
        while (climbing) {
            if (node.decrementPendingCountUnlessZero() != 0) {
                climbing = false;
            } else {
                node.onCompletion(caller);
                node.quietlyComplete();
                CountedCompleter<?> parent = node.getCompleter();
                if (parent == null) {
                    climbing = false;
                } else {
                    caller = node;
                    node = parent;
                }
            }
        }
    }

    // Same walk, but without running onCompletion along the way — for callers that have
    // already done whatever folding they needed.
    public final void propagateCompletion() {
        CountedCompleter<?> node = this;
        boolean climbing = true;
        while (climbing) {
            if (node.decrementPendingCountUnlessZero() != 0) {
                climbing = false;
            } else {
                node.quietlyComplete();
                CountedCompleter<?> parent = node.getCompleter();
                if (parent == null) {
                    climbing = false;
                } else {
                    node = parent;
                }
            }
        }
    }

    // Complete with an explicit value, ignoring the count: a short circuit, for a search that
    // has found its answer and does not care about the children still running.
    public void complete(T rawResult) {
        setRawResult(rawResult);
        onCompletion(this);
        quietlyComplete();
        CountedCompleter<?> parent = getCompleter();
        if (parent != null) {
            parent.tryComplete();
        }
    }

    // The manual form of the same protocol. Returns this task if the caller is the last
    // child — meaning it may proceed up — and null if others are still outstanding.
    public final CountedCompleter<?> firstComplete() {
        CountedCompleter<?> next = null;
        if (decrementPendingCountUnlessZero() == 0) {
            next = this;
        }
        return next;
    }

    // Continue the manual walk: complete this task and hand back the completer to visit
    // next, or null when the walk is over or another child is still outstanding.
    public final CountedCompleter<?> nextComplete() {
        CountedCompleter<?> parent = getCompleter();
        CountedCompleter<?> next = null;
        if (parent == null) {
            quietlyCompleteRoot();
        } else {
            next = parent.firstComplete();
        }
        return next;
    }

    public final void quietlyCompleteRoot() {
        CountedCompleter<?> root = getRoot();
        root.quietlyComplete();
    }

    // Run pending work on behalf of this tree. With no local task queues to raid, there is
    // nothing to help with; the tasks are already running on threads of their own.
    public final void helpComplete(int maxTasks) {
    }

    // ---------------------------------------------------------------- ForkJoinTask contract

    // False, always: a CountedCompleter is *not* complete when compute() returns. That single
    // return value is what distinguishes it from a RecursiveTask.
    protected final boolean exec() {
        compute();
        return false;
    }

    // A CountedCompleter carries no result of its own unless a subclass adds one.
    public T getRawResult() {
        return null;
    }

    protected void setRawResult(T value) {
    }
}
