package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;

// Una fila de `LocalVariableTable` (JVMS §4.7.13), con los bci crudos en vez de etiquetas. La
// versión con etiquetas es {@link java.lang.classfile.instruction.LocalVariable}.
//
// No tiene fábrica, y en el JDK tampoco: una fila con bci crudos sólo tiene sentido dentro del
// atributo del que se leyó, porque los números son posiciones de ESE arreglo `code`. Para armar una
// tabla nueva está la versión con etiquetas.
public interface LocalVariableInfo {

    /** El bci donde empieza el alcance. */
    int startPc();

    /** Cuántos bytes dura el alcance. */
    int length();

    /** El nombre de la variable. */
    Utf8Entry name();

    /** El descriptor del tipo. */
    Utf8Entry type();

    /** El tipo de la variable. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** La ranura de variable local. */
    int slot();
}
