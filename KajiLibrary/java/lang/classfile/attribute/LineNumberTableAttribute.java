package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `LineNumberTable` (JVMS §4.7.12): el mapa de bci a línea del fuente. Es opcional y sólo sirve para
// depurar; sin él, una traza de pila no puede decir en qué línea estaba.
public interface LineNumberTableAttribute extends Attribute<LineNumberTableAttribute> {

    /** Las filas, en el orden del archivo. */
    List<LineNumberInfo> lineNumbers();

    /** El atributo con estas filas. */
    public static LineNumberTableAttribute of(List<LineNumberInfo> lines) {
        return TypedAttributes.lineNumberTable(lines);
    }
}
