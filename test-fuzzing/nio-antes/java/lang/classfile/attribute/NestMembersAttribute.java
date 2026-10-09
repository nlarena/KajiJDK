package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `NestMembers` (JVMS §4.7.29): the members of the nest this class heads. See the note on
// {@link NestHostAttribute}: a class carries this attribute or the other one, never both.
public interface NestMembersAttribute extends Attribute<NestMembersAttribute>, ClassElement {

    /** The nest's members. */
    List<ClassEntry> nestMembers();

    /** The attribute with these members. */
    public static NestMembersAttribute of(List<ClassEntry> nestMembers) {
        return TypedAttributes.nestMembers(nestMembers);
    }

    /** The attribute with these members. */
    public static NestMembersAttribute of(ClassEntry... nestMembers) {
        return TypedAttributes.nestMembers(TypedAttributes.listOfClasses(nestMembers));
    }

    /** The attribute with these members. */
    public static NestMembersAttribute ofSymbols(List<ClassDesc> nestMembers) {
        return TypedAttributes.nestMembers(TypedAttributes.classEntries(nestMembers));
    }

    /** The attribute with these members. */
    public static NestMembersAttribute ofSymbols(ClassDesc... nestMembers) {
        return TypedAttributes.nestMembers(TypedAttributes.classEntries(nestMembers));
    }
}
