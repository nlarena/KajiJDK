package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;

// The base class for an attribute the JVMS does not define and an application wants to model all the
// same. Whoever extends it brings their own {@link AttributeMapper}, and with that the attribute goes
// into and comes out of the file like any of the known ones.
//
// It implements the four element interfaces because a custom attribute can appear at any of the four
// places where the format admits attributes.
public abstract class CustomAttribute<T extends CustomAttribute<T>>
        implements Attribute<T>, CodeElement, ClassElement, MethodElement, FieldElement {

    private final AttributeMapper<T> mapper;

    /** With the mapper that knows how to read and write this attribute. */
    protected CustomAttribute(AttributeMapper<T> mapper) {
        this.mapper = mapper;
    }

    public final AttributeMapper<T> attributeMapper() {
        return this.mapper;
    }

    public Utf8Entry attributeName() {
        throw new UnsupportedOperationException(
                "a hand-built CustomAttribute has no pool entry yet");
    }
}
