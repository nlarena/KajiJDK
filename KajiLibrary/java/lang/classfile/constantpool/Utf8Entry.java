package java.lang.classfile.constantpool;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

// `CONSTANT_Utf8_info` (JVMS §4.4.7): la cadena en UTF-8 modificado que está debajo de casi todo lo
// demás — nombres de clase, de miembro, descriptores, nombres de atributo y el contenido de un
// `CONSTANT_String`.
//
// Es un `CharSequence` a propósito: permite comparar contra un nombre sin materializar el `String`.
// `isFieldType`/`isMethodType` existen por lo mismo — comparar la cadena cruda contra el descriptor
// de un `ClassDesc` evita construir el descriptor del otro lado.
public interface Utf8Entry extends CharSequence, AnnotationConstantValueEntry {

    /** El contenido como `String`. */
    String stringValue();

    /** Si el contenido es exactamente `s`, sin construir el `String` intermedio. */
    boolean equalsString(String s);

    /** Si el contenido es el descriptor de campo de `desc`. */
    boolean isFieldType(ClassDesc desc);

    /** Si el contenido es el descriptor de método de `desc`. */
    boolean isMethodType(MethodTypeDesc desc);
}
