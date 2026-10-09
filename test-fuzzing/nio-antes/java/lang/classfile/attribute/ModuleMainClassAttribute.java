package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// `ModuleMainClass` (JVMS §4.7.27): the module's main class. Only in a `module-info.class`.
public interface ModuleMainClassAttribute extends Attribute<ModuleMainClassAttribute>, ClassElement {

    /** The main class. */
    ClassEntry mainClass();

    /** The attribute with this class. */
    public static ModuleMainClassAttribute of(ClassEntry mainClass) {
        return TypedAttributes.moduleMainClass(mainClass);
    }

    /** The attribute with this class. */
    public static ModuleMainClassAttribute of(ClassDesc mainClass) {
        return TypedAttributes.moduleMainClass(TypedAttributes.classEntry(mainClass));
    }
}
