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

// `Module` (JVMS §4.7.25): la declaración entera de un módulo. Es el atributo más grande del
// formato y el único que reemplaza al contenido de la clase en vez de acompañarlo — un
// `module-info.class` no tiene campos ni métodos, sólo esto.
public interface ModuleAttribute extends Attribute<ModuleAttribute>, ClassElement {

    /** El nombre del módulo. */
    ModuleEntry moduleName();

    /** Las banderas del módulo, como máscara. */
    int moduleFlagsMask();

    /** Las banderas del módulo, como conjunto. */
    default Set<AccessFlag> moduleFlags() {
        return AccessFlag.maskToAccessFlags(moduleFlagsMask(), AccessFlag.Location.MODULE);
    }

    /** Si esta bandera está puesta. */
    default boolean has(AccessFlag flag) {
        return (moduleFlagsMask() & flag.mask()) != 0;
    }

    /** La versión del módulo, si está. */
    Optional<Utf8Entry> moduleVersion();

    /** Las cláusulas `requires`. */
    List<ModuleRequireInfo> requires();

    /** Las cláusulas `exports`. */
    List<ModuleExportInfo> exports();

    /** Las cláusulas `opens`. */
    List<ModuleOpenInfo> opens();

    /** Los servicios que el módulo usa. */
    List<ClassEntry> uses();

    /** Las cláusulas `provides`. */
    List<ModuleProvideInfo> provides();

    /** El atributo con todas sus partes. */
    public static ModuleAttribute of(ModuleEntry moduleName, int moduleFlags,
            Utf8Entry moduleVersion, Collection<ModuleRequireInfo> requires,
            Collection<ModuleExportInfo> exports, Collection<ModuleOpenInfo> opens,
            Collection<ClassEntry> uses, Collection<ModuleProvideInfo> provides) {
        return TypedAttributes.module(moduleName, moduleFlags, moduleVersion, requires, exports,
                opens, uses, provides);
    }

    /** El atributo que arma `handler` sobre un constructor vacío. */
    public static ModuleAttribute of(ModuleDesc moduleName,
            Consumer<ModuleAttributeBuilder> handler) {
        return of(TypedAttributes.moduleEntry(moduleName), handler);
    }

    /** El atributo que arma `handler` sobre un constructor vacío. */
    public static ModuleAttribute of(ModuleEntry moduleName,
            Consumer<ModuleAttributeBuilder> handler) {
        return TypedAttributes.buildModule(moduleName, handler);
    }

    /**
     * El constructor incremental de un `Module`. Cada método devuelve el mismo constructor, así que
     * las cláusulas se encadenan; `moduleName` y las banderas se pisan, las cláusulas se acumulan.
     */
    public interface ModuleAttributeBuilder {

        /** Cambia el nombre del módulo. */
        ModuleAttributeBuilder moduleName(ModuleDesc moduleName);

        /** Pone las banderas del módulo. */
        ModuleAttributeBuilder moduleFlags(int flagsMask);

        /** Pone las banderas del módulo. */
        default ModuleAttributeBuilder moduleFlags(AccessFlag... moduleFlags) {
            return moduleFlags(TypedAttributes.mask(moduleFlags));
        }

        /** Pone la versión del módulo. */
        ModuleAttributeBuilder moduleVersion(String version);

        /** Agrega un `requires`. */
        ModuleAttributeBuilder requires(ModuleDesc module, int requiresFlagsMask, String version);

        /** Agrega un `requires`. */
        default ModuleAttributeBuilder requires(ModuleDesc module,
                Collection<AccessFlag> requiresFlags, String version) {
            return requires(module, TypedAttributes.mask(requiresFlags), version);
        }

        /** Agrega un `requires` ya armado. */
        ModuleAttributeBuilder requires(ModuleRequireInfo requires);

        /** Agrega un `exports`. */
        ModuleAttributeBuilder exports(PackageDesc pkge, int exportsFlagsMask,
                ModuleDesc... exportsToModules);

        /** Agrega un `exports`. */
        default ModuleAttributeBuilder exports(PackageDesc pkge, Collection<AccessFlag> exportsFlags,
                ModuleDesc... exportsToModules) {
            return exports(pkge, TypedAttributes.mask(exportsFlags), exportsToModules);
        }

        /** Agrega un `exports` ya armado. */
        ModuleAttributeBuilder exports(ModuleExportInfo exports);

        /** Agrega un `opens`. */
        ModuleAttributeBuilder opens(PackageDesc pkge, int opensFlagsMask,
                ModuleDesc... opensToModules);

        /** Agrega un `opens`. */
        default ModuleAttributeBuilder opens(PackageDesc pkge, Collection<AccessFlag> opensFlags,
                ModuleDesc... opensToModules) {
            return opens(pkge, TypedAttributes.mask(opensFlags), opensToModules);
        }

        /** Agrega un `opens` ya armado. */
        ModuleAttributeBuilder opens(ModuleOpenInfo opens);

        /** Agrega un servicio usado. */
        ModuleAttributeBuilder uses(ClassDesc service);

        /** Agrega un servicio usado. */
        ModuleAttributeBuilder uses(ClassEntry uses);

        /** Agrega un `provides`. */
        ModuleAttributeBuilder provides(ClassDesc service, ClassDesc... implClasses);

        /** Agrega un `provides` ya armado. */
        ModuleAttributeBuilder provides(ModuleProvideInfo provides);
    }
}
