package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.MethodTypeDesc;
import java.util.Optional;

// Un método ya leído. Es también un {@link ClassElement}, por lo mismo que {@link FieldModel}.
public interface MethodModel extends CompoundElement<MethodElement>, AttributedElement, ClassElement {

    /** El `access_flags` del método. */
    AccessFlags flags();

    /** La clase que lo declara, si este modelo salió de leer una. */
    Optional<ClassModel> parent();

    /** El nombre. */
    Utf8Entry methodName();

    /** El descriptor. */
    Utf8Entry methodType();

    /** El tipo del método. */
    default MethodTypeDesc methodTypeSymbol() {
        return MethodTypeDesc.ofDescriptor(methodType().stringValue());
    }

    /** El cuerpo; vacío en un `abstract` o un `native`. */
    Optional<CodeModel> code();
}
