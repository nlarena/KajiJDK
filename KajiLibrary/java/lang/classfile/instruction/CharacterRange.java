package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import jdk.internal.classfile.impl.Instructions;

// A row of the `CharacterRangeTable`, the attribute `javac -Xjcov` emits in order to map a stretch of
// bytecode to a stretch of source CHARACTERS, more precisely than the line number does. It is not
// from the JVMS: it is an extension of the reference implementation, and that is why the flags here
// appear in no section of the standard.
//
// The pair of integers in `characterRangeStart()` and `characterRangeEnd()` packs line and column:
// the low ten bits are the column and the rest the line.
public interface CharacterRange extends PseudoInstruction {

    /** The range covers a statement. */
    public static final int FLAG_STATEMENT = 0x0001;
    /** The range covers a block. */
    public static final int FLAG_BLOCK = 0x0002;
    /** The range covers an assignment. */
    public static final int FLAG_ASSIGNMENT = 0x0004;
    /** The range covers the condition that decides a branch. */
    public static final int FLAG_FLOW_CONTROLLER = 0x0008;
    /** The range is a branch's destination. */
    public static final int FLAG_FLOW_TARGET = 0x0010;
    /** The range covers an invocation. */
    public static final int FLAG_INVOKE = 0x0020;
    /** The range covers an object creation. */
    public static final int FLAG_CREATE = 0x0040;
    /** The range is a condition's true branch. */
    public static final int FLAG_BRANCH_TRUE = 0x0080;
    /** The range is a condition's false branch. */
    public static final int FLAG_BRANCH_FALSE = 0x0100;

    /** Where the stretch of bytecode begins. */
    Label startScope();

    /** Where it ends, exclusive. */
    Label endScope();

    /** Line and column where the stretch of source begins. */
    int characterRangeStart();

    /** Line and column where it ends. */
    int characterRangeEnd();

    /** The `FLAG_*` flags combined with or. */
    int flags();

    /** The row with these values. */
    public static CharacterRange of(Label startScope, Label endScope, int characterRangeStart,
            int characterRangeEnd, int flags) {
        return Instructions.characterRange(startScope, endScope, characterRangeStart,
                characterRangeEnd, flags);
    }
}
