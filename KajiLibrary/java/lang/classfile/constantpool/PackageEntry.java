package java.lang.classfile.constantpool;

import java.lang.constant.PackageDesc;

// `CONSTANT_Package_info` (JVMS §4.4.12): sólo puede aparecer en un `module-info.class`, dentro del
// atributo `Module`. El `Utf8` que apunta lleva el nombre interno del paquete (`java/lang`).
public interface PackageEntry extends PoolEntry {

    /** La entrada `Utf8` con el nombre interno del paquete. */
    Utf8Entry name();

    /** El descriptor nominal del paquete. */
    PackageDesc asSymbol();

    /** Si esta entrada nombra exactamente a `desc`. */
    boolean matches(PackageDesc desc);
}
