package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;

// El `super_class` de una clase, como elemento. `java.lang.Object` y un `module-info` no tienen: en
// el archivo el índice es 0, y entonces este elemento simplemente no aparece.
public interface Superclass extends ClassElement {

    /** La entrada de pool de la superclase. */
    ClassEntry superclassEntry();

    /** El elemento para esta superclase. */
    public static Superclass of(ClassEntry superclassEntry) {
        return new jdk.internal.classfile.impl.SuperclassImpl(superclassEntry);
    }
}
