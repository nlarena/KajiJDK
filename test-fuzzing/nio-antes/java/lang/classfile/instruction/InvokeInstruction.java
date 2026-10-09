package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.MethodTypeDesc;
import jdk.internal.classfile.impl.Instructions;

// An invocation that is not `invokedynamic`. Two oddities of the format are exposed here because
// they cannot be hidden:
//
//   1. `isInterface()` is NOT worked out from the opcode. `invokestatic` and `invokespecial` may
//      point either at a `CONSTANT_Methodref` or at a `CONSTANT_InterfaceMethodref`, and which of the
//      two it is changes how the JVM resolves the method.
//   2. `count()` is the `count` byte only `invokeinterface` carries, and it is 0 in the other three.
//      It is redundant with the descriptor --the JVM could work it out-- but the file stores it all
//      the same.
public interface InvokeInstruction extends Instruction {

    /** The pool entry holding the method. */
    MemberRefEntry method();

    /** Whether the reference is a `CONSTANT_InterfaceMethodref`. */
    boolean isInterface();

    /** `invokeinterface`'s `count`; 0 in the others. */
    int count();

    /** The class or interface that declares the method. */
    default ClassEntry owner() {
        return method().owner();
    }

    /** The method's name. */
    default Utf8Entry name() {
        return method().nameAndType().name();
    }

    /** The method's descriptor, as a `Utf8`. */
    default Utf8Entry type() {
        return method().nameAndType().type();
    }

    /** The method's descriptor. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }

    /** This opcode's invocation of this method. */
    public static InvokeInstruction of(Opcode op, MemberRefEntry method) {
        return Instructions.invoke(op, method);
    }

    /** This opcode's invocation of the method `name` of type `type` in `owner`. */
    public static InvokeInstruction of(Opcode op, ClassEntry owner, Utf8Entry name, Utf8Entry type,
            boolean isInterface) {
        return Instructions.invoke(op, owner, name, type, isInterface);
    }

    /** This opcode's invocation of the method `nameAndType` names in `owner`. */
    public static InvokeInstruction of(Opcode op, ClassEntry owner, NameAndTypeEntry nameAndType,
            boolean isInterface) {
        return Instructions.invoke(op, owner, nameAndType, isInterface);
    }
}
