package jdk.internal.classfile.impl;

import java.lang.classfile.Opcode;

// From byte to `Opcode`. Two tables because the format has two name spaces: the 202 one-byte
// opcodes and the 12 that exist only behind a `wide`, whose `bytecode()` is the pair
// `(0xC4 << 8) | b`.
final class OpcodeTable {

    static final int WIDE = 0xC4;

    private static final Opcode[] SIMPLE = new Opcode[256];
    private static final Opcode[] WIDENED = new Opcode[256];

    static {
        Opcode[] all = Opcode.values();
        for (int i = 0; i < all.length; i++) {
            Opcode o = all[i];
            if (o.isWide()) {
                WIDENED[o.bytecode() & 0xFF] = o;
            } else {
                SIMPLE[o.bytecode()] = o;
            }
        }
    }

    private OpcodeTable() {
    }

    /** The one-byte opcode, or `null` if that byte is none. */
    static Opcode simple(int b) {
        return SIMPLE[b & 0xFF];
    }

    /** The opcode following a `wide`, or `null` if `wide` does not admit it. */
    static Opcode wide(int b) {
        return WIDENED[b & 0xFF];
    }
}
