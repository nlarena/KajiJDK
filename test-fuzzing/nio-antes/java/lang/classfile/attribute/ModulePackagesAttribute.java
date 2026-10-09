package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.constant.PackageDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModulePackages` (JVMS §4.7.26): ALL of the module's packages, exported or not. It differs from the
// `Module` attribute's `exports`: this is the complete inventory, and it is what lets the module
// system know which package belongs to whom without opening the whole jar.
public interface ModulePackagesAttribute extends Attribute<ModulePackagesAttribute>, ClassElement {

    /** The packages. */
    List<PackageEntry> packages();

    /** The attribute with these packages. */
    public static ModulePackagesAttribute of(List<PackageEntry> packages) {
        return TypedAttributes.modulePackages(packages);
    }

    /** The attribute with these packages. */
    public static ModulePackagesAttribute of(PackageEntry... packages) {
        return TypedAttributes.modulePackages(TypedAttributes.listOf(packages));
    }

    /** The attribute with these packages. */
    public static ModulePackagesAttribute ofNames(List<PackageDesc> packages) {
        return TypedAttributes.modulePackages(TypedAttributes.packageEntries(packages));
    }

    /** The attribute with these packages. */
    public static ModulePackagesAttribute ofNames(PackageDesc... packages) {
        return TypedAttributes.modulePackages(TypedAttributes.packageEntries(packages));
    }
}
