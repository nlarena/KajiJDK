package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Optional;
import jdk.internal.classfile.impl.TypedAttributes;

// `EnclosingMethod` (JVMS §4.7.7): where a local or anonymous class was declared. The method is
// OPTIONAL and its absence is not missing data: it means the class was declared in an instance or
// field initialiser, and not inside a method.
public interface EnclosingMethodAttribute
        extends Attribute<EnclosingMethodAttribute>, ClassElement {

    /** The class enclosing it. */
    ClassEntry enclosingClass();

    /** The method enclosing it, if there is one. */
    Optional<NameAndTypeEntry> enclosingMethod();

    /** The name of the method enclosing it. */
    default Optional<Utf8Entry> enclosingMethodName() {
        Optional<NameAndTypeEntry> m = enclosingMethod();
        if (!m.isPresent()) {
            return Optional.<Utf8Entry>empty();
        }
        return Optional.of(m.get().name());
    }

    /** The descriptor of the method enclosing it. */
    default Optional<Utf8Entry> enclosingMethodType() {
        Optional<NameAndTypeEntry> m = enclosingMethod();
        if (!m.isPresent()) {
            return Optional.<Utf8Entry>empty();
        }
        return Optional.of(m.get().type());
    }

    /** The type of the method enclosing it. */
    default Optional<MethodTypeDesc> enclosingMethodTypeSymbol() {
        Optional<Utf8Entry> t = enclosingMethodType();
        if (!t.isPresent()) {
            return Optional.<MethodTypeDesc>empty();
        }
        return Optional.of(MethodTypeDesc.ofDescriptor(t.get().stringValue()));
    }

    /** The attribute with this class and this method. */
    public static EnclosingMethodAttribute of(ClassEntry className,
            Optional<NameAndTypeEntry> method) {
        return TypedAttributes.enclosingMethod(className, method);
    }

    /** The attribute with this class and this method. */
    public static EnclosingMethodAttribute of(ClassDesc className, Optional<String> methodName,
            Optional<MethodTypeDesc> methodType) {
        return TypedAttributes.enclosingMethod(className, methodName, methodType);
    }
}
