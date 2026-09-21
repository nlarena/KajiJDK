package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;

// The common shape of `CONSTANT_Dynamic` and `CONSTANT_InvokeDynamic` (JVMS §4.4.10): an index into
// the class's `BootstrapMethods` attribute plus a `NameAndType`. That index is NOT a pool index; it
// points into the attribute's table, and that is why these two entries cannot be fully resolved
// without having read that attribute first.
public interface DynamicConstantPoolEntry extends PoolEntry {

    /** The index within the `BootstrapMethods` attribute's table. */
    int bootstrapMethodIndex();

    /** That table's entry. */
    BootstrapMethodEntry bootstrap();

    /** The name/descriptor pair. */
    NameAndTypeEntry nameAndType();

    /** A shortcut to `nameAndType().name()`. */
    default Utf8Entry name() {
        return nameAndType().name();
    }

    /** A shortcut to `nameAndType().type()`. */
    default Utf8Entry type() {
        return nameAndType().type();
    }
}
