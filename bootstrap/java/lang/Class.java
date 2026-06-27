package java.lang;

// Minimal java.lang.Class — the runtime representation of a class. In our VM the
// `Class<…>` object IS the heap mirror; getClass() just hands back that reference.
public final class Class<T> {
    // isInstance(obj): is obj an instance of this class? Native — it's the subtype
    // check the VM does (reusing is_subtype over the class hierarchy).
    public native boolean isInstance(Object obj);
}
