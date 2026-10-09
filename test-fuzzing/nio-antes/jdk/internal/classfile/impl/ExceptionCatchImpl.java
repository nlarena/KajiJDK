package jdk.internal.classfile.impl;

import java.lang.classfile.Label;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.ExceptionCatch;
import java.util.Optional;

// A row of the `exception_table`.
public final class ExceptionCatchImpl implements ExceptionCatch {

    private final Label handler;
    private final Label start;
    private final Label end;
    private final Optional<ClassEntry> type;

    public ExceptionCatchImpl(Label handler, Label start, Label end,
            Optional<ClassEntry> type) {
        this.handler = handler;
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public Label handler() {
        return this.handler;
    }

    public Label tryStart() {
        return this.start;
    }

    public Label tryEnd() {
        return this.end;
    }

    public Optional<ClassEntry> catchType() {
        return this.type;
    }

    public String toString() {
        return "ExceptionCatch[" + this.start + ".." + this.end + " -> " + this.handler
                + " : " + (this.type.isPresent() ? this.type.get().asInternalName() : "any") + "]";
    }
}
