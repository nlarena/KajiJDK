package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import jdk.internal.classfile.impl.Instructions;

// Un acceso a campo: `getstatic`, `putstatic`, `getfield` o `putfield`. El operando es un
// `CONSTANT_Fieldref`, y los accesores `default` de acá son atajos para bajar por él sin escribir la
// cadena entera.
public interface FieldInstruction extends Instruction {

    /** La entrada del pool con el campo. */
    FieldRefEntry field();

    /** La clase que declara el campo. */
    default ClassEntry owner() {
        return field().owner();
    }

    /** El nombre del campo. */
    default Utf8Entry name() {
        return field().nameAndType().name();
    }

    /** El descriptor del campo, como `Utf8`. */
    default Utf8Entry type() {
        return field().nameAndType().type();
    }

    /** El tipo del campo. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** El acceso de este opcode a este campo. */
    public static FieldInstruction of(Opcode op, FieldRefEntry field) {
        return Instructions.field(op, field);
    }

    /** El acceso de este opcode al campo `name` de tipo `type` en `owner`. */
    public static FieldInstruction of(Opcode op, ClassEntry owner, Utf8Entry name, Utf8Entry type) {
        return Instructions.field(op, owner, name, type);
    }

    /** El acceso de este opcode al campo que nombra `nameAndType` en `owner`. */
    public static FieldInstruction of(Opcode op, ClassEntry owner, NameAndTypeEntry nameAndType) {
        return Instructions.field(op, owner, nameAndType);
    }
}
