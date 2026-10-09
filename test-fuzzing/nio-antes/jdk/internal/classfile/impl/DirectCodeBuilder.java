package jdk.internal.classfile.impl;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.TypeKind;
import java.lang.classfile.attribute.CharacterRangeInfo;
import java.lang.classfile.attribute.LineNumberInfo;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.CharacterRange;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.LineNumber;
import java.lang.classfile.instruction.LoadConstantInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.classfile.instruction.LocalVariableType;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link CodeBuilder} that writes real bytes.
 *
 * <p>It gathers the elements it is given and on closing serialises them into a `Code` attribute.
 * The writing is one pass with patches: each jump leaves its operand blank and notes that it has to
 * be completed; at the end all the offsets are known and the gaps are filled. It is the only way of
 * writing a forward jump without walking the method twice.
 *
 * <h2>What this writer does NOT do, and it has to be known</h2>
 *
 * <p><strong>It does not synthesise a `StackMapTable`.</strong> If the caller adds one --and it
 * can, with {@link java.lang.classfile.attribute.StackMapTableAttribute}-- it is written as it
 * stands; if not, the `Code` comes out without it. The consequence is concrete and worth keeping in
 * mind: a class of version 50 or higher with jumps and without `StackMapTable` **does not pass a
 * JVM's verifier**. The JDK computes it by itself; this does not, and computing it is not a detail
 * but a type inference over the whole flow graph, with the common supertype of each join -- which
 * is exactly what {@link java.lang.classfile.ClassHierarchyResolver} exists for.
 *
 * <p>What it does do, and does well: `max_stack` by walking the graph (see {@link StackCounter}),
 * `max_locals` by the highest slot used, the exception table, and the debugging attributes.
 */
public final class DirectCodeBuilder implements CodeBuilder {

    private final ConstantPoolBuilder pool;
    private final List<CodeElement> elements = new ArrayList<CodeElement>();
    private final BuilderLabel start = new BuilderLabel();
    private final BuilderLabel end = new BuilderLabel();
    private final boolean isStatic;
    private final int[] paramSlots;
    private int nextSlot;

    /**
     * @param descriptor the method's descriptor, to know which slot each parameter falls in
     * @param isStatic whether there is no receiver
     */
    DirectCodeBuilder(ConstantPoolBuilder pool, String descriptor, boolean isStatic) {
        this.pool = pool;
        this.isStatic = isStatic;
        List<Integer> slots = new ArrayList<Integer>();
        int slot = isStatic ? 0 : 1;
        int i = descriptor.indexOf('(') + 1;
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            int j = DirectCodeBuilder.endOfType(descriptor, i);
            slots.add(Integer.valueOf(slot));
            slot = slot + TypeKind.fromDescriptor(descriptor.substring(i, j)).slotSize();
            i = j;
        }
        this.paramSlots = new int[slots.size()];
        for (int k = 0; k < slots.size(); k++) {
            this.paramSlots[k] = slots.get(k).intValue();
        }
        this.nextSlot = slot;
    }

    static int endOfType(String desc, int i) {
        int j = i;
        while (j < desc.length() && desc.charAt(j) == '[') {
            j = j + 1;
        }
        if (j < desc.length() && desc.charAt(j) == 'L') {
            int end = desc.indexOf(';', j);
            return end < 0 ? desc.length() : end + 1;
        }
        return j + 1;
    }

    public CodeBuilder with(CodeElement e) {
        this.elements.add(e);
        return this;
    }

    public ConstantPoolBuilder constantPool() {
        return this.pool;
    }

    public Label newLabel() {
        return new BuilderLabel();
    }

    public Label startLabel() {
        return this.start;
    }

    public Label endLabel() {
        return this.end;
    }

    public int receiverSlot() {
        if (this.isStatic) {
            throw new IllegalStateException("a static method has no receiver");
        }
        return 0;
    }

    public int parameterSlot(int paramNo) {
        if (paramNo < 0 || paramNo >= this.paramSlots.length) {
            throw new IndexOutOfBoundsException("there is no parameter " + paramNo);
        }
        return this.paramSlots[paramNo];
    }

    public int allocateLocal(TypeKind typeKind) {
        int s = this.nextSlot;
        this.nextSlot = this.nextSlot + typeKind.slotSize();
        return s;
    }

    public CodeBuilder.CatchBuilder catchBuilder(Label tryStart, Label tryEnd, Label endLabel) {
        return new CatchBuilderImpl(this, tryStart, tryEnd, endLabel);
    }

    public CodeBuilder transformingBuilder(CodeTransform transform) {
        return Transforms.chainedCodeBuilder(this, transform);
    }

    // ---- serialisation --------------------------------------------------------------------------

    /** The `Code` attribute with everything accumulated. */
    void writeCode(BufWriterImpl buf) {
        Emission em = this.emit();
        buf.writeIndex(this.pool.utf8Entry("Code"));
        int lenPos = buf.size();
        buf.writeInt(0);
        buf.writeU2(em.maxStack);
        buf.writeU2(Math.max(em.maxLocals, this.nextSlot));
        buf.writeInt(em.code.length);
        buf.writeBytes(em.code);
        buf.writeU2(em.handlers.size());
        for (int i = 0; i < em.handlers.size(); i++) {
            ExceptionCatch h = em.handlers.get(i);
            buf.writeU2(em.offsetOf(h.tryStart()));
            buf.writeU2(em.offsetOf(h.tryEnd()));
            buf.writeU2(em.offsetOf(h.handler()));
            Optional<ClassEntry> t = h.catchType();
            buf.writeIndexOrZero(t.isPresent() ? t.get() : null);
        }
        this.writeCodeAttributes(buf, em);
        buf.patchInt(lenPos, 4, buf.size() - lenPos - 4);
    }

    private void writeCodeAttributes(BufWriterImpl buf, Emission em) {
        List<Attribute<?>> extra = em.attributes;
        int n = extra.size();
        boolean lines = !em.lineNumbers.isEmpty();
        boolean vars = !em.locals.isEmpty();
        boolean varTypes = !em.localTypes.isEmpty();
        boolean ranges = !em.ranges.isEmpty();
        buf.writeU2(n + (lines ? 1 : 0) + (vars ? 1 : 0) + (varTypes ? 1 : 0) + (ranges ? 1 : 0));
        if (lines) {
            AttributeWriter.write(buf, TypedAttributes.lineNumberTable(em.lineNumbers));
        }
        if (vars) {
            AttributeWriter.write(buf, TypedAttributes.localVariableTable(em.locals));
        }
        if (varTypes) {
            AttributeWriter.write(buf, TypedAttributes.localVariableTypeTable(em.localTypes));
        }
        if (ranges) {
            AttributeWriter.write(buf, TypedAttributes.characterRangeTable(em.ranges));
        }
        for (int i = 0; i < n; i++) {
            AttributeWriter.write(buf, extra.get(i));
        }
    }

    // The result of walking the elements: the bytes of the code, where each label fell, and
    // everything that goes outside the `code` array.
    private static final class Emission {

        byte[] code;
        int maxStack;
        int maxLocals;
        final Map<Label, Integer> offsets = new HashMap<Label, Integer>();
        final List<ExceptionCatch> handlers = new ArrayList<ExceptionCatch>();
        final List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();
        final List<LineNumberInfo> lineNumbers = new ArrayList<LineNumberInfo>();
        final List<java.lang.classfile.attribute.LocalVariableInfo> locals =
                new ArrayList<java.lang.classfile.attribute.LocalVariableInfo>();
        final List<java.lang.classfile.attribute.LocalVariableTypeInfo> localTypes =
                new ArrayList<java.lang.classfile.attribute.LocalVariableTypeInfo>();
        final List<CharacterRangeInfo> ranges = new ArrayList<CharacterRangeInfo>();

        int offsetOf(Label l) {
            Integer at = this.offsets.get(l);
            if (at == null) {
                throw new IllegalStateException(
                        "there is a label nobody bound to any position");
            }
            return at.intValue();
        }
    }

    private Emission emit() {
        Emission em = new Emission();
        BufWriterImpl code = new BufWriterImpl(this.pool);
        List<Object[]> fixups = new ArrayList<Object[]>();
        List<Instruction> instructions = new ArrayList<Instruction>();
        List<Object> raw = new ArrayList<Object>();
        int maxSlot = this.nextSlot;

        em.offsets.put(this.start, Integer.valueOf(0));
        for (int i = 0; i < this.elements.size(); i++) {
            CodeElement e = this.elements.get(i);
            raw.add(e);
            if (e instanceof LabelTarget) {
                em.offsets.put(((LabelTarget) e).label(), Integer.valueOf(code.size()));
                continue;
            }
            if (e instanceof ExceptionCatch) {
                em.handlers.add((ExceptionCatch) e);
                continue;
            }
            if (e instanceof LineNumber) {
                em.lineNumbers.add(TypedAttributes.lineNumberInfo(code.size(),
                        ((LineNumber) e).line()));
                continue;
            }
            if (e instanceof LocalVariable) {
                em.locals.add(new PendingLocal((LocalVariable) e));
                maxSlot = Math.max(maxSlot, ((LocalVariable) e).slot() + 1);
                continue;
            }
            if (e instanceof LocalVariableType) {
                em.localTypes.add(new PendingLocalType((LocalVariableType) e));
                continue;
            }
            if (e instanceof CharacterRange) {
                em.ranges.add(new PendingRange((CharacterRange) e));
                continue;
            }
            if (e instanceof Attribute) {
                em.attributes.add((Attribute<?>) e);
                continue;
            }
            if (e instanceof Instruction) {
                Instruction ins = (Instruction) e;
                instructions.add(ins);
                maxSlot = Math.max(maxSlot, DirectCodeBuilder.slotUsed(ins));
                this.emitOne(code, ins, fixups);
                continue;
            }
            throw new IllegalArgumentException("cannot write the element " + e);
        }
        em.offsets.put(this.end, Integer.valueOf(code.size()));

        byte[] bytes = code.toByteArray();
        DirectCodeBuilder.patch(bytes, fixups, em);
        em.code = bytes;
        em.maxLocals = maxSlot;
        em.maxStack = StackCounter.maxStack(instructions, em.handlers,
                StackCounter.indexLabels(raw));
        // The labels of the debugging attributes are resolved only here, when it is already known
        // where all of them fell: a `LocalVariable` may name a label that is bound later.
        DirectCodeBuilder.resolvePending(em);
        return em;
    }

    private static int slotUsed(Instruction ins) {
        Opcode.Kind k = ins.opcode().kind();
        if (k == Opcode.Kind.LOAD) {
            LoadInstruction l = (LoadInstruction) ins;
            return l.slot() + l.typeKind().slotSize();
        }
        if (k == Opcode.Kind.STORE) {
            StoreInstruction s = (StoreInstruction) ins;
            return s.slot() + s.typeKind().slotSize();
        }
        if (k == Opcode.Kind.INCREMENT) {
            return ((IncrementInstruction) ins).slot() + 1;
        }
        return 0;
    }

    // Each patch is (position, width, label, base): the offset is written **relative** to the jump
    // instruction, not absolute, which is how the format keeps them.
    private static void patch(byte[] bytes, List<Object[]> fixups, Emission em) {
        for (int i = 0; i < fixups.size(); i++) {
            Object[] f = fixups.get(i);
            int at = ((Integer) f[0]).intValue();
            int width = ((Integer) f[1]).intValue();
            Label l = (Label) f[2];
            int base = ((Integer) f[3]).intValue();
            int delta = em.offsetOf(l) - base;
            for (int b = 0; b < width; b++) {
                bytes[at + b] = (byte) (delta >> ((width - 1 - b) * 8));
            }
        }
    }

    private static void resolvePending(Emission em) {
        for (int i = 0; i < em.locals.size(); i++) {
            ((PendingLocal) em.locals.get(i)).resolve(em);
        }
        for (int i = 0; i < em.localTypes.size(); i++) {
            ((PendingLocalType) em.localTypes.get(i)).resolve(em);
        }
        for (int i = 0; i < em.ranges.size(); i++) {
            ((PendingRange) em.ranges.get(i)).resolve(em);
        }
    }

    // The encoding of an instruction. The dispatch is by the opcode's **kind** and not by its
    // value: the format groups by shape --no operand, with an index, with a slot-- and that is the
    // grouping that makes this method short.
    private void emitOne(BufWriterImpl code, Instruction ins, List<Object[]> fixups) {
        Opcode op = ins.opcode();
        Opcode.Kind k = op.kind();
        int at = code.size();

        if (k == Opcode.Kind.BRANCH) {
            code.writeU1(op.bytecode());
            int width = op == Opcode.GOTO_W || op == Opcode.JSR_W ? 4 : 2;
            fixups.add(new Object[] { Integer.valueOf(code.size()), Integer.valueOf(width),
                    ((BranchInstruction) ins).target(), Integer.valueOf(at) });
            for (int i = 0; i < width; i++) {
                code.writeU1(0);
            }
            return;
        }
        if (k == Opcode.Kind.TABLE_SWITCH) {
            TableSwitchInstruction ts = (TableSwitchInstruction) ins;
            code.writeU1(op.bytecode());
            this.pad(code, at);
            fixups.add(new Object[] { Integer.valueOf(code.size()), Integer.valueOf(4),
                    ts.defaultTarget(), Integer.valueOf(at) });
            code.writeInt(0);
            code.writeInt(ts.lowValue());
            code.writeInt(ts.highValue());
            // The table has one entry per value of the range, not one per case: the gaps point at
            // the default target. It is what tells a `tableswitch` from a `lookupswitch`.
            for (int v = ts.lowValue(); v <= ts.highValue(); v++) {
                Label t = DirectCodeBuilder.caseTarget(ts.cases(), v, ts.defaultTarget());
                fixups.add(new Object[] { Integer.valueOf(code.size()), Integer.valueOf(4), t,
                        Integer.valueOf(at) });
                code.writeInt(0);
            }
            return;
        }
        if (k == Opcode.Kind.LOOKUP_SWITCH) {
            LookupSwitchInstruction ls = (LookupSwitchInstruction) ins;
            code.writeU1(op.bytecode());
            this.pad(code, at);
            fixups.add(new Object[] { Integer.valueOf(code.size()), Integer.valueOf(4),
                    ls.defaultTarget(), Integer.valueOf(at) });
            code.writeInt(0);
            List<SwitchCase> cs = ls.cases();
            code.writeInt(cs.size());
            for (int i = 0; i < cs.size(); i++) {
                code.writeInt(cs.get(i).caseValue());
                fixups.add(new Object[] { Integer.valueOf(code.size()), Integer.valueOf(4),
                        cs.get(i).target(), Integer.valueOf(at) });
                code.writeInt(0);
            }
            return;
        }
        if (k == Opcode.Kind.LOAD || k == Opcode.Kind.STORE) {
            int slot = k == Opcode.Kind.LOAD ? ((LoadInstruction) ins).slot()
                    : ((StoreInstruction) ins).slot();
            if (op.sizeIfFixed() == 1) {
                code.writeU1(op.bytecode()); // the `_0`..`_3` forms carry the slot in the opcode
                return;
            }
            if (slot > 255) {
                // `wide`: the same opcode with the slot in two bytes. It is the only way of naming
                // a high slot, and a generated method with many variables gets there sooner than
                // one expects.
                code.writeU1(0xC4);
                code.writeU1(op.bytecode());
                code.writeU2(slot);
                return;
            }
            code.writeU1(op.bytecode());
            code.writeU1(slot);
            return;
        }
        if (k == Opcode.Kind.INCREMENT) {
            IncrementInstruction inc = (IncrementInstruction) ins;
            if (inc.slot() > 255 || inc.constant() > 127 || inc.constant() < -128) {
                code.writeU1(0xC4);
                code.writeU1(op.bytecode());
                code.writeU2(inc.slot());
                code.writeU2(inc.constant());
                return;
            }
            code.writeU1(op.bytecode());
            code.writeU1(inc.slot());
            code.writeU1(inc.constant());
            return;
        }
        if (k == Opcode.Kind.FIELD_ACCESS) {
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.FieldRefEntry fr =
                    ((FieldInstruction) ins).field();
            code.writeIndex(fr);
            return;
        }
        if (k == Opcode.Kind.INVOKE) {
            InvokeInstruction inv = (InvokeInstruction) ins;
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.MemberRefEntry mr = inv.method();
            code.writeIndex(mr);
            if (op == Opcode.INVOKEINTERFACE) {
                // The `count` and the padding zero that only this form carries.
                code.writeU1(inv.count());
                code.writeU1(0);
            }
            return;
        }
        if (k == Opcode.Kind.INVOKE_DYNAMIC) {
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.InvokeDynamicEntry id =
                    ((InvokeDynamicInstruction) ins).invokedynamic();
            code.writeIndex(id);
            code.writeU2(0);
            return;
        }
        if (k == Opcode.Kind.NEW_OBJECT) {
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.ClassEntry ce =
                    ((NewObjectInstruction) ins).className();
            code.writeIndex(ce);
            return;
        }
        if (k == Opcode.Kind.NEW_REF_ARRAY) {
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.ClassEntry ct =
                    ((NewReferenceArrayInstruction) ins).componentType();
            code.writeIndex(ct);
            return;
        }
        if (k == Opcode.Kind.NEW_PRIMITIVE_ARRAY) {
            code.writeU1(op.bytecode());
            code.writeU1(((NewPrimitiveArrayInstruction) ins).typeKind().newarrayCode());
            return;
        }
        if (k == Opcode.Kind.NEW_MULTI_ARRAY) {
            NewMultiArrayInstruction m = (NewMultiArrayInstruction) ins;
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.ClassEntry at2 = m.arrayType();
            code.writeIndex(at2);
            code.writeU1(m.dimensions());
            return;
        }
        if (k == Opcode.Kind.TYPE_CHECK) {
            code.writeU1(op.bytecode());
            java.lang.classfile.constantpool.ClassEntry tc =
                    ((TypeCheckInstruction) ins).type();
            code.writeIndex(tc);
            return;
        }
        if (k == Opcode.Kind.CONSTANT) {
            this.emitConstant(code, ins, op);
            return;
        }
        // The ones that carry no operand: the opcode and nothing else.
        code.writeU1(op.bytecode());
    }

    private void emitConstant(BufWriterImpl code, Instruction ins, Opcode op) {
        if (op == Opcode.BIPUSH || op == Opcode.SIPUSH) {
            // It asks for `ConstantInstruction` and casts, instead of asking for
            // `ArgumentConstantInstruction`, which narrows the return to `Integer`. The note said
            // our javac does not resolve a method declared in a NESTED interface that overrides the
            // one of the interface enclosing it; the frozen javac does now, and binds
            // `constantValue()` with the `Integer` return (checked 2026-09-18). Through the
            // supertype works as well, and the value is the same.
            java.lang.constant.ConstantDesc cv = ((ConstantInstruction) ins).constantValue();
            Integer v = (Integer) cv;
            code.writeU1(op.bytecode());
            if (op == Opcode.BIPUSH) {
                code.writeU1(v.intValue());
            } else {
                code.writeU2(v.intValue());
            }
            return;
        }
        if (op == Opcode.LDC || op == Opcode.LDC_W || op == Opcode.LDC2_W) {
            // With the local of the declared type in between: our javac does not always chain
            // through a method inherited from the return's supertype.
            java.lang.classfile.constantpool.LoadableConstantEntry entry =
                    Instructions.constantEntryOf(ins);
            // Through `indexOf` and not through `entry.index()`: the entry may come from the
            // original model's pool, and its index there means nothing here.
            int index = code.indexOf(entry);
            // `ldc` names the entry in ONE byte. An entry with a high index does not fit, and there
            // the wide form is not an option but the only one that exists -- that is why the opcode
            // is corrected instead of failing.
            if (op == Opcode.LDC && index > 255) {
                code.writeU1(Opcode.LDC_W.bytecode());
                code.writeU2(index);
                return;
            }
            code.writeU1(op.bytecode());
            if (op == Opcode.LDC) {
                code.writeU1(index);
            } else {
                code.writeU2(index);
            }
            return;
        }
        // The `iconst_*`, `aconst_null` and company: the opcode carries the value.
        code.writeU1(op.bytecode());
    }

    // The padding of a switch: its table starts at the next multiple of 4 **counted from the start
    // of the method**, not from the opcode.
    private void pad(BufWriterImpl code, int opcodeAt) {
        int pad = (4 - ((opcodeAt + 1) % 4)) % 4;
        for (int i = 0; i < pad; i++) {
            code.writeU1(0);
        }
    }

    private static Label caseTarget(List<SwitchCase> cases, int value, Label byDefault) {
        for (int i = 0; i < cases.size(); i++) {
            if (cases.get(i).caseValue() == value) {
                return cases.get(i).target();
            }
        }
        return byDefault;
    }

    // ---- the debugging entries that wait for the labels to be resolved --------------------------

    private static final class PendingLocal
            implements java.lang.classfile.attribute.LocalVariableInfo {

        private final LocalVariable src;
        private int startPc;
        private int length;

        PendingLocal(LocalVariable src) {
            this.src = src;
        }

        void resolve(Emission em) {
            this.startPc = em.offsetOf(this.src.startScope());
            this.length = em.offsetOf(this.src.endScope()) - this.startPc;
        }

        public int startPc() {
            return this.startPc;
        }

        public int length() {
            return this.length;
        }

        public Utf8Entry name() {
            return this.src.name();
        }

        public Utf8Entry type() {
            return this.src.type();
        }

        public int slot() {
            return this.src.slot();
        }
    }

    private static final class PendingLocalType
            implements java.lang.classfile.attribute.LocalVariableTypeInfo {

        private final LocalVariableType src;
        private int startPc;
        private int length;

        PendingLocalType(LocalVariableType src) {
            this.src = src;
        }

        void resolve(Emission em) {
            this.startPc = em.offsetOf(this.src.startScope());
            this.length = em.offsetOf(this.src.endScope()) - this.startPc;
        }

        public int startPc() {
            return this.startPc;
        }

        public int length() {
            return this.length;
        }

        public Utf8Entry name() {
            return this.src.name();
        }

        public Utf8Entry signature() {
            return this.src.signature();
        }

        public int slot() {
            return this.src.slot();
        }
    }

    private static final class PendingRange implements CharacterRangeInfo {

        private final CharacterRange src;
        private int startPc;
        private int endPc;

        PendingRange(CharacterRange src) {
            this.src = src;
        }

        void resolve(Emission em) {
            this.startPc = em.offsetOf(this.src.startScope());
            this.endPc = em.offsetOf(this.src.endScope());
        }

        public int startPc() {
            return this.startPc;
        }

        public int endPc() {
            return this.endPc;
        }

        public int characterRangeStart() {
            return this.src.characterRangeStart();
        }

        public int characterRangeEnd() {
            return this.src.characterRangeEnd();
        }

        public int flags() {
            return this.src.flags();
        }
    }
}

// A label that does not know yet where it falls. It is compared by identity --it is what the
// contract of `Label` demands-- and that is why it carries no state: two different labels are never
// equal even if they end up at the same offset.
final class BuilderLabel implements Label {

    public String toString() {
        return "Label@" + System.identityHashCode(this);
    }
}

// The `catch` builder of `CodeBuilder.trying`. Each `catching` writes the body of the handler and
// adds the entry to the exception table.
final class CatchBuilderImpl implements CodeBuilder.CatchBuilder {

    private final CodeBuilder builder;
    private final Label tryStart;
    private final Label tryEnd;
    private final Label end;
    private boolean closed;

    CatchBuilderImpl(CodeBuilder builder, Label tryStart, Label tryEnd, Label end) {
        this.builder = builder;
        this.tryStart = tryStart;
        this.tryEnd = tryEnd;
        this.end = end;
    }

    public CodeBuilder.CatchBuilder catching(java.lang.constant.ClassDesc exceptionType,
            java.util.function.Consumer<CodeBuilder> catchHandler) {
        List<java.lang.constant.ClassDesc> single =
                new ArrayList<java.lang.constant.ClassDesc>();
        single.add(exceptionType);
        return this.catchingMulti(single, catchHandler);
    }

    public CodeBuilder.CatchBuilder catchingMulti(
            List<java.lang.constant.ClassDesc> exceptionTypes,
            java.util.function.Consumer<CodeBuilder> catchHandler) {
        if (this.closed) {
            throw new IllegalStateException(
                    "there can be no other handler after catchingAll: it would be unreachable");
        }
        Label handler = this.builder.newBoundLabel();
        for (int i = 0; i < exceptionTypes.size(); i++) {
            this.builder.exceptionCatch(this.tryStart, this.tryEnd, handler,
                    exceptionTypes.get(i));
        }
        catchHandler.accept(this.builder);
        this.builder.goto_(this.end);
        return this;
    }

    public void catchingAll(java.util.function.Consumer<CodeBuilder> catchAllHandler) {
        if (this.closed) {
            throw new IllegalStateException("there is already a catchingAll");
        }
        this.closed = true;
        Label handler = this.builder.newBoundLabel();
        this.builder.exceptionCatchAll(this.tryStart, this.tryEnd, handler);
        catchAllHandler.accept(this.builder);
        this.builder.goto_(this.end);
    }
}
