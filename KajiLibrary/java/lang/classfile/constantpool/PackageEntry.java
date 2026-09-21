package java.lang.classfile.constantpool;

import java.lang.constant.PackageDesc;

// `CONSTANT_Package_info` (JVMS §4.4.12): it may only appear in a `module-info.class`, inside the
// `Module` attribute. The `Utf8` it points at carries the package's internal name (`java/lang`).
public interface PackageEntry extends PoolEntry {

    /** The `Utf8` entry holding the package's internal name. */
    Utf8Entry name();

    /** The package's nominal descriptor. */
    PackageDesc asSymbol();

    /** Whether this entry names exactly `desc`. */
    boolean matches(PackageDesc desc);
}
