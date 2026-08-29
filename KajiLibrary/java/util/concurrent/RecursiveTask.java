package java.util.concurrent;

// A fork/join task that returns a value — the summing, counting, searching half of divide
// and conquer. Like {@link RecursiveAction} it fixes {@link ForkJoinTask}'s three abstract
// members so that a subclass writes only `compute`; unlike it, there is a slot to keep what
// compute() produced, which `join` then hands back.
public abstract class RecursiveTask<V> extends ForkJoinTask<V> {

    // Where compute()'s answer lands. Not volatile: it is written before the task is marked
    // done and read after, and the completion monitor orders the two.
    V result;

    public RecursiveTask() {
    }

    // The work. Fork subtasks, join them, combine — and return the combination.
    protected abstract V compute();

    public final V getRawResult() {
        return result;
    }

    protected final void setRawResult(V value) {
        result = value;
    }

    protected final boolean exec() {
        result = compute();
        return true;
    }
}
