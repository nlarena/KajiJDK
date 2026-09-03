package jdk.internal.classfile.impl;

import java.lang.classfile.Label;

// Una etiqueta que ya sabe su posición: sale de leer un `.class`, donde los destinos son offsets y
// no incógnitas. Se compara por identidad, como manda el contrato de `Label`.
public final class LabelImpl implements Label {

    private final int bci;

    public LabelImpl(int bci) {
        this.bci = bci;
    }

    /** El offset dentro del arreglo `code`. */
    public int bci() {
        return this.bci;
    }

    public String toString() {
        return "Label@" + this.bci;
    }
}
