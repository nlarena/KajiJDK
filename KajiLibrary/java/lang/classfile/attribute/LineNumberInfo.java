package java.lang.classfile.attribute;

import jdk.internal.classfile.impl.TypedAttributes;

// A row of `LineNumberTable` (JVMS §4.7.12): from bci `startPc()` on, the code comes from line
// `lineNumber()`. The table need not be sorted nor cover the whole method.
public interface LineNumberInfo {

    /** The bci where the stretch starts. */
    int startPc();

    /** The source line number. */
    int lineNumber();

    /** The row with these values. */
    public static LineNumberInfo of(int startPc, int lineNumber) {
        return TypedAttributes.lineNumberInfo(startPc, lineNumber);
    }
}
