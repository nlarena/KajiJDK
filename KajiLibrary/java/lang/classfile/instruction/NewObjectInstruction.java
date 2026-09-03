package java.lang.classfile.instruction;

import java.lang.classfile.Instruction;
import java.lang.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.impl.Instructions;

// `new`. Crea la instancia sin inicializarla: el `invokespecial` al constructor es otra instrucción,
// y entre las dos el verificador trata al objeto como un tipo aparte que no se puede usar.
public interface NewObjectInstruction extends Instruction {

    /** La clase que se instancia. */
    ClassEntry className();

    /** El `new` de esta clase. */
    public static NewObjectInstruction of(ClassEntry className) {
        return Instructions.newObject(className);
    }
}
