package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;

// `CONSTANT_MethodHandle_info` (JVMS §4.4.8): un `reference_kind` de 1 a 9 y la referencia a miembro
// sobre la que actúa. El `kind` decide qué clase de referencia es legal: 1..4 exigen un `Fieldref`,
// 5, 6, 7 y 9 un `Methodref` o un `InterfaceMethodref`, y 8 un `Methodref` a un `<init>`.
public interface MethodHandleEntry extends LoadableConstantEntry {

    /** El `reference_kind`, de 1 a 9. */
    int kind();

    /** El miembro al que apunta. */
    MemberRefEntry reference();

    /** El descriptor nominal del method handle. */
    DirectMethodHandleDesc asSymbol();

    default ConstantDesc constantValue() {
        return asSymbol();
    }
}
