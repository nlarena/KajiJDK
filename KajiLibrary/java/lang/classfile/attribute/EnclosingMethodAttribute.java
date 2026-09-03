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

// `EnclosingMethod` (JVMS §4.7.7): dónde se declaró una clase local o anónima. El método es
// OPCIONAL y su ausencia no es un dato faltante: significa que la clase se declaró en un
// inicializador de instancia o de campo, y no dentro de un método.
public interface EnclosingMethodAttribute
        extends Attribute<EnclosingMethodAttribute>, ClassElement {

    /** La clase que la encierra. */
    ClassEntry enclosingClass();

    /** El método que la encierra, si hay uno. */
    Optional<NameAndTypeEntry> enclosingMethod();

    /** El nombre del método que la encierra. */
    default Optional<Utf8Entry> enclosingMethodName() {
        Optional<NameAndTypeEntry> m = enclosingMethod();
        if (!m.isPresent()) {
            return Optional.<Utf8Entry>empty();
        }
        return Optional.of(m.get().name());
    }

    /** El descriptor del método que la encierra. */
    default Optional<Utf8Entry> enclosingMethodType() {
        Optional<NameAndTypeEntry> m = enclosingMethod();
        if (!m.isPresent()) {
            return Optional.<Utf8Entry>empty();
        }
        return Optional.of(m.get().type());
    }

    /** El tipo del método que la encierra. */
    default Optional<MethodTypeDesc> enclosingMethodTypeSymbol() {
        Optional<Utf8Entry> t = enclosingMethodType();
        if (!t.isPresent()) {
            return Optional.<MethodTypeDesc>empty();
        }
        return Optional.of(MethodTypeDesc.ofDescriptor(t.get().stringValue()));
    }

    /** El atributo con esta clase y este método. */
    public static EnclosingMethodAttribute of(ClassEntry className,
            Optional<NameAndTypeEntry> method) {
        return TypedAttributes.enclosingMethod(className, method);
    }

    /** El atributo con esta clase y este método. */
    public static EnclosingMethodAttribute of(ClassDesc className, Optional<String> methodName,
            Optional<MethodTypeDesc> methodType) {
        return TypedAttributes.enclosingMethod(className, methodName, methodType);
    }
}
