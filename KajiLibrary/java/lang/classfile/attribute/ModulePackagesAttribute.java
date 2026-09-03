package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.constant.PackageDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModulePackages` (JVMS §4.7.26): TODOS los paquetes del módulo, exportados o no. Es distinto de
// los `exports` del atributo `Module`: esto es el inventario completo, y sirve para que el sistema
// de módulos sepa qué paquete pertenece a quién sin abrir el jar entero.
public interface ModulePackagesAttribute extends Attribute<ModulePackagesAttribute>, ClassElement {

    /** Los paquetes. */
    List<PackageEntry> packages();

    /** El atributo con estos paquetes. */
    public static ModulePackagesAttribute of(List<PackageEntry> packages) {
        return TypedAttributes.modulePackages(packages);
    }

    /** El atributo con estos paquetes. */
    public static ModulePackagesAttribute of(PackageEntry... packages) {
        return TypedAttributes.modulePackages(TypedAttributes.listOf(packages));
    }

    /** El atributo con estos paquetes. */
    public static ModulePackagesAttribute ofNames(List<PackageDesc> packages) {
        return TypedAttributes.modulePackages(TypedAttributes.packageEntries(packages));
    }

    /** El atributo con estos paquetes. */
    public static ModulePackagesAttribute ofNames(PackageDesc... packages) {
        return TypedAttributes.modulePackages(TypedAttributes.packageEntries(packages));
    }
}
