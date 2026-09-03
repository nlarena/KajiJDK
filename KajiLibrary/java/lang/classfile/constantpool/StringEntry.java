package java.lang.classfile.constantpool;

// `CONSTANT_String_info` (JVMS §4.4.3): una indirección a un `CONSTANT_Utf8`. La indirección importa
// — el mismo `Utf8` puede ser a la vez el contenido de un `String` y el nombre de un método, y el
// pool guarda una sola copia.
public interface StringEntry extends ConstantValueEntry {

    /** La entrada `Utf8` con el contenido. */
    Utf8Entry utf8();

    /** El contenido como `String`. */
    String stringValue();

    /** Si el contenido es exactamente `s`. */
    boolean equalsString(String s);
}
