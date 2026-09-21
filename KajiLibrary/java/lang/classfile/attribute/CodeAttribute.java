package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Label;

// `Code` (JVMS §4.7.3) seen as an attribute. It is the LOW-level view of the same object
// {@link CodeModel} shows piece by piece: here are the raw bytes and the bci, there the instructions.
// The two are the same instance -- which is why this interface extends that one.
//
// It has no factory: a `Code` only exists inside a method and its contents are built with a
// `CodeBuilder`, which is also what resolves labels to bci. Making one loose would give an attribute
// whose `labelToBci` means nothing.
public interface CodeAttribute extends Attribute<CodeAttribute>, CodeModel {

    /** The attribute's `max_locals`. */
    int maxLocals();

    /** The attribute's `max_stack`. */
    int maxStack();

    /** The length of the `code` array. */
    int codeLength();

    /** A copy of the `code` array. */
    byte[] codeArray();

    /** This label's bci, or -1 if it does not belong to this body. */
    int labelToBci(Label label);
}
