package java.lang.classfile.attribute;

import jdk.internal.classfile.impl.TypedAttributes;

// Una fila de `CharacterRangeTable`, la versión "tabla" de
// {@link java.lang.classfile.instruction.CharacterRange}: mientras aquélla usa etiquetas, ésta usa
// los bci crudos del archivo. Ver ahí la explicación del atributo y del empaquetado de línea y
// columna en un `int`.
public interface CharacterRangeInfo {

    /** El bci donde empieza el tramo de bytecode. */
    int startPc();

    /** El bci donde termina, sin incluirlo. */
    int endPc();

    /** Línea y columna donde empieza el tramo de fuente. */
    int characterRangeStart();

    /** Línea y columna donde termina. */
    int characterRangeEnd();

    /** Las banderas `CharacterRange.FLAG_*` combinadas con or. */
    int flags();

    /** La fila con estos valores. */
    public static CharacterRangeInfo of(int startPc, int endPc, int characterRangeStart,
            int characterRangeEnd, int flags) {
        return TypedAttributes.characterRangeInfo(startPc, endPc, characterRangeStart,
                characterRangeEnd, flags);
    }
}
