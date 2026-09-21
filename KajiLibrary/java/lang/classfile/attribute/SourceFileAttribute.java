package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `SourceFile` (JVMS §4.7.10): the source file's NAME, with no directory. It is not a path and it is
// no good for opening it; it is what a debugger uses along with the class's package to look for it.
public interface SourceFileAttribute extends Attribute<SourceFileAttribute>, ClassElement {

    /** The source file's name. */
    Utf8Entry sourceFile();

    /** The attribute with this name. */
    public static SourceFileAttribute of(String sourceFile) {
        return TypedAttributes.sourceFile(TypedAttributes.utf8(sourceFile));
    }

    /** The attribute with this name. */
    public static SourceFileAttribute of(Utf8Entry sourceFile) {
        return TypedAttributes.sourceFile(sourceFile);
    }
}
