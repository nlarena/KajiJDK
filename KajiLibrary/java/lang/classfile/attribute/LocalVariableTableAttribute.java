package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `LocalVariableTable` (JVMS §4.7.13): the names and types of the local variables. Optional; without
// it a debugger sees the slots but does not know what they were called.
public interface LocalVariableTableAttribute extends Attribute<LocalVariableTableAttribute> {

    /** The rows, in file order. */
    List<LocalVariableInfo> localVariables();

    /** The attribute with these rows. */
    public static LocalVariableTableAttribute of(List<LocalVariableInfo> localVariables) {
        return TypedAttributes.localVariableTable(localVariables);
    }
}
