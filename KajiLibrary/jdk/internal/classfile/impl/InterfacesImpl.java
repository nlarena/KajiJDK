package jdk.internal.classfile.impl;

import java.lang.classfile.Interfaces;
import java.lang.classfile.constantpool.ClassEntry;
import java.util.Collections;
import java.util.List;

// The list of interfaces, as a single class element.
public final class InterfacesImpl implements Interfaces {

    private final List<ClassEntry> interfaces;

    public InterfacesImpl(List<ClassEntry> interfaces) {
        this.interfaces = Collections.unmodifiableList(interfaces);
    }

    public List<ClassEntry> interfaces() {
        return this.interfaces;
    }

    public String toString() {
        return "Interfaces" + this.interfaces.toString();
    }
}
