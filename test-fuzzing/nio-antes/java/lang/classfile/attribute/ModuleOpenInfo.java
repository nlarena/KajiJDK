package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// An `opens` clause of the `Module` attribute (JVMS §4.7.25): the package becomes reachable by deep
// reflection. An empty `opensTo()` means opening to EVERYONE -- the empty list and the absence of
// recipients are the same thing in this format.
//
// A module declared `open` carries none of these clauses: the `Module` attribute's `ACC_OPEN` bit
// opens all of its packages at once, and mixing the two is a structural error.
public interface ModuleOpenInfo {

    /** The opened package. */
    PackageEntry openedPackage();

    /** The flags, as a mask. */
    int opensFlagsMask();

    /** The flags, as a set. */
    default java.util.Set<AccessFlag> opensFlags() {
        return AccessFlag.maskToAccessFlags(opensFlagsMask(), AccessFlag.Location.MODULE_OPENS);
    }

    /** Whether this flag is set. */
    default boolean has(AccessFlag flag) {
        return (opensFlagsMask() & flag.mask()) != 0;
    }

    /** Which modules it is opened to; empty means to all of them. */
    List<ModuleEntry> opensTo();

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageEntry opens, int openFlags,
            List<ModuleEntry> opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, openFlags, opensTo);
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageEntry opens, Collection<AccessFlag> openFlags,
            List<ModuleEntry> opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, TypedAttributes.mask(openFlags),
                opensTo);
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageEntry opens, int openFlags,
            ModuleEntry... opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, openFlags,
                TypedAttributes.listOfModules(opensTo));
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageEntry opens, Collection<AccessFlag> openFlags,
            ModuleEntry... opensTo) {
        return TypedAttributes.moduleOpenInfo(opens, TypedAttributes.mask(openFlags),
                TypedAttributes.listOfModules(opensTo));
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageDesc opens, int openFlags,
            List<ModuleDesc> opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens), openFlags,
                TypedAttributes.moduleEntries(opensTo));
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageDesc opens, Collection<AccessFlag> openFlags,
            List<ModuleDesc> opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens),
                TypedAttributes.mask(openFlags), TypedAttributes.moduleEntries(opensTo));
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageDesc opens, int openFlags,
            ModuleDesc... opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens), openFlags,
                TypedAttributes.moduleEntries(opensTo));
    }

    /** The clause with these values. */
    public static ModuleOpenInfo of(PackageDesc opens, Collection<AccessFlag> openFlags,
            ModuleDesc... opensTo) {
        return TypedAttributes.moduleOpenInfo(TypedAttributes.packageEntry(opens),
                TypedAttributes.mask(openFlags), TypedAttributes.moduleEntries(opensTo));
    }
}
