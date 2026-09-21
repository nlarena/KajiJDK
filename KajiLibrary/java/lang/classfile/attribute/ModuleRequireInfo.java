package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ModuleDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import jdk.internal.classfile.impl.TypedAttributes;

// A `requires` clause of the `Module` attribute (JVMS §4.7.25). The flags here are what tell
// `requires transitive` from `requires static`, and `java.base` always shows up even when it was not
// written: the format demands that it be there.
public interface ModuleRequireInfo {

    /** The required module. */
    ModuleEntry requires();

    /** The flags, as a mask. */
    int requiresFlagsMask();

    /** The flags, as a set. */
    default Set<AccessFlag> requiresFlags() {
        return AccessFlag.maskToAccessFlags(requiresFlagsMask(),
                AccessFlag.Location.MODULE_REQUIRES);
    }

    /** The required version, if it is there. */
    Optional<Utf8Entry> requiresVersion();

    /** Whether this flag is set. */
    default boolean has(AccessFlag flag) {
        return (requiresFlagsMask() & flag.mask()) != 0;
    }

    /** The clause with these values. */
    public static ModuleRequireInfo of(ModuleEntry requires, int requiresFlags,
            Utf8Entry requiresVersion) {
        return TypedAttributes.moduleRequireInfo(requires, requiresFlags, requiresVersion);
    }

    /** The clause with these values. */
    public static ModuleRequireInfo of(ModuleEntry requires, Collection<AccessFlag> requiresFlags,
            Utf8Entry requiresVersion) {
        return TypedAttributes.moduleRequireInfo(requires, TypedAttributes.mask(requiresFlags),
                requiresVersion);
    }

    /** The clause with these values. */
    public static ModuleRequireInfo of(ModuleDesc requires, int requiresFlags,
            String requiresVersion) {
        return TypedAttributes.moduleRequireInfo(TypedAttributes.moduleEntry(requires),
                requiresFlags, TypedAttributes.utf8OrNull(requiresVersion));
    }

    /** The clause with these values. */
    public static ModuleRequireInfo of(ModuleDesc requires, Collection<AccessFlag> requiresFlags,
            String requiresVersion) {
        return TypedAttributes.moduleRequireInfo(TypedAttributes.moduleEntry(requires),
                TypedAttributes.mask(requiresFlags), TypedAttributes.utf8OrNull(requiresVersion));
    }
}
