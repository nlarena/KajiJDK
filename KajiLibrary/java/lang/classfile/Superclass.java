package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;

// A class's `super_class`, as an element. `java.lang.Object` and a `module-info` have none: in the
// file the index is 0, and then this element simply does not appear.
public interface Superclass extends ClassElement {

    /** The superclass's pool entry. */
    ClassEntry superclassEntry();

    /** The element for this superclass. */
    public static Superclass of(ClassEntry superclassEntry) {
        return new jdk.internal.classfile.impl.SuperclassImpl(superclassEntry);
    }
}
