package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.constant.ModuleDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// Una fila de `ModuleHashes`: el hash de un módulo del que éste depende. Es del JDK, no del JVMS, y
// lo usa `jlink` para detectar que una imagen se armó con piezas que no van juntas.
public interface ModuleHashInfo {

    /** El módulo. */
    ModuleEntry moduleName();

    /** Una copia del hash. */
    byte[] hash();

    /** La fila con estos valores. */
    public static ModuleHashInfo of(ModuleEntry moduleName, byte[] hash) {
        return TypedAttributes.moduleHashInfo(moduleName, hash);
    }

    /** La fila con estos valores. */
    public static ModuleHashInfo of(ModuleDesc moduleName, byte[] hash) {
        return TypedAttributes.moduleHashInfo(TypedAttributes.moduleEntry(moduleName), hash);
    }
}
