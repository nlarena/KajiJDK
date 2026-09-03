package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import jdk.internal.classfile.impl.Instructions;

// Una fila del `CharacterRangeTable`, el atributo que `javac -Xjcov` emite para mapear un tramo de
// bytecode a un tramo de CARACTERES del fuente, con más precisión que el número de línea. No es del
// JVMS: es una extensión de la implementación de referencia, y por eso las banderas de acá no
// figuran en ninguna sección del estándar.
//
// El par de enteros de `characterRangeStart()` y `characterRangeEnd()` empaqueta línea y columna:
// los diez bits bajos son la columna y el resto la línea.
public interface CharacterRange extends PseudoInstruction {

    /** El rango cubre una sentencia. */
    public static final int FLAG_STATEMENT = 0x0001;
    /** El rango cubre un bloque. */
    public static final int FLAG_BLOCK = 0x0002;
    /** El rango cubre una asignación. */
    public static final int FLAG_ASSIGNMENT = 0x0004;
    /** El rango cubre la condición que decide un salto. */
    public static final int FLAG_FLOW_CONTROLLER = 0x0008;
    /** El rango es el destino de un salto. */
    public static final int FLAG_FLOW_TARGET = 0x0010;
    /** El rango cubre una invocación. */
    public static final int FLAG_INVOKE = 0x0020;
    /** El rango cubre una creación de objeto. */
    public static final int FLAG_CREATE = 0x0040;
    /** El rango es la rama verdadera de una condición. */
    public static final int FLAG_BRANCH_TRUE = 0x0080;
    /** El rango es la rama falsa de una condición. */
    public static final int FLAG_BRANCH_FALSE = 0x0100;

    /** Dónde empieza el tramo de bytecode. */
    Label startScope();

    /** Dónde termina, sin incluirlo. */
    Label endScope();

    /** Línea y columna donde empieza el tramo de fuente. */
    int characterRangeStart();

    /** Línea y columna donde termina. */
    int characterRangeEnd();

    /** Las banderas `FLAG_*` combinadas con or. */
    int flags();

    /** La fila con estos valores. */
    public static CharacterRange of(Label startScope, Label endScope, int characterRangeStart,
            int characterRangeEnd, int flags) {
        return Instructions.characterRange(startScope, endScope, characterRangeStart,
                characterRangeEnd, flags);
    }
}
