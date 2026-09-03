package java.lang.classfile.constantpool;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;

// `CONSTANT_Class_info` (JVMS §4.4.1). Su `name_index` apunta a un `Utf8` con el *nombre interno*
// de la clase (`java/lang/String`) o, para un arreglo, directamente con su descriptor (`[[I`). Esa
// asimetría del formato es la razón de que `asInternalName()` y `asSymbol()` sean cosas distintas.
public interface ClassEntry extends LoadableConstantEntry {

    /** La entrada `Utf8` con el nombre, tal cual está en el archivo. */
    Utf8Entry name();

    /** El nombre interno: `java/lang/String`, o `[[I` si es un arreglo. */
    String asInternalName();

    /** El descriptor nominal de la clase o del arreglo. */
    ClassDesc asSymbol();

    /** Si esta entrada nombra exactamente a `desc`. */
    boolean matches(ClassDesc desc);

    /** Una `CONSTANT_Class` cargada con `ldc` da un `Class`; su descriptor es el `ClassDesc`. */
    default ConstantDesc constantValue() {
        return asSymbol();
    }
}
