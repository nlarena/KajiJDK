package java.lang.classfile.attribute;

import java.lang.classfile.constantpool.Utf8Entry;

// Una fila de `LocalVariableTypeTable` (JVMS §4.7.14), con los bci crudos. Vale la misma nota que en
// {@link LocalVariableInfo} sobre por qué no tiene fábrica.
public interface LocalVariableTypeInfo {

    /** El bci donde empieza el alcance. */
    int startPc();

    /** Cuántos bytes dura el alcance. */
    int length();

    /** El nombre de la variable. */
    Utf8Entry name();

    /** La firma genérica. */
    Utf8Entry signature();

    /** La ranura de variable local. */
    int slot();
}
