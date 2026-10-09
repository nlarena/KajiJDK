package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import jdk.internal.classfile.impl.TypedAttributes;

// `Module` (JVMS §4.7.25): a module's whole declaration. It is the format's largest attribute and the
// only one that replaces the class's contents instead of accompanying them -- a `module-info.class`
// has no fields and no methods, only this.
public interface ModuleAttribute extends Attribute<ModuleAttribute>, ClassElement {

    /** The module's name. */
    ModuleEntry moduleName();

    /** The module's flags, as a mask. */
    int moduleFlagsMask();

    /** The module's flags, as a set. */
    default Set<AccessFlag> moduleFlags() {
        return AccessFlag.maskToAccessFlags(moduleFlagsMask(), AccessFlag.Location.MODULE);
    }

    /** Whether this flag is set. */
    default boolean has(AccessFlag flag) {
        return (moduleFlagsMask() & flag.mask()) != 0;
    }

    /** The module's version, if it is there. */
    Optional<Utf8Entry> moduleVersion();

    /** The `requires` clauses. */
    List<ModuleRequireInfo> requires();

    /** The `exports` clauses. */
    List<ModuleExportInfo> exports();

    /** The `opens` clauses. */
    List<ModuleOpenInfo> opens();

    /** The services the module uses. */
    List<ClassEntry> uses();

    /** The `provides` clauses. */
    List<ModuleProvideInfo> provides();

    /** The attribute with all its parts. */
    public static ModuleAttribute of(ModuleEntry moduleName, int moduleFlags,
            Utf8Entry moduleVersion, Collection<ModuleRequireInfo> requires,
            Collection<ModuleExportInfo> exports, Collection<ModuleOpenInfo> opens,
            Collection<ClassEntry> uses, Collection<ModuleProvideInfo> provides) {
        return TypedAttributes.module(moduleName, moduleFlags, moduleVersion, requires, exports,
                opens, uses, provides);
    }

    /** The attribute `handler` builds on top of an empty builder. */
    public static ModuleAttribute of(ModuleDesc moduleName,
            Consumer<ModuleAttributeBuilder> handler) {
        return of(TypedAttributes.moduleEntry(moduleName), handler);
    }

    /** The attribute `handler` builds on top of an empty builder. */
    public static ModuleAttribute of(ModuleEntry moduleName,
            Consumer<ModuleAttributeBuilder> handler) {
        return TypedAttributes.buildModule(moduleName, handler);
    }

    /**
     * A `Module`'s incremental builder. Every method returns the same builder, so the clauses chain;
     * `moduleName` and the flags overwrite, the clauses accumulate.
     */
    public interface ModuleAttributeBuilder {

        /** It changes the module's name. */
        ModuleAttributeBuilder moduleName(ModuleDesc moduleName);

        /** It sets the module's flags. */
        ModuleAttributeBuilder moduleFlags(int flagsMask);

        /** It sets the module's flags. */
        default ModuleAttributeBuilder moduleFlags(AccessFlag... moduleFlags) {
            return moduleFlags(TypedAttributes.mask(moduleFlags));
        }

        /** It sets the module's version. */
        ModuleAttributeBuilder moduleVersion(String version);

        /** It adds a `requires`. */
        ModuleAttributeBuilder requires(ModuleDesc module, int requiresFlagsMask, String version);

        /** It adds a `requires`. */
        default ModuleAttributeBuilder requires(ModuleDesc module,
                Collection<AccessFlag> requiresFlags, String version) {
            return requires(module, TypedAttributes.mask(requiresFlags), version);
        }

        /** It adds an already built `requires`. */
        ModuleAttributeBuilder requires(ModuleRequireInfo requires);

        /** It adds an `exports`. */
        ModuleAttributeBuilder exports(PackageDesc pkge, int exportsFlagsMask,
                ModuleDesc... exportsToModules);

        /** It adds an `exports`. */
        default ModuleAttributeBuilder exports(PackageDesc pkge, Collection<AccessFlag> exportsFlags,
                ModuleDesc... exportsToModules) {
            return exports(pkge, TypedAttributes.mask(exportsFlags), exportsToModules);
        }

        /** It adds an already built `exports`. */
        ModuleAttributeBuilder exports(ModuleExportInfo exports);

        /** It adds an `opens`. */
        ModuleAttributeBuilder opens(PackageDesc pkge, int opensFlagsMask,
                ModuleDesc... opensToModules);

        /** It adds an `opens`. */
        default ModuleAttributeBuilder opens(PackageDesc pkge, Collection<AccessFlag> opensFlags,
                ModuleDesc... opensToModules) {
            return opens(pkge, TypedAttributes.mask(opensFlags), opensToModules);
        }

        /** It adds an already built `opens`. */
        ModuleAttributeBuilder opens(ModuleOpenInfo opens);

        /** It adds a used service. */
        ModuleAttributeBuilder uses(ClassDesc service);

        /** It adds a used service. */
        ModuleAttributeBuilder uses(ClassEntry uses);

        /** It adds a `provides`. */
        ModuleAttributeBuilder provides(ClassDesc service, ClassDesc... implClasses);

        /** It adds an already built `provides`. */
        ModuleAttributeBuilder provides(ModuleProvideInfo provides);
    }
}
