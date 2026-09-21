package jdk.internal.classfile.impl;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;

// An instruction decoded from the `code` array: its opcode, its real size and where it starts.
//
// The note said this is what KajiLibrary returns for each instruction, for lack of the
// `java.lang.classfile.instruction` package. **That is no longer so**: the package is here, and
// `Instructions.decode` returns its typed instructions -- `LoadInstruction`, `BranchInstruction`,
// ... -- with their operands interpreted. Nothing constructs this class any more.
public final class RawInstructionImpl implements Instruction {

    private final Opcode opcode;
    private final int bci;
    private final int size;

    RawInstructionImpl(Opcode opcode, int bci, int size) {
        this.opcode = opcode;
        this.bci = bci;
        this.size = size;
    }

    public Opcode opcode() {
        return this.opcode;
    }

    public int sizeInBytes() {
        return this.size;
    }

    /** The offset of this instruction within the `code` array. */
    public int bci() {
        return this.bci;
    }

    public String toString() {
        return this.bci + ": " + this.opcode.name();
    }
}
