package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleResolution`: banderas que le dicen a `jlink` y al sistema de módulos cómo tratar a este
// módulo (no resolverlo por omisión, avisar que está obsoleto, …). Es del JDK, no del JVMS.
public interface ModuleResolutionAttribute
        extends Attribute<ModuleResolutionAttribute>, ClassElement {

    /** Las banderas, tal cual están en el archivo. */
    int resolutionFlags();

    /** El atributo con estas banderas. */
    public static ModuleResolutionAttribute of(int resolutionFlags) {
        return TypedAttributes.moduleResolution(resolutionFlags);
    }
}
