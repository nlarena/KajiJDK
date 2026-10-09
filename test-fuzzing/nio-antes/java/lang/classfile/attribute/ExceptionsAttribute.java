package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `Exceptions` (JVMS §4.7.5): the exceptions the method declares with `throws`. The JVM does NOT
// enforce them --checking checked exceptions is the compiler's job--, so this attribute is
// information for whoever reads the class, not a constraint at run time.
public interface ExceptionsAttribute extends Attribute<ExceptionsAttribute>, MethodElement {

    /** The declared exceptions. */
    List<ClassEntry> exceptions();

    /** The attribute with these exceptions. */
    public static ExceptionsAttribute of(List<ClassEntry> exceptions) {
        return TypedAttributes.exceptions(exceptions);
    }

    /** The attribute with these exceptions. */
    public static ExceptionsAttribute of(ClassEntry... exceptions) {
        return TypedAttributes.exceptions(TypedAttributes.listOfClasses(exceptions));
    }

    /** The attribute with these exceptions. */
    public static ExceptionsAttribute ofSymbols(List<ClassDesc> exceptions) {
        return TypedAttributes.exceptions(TypedAttributes.classEntries(exceptions));
    }

    /** The attribute with these exceptions. */
    public static ExceptionsAttribute ofSymbols(ClassDesc... exceptions) {
        return TypedAttributes.exceptions(TypedAttributes.classEntries(exceptions));
    }
}
