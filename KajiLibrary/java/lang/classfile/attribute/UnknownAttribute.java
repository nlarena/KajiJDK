package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;

// An attribute whose name is neither in {@link java.lang.classfile.Attributes} nor among the reader's
// custom mappers. The format makes it possible to skip it --the length is in the header-- and this
// interface also allows keeping it: the name and the bytes come out just as they went in.
//
// It has no factory, and that is no oversight: an unknown attribute only shows up when READING. To
// invent one there is {@link java.lang.classfile.CustomAttribute}, which brings its own mapper.
public interface UnknownAttribute extends Attribute<UnknownAttribute>, ClassElement, MethodElement,
        FieldElement, CodeElement {

    /** A copy of the attribute's body, without the name or the length. */
    byte[] contents();
}
