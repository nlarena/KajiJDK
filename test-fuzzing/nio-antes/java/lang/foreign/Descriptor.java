package java.lang.foreign;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// The implementation of `FunctionDescriptor`. Package-private: it is reached through
// `FunctionDescriptor.of`.
//
// Immutable, like the layouts and for the same reason: a descriptor is shared, and an `append` that
// mutated would change the signature for everyone holding it.
final class Descriptor implements FunctionDescriptor {

    private final MemoryLayout returnLayout0;
    private final List<MemoryLayout> arguments;

    private Descriptor(MemoryLayout returnLayout0, List<MemoryLayout> arguments) {
        this.returnLayout0 = returnLayout0;
        this.arguments = arguments;
    }

    static FunctionDescriptor create(MemoryLayout returnLayout0, MemoryLayout[] arguments) {
        return new Descriptor(returnLayout0, asList(arguments));
    }

    private static List<MemoryLayout> asList(MemoryLayout[] ls) {
        if (ls == null) {
            throw new IllegalArgumentException("the arguments cannot be null");
        }
        List<MemoryLayout> out = new ArrayList<MemoryLayout>();
        int i = 0;
        while (i < ls.length) {
            if (ls[i] == null) {
                throw new IllegalArgumentException("an argument is null");
            }
            out.add(ls[i]);
            i = i + 1;
        }
        return out;
    }

    public Optional<MemoryLayout> returnLayout() {
        return Optional.ofNullable(this.returnLayout0);
    }

    public List<MemoryLayout> argumentLayouts() {
        return Collections.unmodifiableList(this.arguments);
    }

    public FunctionDescriptor changeReturnLayout(MemoryLayout newReturn) {
        if (newReturn == null) {
            throw new IllegalArgumentException("the return cannot be null; use dropReturnLayout");
        }
        return new Descriptor(newReturn, this.arguments);
    }

    public FunctionDescriptor dropReturnLayout() {
        return new Descriptor(null, this.arguments);
    }

    public FunctionDescriptor appendArgumentLayouts(MemoryLayout... addedLayouts) {
        return this.insertArgumentLayouts(this.arguments.size(), addedLayouts);
    }

    public FunctionDescriptor insertArgumentLayouts(int index, MemoryLayout... addedLayouts) {
        if (index < 0 || index > this.arguments.size()) {
            throw new IllegalArgumentException("position out of range: " + index);
        }
        List<MemoryLayout> updated = new ArrayList<MemoryLayout>(this.arguments);
        updated.addAll(index, asList(addedLayouts));
        return new Descriptor(this.returnLayout0, updated);
    }

    public MethodType toMethodType() {
        Class<?> ret = this.returnLayout0 == null ? Void.TYPE : carrierOf(this.returnLayout0);
        Class<?>[] params = new Class<?>[this.arguments.size()];
        int i = 0;
        while (i < this.arguments.size()) {
            params[i] = carrierOf(this.arguments.get(i));
            i = i + 1;
        }
        return MethodType.methodType(ret, params);
    }

    // A composite layout has no Java type of its own: a struct "is" neither an `int` nor a
    // `MemorySegment` in the method's signature -- the linker decides how to pass it according to the
    // calling convention, and that decision does not live here. Rejected instead of picking one.
    private static Class<?> carrierOf(MemoryLayout l) {
        if (!(l instanceof ValueLayout)) {
            throw new UnsupportedOperationException(
                    "only a value layout has a Java type carrying it: " + l);
        }
        return ((ValueLayout) l).carrier();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Descriptor)) {
            return false;
        }
        Descriptor other = (Descriptor) obj;
        boolean sameReturn = this.returnLayout0 == null ? other.returnLayout0 == null
                : this.returnLayout0.equals(other.returnLayout0);
        return sameReturn && this.arguments.equals(other.arguments);
    }

    public int hashCode() {
        return this.arguments.hashCode() * 31
                + (this.returnLayout0 == null ? 0 : this.returnLayout0.hashCode());
    }

    // `(j8)i4` with a return, `(i4)v` without one. `void`'s `v` is not a layout: it is the mark that
    // there is none, and that is why it is printed separately.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        int i = 0;
        while (i < this.arguments.size()) {
            sb.append(this.arguments.get(i).toString());
            i = i + 1;
        }
        sb.append(')');
        if (this.returnLayout0 == null) {
            sb.append('v');
        } else {
            sb.append(this.returnLayout0.toString());
        }
        return sb.toString();
    }
}
