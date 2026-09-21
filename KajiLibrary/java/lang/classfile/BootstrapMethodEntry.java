package java.lang.classfile;

import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.util.List;

// A row of the `BootstrapMethods` attribute's table (JVMS §4.7.23): the bootstrap method handle and
// its static arguments. It is not a `PoolEntry` --it does not live in the constant pool-- but it is
// indexed just as one from `CONSTANT_Dynamic` and `CONSTANT_InvokeDynamic`, and that is why the API
// treats it as part of the pool.
public interface BootstrapMethodEntry {

    /** The pool it belongs to. */
    ConstantPool constantPool();

    /** This row's index within the table. */
    int bsmIndex();

    /** The bootstrap method handle. */
    MethodHandleEntry bootstrapMethod();

    /** The static arguments, in order. */
    List<LoadableConstantEntry> arguments();
}
