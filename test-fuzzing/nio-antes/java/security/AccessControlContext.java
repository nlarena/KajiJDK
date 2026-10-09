package java.security;

// The photograph of the protection domains in play at a given moment.
//
// The idea it encodes is that of the **least privilege of the stack**: an operation is allowed only
// if **every** domain of the chain of calls allows it. One of them not having it is enough for it
// to be denied, and that is why trusted code called from untrusted code cannot do more than the
// caller could — which is what prevents the confused deputy attack.
//
// Keeping the context in an object allows it to be recovered afterwards: a job that is queued and
// run in another thread runs with the context of whoever queued it, not with that of the thread
// that takes it out of the queue, which could be much more privileged.
//
// **`checkPermission` always throws.** Just like `Permission.checkGuard` and for the same reason:
// with no `SecurityManager` there is nobody to answer, and JDK 25 answers by denying
// —`AccessControlException: checking permissions is not supported`— instead of letting it through.
// Checked against the real JDK.
@Deprecated
public final class AccessControlContext {

    private final ProtectionDomain[] context;

    private final DomainCombiner combiner;

    // The context made up of those domains.
    //
    // The nulls and the repeated ones are filtered out: a domain twice does not restrict more than
    // once, and leaving them would make two equivalent contexts not be equal.
    public AccessControlContext(ProtectionDomain[] context) {
        if (context == null) {
            throw new NullPointerException("null context");
        }
        java.util.ArrayList<ProtectionDomain> unique =
            new java.util.ArrayList<ProtectionDomain>();
        int i = 0;
        while (i < context.length) {
            ProtectionDomain pd = context[i];
            if (pd != null && !unique.contains(pd)) {
                unique.add(pd);
            }
            i = i + 1;
        }
        this.context = new ProtectionDomain[unique.size()];
        int j = 0;
        while (j < unique.size()) {
            this.context[j] = unique.get(j);
            j = j + 1;
        }
        this.combiner = null;
    }

    // The same context, but with a domain combiner associated.
    public AccessControlContext(AccessControlContext acc, DomainCombiner combiner) {
        if (acc == null) {
            throw new NullPointerException("null context");
        }
        this.context = acc.context;
        this.combiner = combiner;
    }

    public DomainCombiner getDomainCombiner() {
        return this.combiner;
    }

    // It always throws. See the header.
    public void checkPermission(Permission perm) throws AccessControlException {
        if (perm == null) {
            throw new NullPointerException("permission can't be null");
        }
        throw new AccessControlException("checking permissions is not supported", perm);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AccessControlContext)) {
            return false;
        }
        AccessControlContext that = (AccessControlContext) obj;
        if (this.combiner == null) {
            if (that.combiner != null) {
                return false;
            }
        } else if (!this.combiner.equals(that.combiner)) {
            return false;
        }
        if (this.context.length != that.context.length) {
            return false;
        }
        // Comparison as a set, not as a list: the order of the stack does not change which
        // permissions result from the intersection.
        int i = 0;
        while (i < this.context.length) {
            boolean hallado = false;
            int j = 0;
            while (j < that.context.length) {
                if (this.context[i].equals(that.context[j])) {
                    hallado = true;
                    j = that.context.length;
                } else {
                    j = j + 1;
                }
            }
            if (!hallado) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        int i = 0;
        while (i < this.context.length) {
            h = h + this.context[i].hashCode();
            i = i + 1;
        }
        return h;
    }
}
