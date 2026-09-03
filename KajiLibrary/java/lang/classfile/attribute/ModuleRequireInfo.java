package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ModuleDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import jdk.internal.classfile.impl.TypedAttributes;

// Una cláusula `requires` del atributo `Module` (JVMS §4.7.25). Las banderas de acá son las que
// distinguen `requires transitive` de `requires static`, y `java.base` aparece siempre aunque no se
// haya escrito: el formato exige que esté.
public interface ModuleRequireInfo {

    /** El módulo requerido. */
    ModuleEntry requires();

    /** Las banderas, como máscara. */
    int requiresFlagsMask();

    /** Las banderas, como conjunto. */
    default Set<AccessFlag> requiresFlags() {
        return AccessFlag.maskToAccessFlags(requiresFlagsMask(),
                AccessFlag.Location.MODULE_REQUIRES);
    }

    /** La versión requerida, si está. */
    Optional<Utf8Entry> requiresVersion();

    /** Si esta bandera está puesta. */
    default boolean has(AccessFlag flag) {
        return (requiresFlagsMask() & flag.mask()) != 0;
    }

    /** La cláusula con estos valores. */
    public static ModuleRequireInfo of(ModuleEntry requires, int requiresFlags,
            Utf8Entry requiresVersion) {
        return TypedAttributes.moduleRequireInfo(requires, requiresFlags, requiresVersion);
    }

    /** La cláusula con estos valores. */
    public static ModuleRequireInfo of(ModuleEntry requires, Collection<AccessFlag> requiresFlags,
            Utf8Entry requiresVersion) {
        return TypedAttributes.moduleRequireInfo(requires, TypedAttributes.mask(requiresFlags),
                requiresVersion);
    }

    /** La cláusula con estos valores. */
    public static ModuleRequireInfo of(ModuleDesc requires, int requiresFlags,
            String requiresVersion) {
        return TypedAttributes.moduleRequireInfo(TypedAttributes.moduleEntry(requires),
                requiresFlags, TypedAttributes.utf8OrNull(requiresVersion));
    }

    /** La cláusula con estos valores. */
    public static ModuleRequireInfo of(ModuleDesc requires, Collection<AccessFlag> requiresFlags,
            String requiresVersion) {
        return TypedAttributes.moduleRequireInfo(TypedAttributes.moduleEntry(requires),
                TypedAttributes.mask(requiresFlags), TypedAttributes.utf8OrNull(requiresVersion));
    }
}
