package java.lang.classfile;

import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.util.Optional;

// A field already read. It is also a {@link ClassElement}: when walking the class, each field shows
// up as one of its pieces.
public interface FieldModel extends CompoundElement<FieldElement>, AttributedElement, ClassElement {

    /** The field's `access_flags`. */
    AccessFlags flags();

    /** The class declaring it, if this model came out of reading one. */
    Optional<ClassModel> parent();

    /** The name. */
    Utf8Entry fieldName();

    /** The descriptor. */
    Utf8Entry fieldType();

    /** The field's type. */
    default ClassDesc fieldTypeSymbol() {
        return ClassDesc.ofDescriptor(fieldType().stringValue());
    }
}
