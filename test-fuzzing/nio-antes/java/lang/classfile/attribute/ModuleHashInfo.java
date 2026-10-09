package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.constant.ModuleDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// A row of `ModuleHashes`: the hash of a module this one depends on. It is the JDK's, not the JVMS's,
// and `jlink` uses it to detect that an image was built out of pieces that do not go together.
public interface ModuleHashInfo {

    /** The module. */
    ModuleEntry moduleName();

    /** A copy of the hash. */
    byte[] hash();

    /** The row with these values. */
    public static ModuleHashInfo of(ModuleEntry moduleName, byte[] hash) {
        return TypedAttributes.moduleHashInfo(moduleName, hash);
    }

    /** The row with these values. */
    public static ModuleHashInfo of(ModuleDesc moduleName, byte[] hash) {
        return TypedAttributes.moduleHashInfo(TypedAttributes.moduleEntry(moduleName), hash);
    }
}
