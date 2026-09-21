package java.lang.classfile.attribute;

import java.lang.classfile.Label;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.util.List;
import jdk.internal.classfile.impl.TypedAttributes;

// A frame of `StackMapTable` (JVMS §4.7.4): the type state --local variables and operand stack-- the
// verifier expects at a point of the code that can be reached by more than one path.
//
// The format stores the frames COMPRESSED: each one is expressed as a difference against the previous
// one (`same_frame`, `chop_frame`, `append_frame`, ...) and the bci is a delta. This interface hands
// them over already expanded --`locals()` and `stack()` are the complete state-- because a difference
// only means something at the position where it sits. `frameType()` keeps the original byte, which is
// what is needed to rewrite the attribute exactly as it was.
public interface StackMapFrameInfo {

    /** The original `frame_type` byte. */
    int frameType();

    /** The bci the frame corresponds to. */
    Label target();

    /** The local variables, expanded. */
    List<VerificationTypeInfo> locals();

    /** The operand stack, expanded. */
    List<VerificationTypeInfo> stack();

    /** A frame with this state; which compressed form the file will use is not the caller's to
     * choose. */
    public static StackMapFrameInfo of(Label target, List<VerificationTypeInfo> locals,
            List<VerificationTypeInfo> stack) {
        return TypedAttributes.stackMapFrame(target, locals, stack);
    }

    /** A verifier type: what is in a slot or at a position of the stack. */
    public interface VerificationTypeInfo {

        /** `top`: the slot holds no usable value. */
        public static final int ITEM_TOP = 0;
        /** `int`, and also `boolean`, `byte`, `char` and `short`. */
        public static final int ITEM_INTEGER = 1;
        /** `float`. */
        public static final int ITEM_FLOAT = 2;
        /** `double`. */
        public static final int ITEM_DOUBLE = 3;
        /** `long`. */
        public static final int ITEM_LONG = 4;
        /** `null`. */
        public static final int ITEM_NULL = 5;
        /** A constructor's `this`, before calling the superclass's. */
        public static final int ITEM_UNINITIALIZED_THIS = 6;
        /** A reference to a concrete class. */
        public static final int ITEM_OBJECT = 7;
        /** An object just created by a `new` that has not been initialised yet. */
        public static final int ITEM_UNINITIALIZED = 8;

        /** The `ITEM_*` tag. */
        int tag();
    }

    /** The seven verifier types carrying nothing but their tag. */
    public enum SimpleVerificationTypeInfo implements VerificationTypeInfo {

        TOP(0),
        INTEGER(1),
        FLOAT(2),
        DOUBLE(3),
        LONG(4),
        NULL(5),
        UNINITIALIZED_THIS(6);

        private final int tag;

        private SimpleVerificationTypeInfo(int tag) {
            this.tag = tag;
        }

        public int tag() {
            return this.tag;
        }
    }

    /** A reference to a concrete class. */
    public interface ObjectVerificationTypeInfo extends VerificationTypeInfo {

        /** The class. */
        ClassEntry className();

        /** The class. */
        default ClassDesc classSymbol() {
            return className().asSymbol();
        }

        /** This class's type. */
        public static ObjectVerificationTypeInfo of(ClassEntry className) {
            return TypedAttributes.objectVerificationType(className);
        }

        /** This class's type. */
        public static ObjectVerificationTypeInfo of(ClassDesc classDesc) {
            return TypedAttributes.objectVerificationType(TypedAttributes.classEntry(classDesc));
        }
    }

    /**
     * An object created by the `new` sitting at `newTarget()` and not yet initialised. The `new`'s bci
     * is part of the TYPE: two uninitialised objects created at different places are different types
     * to the verifier, which is what lets it check that each one gets its own `invokespecial`.
     */
    public interface UninitializedVerificationTypeInfo extends VerificationTypeInfo {

        /** The bci of the `new` that created it. */
        Label newTarget();

        /** The type of the object created at `newTarget`. */
        public static UninitializedVerificationTypeInfo of(Label newTarget) {
            return TypedAttributes.uninitializedVerificationType(newTarget);
        }
    }
}
