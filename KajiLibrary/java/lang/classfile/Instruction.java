package java.lang.classfile;

// Una instrucción del arreglo `code` (JVMS §6). Lo único común a todas es qué opcode son y cuántos
// bytes ocupan; el resto —operandos, destino de salto, entrada del pool— lo dice cada subtipo.
public interface Instruction extends CodeElement {

    /** El opcode. */
    Opcode opcode();

    /** Cuántos bytes ocupa, incluido el opcode. */
    int sizeInBytes();
}
