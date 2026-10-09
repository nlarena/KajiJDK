package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;

// A row of `LocalVariableTable` (JVMS §4.7.13), with raw bci instead of labels. The version with
// labels is {@link java.lang.classfile.instruction.LocalVariable}.
//
// It has no factory, and it has none in the JDK either: a row with raw bci only makes sense inside
// the attribute it was read from, because the numbers are positions of THAT `code` array. To build a
// new table there is the version with labels.
public interface LocalVariableInfo {

    /** The bci where the scope starts. */
    int startPc();

    /** How many bytes the scope lasts. */
    int length();

    /** The variable's name. */
    Utf8Entry name();

    /** The type's descriptor. */
    Utf8Entry type();

    /** The variable's type. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** The local variable slot. */
    int slot();
}
