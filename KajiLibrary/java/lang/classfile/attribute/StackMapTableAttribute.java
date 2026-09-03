package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `StackMapTable` (JVMS §4.7.4): los cuadros de tipos que el verificador usa para comprobar el
// método de una pasada en vez de por punto fijo. Es obligatorio desde la versión mayor 50 en todo
// método con saltos hacia atrás o con manejadores, y un archivo al que le falte o lo tenga mal es
// rechazado con `VerifyError` al cargar.
public interface StackMapTableAttribute extends Attribute<StackMapTableAttribute>, CodeElement {

    /** Los cuadros, en el orden del archivo. */
    List<StackMapFrameInfo> entries();

    /** El atributo con estos cuadros. */
    public static StackMapTableAttribute of(List<StackMapFrameInfo> entries) {
        return TypedAttributes.stackMapTable(entries);
    }
}
