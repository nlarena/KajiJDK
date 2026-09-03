package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.util.Collections;
import java.util.List;

// Una fila de la tabla del atributo `BootstrapMethods`.
public final class BootstrapMethodEntryImpl implements BootstrapMethodEntry {

    private final ConstantPool pool;
    private final int indice;
    private final MethodHandleEntry handle;
    private final List<LoadableConstantEntry> argumentos;

    public BootstrapMethodEntryImpl(ConstantPool pool, int indice, MethodHandleEntry handle,
            List<LoadableConstantEntry> argumentos) {
        this.pool = pool;
        this.indice = indice;
        this.handle = handle;
        this.argumentos = Collections.unmodifiableList(argumentos);
    }

    public ConstantPool constantPool() {
        return this.pool;
    }

    public int bsmIndex() {
        return this.indice;
    }

    public MethodHandleEntry bootstrapMethod() {
        return this.handle;
    }

    public List<LoadableConstantEntry> arguments() {
        return this.argumentos;
    }

    public String toString() {
        return "BootstrapMethod#" + this.indice + " " + this.handle.toString();
    }
}
