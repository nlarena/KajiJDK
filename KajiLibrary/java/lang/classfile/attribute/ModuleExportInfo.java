package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// An `exports` clause of the `Module` attribute (JVMS §4.7.25). An empty `exportsTo()` means
// exporting to EVERYONE: the empty list and the absence of recipients are the same thing in this
// format, and that is why an `exports foo to bar` with an empty list would be a mistake by whoever
// wrote it.
public interface ModuleExportInfo {

    /** The exported package. */
    PackageEntry exportedPackage();

    /** The flags, as a mask. */
    int exportsFlagsMask();

    /** The flags, as a set. */
    default java.util.Set<AccessFlag> exportsFlags() {
        return AccessFlag.maskToAccessFlags(exportsFlagsMask(), AccessFlag.Location.MODULE_EXPORTS);
    }

    /** Whether this flag is set. */
    default boolean has(AccessFlag flag) {
        return (exportsFlagsMask() & flag.mask()) != 0;
    }

    /** Which modules it is exported to; empty means to all of them. */
    List<ModuleEntry> exportsTo();

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageEntry exports, int exportFlags,
            List<ModuleEntry> exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, exportFlags, exportsTo);
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageEntry exports, Collection<AccessFlag> exportFlags,
            List<ModuleEntry> exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, TypedAttributes.mask(exportFlags),
                exportsTo);
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageEntry exports, int exportFlags,
            ModuleEntry... exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, exportFlags,
                TypedAttributes.listOfModules(exportsTo));
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageEntry exports, Collection<AccessFlag> exportFlags,
            ModuleEntry... exportsTo) {
        return TypedAttributes.moduleExportInfo(exports, TypedAttributes.mask(exportFlags),
                TypedAttributes.listOfModules(exportsTo));
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageDesc exports, int exportFlags,
            List<ModuleDesc> exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports), exportFlags,
                TypedAttributes.moduleEntries(exportsTo));
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageDesc exports, Collection<AccessFlag> exportFlags,
            List<ModuleDesc> exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports),
                TypedAttributes.mask(exportFlags), TypedAttributes.moduleEntries(exportsTo));
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageDesc exports, int exportFlags,
            ModuleDesc... exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports), exportFlags,
                TypedAttributes.moduleEntries(exportsTo));
    }

    /** The clause with these values. */
    public static ModuleExportInfo of(PackageDesc exports, Collection<AccessFlag> exportFlags,
            ModuleDesc... exportsTo) {
        return TypedAttributes.moduleExportInfo(TypedAttributes.packageEntry(exports),
                TypedAttributes.mask(exportFlags), TypedAttributes.moduleEntries(exportsTo));
    }
}
