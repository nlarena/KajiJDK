package java.lang.classfile.attribute;

import jdk.internal.classfile.impl.TypedAttributes;

// A row of `CharacterRangeTable`, the "table" version of
// {@link java.lang.classfile.instruction.CharacterRange}: where that one uses labels, this one uses
// the file's raw bci. See there for the explanation of the attribute and of how line and column are
// packed into an `int`.
public interface CharacterRangeInfo {

    /** The bci where the bytecode stretch starts. */
    int startPc();

    /** The bci where it ends, exclusive. */
    int endPc();

    /** Line and column where the source stretch starts. */
    int characterRangeStart();

    /** Line and column where it ends. */
    int characterRangeEnd();

    /** The `CharacterRange.FLAG_*` flags or-ed together. */
    int flags();

    /** The row with these values. */
    public static CharacterRangeInfo of(int startPc, int endPc, int characterRangeStart,
            int characterRangeEnd, int flags) {
        return TypedAttributes.characterRangeInfo(startPc, endPc, characterRangeStart,
                characterRangeEnd, flags);
    }
}
