package java.lang.classfile.instruction;

import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.Signature;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.Instructions;

// Una fila del `LocalVariableTypeTable`. Es la gemela genérica de {@link LocalVariable}: existe
// aparte y no como un campo más de aquélla porque el formato sólo la emite para las variables cuyo
// tipo NO se puede escribir como descriptor, y una variable puede estar en las dos tablas a la vez.
public interface LocalVariableType extends PseudoInstruction {

    /** La ranura de variable local. */
    int slot();

    /** El nombre de la variable. */
    Utf8Entry name();

    /** La firma genérica, como `Utf8`. */
    Utf8Entry signature();

    /** La firma genérica ya analizada. */
    default Signature signatureSymbol() {
        return Signature.parseFrom(signature().stringValue());
    }

    /** Dónde empieza el alcance. */
    Label startScope();

    /** Dónde termina, sin incluirlo. */
    Label endScope();

    /** La fila con estos valores. */
    public static LocalVariableType of(int slot, Utf8Entry name, Utf8Entry signature,
            Label startScope, Label endScope) {
        return Instructions.localVariableType(slot, name, signature, startScope, endScope);
    }

    /** La fila con estos valores. */
    public static LocalVariableType of(int slot, String name, Signature signature, Label startScope,
            Label endScope) {
        return Instructions.localVariableType(slot, name, signature, startScope, endScope);
    }
}
