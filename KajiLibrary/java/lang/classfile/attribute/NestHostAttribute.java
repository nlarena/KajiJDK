package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// `NestHost` (JVMS §4.7.28): quién es el anfitrión del nido al que esta clase pertenece. Es la
// mitad del mecanismo que reemplazó a los métodos puente sintéticos entre una clase y sus anidadas:
// dos miembros del mismo nido acceden a sus privados sin intermediarios. La otra mitad es
// {@link NestMembersAttribute}, y las dos tienen que coincidir o la JVM no reconoce el nido.
public interface NestHostAttribute extends Attribute<NestHostAttribute>, ClassElement {

    /** El anfitrión del nido. */
    ClassEntry nestHost();

    /** El atributo con este anfitrión. */
    public static NestHostAttribute of(ClassEntry nestHost) {
        return TypedAttributes.nestHost(nestHost);
    }

    /** El atributo con este anfitrión. */
    public static NestHostAttribute of(ClassDesc nestHost) {
        return TypedAttributes.nestHost(TypedAttributes.classEntry(nestHost));
    }
}
