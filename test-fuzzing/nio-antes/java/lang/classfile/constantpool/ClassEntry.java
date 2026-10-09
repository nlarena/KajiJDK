package java.lang.classfile.constantpool;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;

// `CONSTANT_Class_info` (JVMS §4.4.1). Its `name_index` points at a `Utf8` holding the class's
// *internal name* (`java/lang/String`) or, for an array, its descriptor directly (`[[I`). That
// asymmetry of the format is the reason `asInternalName()` and `asSymbol()` are different things.
public interface ClassEntry extends LoadableConstantEntry {

    /** The `Utf8` entry holding the name, exactly as it is in the file. */
    Utf8Entry name();

    /** The internal name: `java/lang/String`, or `[[I` if it is an array. */
    String asInternalName();

    /** The class's or the array's nominal descriptor. */
    ClassDesc asSymbol();

    /** Whether this entry names exactly `desc`. */
    boolean matches(ClassDesc desc);

    /** A `CONSTANT_Class` loaded with `ldc` gives a `Class`; its descriptor is the
     * `ClassDesc`. */
    default ConstantDesc constantValue() {
        return asSymbol();
    }
}
