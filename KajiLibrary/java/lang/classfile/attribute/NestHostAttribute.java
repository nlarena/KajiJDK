package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// `NestHost` (JVMS §4.7.28): who hosts the nest this class belongs to. It is half of the mechanism
// that replaced the synthetic bridge methods between a class and its nested ones: two members of the
// same nest reach each other's privates with no go-between. The other half is
// {@link NestMembersAttribute}, and the two have to agree or the JVM does not recognise the nest.
public interface NestHostAttribute extends Attribute<NestHostAttribute>, ClassElement {

    /** The nest's host. */
    ClassEntry nestHost();

    /** The attribute with this host. */
    public static NestHostAttribute of(ClassEntry nestHost) {
        return TypedAttributes.nestHost(nestHost);
    }

    /** The attribute with this host. */
    public static NestHostAttribute of(ClassDesc nestHost) {
        return TypedAttributes.nestHost(TypedAttributes.classEntry(nestHost));
    }
}
