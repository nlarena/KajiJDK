package java.lang.classfile.constantpool;

import java.lang.constant.ModuleDesc;

// `CONSTANT_Module_info` (JVMS §4.4.11): like `Package`, valid only in a `module-info.class`. The
// `Utf8` carries the module's name, with dots and untranslated.
public interface ModuleEntry extends PoolEntry {

    /** The `Utf8` entry holding the module's name. */
    Utf8Entry name();

    /** The module's nominal descriptor. */
    ModuleDesc asSymbol();

    /** Whether this entry names exactly `desc`. */
    boolean matches(ModuleDesc desc);
}
