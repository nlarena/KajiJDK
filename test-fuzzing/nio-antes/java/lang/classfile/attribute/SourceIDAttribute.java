package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `SourceID`: the id the build tool gave the source. It is not in the JVMS --it is the reference
// implementation's-- and it goes hand in hand with `CompilationID`.
public interface SourceIDAttribute extends Attribute<SourceIDAttribute>, ClassElement {

    /** The id. */
    Utf8Entry sourceId();

    /** The attribute with this id. */
    public static SourceIDAttribute of(Utf8Entry sourceId) {
        return TypedAttributes.sourceId(sourceId);
    }

    /** The attribute with this id. */
    public static SourceIDAttribute of(String sourceId) {
        return TypedAttributes.sourceId(TypedAttributes.utf8(sourceId));
    }
}
