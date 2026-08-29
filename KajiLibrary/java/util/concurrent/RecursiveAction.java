package java.util.concurrent;

// A fork/join task that computes but returns nothing — sorting an array in place, applying a
// transform across a range. All it does is fix {@link ForkJoinTask}'s three abstract members
// so that a subclass writes only `compute`.
//
// The result type is Void and there is nowhere to put a value, which is exactly the point:
// the work is the side effect, and the task's completion is the only thing to wait for.
public abstract class RecursiveAction extends ForkJoinTask<Void> {

    public RecursiveAction() {
    }

    // The work. Split here and fork subtasks when the problem is too big to do directly.
    protected abstract void compute();

    // Always null: there is no result to hand back.
    public final Void getRawResult() {
        return null;
    }

    // Nothing to store, so nothing to do.
    protected final void setRawResult(Void value) {
    }

    // True: a RecursiveAction is finished the moment compute() returns.
    protected final boolean exec() {
        compute();
        return true;
    }
}
