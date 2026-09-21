package jdk.internal.classfile.impl;

import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributeMapper.AttributeStability;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.BufWriter;
import java.lang.classfile.ClassReader;

// The mapper of an attribute known by its name, which reads and writes it without interpreting it.
// Read: it copies the body. Write: name, length and the same body. That symmetry is what allows
// copying an attribute from one file to another even without knowing what it says.
public final class AttributeMapperImpl implements AttributeMapper<RawAttribute> {

    private final String name;
    private final AttributeStability stability;
    private final boolean allowMultiple;

    public AttributeMapperImpl(String name, AttributeStability stability, boolean allowMultiple) {
        this.name = name;
        this.stability = stability;
        this.allowMultiple = allowMultiple;
    }

    public String name() {
        return this.name;
    }

    // `pos` is the offset of the first byte of the body. The length is read from the four bytes
    // right before it, which is where the format puts it (§4.7).
    public RawAttribute readAttribute(AttributedElement enclosing, ClassReader cf, int pos) {
        int len = cf.readInt(pos - 4);
        if (len < 0 || pos + len > cf.classfileLength()) {
            throw new IllegalArgumentException(
                    "attribute " + this.name + " with length " + len + " past the end of the file");
        }
        // The local in between was needed because the compiler used to erase `T` to its bound when
        // the generic call went straight in as an argument, and then did not find the constructor.
        // The frozen javac compiles the direct form now (checked 2026-09-18); the local is
        // harmless.
        java.lang.classfile.constantpool.Utf8Entry name =
                cf.readEntryOrNull(pos - 6, java.lang.classfile.constantpool.Utf8Entry.class);
        return new RawAttribute(name, this, cf.readBytes(pos, len));
    }

    public void writeAttribute(BufWriter buf, RawAttribute attr) {
        buf.writeIndex(attr.attributeName());
        byte[] body = attr.raw();
        buf.writeInt(body.length);
        buf.writeBytes(body);
    }

    public boolean allowMultiple() {
        return this.allowMultiple;
    }

    public AttributeStability stability() {
        return this.stability;
    }

    public String toString() {
        return "AttributeMapper[" + this.name + "]";
    }
}
