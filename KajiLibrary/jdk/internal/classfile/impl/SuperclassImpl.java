package jdk.internal.classfile.impl;

import java.lang.classfile.Superclass;
import java.lang.classfile.constantpool.ClassEntry;

// La superclase, como elemento de clase.
public final class SuperclassImpl implements Superclass {

    private final ClassEntry entrada;

    public SuperclassImpl(ClassEntry entrada) {
        this.entrada = entrada;
    }

    public ClassEntry superclassEntry() {
        return this.entrada;
    }

    public String toString() {
        return "Superclass[" + this.entrada.asInternalName() + "]";
    }
}
