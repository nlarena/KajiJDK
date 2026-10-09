package jdk.internal.classfile.impl;

import java.lang.classfile.Superclass;
import java.lang.classfile.constantpool.ClassEntry;

// The superclass, as a class element.
public final class SuperclassImpl implements Superclass {

    private final ClassEntry entry;

    public SuperclassImpl(ClassEntry entry) {
        this.entry = entry;
    }

    public ClassEntry superclassEntry() {
        return this.entry;
    }

    public String toString() {
        return "Superclass[" + this.entry.asInternalName() + "]";
    }
}
