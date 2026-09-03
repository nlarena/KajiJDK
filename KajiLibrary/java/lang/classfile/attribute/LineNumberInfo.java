package java.lang.classfile.attribute;

import jdk.internal.classfile.impl.TypedAttributes;

// Una fila de `LineNumberTable` (JVMS §4.7.12): desde el bci `startPc()`, el código viene de la
// línea `lineNumber()`. La tabla no tiene por qué estar ordenada ni cubrir todo el método.
public interface LineNumberInfo {

    /** El bci donde empieza el tramo. */
    int startPc();

    /** El número de línea del fuente. */
    int lineNumber();

    /** La fila con estos valores. */
    public static LineNumberInfo of(int startPc, int lineNumber) {
        return TypedAttributes.lineNumberInfo(startPc, lineNumber);
    }
}
