package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `Record` (JVMS §4.7.30): la lista de componentes de un `record`. Su presencia es lo que hace que
// la JVM trate a la clase como record; el bit de acceso no alcanza.
public interface RecordAttribute extends Attribute<RecordAttribute>, ClassElement {

    /** Los componentes, en el orden de la declaración. */
    List<RecordComponentInfo> components();

    /** El atributo con estos componentes. */
    public static RecordAttribute of(List<RecordComponentInfo> components) {
        return TypedAttributes.record(components);
    }

    /** El atributo con estos componentes. */
    public static RecordAttribute of(RecordComponentInfo... components) {
        return TypedAttributes.record(TypedAttributes.listOf(components));
    }
}
