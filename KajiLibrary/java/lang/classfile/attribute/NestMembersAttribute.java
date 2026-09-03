package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `NestMembers` (JVMS §4.7.29): los miembros del nido que esta clase encabeza. Ver la nota de
// {@link NestHostAttribute}: una clase lleva este atributo o el otro, nunca los dos.
public interface NestMembersAttribute extends Attribute<NestMembersAttribute>, ClassElement {

    /** Los miembros del nido. */
    List<ClassEntry> nestMembers();

    /** El atributo con estos miembros. */
    public static NestMembersAttribute of(List<ClassEntry> nestMembers) {
        return TypedAttributes.nestMembers(nestMembers);
    }

    /** El atributo con estos miembros. */
    public static NestMembersAttribute of(ClassEntry... nestMembers) {
        return TypedAttributes.nestMembers(TypedAttributes.listOfClasses(nestMembers));
    }

    /** El atributo con estos miembros. */
    public static NestMembersAttribute ofSymbols(List<ClassDesc> nestMembers) {
        return TypedAttributes.nestMembers(TypedAttributes.classEntries(nestMembers));
    }

    /** El atributo con estos miembros. */
    public static NestMembersAttribute ofSymbols(ClassDesc... nestMembers) {
        return TypedAttributes.nestMembers(TypedAttributes.classEntries(nestMembers));
    }
}
