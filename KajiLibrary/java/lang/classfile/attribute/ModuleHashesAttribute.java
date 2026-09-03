package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleHashes`: el algoritmo y los hashes de los módulos dependientes. Ver {@link ModuleHashInfo}.
public interface ModuleHashesAttribute extends Attribute<ModuleHashesAttribute>, ClassElement {

    /** El nombre del algoritmo de hash. */
    Utf8Entry algorithm();

    /** Las filas. */
    List<ModuleHashInfo> hashes();

    /** El atributo con estos valores. */
    public static ModuleHashesAttribute of(String algorithm, List<ModuleHashInfo> hashes) {
        return TypedAttributes.moduleHashes(TypedAttributes.utf8(algorithm), hashes);
    }

    /** El atributo con estos valores. */
    public static ModuleHashesAttribute of(String algorithm, ModuleHashInfo... hashes) {
        return TypedAttributes.moduleHashes(TypedAttributes.utf8(algorithm),
                TypedAttributes.listOf(hashes));
    }

    /** El atributo con estos valores. */
    public static ModuleHashesAttribute of(Utf8Entry algorithm, List<ModuleHashInfo> hashes) {
        return TypedAttributes.moduleHashes(algorithm, hashes);
    }

    /** El atributo con estos valores. */
    public static ModuleHashesAttribute of(Utf8Entry algorithm, ModuleHashInfo... hashes) {
        return TypedAttributes.moduleHashes(algorithm, TypedAttributes.listOf(hashes));
    }
}
