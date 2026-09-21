package java.lang.classfile;

// An instruction of the `code` array (JVMS §6). All they have in common is which opcode they are and
// how many bytes they take; the rest --operands, jump target, pool entry-- each subtype says.
public interface Instruction extends CodeElement {

    /** The opcode. */
    Opcode opcode();

    /** How many bytes it takes, the opcode included. */
    int sizeInBytes();
}
