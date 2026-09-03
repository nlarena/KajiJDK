package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleTarget`: la plataforma para la que se construyó un módulo nativo (`linux-amd64`). Es del
// JDK y no del JVMS, y sólo aparece en un `module-info.class`.
public interface ModuleTargetAttribute extends Attribute<ModuleTargetAttribute>, ClassElement {

    /** La plataforma. */
    Utf8Entry targetPlatform();

    /** El atributo con esta plataforma. */
    public static ModuleTargetAttribute of(String targetPlatform) {
        return TypedAttributes.moduleTarget(TypedAttributes.utf8(targetPlatform));
    }

    /** El atributo con esta plataforma. */
    public static ModuleTargetAttribute of(Utf8Entry targetPlatform) {
        return TypedAttributes.moduleTarget(targetPlatform);
    }
}
