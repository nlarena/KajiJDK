package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `CompilationID`: el identificador de la compilación que produjo la clase. Tampoco está en el
// JVMS; lo emite la implementación de referencia junto con `SourceID` para poder correlacionar un
// `.class` con la corrida que lo generó.
public interface CompilationIDAttribute extends Attribute<CompilationIDAttribute>, ClassElement {

    /** El identificador. */
    Utf8Entry compilationId();

    /** El atributo con este identificador. */
    public static CompilationIDAttribute of(Utf8Entry compilationId) {
        return TypedAttributes.compilationId(compilationId);
    }

    /** El atributo con este identificador. */
    public static CompilationIDAttribute of(String compilationId) {
        return TypedAttributes.compilationId(TypedAttributes.utf8(compilationId));
    }
}
