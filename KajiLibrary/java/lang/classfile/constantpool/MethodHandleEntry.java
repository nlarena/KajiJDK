package java.lang.classfile.constantpool;

import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;

// `CONSTANT_MethodHandle_info` (JVMS §4.4.8): a `reference_kind` from 1 to 9 and the member
// reference it acts on. The `kind` decides which sort of reference is legal: 1..4 demand a
// `Fieldref`, 5, 6, 7 and 9 a `Methodref` or an `InterfaceMethodref`, and 8 a `Methodref` to an
// `<init>`.
public interface MethodHandleEntry extends LoadableConstantEntry {

    /** The `reference_kind`, from 1 to 9. */
    int kind();

    /** The member it points at. */
    MemberRefEntry reference();

    /** The method handle's nominal descriptor. */
    DirectMethodHandleDesc asSymbol();

    default ConstantDesc constantValue() {
        return asSymbol();
    }
}
