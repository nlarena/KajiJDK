package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// Una cláusula `provides ... with ...` del atributo `Module` (JVMS §4.7.25): el servicio y las
// implementaciones que este módulo aporta.
public interface ModuleProvideInfo {

    /** La interfaz de servicio. */
    ClassEntry provides();

    /** Las implementaciones. */
    List<ClassEntry> providesWith();

    /** La cláusula con estos valores. */
    public static ModuleProvideInfo of(ClassEntry provides, List<ClassEntry> providesWith) {
        return TypedAttributes.moduleProvideInfo(provides, providesWith);
    }

    /** La cláusula con estos valores. */
    public static ModuleProvideInfo of(ClassEntry provides, ClassEntry... providesWith) {
        return TypedAttributes.moduleProvideInfo(provides,
                TypedAttributes.listOfClasses(providesWith));
    }

    /** La cláusula con estos valores. */
    public static ModuleProvideInfo of(ClassDesc provides, List<ClassDesc> providesWith) {
        return TypedAttributes.moduleProvideInfo(TypedAttributes.classEntry(provides),
                TypedAttributes.classEntries(providesWith));
    }

    /** La cláusula con estos valores. */
    public static ModuleProvideInfo of(ClassDesc provides, ClassDesc... providesWith) {
        return TypedAttributes.moduleProvideInfo(TypedAttributes.classEntry(provides),
                TypedAttributes.classEntries(providesWith));
    }
}
