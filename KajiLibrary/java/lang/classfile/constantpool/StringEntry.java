package java.lang.classfile.constantpool;

// `CONSTANT_String_info` (JVMS §4.4.3): an indirection to a `CONSTANT_Utf8`. The indirection matters
// -- the same `Utf8` can be at once a `String`'s contents and a method's name, and the pool keeps a
// single copy.
public interface StringEntry extends ConstantValueEntry {

    /** The `Utf8` entry holding the contents. */
    Utf8Entry utf8();

    /** The contents as a `String`. */
    String stringValue();

    /** Whether the contents are exactly `s`. */
    boolean equalsString(String s);
}
