package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;

// `CONSTANT_MethodType_info` (JVMS §4.4.9): un descriptor de método usado como constante cargable.
// Con `ldc` produce un `java.lang.invoke.MethodType`.
public interface MethodTypeEntry extends LoadableConstantEntry {

    /** La entrada `Utf8` con el descriptor. */
    Utf8Entry descriptor();

    /** El descriptor nominal del tipo de método. */
    MethodTypeDesc asSymbol();

    /** Si esta entrada describe exactamente a `desc`. */
    boolean matches(MethodTypeDesc desc);

    default ConstantDesc constantValue() {
        return asSymbol();
    }
}
