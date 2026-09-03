package jdk.internal.classfile.impl;

import java.lang.classfile.Opcode;

// De byte a `Opcode`. Dos tablas porque el formato tiene dos espacios de nombres: los 202 opcodes de
// un byte y los 12 que sólo existen detrás de un `wide`, cuyo `bytecode()` es el par `(0xC4 << 8) | b`.
final class OpcodeTable {

    static final int WIDE = 0xC4;

    private static final Opcode[] SIMPLES = new Opcode[256];
    private static final Opcode[] ENSANCHADOS = new Opcode[256];

    static {
        Opcode[] todos = Opcode.values();
        for (int i = 0; i < todos.length; i++) {
            Opcode o = todos[i];
            if (o.isWide()) {
                ENSANCHADOS[o.bytecode() & 0xFF] = o;
            } else {
                SIMPLES[o.bytecode()] = o;
            }
        }
    }

    private OpcodeTable() {
    }

    /** El opcode de un byte, o `null` si ese byte no es ninguno. */
    static Opcode simple(int b) {
        return SIMPLES[b & 0xFF];
    }

    /** El opcode que sigue a un `wide`, o `null` si `wide` no lo admite. */
    static Opcode wide(int b) {
        return ENSANCHADOS[b & 0xFF];
    }
}
