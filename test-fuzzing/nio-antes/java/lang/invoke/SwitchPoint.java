package java.lang.invoke;

// A one-way switch shared by many call sites: valid until invalidated, never valid again. The
// asymmetry is the point — a JIT can assume the fast path and compile it as if the guard were not
// there, because invalidation is a rare, global event that deoptimises everything at once.
// Optimistic assumptions in a language runtime ("nobody has overridden this method yet") are
// exactly this shape.
public class SwitchPoint {

    private boolean invalidated;

    public SwitchPoint() {
    }

    public boolean hasBeenInvalidated() {
        return invalidated;
    }

    // Picks `target` while valid and `fallback` afterwards — a handle that changes behaviour
    // without anyone re-linking the call site. Needs a handle factory, which is missing.
    public MethodHandle guardWithTest(MethodHandle target, MethodHandle fallback) {
        throw new UnsupportedOperationException("no guarded handle without a factory");
    }

    // Invalidates a whole batch at once. Doing it in one operation is not a convenience: it is
    // what lets the VM pay for a single deoptimisation instead of one per switch point.
    public static void invalidateAll(SwitchPoint[] switchPoints) {
        int i = 0;
        while (i < switchPoints.length) {
            switchPoints[i].invalidated = true;
            i = i + 1;
        }
    }
}
