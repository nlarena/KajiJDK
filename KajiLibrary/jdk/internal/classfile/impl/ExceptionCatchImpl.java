package jdk.internal.classfile.impl;

import java.lang.classfile.Label;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.ExceptionCatch;
import java.util.Optional;

// Una fila de la `exception_table`.
public final class ExceptionCatchImpl implements ExceptionCatch {

    private final Label manejador;
    private final Label inicio;
    private final Label fin;
    private final Optional<ClassEntry> type;

    public ExceptionCatchImpl(Label manejador, Label inicio, Label fin,
            Optional<ClassEntry> type) {
        this.manejador = manejador;
        this.inicio = inicio;
        this.fin = fin;
        this.type = type;
    }

    public Label handler() {
        return this.manejador;
    }

    public Label tryStart() {
        return this.inicio;
    }

    public Label tryEnd() {
        return this.fin;
    }

    public Optional<ClassEntry> catchType() {
        return this.type;
    }

    public String toString() {
        return "ExceptionCatch[" + this.inicio + ".." + this.fin + " -> " + this.manejador
                + " : " + (this.type.isPresent() ? this.type.get().asInternalName() : "any") + "]";
    }
}
