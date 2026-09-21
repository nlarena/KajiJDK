package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `LocalVariableTypeTable` (JVMS §4.7.14): the generic signatures of the local variables whose type
// cannot be written as a descriptor. It coexists with `LocalVariableTable` and does not replace
// it.
public interface LocalVariableTypeTableAttribute
        extends Attribute<LocalVariableTypeTableAttribute> {

    /** The rows, in file order. */
    List<LocalVariableTypeInfo> localVariableTypes();

    /** The attribute with these rows. */
    public static LocalVariableTypeTableAttribute of(List<LocalVariableTypeInfo> localVariableTypes) {
        return TypedAttributes.localVariableTypeTable(localVariableTypes);
    }
}
