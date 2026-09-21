package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import jdk.internal.classfile.impl.TypedAttributes;

// `SourceDebugExtension` (JVMS §4.7.11): free bytes for the debugger, in practice JSR-45's SMAP map
// relating the bytecode to a source that is not Java (a JSP, say). The JVMS says it is modified UTF-8
// but imposes no structure, so the API hands it over raw.
public interface SourceDebugExtensionAttribute
        extends Attribute<SourceDebugExtensionAttribute>, ClassElement {

    /** A copy of the body. */
    byte[] contents();

    /** The attribute with these bytes. */
    public static SourceDebugExtensionAttribute of(byte[] contents) {
        return TypedAttributes.sourceDebugExtension(contents);
    }
}
