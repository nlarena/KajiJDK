package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.MethodElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `Exceptions` (JVMS §4.7.5): las excepciones que el método declara con `throws`. La JVM NO las
// hace cumplir —el chequeo de excepciones verificadas es del compilador—, así que este atributo es
// información para quien lea la clase, no una restricción en ejecución.
public interface ExceptionsAttribute extends Attribute<ExceptionsAttribute>, MethodElement {

    /** Las excepciones declaradas. */
    List<ClassEntry> exceptions();

    /** El atributo con estas excepciones. */
    public static ExceptionsAttribute of(List<ClassEntry> exceptions) {
        return TypedAttributes.exceptions(exceptions);
    }

    /** El atributo con estas excepciones. */
    public static ExceptionsAttribute of(ClassEntry... exceptions) {
        return TypedAttributes.exceptions(TypedAttributes.listOfClasses(exceptions));
    }

    /** El atributo con estas excepciones. */
    public static ExceptionsAttribute ofSymbols(List<ClassDesc> exceptions) {
        return TypedAttributes.exceptions(TypedAttributes.classEntries(exceptions));
    }

    /** El atributo con estas excepciones. */
    public static ExceptionsAttribute ofSymbols(ClassDesc... exceptions) {
        return TypedAttributes.exceptions(TypedAttributes.classEntries(exceptions));
    }
}
