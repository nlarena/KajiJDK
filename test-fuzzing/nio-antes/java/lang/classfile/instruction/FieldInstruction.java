package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Instructions;

// A field access: `getstatic`, `putstatic`, `getfield` or `putfield`. The operand is a
// `CONSTANT_Fieldref`, and the `default` accessors here are shortcuts for climbing down through it
// without writing the whole chain.
public interface FieldInstruction extends Instruction {

    /** The pool entry holding the field. */
    FieldRefEntry field();

    /** The class that declares the field. */
    default ClassEntry owner() {
        return field().owner();
    }

    /** The field's name. */
    default Utf8Entry name() {
        return field().nameAndType().name();
    }

    /** The field's descriptor, as a `Utf8`. */
    default Utf8Entry type() {
        return field().nameAndType().type();
    }

    /** The field's type. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** This opcode's access to this field. */
    public static FieldInstruction of(Opcode op, FieldRefEntry field) {
        return Instructions.field(op, field);
    }

    /** This opcode's access to the field `name` of type `type` in `owner`. */
    public static FieldInstruction of(Opcode op, ClassEntry owner, Utf8Entry name, Utf8Entry type) {
        return Instructions.field(op, owner, name, type);
    }

    /** This opcode's access to the field `nameAndType` names in `owner`. */
    public static FieldInstruction of(Opcode op, ClassEntry owner, NameAndTypeEntry nameAndType) {
        return Instructions.field(op, owner, nameAndType);
    }
}
