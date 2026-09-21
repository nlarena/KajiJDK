package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.MethodTypeDesc;
import java.util.Optional;

// A method already read. It is also a {@link ClassElement}, for the same reason as
// {@link FieldModel}.
public interface MethodModel extends CompoundElement<MethodElement>, AttributedElement, ClassElement {

    /** The method's `access_flags`. */
    AccessFlags flags();

    /** The class declaring it, if this model came out of reading one. */
    Optional<ClassModel> parent();

    /** The name. */
    Utf8Entry methodName();

    /** The descriptor. */
    Utf8Entry methodType();

    /** The method's type. */
    default MethodTypeDesc methodTypeSymbol() {
        return MethodTypeDesc.ofDescriptor(methodType().stringValue());
    }

    /** The body; empty on an `abstract` or a `native`. */
    Optional<CodeModel> code();
}
