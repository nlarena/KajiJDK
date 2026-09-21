package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// A `provides ... with ...` clause of the `Module` attribute (JVMS §4.7.25): the service and the
// implementations this module contributes.
public interface ModuleProvideInfo {

    /** The service interface. */
    ClassEntry provides();

    /** The implementations. */
    List<ClassEntry> providesWith();

    /** The clause with these values. */
    public static ModuleProvideInfo of(ClassEntry provides, List<ClassEntry> providesWith) {
        return TypedAttributes.moduleProvideInfo(provides, providesWith);
    }

    /** The clause with these values. */
    public static ModuleProvideInfo of(ClassEntry provides, ClassEntry... providesWith) {
        return TypedAttributes.moduleProvideInfo(provides,
                TypedAttributes.listOfClasses(providesWith));
    }

    /** The clause with these values. */
    public static ModuleProvideInfo of(ClassDesc provides, List<ClassDesc> providesWith) {
        return TypedAttributes.moduleProvideInfo(TypedAttributes.classEntry(provides),
                TypedAttributes.classEntries(providesWith));
    }

    /** The clause with these values. */
    public static ModuleProvideInfo of(ClassDesc provides, ClassDesc... providesWith) {
        return TypedAttributes.moduleProvideInfo(TypedAttributes.classEntry(provides),
                TypedAttributes.classEntries(providesWith));
    }
}
