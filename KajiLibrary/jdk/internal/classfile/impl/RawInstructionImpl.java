package jdk.internal.classfile.impl;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;

// Una instrucción decodificada del arreglo `code`: su opcode, su tamaño real y dónde empieza.
//
// ALCANCE: el JDK devuelve, para cada instrucción, una de las cuarenta y pico de interfaces de
// `java.lang.classfile.instruction` —`LoadInstruction`, `BranchInstruction`, …— con sus operandos ya
// interpretados. KajiLibrary no tiene ese paquete, así que devuelve esto: un `Instruction` de verdad,
// con el opcode de verdad y el tamaño de verdad, pero sin los operandos. No es lo mismo, y no
// pretende serlo; lo que NO hace es saltearse instrucciones ni inventar tamaños, que es lo que haría
// que un recorrido del cuerpo del método mintiera.
public final class RawInstructionImpl implements Instruction {

    private final Opcode opcode;
    private final int bci;
    private final int tamanio;

    RawInstructionImpl(Opcode opcode, int bci, int tamanio) {
        this.opcode = opcode;
        this.bci = bci;
        this.tamanio = tamanio;
    }

    public Opcode opcode() {
        return this.opcode;
    }

    public int sizeInBytes() {
        return this.tamanio;
    }

    /** El offset de esta instrucción dentro del arreglo `code`. */
    public int bci() {
        return this.bci;
    }

    public String toString() {
        return this.bci + ": " + this.opcode.name();
    }
}
