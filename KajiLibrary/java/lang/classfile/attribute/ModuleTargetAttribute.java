package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleTarget`: the platform a native module was built for (`linux-amd64`). It is the JDK's and not
// the JVMS's, and it only shows up in a `module-info.class`.
public interface ModuleTargetAttribute extends Attribute<ModuleTargetAttribute>, ClassElement {

    /** The platform. */
    Utf8Entry targetPlatform();

    /** The attribute with this platform. */
    public static ModuleTargetAttribute of(String targetPlatform) {
        return TypedAttributes.moduleTarget(TypedAttributes.utf8(targetPlatform));
    }

    /** The attribute with this platform. */
    public static ModuleTargetAttribute of(Utf8Entry targetPlatform) {
        return TypedAttributes.moduleTarget(targetPlatform);
    }
}
