package jdk.internal.classfile.impl;

import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.ClassElement;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.constantpool.Utf8Entry;

// An attribute read without interpreting it: the name's `Utf8`, the mapper that recognised it and
// the body in bytes. It is what `ClassModel` returns for EVERY attribute it reads, including the
// ones the JVMS defines -- see the scope note in `java.lang.classfile.Attributes`. The note said it
// is what KajiLibrary returns for all attributes; the typed ones exist too (`TypedAttributes`,
// read and written through `AttributeMappers`), and they are what the builders write -- it is
// reading a class that still yields this.
//
// It implements the four element interfaces because an attribute may appear in the four places
// where the format admits them, and the model that contains it emits it as one of its pieces.
public final class RawAttribute
        implements Attribute<RawAttribute>, ClassElement, MethodElement, FieldElement, CodeElement {

    private final Utf8Entry name;
    private final AttributeMapper<RawAttribute> mapper;
    private final byte[] payload;

    public RawAttribute(Utf8Entry name, AttributeMapper<RawAttribute> mapper, byte[] payload) {
        this.name = name;
        this.mapper = mapper;
        this.payload = payload;
    }

    public Utf8Entry attributeName() {
        return this.name;
    }

    public AttributeMapper<RawAttribute> attributeMapper() {
        return this.mapper;
    }

    /** A copy of the attribute's body, without the name or the length. */
    public byte[] payload() {
        byte[] copy = new byte[this.payload.length];
        System.arraycopy(this.payload, 0, copy, 0, this.payload.length);
        return copy;
    }

    /** The length of the body. */
    public int payloadLength() {
        return this.payload.length;
    }

    // Without copying: for whoever writes the attribute back.
    byte[] raw() {
        return this.payload;
    }

    public String toString() {
        return "Attribute[" + this.name.stringValue() + ", " + this.payload.length + " bytes]";
    }
}
