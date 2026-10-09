package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;

// An attribute (JVMS §4.7): a name and a body whose format depends on the name. The type parameter
// refers to itself --`Attribute<A extends Attribute<A>>`-- so that `attributeMapper()` returns *this*
// attribute's mapper and not just any.
public interface Attribute<A extends Attribute<A>> extends ClassFileElement {

    /** The `Utf8` with the attribute's name. */
    Utf8Entry attributeName();

    /** The mapper that knows how to read and write it. */
    AttributeMapper<A> attributeMapper();
}
