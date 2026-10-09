package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.Utf8Entry;

// A row of `LocalVariableTypeTable` (JVMS §4.7.14), with raw bci. The same note as in
// {@link LocalVariableInfo} holds on why it has no factory.
public interface LocalVariableTypeInfo {

    /** The bci where the scope starts. */
    int startPc();

    /** How many bytes the scope lasts. */
    int length();

    /** The variable's name. */
    Utf8Entry name();

    /** The generic signature. */
    Utf8Entry signature();

    /** The local variable slot. */
    int slot();
}
