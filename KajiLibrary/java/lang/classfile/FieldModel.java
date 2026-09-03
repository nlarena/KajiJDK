package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.Optional;

// Un campo ya leído. Es también un {@link ClassElement}: al recorrer la clase, cada campo aparece
// como una de sus piezas.
public interface FieldModel extends CompoundElement<FieldElement>, AttributedElement, ClassElement {

    /** El `access_flags` del campo. */
    AccessFlags flags();

    /** La clase que lo declara, si este modelo salió de leer una. */
    Optional<ClassModel> parent();

    /** El nombre. */
    Utf8Entry fieldName();

    /** El descriptor. */
    Utf8Entry fieldType();

    /** El tipo del campo. */
    default ClassDesc fieldTypeSymbol() {
        return ClassDesc.ofDescriptor(fieldType().stringValue());
    }
}
