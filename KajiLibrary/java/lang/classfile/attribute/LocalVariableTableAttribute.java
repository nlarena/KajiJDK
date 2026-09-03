package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `LocalVariableTable` (JVMS §4.7.13): los nombres y tipos de las variables locales. Opcional; sin
// él un depurador ve las ranuras pero no sabe cómo se llamaban.
public interface LocalVariableTableAttribute extends Attribute<LocalVariableTableAttribute> {

    /** Las filas, en el orden del archivo. */
    List<LocalVariableInfo> localVariables();

    /** El atributo con estas filas. */
    public static LocalVariableTableAttribute of(List<LocalVariableInfo> localVariables) {
        return TypedAttributes.localVariableTable(localVariables);
    }
}
