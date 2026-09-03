package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `LocalVariableTypeTable` (JVMS §4.7.14): las firmas genéricas de las variables locales cuyo tipo
// no se puede escribir como descriptor. Convive con `LocalVariableTable` y no la reemplaza.
public interface LocalVariableTypeTableAttribute
        extends Attribute<LocalVariableTypeTableAttribute> {

    /** Las filas, en el orden del archivo. */
    List<LocalVariableTypeInfo> localVariableTypes();

    /** El atributo con estas filas. */
    public static LocalVariableTypeTableAttribute of(List<LocalVariableTypeInfo> localVariableTypes) {
        return TypedAttributes.localVariableTypeTable(localVariableTypes);
    }
}
