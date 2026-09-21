package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeElement;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// `StackMapTable` (JVMS §4.7.4): the type frames the verifier uses to check the method in one pass
// instead of by fixed point. It is mandatory from major version 50 on in every method with backward
// jumps or with handlers, and a file missing it or getting it wrong is rejected with `VerifyError` at
// load time.
public interface StackMapTableAttribute extends Attribute<StackMapTableAttribute>, CodeElement {

    /** The frames, in file order. */
    List<StackMapFrameInfo> entries();

    /** The attribute with these frames. */
    public static StackMapTableAttribute of(List<StackMapFrameInfo> entries) {
        return TypedAttributes.stackMapTable(entries);
    }
}
