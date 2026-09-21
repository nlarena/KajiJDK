package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `LineNumberTable` (JVMS §4.7.12): the map from bci to source line. It is optional and only good
// for debugging; without it, a stack trace cannot say which line it was on.
public interface LineNumberTableAttribute extends Attribute<LineNumberTableAttribute> {

    /** The rows, in file order. */
    List<LineNumberInfo> lineNumbers();

    /** The attribute with these rows. */
    public static LineNumberTableAttribute of(List<LineNumberInfo> lines) {
        return TypedAttributes.lineNumberTable(lines);
    }
}
