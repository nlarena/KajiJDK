package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// Una cláusula `opens` del atributo `Module` (JVMS §4.7.25): el paquete queda accesible por
// reflexión profunda. `opensTo()` vacío significa abrir a TODOS — la lista vacía y la ausencia de
// destinatarios son la misma cosa en este formato.
//
// Un módulo declarado `open` no lleva ninguna de estas cláusulas: el bit `ACC_OPEN` del atributo
// `Module` abre todos sus paquetes de una, y mezclar las dos cosas es un error de estructura.
public interface ModuleOpenInfo {

    /** El paquete abierto. */
    PackageEntry openedPackage();

    /** Las banderas, como máscara. */
    int opensFlagsMask();

    /** Las banderas, como conjunto. */
    default java.util.Set<AccessFlag> opensFlags() {
        return AccessFlag.maskToAccessFlags(opensFlagsMask(), AccessFlag.Location.MODULE_OPENS);
    }

    /** Si esta bandera está puesta. */
    default boolean has(AccessFlag flag) {
        return (opensFlagsMask() & flag.mask()) != 0;
    }

    /** A qué módulos se exporta; vacío quiere decir a todos. */
    List<ModuleEntry> opensTo();

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageEntry opens, int openFlags,
            List<ModuleEntry> opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, openFlags, opensTo);
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageEntry opens, Collection<AccessFlag> openFlags,
            List<ModuleEntry> opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, TypedAttributes.mask(openFlags),
                opensTo);
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageEntry opens, int openFlags,
            ModuleEntry... opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, openFlags,
                TypedAttributes.listOfModules(opensTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageEntry opens, Collection<AccessFlag> openFlags,
            ModuleEntry... opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, TypedAttributes.mask(openFlags),
                TypedAttributes.listOfModules(opensTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageDesc opens, int openFlags,
            List<ModuleDesc> opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens), openFlags,
                TypedAttributes.moduleEntries(opensTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageDesc opens, Collection<AccessFlag> openFlags,
            List<ModuleDesc> opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens),
                TypedAttributes.mask(openFlags), TypedAttributes.moduleEntries(opensTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageDesc opens, int openFlags,
            ModuleDesc... opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens), openFlags,
                TypedAttributes.moduleEntries(opensTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleOpenInfo of(PackageDesc opens, Collection<AccessFlag> openFlags,
            ModuleDesc... opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens),
                TypedAttributes.mask(openFlags), TypedAttributes.moduleEntries(opensTo));
    }
}
