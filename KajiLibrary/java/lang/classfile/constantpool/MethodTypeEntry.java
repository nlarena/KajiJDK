package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;

// `CONSTANT_MethodType_info` (JVMS §4.4.9): a method descriptor used as a loadable constant. With
// `ldc` it produces a `java.lang.invoke.MethodType`.
public interface MethodTypeEntry extends LoadableConstantEntry {

    /** The `Utf8` entry holding the descriptor. */
    Utf8Entry descriptor();

    /** The method type's nominal descriptor. */
    MethodTypeDesc asSymbol();

    /** Whether this entry describes exactly `desc`. */
    boolean matches(MethodTypeDesc desc);

    default ConstantDesc constantValue() {
        return asSymbol();
    }
}
