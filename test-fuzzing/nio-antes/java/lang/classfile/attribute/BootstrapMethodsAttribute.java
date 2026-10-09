package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.BootstrapMethodEntry;
import java.util.List;

// `BootstrapMethods` (JVMS §4.7.23): the table `invokedynamic` and `CONSTANT_Dynamic` resolve
// against. It is half pool and half attribute: the pool's dynamic entries index it, so without it the
// pool cannot be fully resolved.
//
// It has no factory, and it has none in the JDK either: the table is managed by
// {@link java.lang.classfile.constantpool.ConstantPoolBuilder} --each `bsmEntry` adds a row to it and
// returns its index--, so building one by hand would leave the pool and the attribute saying
// different things.
public interface BootstrapMethodsAttribute extends Attribute<BootstrapMethodsAttribute> {

    /** The rows, in file order. */
    List<BootstrapMethodEntry> bootstrapMethods();

    /** How many rows there are. */
    int bootstrapMethodsSize();
}
