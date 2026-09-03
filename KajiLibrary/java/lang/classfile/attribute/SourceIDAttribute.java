package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `SourceID`: el identificador que la herramienta de construcción le puso al fuente. No está en el
// JVMS —es de la implementación de referencia— y va de la mano con `CompilationID`.
public interface SourceIDAttribute extends Attribute<SourceIDAttribute>, ClassElement {

    /** El identificador. */
    Utf8Entry sourceId();

    /** El atributo con este identificador. */
    public static SourceIDAttribute of(Utf8Entry sourceId) {
        return TypedAttributes.sourceId(sourceId);
    }

    /** El atributo con este identificador. */
    public static SourceIDAttribute of(String sourceId) {
        return TypedAttributes.sourceId(TypedAttributes.utf8(sourceId));
    }
}
