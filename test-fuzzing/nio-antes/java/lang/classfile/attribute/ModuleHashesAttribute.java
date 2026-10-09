package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleHashes`: the algorithm and the hashes of the dependent modules. See
// {@link ModuleHashInfo}.
public interface ModuleHashesAttribute extends Attribute<ModuleHashesAttribute>, ClassElement {

    /** The hash algorithm's name. */
    Utf8Entry algorithm();

    /** The rows. */
    List<ModuleHashInfo> hashes();

    /** The attribute with these values. */
    public static ModuleHashesAttribute of(String algorithm, List<ModuleHashInfo> hashes) {
        return TypedAttributes.moduleHashes(TypedAttributes.utf8(algorithm), hashes);
    }

    /** The attribute with these values. */
    public static ModuleHashesAttribute of(String algorithm, ModuleHashInfo... hashes) {
        return TypedAttributes.moduleHashes(TypedAttributes.utf8(algorithm),
                TypedAttributes.listOf(hashes));
    }

    /** The attribute with these values. */
    public static ModuleHashesAttribute of(Utf8Entry algorithm, List<ModuleHashInfo> hashes) {
        return TypedAttributes.moduleHashes(algorithm, hashes);
    }

    /** The attribute with these values. */
    public static ModuleHashesAttribute of(Utf8Entry algorithm, ModuleHashInfo... hashes) {
        return TypedAttributes.moduleHashes(algorithm, TypedAttributes.listOf(hashes));
    }
}
