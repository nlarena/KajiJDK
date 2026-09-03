package java.lang.classfile.constantpool;

import java.lang.constant.ModuleDesc;

// `CONSTANT_Module_info` (JVMS §4.4.11): igual que `Package`, sólo válida en un `module-info.class`.
// El `Utf8` lleva el nombre del módulo, con puntos y sin traducir.
public interface ModuleEntry extends PoolEntry {

    /** La entrada `Utf8` con el nombre del módulo. */
    Utf8Entry name();

    /** El descriptor nominal del módulo. */
    ModuleDesc asSymbol();

    /** Si esta entrada nombra exactamente a `desc`. */
    boolean matches(ModuleDesc desc);
}
