package jdk.internal.classfile.impl;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;

// The pool of the loose entries.
//
// A good part of the public factories of `java.lang.classfile` receive a `String`, an `int` or a
// `ClassDesc` instead of a pool entry --`AnnotationValue.ofString("hello")`,
// `TypeCheckInstruction.of(CHECKCAST, CD_String)`-- and have to return an object that ALREADY has
// its entry. That entry comes from here. The JDK does exactly the same with its
// `TemporaryConstantPool`.
//
// What has to be kept in mind, and it is the only visible consequence: an entry of this pool
// belongs to this pool and not to that of the class being written, so `entry.constantPool()` is not
// going to be the destination pool and the writer has to adopt it. `ConstantPoolBuilderImpl`
// already does so with every foreign entry, so nothing more is needed from whoever uses it.
//
// The pool deduplicates by value, so asking a thousand times for `"hello"` does not make it grow;
// asking for a million different texts does, and at 65535 indices it throws. It is the same ceiling
// any `.class` has and nobody gets there making loose constants, but it is as well that it be said.
public final class TemporaryConstantPool {

    private static final ConstantPoolBuilder POOL = ConstantPoolBuilder.of();

    private TemporaryConstantPool() {
    }

    /** The pool itself, for whoever needs an entry that has no shortcut here. */
    public static ConstantPoolBuilder pool() {
        return POOL;
    }

    /** A loose `CONSTANT_Utf8`. */
    public static Utf8Entry utf8(String s) {
        if (s == null) {
            throw new NullPointerException("utf8");
        }
        synchronized (POOL) {
            return POOL.utf8Entry(s);
        }
    }

    /** A loose `CONSTANT_Class`. */
    public static ClassEntry classEntry(ClassDesc d) {
        if (d == null) {
            throw new NullPointerException("clase");
        }
        synchronized (POOL) {
            return POOL.classEntry(d);
        }
    }

    /** A loose `CONSTANT_Class` with this internal name. */
    public static ClassEntry classEntry(Utf8Entry name) {
        synchronized (POOL) {
            return POOL.classEntry(name);
        }
    }

    /** A loose `CONSTANT_NameAndType`. */
    public static NameAndTypeEntry nameAndType(Utf8Entry name, Utf8Entry type) {
        synchronized (POOL) {
            return POOL.nameAndTypeEntry(name, type);
        }
    }

    /** A loose `CONSTANT_Fieldref`. */
    public static FieldRefEntry fieldRef(ClassEntry owner, NameAndTypeEntry nat) {
        synchronized (POOL) {
            return POOL.fieldRefEntry(owner, nat);
        }
    }

    /** A loose `CONSTANT_Methodref`. */
    public static MethodRefEntry methodRef(ClassEntry owner, NameAndTypeEntry nat) {
        synchronized (POOL) {
            return POOL.methodRefEntry(owner, nat);
        }
    }

    /** A loose `CONSTANT_InterfaceMethodref`. */
    public static InterfaceMethodRefEntry interfaceMethodRef(ClassEntry owner, NameAndTypeEntry nat) {
        synchronized (POOL) {
            return POOL.interfaceMethodRefEntry(owner, nat);
        }
    }

    /** A loose `CONSTANT_Integer`. */
    public static IntegerEntry intEntry(int value) {
        synchronized (POOL) {
            return POOL.intEntry(value);
        }
    }

    /** A loose `CONSTANT_Long`. */
    public static LongEntry longEntry(long value) {
        synchronized (POOL) {
            return POOL.longEntry(value);
        }
    }

    /** A loose `CONSTANT_Float`. */
    public static FloatEntry floatEntry(float value) {
        synchronized (POOL) {
            return POOL.floatEntry(value);
        }
    }

    /** A loose `CONSTANT_Double`. */
    public static DoubleEntry doubleEntry(double value) {
        synchronized (POOL) {
            return POOL.doubleEntry(value);
        }
    }
}
