package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `CompilationID`: the id of the compilation that produced the class. It is not in the JVMS either;
// the reference implementation emits it along with `SourceID` so a `.class` can be correlated with
// the run that generated it.
public interface CompilationIDAttribute extends Attribute<CompilationIDAttribute>, ClassElement {

    /** The id. */
    Utf8Entry compilationId();

    /** The attribute with this id. */
    public static CompilationIDAttribute of(Utf8Entry compilationId) {
        return TypedAttributes.compilationId(compilationId);
    }

    /** The attribute with this id. */
    public static CompilationIDAttribute of(String compilationId) {
        return TypedAttributes.compilationId(TypedAttributes.utf8(compilationId));
    }
}
