package java.lang.classfile.instruction;

import java.lang.classfile.PseudoInstruction;
import jdk.internal.classfile.impl.Instructions;

// Una fila del `LineNumberTable` vista desde el cuerpo del método: a partir de acá, las
// instrucciones vienen de esta línea del fuente.
public interface LineNumber extends PseudoInstruction {

    /** El número de línea. */
    int line();

    /** La marca de esta línea. */
    public static LineNumber of(int line) {
        return Instructions.lineNumber(line);
    }
}
