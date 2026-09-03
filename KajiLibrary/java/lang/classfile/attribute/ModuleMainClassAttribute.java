package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleMainClass` (JVMS §4.7.27): la clase principal del módulo. Sólo en un `module-info.class`.
public interface ModuleMainClassAttribute extends Attribute<ModuleMainClassAttribute>, ClassElement {

    /** La clase principal. */
    ClassEntry mainClass();

    /** El atributo con esta clase. */
    public static ModuleMainClassAttribute of(ClassEntry mainClass) {
        return TypedAttributes.moduleMainClass(mainClass);
    }

    /** El atributo con esta clase. */
    public static ModuleMainClassAttribute of(ClassDesc mainClass) {
        return TypedAttributes.moduleMainClass(TypedAttributes.classEntry(mainClass));
    }
}
