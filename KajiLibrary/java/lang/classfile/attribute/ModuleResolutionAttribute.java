package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleResolution`: flags telling `jlink` and the module system how to treat this module (do not
// resolve it by default, warn that it is deprecated, ...). It is the JDK's, not the JVMS's.
public interface ModuleResolutionAttribute
        extends Attribute<ModuleResolutionAttribute>, ClassElement {

    /** The flags, just as they are in the file. */
    int resolutionFlags();

    /** The attribute with these flags. */
    public static ModuleResolutionAttribute of(int resolutionFlags) {
        return TypedAttributes.moduleResolution(resolutionFlags);
    }
}
