package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.MethodTypeDesc;
import jdk.internal.classfile.impl.Instructions;

// Una invocación que no es `invokedynamic`. Dos rarezas del formato quedan expuestas acá porque no
// se pueden esconder:
//
//   1. `isInterface()` NO se deduce del opcode. `invokestatic` e `invokespecial` pueden apuntar
//      tanto a un `CONSTANT_Methodref` como a un `CONSTANT_InterfaceMethodref`, y cuál de los dos
//      sea cambia cómo la JVM resuelve el método.
//   2. `count()` es el byte `count` que sólo `invokeinterface` lleva, y vale 0 en las otras tres.
//      Es redundante con el descriptor —la JVM podría calcularlo— pero el archivo lo guarda igual.
public interface InvokeInstruction extends Instruction {

    /** La entrada del pool con el método. */
    MemberRefEntry method();

    /** Si la referencia es un `CONSTANT_InterfaceMethodref`. */
    boolean isInterface();

    /** El `count` de `invokeinterface`; 0 en las demás. */
    int count();

    /** La clase o interfaz que declara el método. */
    default ClassEntry owner() {
        return method().owner();
    }

    /** El nombre del método. */
    default Utf8Entry name() {
        return method().nameAndType().name();
    }

    /** El descriptor del método, como `Utf8`. */
    default Utf8Entry type() {
        return method().nameAndType().type();
    }

    /** El descriptor del método. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }

    /** La invocación de este opcode a este método. */
    public static InvokeInstruction of(Opcode op, MemberRefEntry method) {
        return Instructions.invoke(op, method);
    }

    /** La invocación de este opcode al método `name` de tipo `type` en `owner`. */
    public static InvokeInstruction of(Opcode op, ClassEntry owner, Utf8Entry name, Utf8Entry type,
            boolean isInterface) {
        return Instructions.invoke(op, owner, name, type, isInterface);
    }

    /** La invocación de este opcode al método que nombra `nameAndType` en `owner`. */
    public static InvokeInstruction of(Opcode op, ClassEntry owner, NameAndTypeEntry nameAndType,
            boolean isInterface) {
        return Instructions.invoke(op, owner, nameAndType, isInterface);
    }
}
