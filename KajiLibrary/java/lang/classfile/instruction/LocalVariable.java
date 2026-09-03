package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Instructions;

// Una fila del `LocalVariableTable`: qué nombre y qué tipo tiene la ranura `slot()` entre
// `startScope()` y `endScope()`. La misma ranura puede ser variables distintas en tramos distintos,
// que es lo que hace que el rango sea parte de la identidad de la fila y no un adorno.
public interface LocalVariable extends PseudoInstruction {

    /** La ranura de variable local. */
    int slot();

    /** El nombre de la variable. */
    Utf8Entry name();

    /** El descriptor del tipo, como `Utf8`. */
    Utf8Entry type();

    /** El tipo de la variable. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** Dónde empieza el alcance. */
    Label startScope();

    /** Dónde termina, sin incluirlo. */
    Label endScope();

    /** La fila con estos valores. */
    public static LocalVariable of(int slot, Utf8Entry name, Utf8Entry descriptor,
            Label startScope, Label endScope) {
        return Instructions.localVariable(slot, name, descriptor, startScope, endScope);
    }

    /** La fila con estos valores. */
    public static LocalVariable of(int slot, String name, ClassDesc descriptor, Label startScope,
            Label endScope) {
        return Instructions.localVariable(slot, name, descriptor, startScope, endScope);
    }
}
