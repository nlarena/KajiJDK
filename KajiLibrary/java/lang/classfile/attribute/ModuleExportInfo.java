package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// Una cláusula `exports` del atributo `Module` (JVMS §4.7.25). `exportsTo()` vacío significa
// exportar a TODOS: la lista vacía y la ausencia de destinatarios son la misma cosa en este formato,
// y por eso un `exports foo to bar` con la lista vacía sería un error de quien lo escribió.
public interface ModuleExportInfo {

    /** El paquete exportado. */
    PackageEntry exportedPackage();

    /** Las banderas, como máscara. */
    int exportsFlagsMask();

    /** Las banderas, como conjunto. */
    default java.util.Set<AccessFlag> exportsFlags() {
        return AccessFlag.maskToAccessFlags(exportsFlagsMask(), AccessFlag.Location.MODULE_EXPORTS);
    }

    /** Si esta bandera está puesta. */
    default boolean has(AccessFlag flag) {
        return (exportsFlagsMask() & flag.mask()) != 0;
    }

    /** A qué módulos se exporta; vacío quiere decir a todos. */
    List<ModuleEntry> exportsTo();

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageEntry exports, int exportFlags,
            List<ModuleEntry> exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, exportFlags, exportsTo);
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageEntry exports, Collection<AccessFlag> exportFlags,
            List<ModuleEntry> exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, TypedAttributes.mask(exportFlags),
                exportsTo);
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageEntry exports, int exportFlags,
            ModuleEntry... exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, exportFlags,
                TypedAttributes.listOfModules(exportsTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageEntry exports, Collection<AccessFlag> exportFlags,
            ModuleEntry... exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, TypedAttributes.mask(exportFlags),
                TypedAttributes.listOfModules(exportsTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageDesc exports, int exportFlags,
            List<ModuleDesc> exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports), exportFlags,
                TypedAttributes.moduleEntries(exportsTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageDesc exports, Collection<AccessFlag> exportFlags,
            List<ModuleDesc> exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports),
                TypedAttributes.mask(exportFlags), TypedAttributes.moduleEntries(exportsTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageDesc exports, int exportFlags,
            ModuleDesc... exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports), exportFlags,
                TypedAttributes.moduleEntries(exportsTo));
    }

    /** La cláusula con estos valores. */
    public static ModuleExportInfo of(PackageDesc exports, Collection<AccessFlag> exportFlags,
            ModuleDesc... exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports),
                TypedAttributes.mask(exportFlags), TypedAttributes.moduleEntries(exportsTo));
    }
}
