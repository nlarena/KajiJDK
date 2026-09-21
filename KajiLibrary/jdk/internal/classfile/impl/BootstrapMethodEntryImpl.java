package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.util.Collections;
import java.util.List;

// A row of the `BootstrapMethods` attribute's table.
public final class BootstrapMethodEntryImpl implements BootstrapMethodEntry {

    private final ConstantPool pool;
    private final int index;
    private final MethodHandleEntry handle;
    private final List<LoadableConstantEntry> arguments;

    public BootstrapMethodEntryImpl(ConstantPool pool, int index, MethodHandleEntry handle,
            List<LoadableConstantEntry> arguments) {
        this.pool = pool;
        this.index = index;
        this.handle = handle;
        this.arguments = Collections.unmodifiableList(arguments);
    }

    public ConstantPool constantPool() {
        return this.pool;
    }

    public int bsmIndex() {
        return this.index;
    }

    public MethodHandleEntry bootstrapMethod() {
        return this.handle;
    }

    public List<LoadableConstantEntry> arguments() {
        return this.arguments;
    }

    public String toString() {
        return "BootstrapMethod#" + this.index + " " + this.handle.toString();
    }
}
