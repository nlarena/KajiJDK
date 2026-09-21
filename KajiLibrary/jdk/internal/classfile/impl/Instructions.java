package jdk.internal.classfile.impl;

import java.lang.classfile.ClassReader;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.Signature;
import java.lang.classfile.TypeKind;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.CharacterRange;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ConstantInstruction.ArgumentConstantInstruction;
import java.lang.classfile.instruction.ConstantInstruction.IntrinsicConstantInstruction;
import java.lang.classfile.instruction.ConstantInstruction.LoadConstantInstruction;
import java.lang.classfile.instruction.ConvertInstruction;
import java.lang.classfile.instruction.DiscontinuedInstruction.JsrInstruction;
import java.lang.classfile.instruction.DiscontinuedInstruction.RetInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LabelTarget;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LineNumber;
import java.lang.classfile.instruction.LocalVariable;
import java.lang.classfile.instruction.LocalVariableType;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.MonitorInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.NopInstruction;
import java.lang.classfile.instruction.OperatorInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.classfile.instruction.StackInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// The implementations of `java.lang.classfile.instruction` and the decoder that takes them out of
// the `code` array.
//
// A single idea organises all of this: the format has MANY more encodings than operations.
// `aload_0`, `aload 0` and `wide aload 0` are three different byte sequences and the same load;
// `bipush 5`, `iconst_5` and `ldc #5` put the same five on the stack by three roads. The API models
// the operation and keeps the opcode, so whoever reads sees the operation and whoever writes keeps
// the exact encoding there was -- it does not rewrite the file "better".
//
// The size of a loose instruction --built by a factory and not read from a file-- is that of the
// opcode. The only two that have none are the switches, whose length depends on the padding up to
// the next multiple of 4 COUNTED FROM THE START OF THE METHOD: without knowing at which bci they
// go, there is no answer. There `sizeInBytes()` returns -1, which is what the JDK answers too.
public final class Instructions {

    private Instructions() {
    }

    // --- Factories ---

    public static ArrayLoadInstruction arrayLoad(Opcode op) {
        require(op, Opcode.Kind.ARRAY_LOAD);
        return new ArrayLoadImpl(op, op.sizeIfFixed());
    }

    public static ArrayStoreInstruction arrayStore(Opcode op) {
        require(op, Opcode.Kind.ARRAY_STORE);
        return new ArrayStoreImpl(op, op.sizeIfFixed());
    }

    public static BranchInstruction branch(Opcode op, Label target) {
        require(op, Opcode.Kind.BRANCH);
        return new BranchImpl(op, op.sizeIfFixed(), target);
    }

    public static NopInstruction nop() {
        return new NopImpl(Opcode.NOP, 1);
    }

    public static ThrowInstruction throwInstruction() {
        return new ThrowImpl(Opcode.ATHROW, 1);
    }

    public static MonitorInstruction monitor(Opcode op) {
        require(op, Opcode.Kind.MONITOR);
        return new MonitorImpl(op, 1);
    }

    public static StackInstruction stack(Opcode op) {
        require(op, Opcode.Kind.STACK);
        return new StackImpl(op, 1);
    }

    public static OperatorInstruction operator(Opcode op) {
        require(op, Opcode.Kind.OPERATOR);
        return new OperatorImpl(op, op.sizeIfFixed());
    }

    public static ConvertInstruction convert(Opcode op) {
        require(op, Opcode.Kind.CONVERT);
        return new ConvertImpl(op, 1);
    }

    public static ConvertInstruction convert(TypeKind from, TypeKind to) {
        Opcode op = convertOpcode(from, to);
        if (op == null) {
            throw new IllegalArgumentException("there is no conversion from " + from + " a " + to);
        }
        return new ConvertImpl(op, 1);
    }

    public static ReturnInstruction returnInstruction(Opcode op) {
        require(op, Opcode.Kind.RETURN);
        return new ReturnImpl(op, 1);
    }

    public static ReturnInstruction returnInstruction(TypeKind typeKind) {
        TypeKind t = typeKind.asLoadable();
        if (t == TypeKind.VOID) {
            return new ReturnImpl(Opcode.RETURN, 1);
        }
        if (t == TypeKind.INT) {
            return new ReturnImpl(Opcode.IRETURN, 1);
        }
        if (t == TypeKind.LONG) {
            return new ReturnImpl(Opcode.LRETURN, 1);
        }
        if (t == TypeKind.FLOAT) {
            return new ReturnImpl(Opcode.FRETURN, 1);
        }
        if (t == TypeKind.DOUBLE) {
            return new ReturnImpl(Opcode.DRETURN, 1);
        }
        return new ReturnImpl(Opcode.ARETURN, 1);
    }

    public static LoadInstruction load(Opcode op, int slot) {
        require(op, Opcode.Kind.LOAD);
        return new LoadImpl(op, op.sizeIfFixed(), slot);
    }

    public static LoadInstruction load(TypeKind typeKind, int slot) {
        Opcode op = localAccessOpcode(typeKind, slot, true);
        return new LoadImpl(op, op.sizeIfFixed(), slot);
    }

    public static StoreInstruction store(Opcode op, int slot) {
        require(op, Opcode.Kind.STORE);
        return new StoreImpl(op, op.sizeIfFixed(), slot);
    }

    public static StoreInstruction store(TypeKind typeKind, int slot) {
        Opcode op = localAccessOpcode(typeKind, slot, false);
        return new StoreImpl(op, op.sizeIfFixed(), slot);
    }

    public static IncrementInstruction increment(int slot, int constant) {
        boolean isShort = slot >= 0 && slot <= 255 && constant >= -128 && constant <= 127;
        Opcode op = isShort ? Opcode.IINC : Opcode.IINC_W;
        return new IncrementImpl(op, op.sizeIfFixed(), slot, constant);
    }

    public static FieldInstruction field(Opcode op, FieldRefEntry field) {
        require(op, Opcode.Kind.FIELD_ACCESS);
        return new FieldImpl(op, op.sizeIfFixed(), field);
    }

    public static FieldInstruction field(Opcode op, ClassEntry owner, Utf8Entry name,
            Utf8Entry typeKind) {
        return field(op, owner, TemporaryConstantPool.nameAndType(name, typeKind));
    }

    public static FieldInstruction field(Opcode op, ClassEntry owner, NameAndTypeEntry nyt) {
        return field(op, TemporaryConstantPool.fieldRef(owner, nyt));
    }

    public static InvokeInstruction invoke(Opcode op, MemberRefEntry method) {
        require(op, Opcode.Kind.INVOKE);
        boolean interfaceRef = method instanceof InterfaceMethodRefEntry;
        return new InvokeImpl(op, op.sizeIfFixed(), method, interfaceRef,
                op == Opcode.INVOKEINTERFACE ? argumentSlotCount(method) : 0);
    }

    public static InvokeInstruction invoke(Opcode op, ClassEntry owner, Utf8Entry name,
            Utf8Entry typeKind, boolean interfaceRef) {
        return invoke(op, owner, TemporaryConstantPool.nameAndType(name, typeKind), interfaceRef);
    }

    public static InvokeInstruction invoke(Opcode op, ClassEntry owner, NameAndTypeEntry nyt,
            boolean interfaceRef) {
        MemberRefEntry ref = interfaceRef ? (MemberRefEntry) TemporaryConstantPool.interfaceMethodRef(owner, nyt)
                : (MemberRefEntry) TemporaryConstantPool.methodRef(owner, nyt);
        require(op, Opcode.Kind.INVOKE);
        return new InvokeImpl(op, op.sizeIfFixed(), ref, interfaceRef,
                op == Opcode.INVOKEINTERFACE ? argumentSlotCount(ref) : 0);
    }

    public static InvokeDynamicInstruction invokeDynamic(InvokeDynamicEntry entry) {
        return new InvokeDynamicImpl(Opcode.INVOKEDYNAMIC, 5, entry);
    }

    public static NewObjectInstruction newObject(ClassEntry classEntry) {
        return new NewObjectImpl(Opcode.NEW, 3, classEntry);
    }

    public static NewPrimitiveArrayInstruction newPrimitiveArray(TypeKind typeKind) {
        // `newarrayCode()` already throws if the type is not primitive; calling it here makes the
        // error show up when the instruction is built and not when it is written.
        typeKind.newarrayCode();
        return new NewPrimitiveArrayImpl(Opcode.NEWARRAY, 2, typeKind);
    }

    public static NewReferenceArrayInstruction newReferenceArray(ClassEntry componentType) {
        return new NewReferenceArrayImpl(Opcode.ANEWARRAY, 3, componentType);
    }

    public static NewMultiArrayInstruction newMultiArray(ClassEntry typeKind, int dimensions) {
        if (dimensions < 1 || dimensions > 255) {
            throw new IllegalArgumentException(
                    "multianewarray with " + dimensions + " dimensions; the format admits 1..255");
        }
        return new NewMultiArrayImpl(Opcode.MULTIANEWARRAY, 4, typeKind, dimensions);
    }

    public static TypeCheckInstruction typeCheck(Opcode op, ClassEntry typeKind) {
        require(op, Opcode.Kind.TYPE_CHECK);
        return new TypeCheckImpl(op, 3, typeKind);
    }

    public static TypeCheckInstruction typeCheck(Opcode op, ClassDesc typeKind) {
        return typeCheck(op, TemporaryConstantPool.classEntry(typeKind));
    }

    public static SwitchCase switchCase(int value, Label target) {
        return new SwitchCaseImpl(value, target);
    }

    public static TableSwitchInstruction tableSwitch(int low, int high, Label defaultTarget,
            List<SwitchCase> cases) {
        if (high < low) {
            throw new IllegalArgumentException("tableswitch with high " + high + " < low " + low);
        }
        return new TableSwitchImpl(Opcode.TABLESWITCH, -1, low, high, defaultTarget, immutableCases(cases));
    }

    public static LookupSwitchInstruction lookupSwitch(Label defaultTarget,
            List<SwitchCase> cases) {
        return new LookupSwitchImpl(Opcode.LOOKUPSWITCH, -1, defaultTarget, immutableCases(cases));
    }

    public static IntrinsicConstantInstruction intrinsicConstant(Opcode op) {
        ConstantDesc v = intrinsicValue(op);
        if (v == null) {
            throw new IllegalArgumentException(op + " does not carry the constant in the opcode");
        }
        return new IntrinsicConstantImpl(op, 1, v);
    }

    public static ArgumentConstantInstruction argumentConstant(Opcode op, int value) {
        if (op != Opcode.BIPUSH && op != Opcode.SIPUSH) {
            throw new IllegalArgumentException(op + " does not carry the constant in the operand");
        }
        if (op == Opcode.BIPUSH && (value < -128 || value > 127)) {
            throw new IllegalArgumentException("bipush with " + value + " does not fit in a byte");
        }
        if (op == Opcode.SIPUSH && (value < -32768 || value > 32767)) {
            throw new IllegalArgumentException("sipush with " + value + " does not fit in a short");
        }
        return new ArgumentConstantImpl(op, op.sizeIfFixed(), Integer.valueOf(value));
    }

    /**
     * The pool entry of an `ldc`, asked through its nested interface.
     *
     * <p>It is here and not in the writer because, the note said, our javac does not resolve a
     * method declared in a **nested** interface that overrides the one of the interface enclosing
     * it, and from this package the implementation can be asked, which declares it directly. The
     * frozen javac does resolve that case now (checked 2026-09-18), so the detour is no longer
     * needed; it is harmless.
     */
    public static LoadableConstantEntry constantEntryOf(Instruction ins) {
        return ((LoadConstantInstruction) ins).constantEntry();
    }

    public static LoadConstantInstruction loadConstant(Opcode op, LoadableConstantEntry entry) {
        if (op != Opcode.LDC && op != Opcode.LDC_W && op != Opcode.LDC2_W) {
            throw new IllegalArgumentException(op + " is not an ldc");
        }
        return new LoadConstantImpl(op, op.sizeIfFixed(), entry);
    }

    /** The type of a constant that travels inside the opcode. */
    public static TypeKind intrinsicConstantTypeKind(Opcode op) {
        int b = op.bytecode();
        if (b == 0x01) {
            return TypeKind.REFERENCE;
        }
        if (b >= 0x02 && b <= 0x08) {
            return TypeKind.INT;
        }
        if (b == 0x09 || b == 0x0A) {
            return TypeKind.LONG;
        }
        if (b >= 0x0B && b <= 0x0D) {
            return TypeKind.FLOAT;
        }
        if (b == 0x0E || b == 0x0F) {
            return TypeKind.DOUBLE;
        }
        throw new IllegalArgumentException(op + " does not carry the constant in the opcode");
    }

    public static JsrInstruction jsr(Opcode op, Label target) {
        if (op != Opcode.JSR && op != Opcode.JSR_W) {
            throw new IllegalArgumentException(op + " is not a jsr");
        }
        return new JsrImpl(op, op.sizeIfFixed(), target);
    }

    public static RetInstruction ret(Opcode op, int slot) {
        if (op != Opcode.RET && op != Opcode.RET_W) {
            throw new IllegalArgumentException(op + " is not a ret");
        }
        return new RetImpl(op, op.sizeIfFixed(), slot);
    }

    public static RetInstruction ret(int slot) {
        Opcode op = (slot >= 0 && slot <= 255) ? Opcode.RET : Opcode.RET_W;
        return new RetImpl(op, op.sizeIfFixed(), slot);
    }

    public static LabelTarget labelTarget(Label label) {
        return new LabelTargetImpl(label);
    }

    public static LineNumber lineNumber(int n) {
        return new LineNumberImpl(n);
    }

    public static LocalVariable localVariable(int slot, Utf8Entry name, Utf8Entry descriptor,
            Label start, Label end) {
        return new LocalVariableImpl(slot, name, descriptor, start, end);
    }

    public static LocalVariable localVariable(int slot, String name, ClassDesc descriptor,
            Label start, Label end) {
        return new LocalVariableImpl(slot, TemporaryConstantPool.utf8(name),
                TemporaryConstantPool.utf8(descriptor.descriptorString()), start, end);
    }

    public static LocalVariableType localVariableType(int slot, Utf8Entry name, Utf8Entry signature,
            Label start, Label end) {
        return new LocalVariableTypeImpl(slot, name, signature, start, end);
    }

    public static LocalVariableType localVariableType(int slot, String name, Signature signature,
            Label start, Label end) {
        return new LocalVariableTypeImpl(slot, TemporaryConstantPool.utf8(name),
                TemporaryConstantPool.utf8(signature.signatureString()), start, end);
    }

    public static CharacterRange characterRange(Label start, Label end, int from, int to,
            int flags) {
        return new CharacterRangeImpl(start, end, from, to, flags);
    }

    // --- Decoding ---

    /**
     * The instruction starting at `codeStart + bci`, already with its operands. `op` and `size`
     * were computed by the walk of the `code` array, which is the one that knows how to handle
     * `wide` and the switches.
     */
    public static Instruction decode(ClassReader cf, int codeStart, int bci, Opcode op,
            int size) {
        int p = codeStart + bci;
        Opcode.Kind k = op.kind();
        if (k == Opcode.Kind.NOP) {
            return new NopImpl(op, size);
        }
        if (k == Opcode.Kind.THROW_EXCEPTION) {
            return new ThrowImpl(op, size);
        }
        if (k == Opcode.Kind.MONITOR) {
            return new MonitorImpl(op, size);
        }
        if (k == Opcode.Kind.STACK) {
            return new StackImpl(op, size);
        }
        if (k == Opcode.Kind.OPERATOR) {
            return new OperatorImpl(op, size);
        }
        if (k == Opcode.Kind.CONVERT) {
            return new ConvertImpl(op, size);
        }
        if (k == Opcode.Kind.RETURN) {
            return new ReturnImpl(op, size);
        }
        if (k == Opcode.Kind.ARRAY_LOAD) {
            return new ArrayLoadImpl(op, size);
        }
        if (k == Opcode.Kind.ARRAY_STORE) {
            return new ArrayStoreImpl(op, size);
        }
        if (k == Opcode.Kind.LOAD) {
            return new LoadImpl(op, size, slotOf(cf, p, op));
        }
        if (k == Opcode.Kind.STORE) {
            return new StoreImpl(op, size, slotOf(cf, p, op));
        }
        if (k == Opcode.Kind.INCREMENT) {
            if (op.isWide()) {
                return new IncrementImpl(op, size, cf.readU2(p + 2), cf.readS2(p + 4));
            }
            return new IncrementImpl(op, size, cf.readU1(p + 1), cf.readS1(p + 2));
        }
        if (k == Opcode.Kind.BRANCH) {
            int target = op == Opcode.GOTO_W ? bci + cf.readInt(p + 1) : bci + cf.readS2(p + 1);
            return new BranchImpl(op, size, new LabelImpl(target));
        }
        if (k == Opcode.Kind.DISCONTINUED_JSR) {
            int target = op == Opcode.JSR_W ? bci + cf.readInt(p + 1) : bci + cf.readS2(p + 1);
            return new JsrImpl(op, size, new LabelImpl(target));
        }
        if (k == Opcode.Kind.DISCONTINUED_RET) {
            return new RetImpl(op, size, op.isWide() ? cf.readU2(p + 2) : cf.readU1(p + 1));
        }
        if (k == Opcode.Kind.FIELD_ACCESS) {
            FieldRefEntry entry = cf.readEntry(p + 1, FieldRefEntry.class);
            return new FieldImpl(op, size, entry);
        }
        if (k == Opcode.Kind.INVOKE) {
            MemberRefEntry entry = cf.readEntry(p + 1, MemberRefEntry.class);
            int count = op == Opcode.INVOKEINTERFACE ? cf.readU1(p + 3) : 0;
            return new InvokeImpl(op, size, entry, entry instanceof InterfaceMethodRefEntry, count);
        }
        if (k == Opcode.Kind.INVOKE_DYNAMIC) {
            InvokeDynamicEntry entry = cf.readEntry(p + 1, InvokeDynamicEntry.class);
            return new InvokeDynamicImpl(op, size, entry);
        }
        if (k == Opcode.Kind.NEW_OBJECT) {
            ClassEntry entry = cf.readEntry(p + 1, ClassEntry.class);
            return new NewObjectImpl(op, size, entry);
        }
        if (k == Opcode.Kind.NEW_PRIMITIVE_ARRAY) {
            return new NewPrimitiveArrayImpl(op, size, TypeKind.fromNewarrayCode(cf.readU1(p + 1)));
        }
        if (k == Opcode.Kind.NEW_REF_ARRAY) {
            ClassEntry entry = cf.readEntry(p + 1, ClassEntry.class);
            return new NewReferenceArrayImpl(op, size, entry);
        }
        if (k == Opcode.Kind.NEW_MULTI_ARRAY) {
            ClassEntry entry = cf.readEntry(p + 1, ClassEntry.class);
            return new NewMultiArrayImpl(op, size, entry, cf.readU1(p + 3));
        }
        if (k == Opcode.Kind.TYPE_CHECK) {
            ClassEntry entry = cf.readEntry(p + 1, ClassEntry.class);
            return new TypeCheckImpl(op, size, entry);
        }
        if (k == Opcode.Kind.CONSTANT) {
            return constant(cf, p, op, size);
        }
        if (k == Opcode.Kind.TABLE_SWITCH) {
            return decodeTableSwitch(cf, codeStart, bci, op, size);
        }
        return decodeLookupSwitch(cf, codeStart, bci, op, size);
    }

    private static Instruction constant(ClassReader cf, int p, Opcode op, int size) {
        if (op == Opcode.BIPUSH) {
            return new ArgumentConstantImpl(op, size, Integer.valueOf(cf.readS1(p + 1)));
        }
        if (op == Opcode.SIPUSH) {
            return new ArgumentConstantImpl(op, size, Integer.valueOf(cf.readS2(p + 1)));
        }
        if (op == Opcode.LDC) {
            LoadableConstantEntry entry = cf.entryByIndex(cf.readU1(p + 1), LoadableConstantEntry.class);
            return new LoadConstantImpl(op, size, entry);
        }
        if (op == Opcode.LDC_W || op == Opcode.LDC2_W) {
            LoadableConstantEntry entry = cf.readEntry(p + 1, LoadableConstantEntry.class);
            return new LoadConstantImpl(op, size, entry);
        }
        return new IntrinsicConstantImpl(op, size, intrinsicValue(op));
    }

    private static Instruction decodeTableSwitch(ClassReader cf, int codeStart, int bci, Opcode op,
            int size) {
        int q = aligned(bci);
        int base = codeStart + q;
        Label defaultTarget = new LabelImpl(bci + cf.readInt(base));
        int low = cf.readInt(base + 4);
        int high = cf.readInt(base + 8);
        List<SwitchCase> cases = new ArrayList<SwitchCase>();
        for (int i = 0; i <= high - low; i++) {
            cases.add(new SwitchCaseImpl(low + i, new LabelImpl(bci + cf.readInt(base + 12 + i * 4))));
        }
        return new TableSwitchImpl(op, size, low, high, defaultTarget,
                Collections.unmodifiableList(cases));
    }

    private static Instruction decodeLookupSwitch(ClassReader cf, int codeStart, int bci, Opcode op,
            int size) {
        int q = aligned(bci);
        int base = codeStart + q;
        Label defaultTarget = new LabelImpl(bci + cf.readInt(base));
        int n = cf.readInt(base + 4);
        List<SwitchCase> cases = new ArrayList<SwitchCase>();
        for (int i = 0; i < n; i++) {
            int value = cf.readInt(base + 8 + i * 8);
            int target = cf.readInt(base + 12 + i * 8);
            cases.add(new SwitchCaseImpl(value, new LabelImpl(bci + target)));
        }
        return new LookupSwitchImpl(op, size, defaultTarget, Collections.unmodifiableList(cases));
    }

    // The first multiple of 4 from the byte following the opcode. It is the padding of §6.5's
    // `tableswitch`, and it is counted from the start of the `code` array, not from the
    // instruction.
    private static int aligned(int bci) {
        int p = bci + 1;
        while ((p & 3) != 0) {
            p++;
        }
        return p;
    }

    private static int slotOf(ClassReader cf, int p, Opcode op) {
        if (op.isWide()) {
            return cf.readU2(p + 2);
        }
        int fixed = fixedSlot(op);
        if (fixed >= 0) {
            return fixed;
        }
        return cf.readU1(p + 1);
    }

    // The twenty `xload_N` and the twenty `xstore_N` forms carry the slot in the opcode itself.
    private static int fixedSlot(Opcode op) {
        int b = op.bytecode();
        if (b >= 0x1A && b <= 0x2D) {
            return (b - 0x1A) & 3;
        }
        if (b >= 0x3B && b <= 0x4E) {
            return (b - 0x3B) & 3;
        }
        return -1;
    }

    // --- Tables ---

    /** The type a typed-name opcode works on. */
    static TypeKind typeKindOf(Opcode op) {
        int b = op.bytecode();
        switch (b) {
            case 0x2E: case 0x4F: return TypeKind.INT;      // iaload, iastore
            case 0x2F: case 0x50: return TypeKind.LONG;     // laload, lastore
            case 0x30: case 0x51: return TypeKind.FLOAT;    // faload, fastore
            case 0x31: case 0x52: return TypeKind.DOUBLE;   // daload, dastore
            case 0x32: case 0x53: return TypeKind.REFERENCE; // aaload, aastore
            case 0x33: case 0x54: return TypeKind.BYTE;     // baload, bastore
            case 0x34: case 0x55: return TypeKind.CHAR;     // caload, castore
            case 0x35: case 0x56: return TypeKind.SHORT;    // saload, sastore
            case 0xAC: return TypeKind.INT;                 // ireturn
            case 0xAD: return TypeKind.LONG;                // lreturn
            case 0xAE: return TypeKind.FLOAT;               // freturn
            case 0xAF: return TypeKind.DOUBLE;              // dreturn
            case 0xB0: return TypeKind.REFERENCE;           // areturn
            case 0xB1: return TypeKind.VOID;                // return
            case 0xBE: return TypeKind.INT;                 // arraylength
            default: break;
        }
        // The `xload`/`xstore` and the operators carry the type in the first letter of the name;
        // for the widened forms the letter is the same, so looking at the name is enough.
        char c = op.name().charAt(0);
        if (c == 'I') {
            return TypeKind.INT;
        }
        if (c == 'L') {
            return TypeKind.LONG;
        }
        if (c == 'F') {
            return TypeKind.FLOAT;
        }
        if (c == 'D') {
            return TypeKind.DOUBLE;
        }
        if (c == 'A') {
            return TypeKind.REFERENCE;
        }
        throw new IllegalArgumentException("cannot deduce the type of " + op);
    }

    private static Opcode localAccessOpcode(TypeKind typeKind, int slot, boolean isLoad) {
        TypeKind t = typeKind.asLoadable();
        if (t == TypeKind.VOID) {
            throw new IllegalArgumentException("there is no local variable of type void");
        }
        int row;
        if (t == TypeKind.INT) {
            row = 0;
        } else if (t == TypeKind.LONG) {
            row = 1;
        } else if (t == TypeKind.FLOAT) {
            row = 2;
        } else if (t == TypeKind.DOUBLE) {
            row = 3;
        } else {
            row = 4;
        }
        if (slot < 0 || slot > 65535) {
            throw new IllegalArgumentException("slot out of range: " + slot);
        }
        int base = isLoad ? 0x15 : 0x36;
        int shortBase = isLoad ? 0x1A : 0x3B;
        if (slot <= 3) {
            return OpcodeTable.simple(shortBase + row * 4 + slot);
        }
        if (slot <= 255) {
            return OpcodeTable.simple(base + row);
        }
        return OpcodeTable.wide(base + row);
    }

    private static Opcode convertOpcode(TypeKind from, TypeKind to) {
        int b = 0;
        if (from == TypeKind.INT) {
            if (to == TypeKind.LONG) {
                b = 0x85;
            } else if (to == TypeKind.FLOAT) {
                b = 0x86;
            } else if (to == TypeKind.DOUBLE) {
                b = 0x87;
            } else if (to == TypeKind.BYTE) {
                b = 0x91;
            } else if (to == TypeKind.CHAR) {
                b = 0x92;
            } else if (to == TypeKind.SHORT) {
                b = 0x93;
            }
        } else if (from == TypeKind.LONG) {
            if (to == TypeKind.INT) {
                b = 0x88;
            } else if (to == TypeKind.FLOAT) {
                b = 0x89;
            } else if (to == TypeKind.DOUBLE) {
                b = 0x8A;
            }
        } else if (from == TypeKind.FLOAT) {
            if (to == TypeKind.INT) {
                b = 0x8B;
            } else if (to == TypeKind.LONG) {
                b = 0x8C;
            } else if (to == TypeKind.DOUBLE) {
                b = 0x8D;
            }
        } else if (from == TypeKind.DOUBLE) {
            if (to == TypeKind.INT) {
                b = 0x8E;
            } else if (to == TypeKind.LONG) {
                b = 0x8F;
            } else if (to == TypeKind.FLOAT) {
                b = 0x90;
            }
        }
        return b == 0 ? null : OpcodeTable.simple(b);
    }

    // The (from, to) pair of a conversion opcode. `i2b`, `i2c` and `i2s` are the three that do not
    // change category on the stack but do change declared type.
    static TypeKind fromTypeOf(Opcode op) {
        int b = op.bytecode();
        if (b >= 0x85 && b <= 0x87) {
            return TypeKind.INT;
        }
        if (b >= 0x88 && b <= 0x8A) {
            return TypeKind.LONG;
        }
        if (b >= 0x8B && b <= 0x8D) {
            return TypeKind.FLOAT;
        }
        if (b >= 0x8E && b <= 0x90) {
            return TypeKind.DOUBLE;
        }
        return TypeKind.INT;
    }

    static TypeKind toTypeOf(Opcode op) {
        switch (op.bytecode()) {
            case 0x85: return TypeKind.LONG;
            case 0x86: return TypeKind.FLOAT;
            case 0x87: return TypeKind.DOUBLE;
            case 0x88: return TypeKind.INT;
            case 0x89: return TypeKind.FLOAT;
            case 0x8A: return TypeKind.DOUBLE;
            case 0x8B: return TypeKind.INT;
            case 0x8C: return TypeKind.LONG;
            case 0x8D: return TypeKind.DOUBLE;
            case 0x8E: return TypeKind.INT;
            case 0x8F: return TypeKind.LONG;
            case 0x90: return TypeKind.FLOAT;
            case 0x91: return TypeKind.BYTE;
            case 0x92: return TypeKind.CHAR;
            case 0x93: return TypeKind.SHORT;
            default:
                throw new IllegalArgumentException(op + " is not a conversion");
        }
    }

    private static ConstantDesc intrinsicValue(Opcode op) {
        switch (op.bytecode()) {
            case 0x01: return ConstantDescs.NULL;
            case 0x02: return Integer.valueOf(-1);
            case 0x03: return Integer.valueOf(0);
            case 0x04: return Integer.valueOf(1);
            case 0x05: return Integer.valueOf(2);
            case 0x06: return Integer.valueOf(3);
            case 0x07: return Integer.valueOf(4);
            case 0x08: return Integer.valueOf(5);
            case 0x09: return Long.valueOf(0L);
            case 0x0A: return Long.valueOf(1L);
            case 0x0B: return Float.valueOf(0.0f);
            case 0x0C: return Float.valueOf(1.0f);
            case 0x0D: return Float.valueOf(2.0f);
            case 0x0E: return Double.valueOf(0.0d);
            case 0x0F: return Double.valueOf(1.0d);
            default: return null;
        }
    }

    // How many slots the arguments plus the receiver take: it is the `count` of `invokeinterface`.
    private static int argumentSlotCount(MemberRefEntry ref) {
        String d = ref.nameAndType().type().stringValue();
        int n = 1;
        int i = 1;
        while (i < d.length() && d.charAt(i) != ')') {
            char c = d.charAt(i);
            if (c == '[') {
                i++;
                continue;
            }
            if (c == 'L') {
                while (i < d.length() && d.charAt(i) != ';') {
                    i++;
                }
                n += 1;
            } else if (c == 'J' || c == 'D') {
                n += 2;
            } else {
                n += 1;
            }
            i++;
        }
        return n;
    }

    private static void require(Opcode op, Opcode.Kind k) {
        if (op.kind() != k) {
            throw new IllegalArgumentException(op + " is not of the family " + k);
        }
    }

    private static List<SwitchCase> immutableCases(List<SwitchCase> cases) {
        List<SwitchCase> list = new ArrayList<SwitchCase>();
        for (int i = 0; i < cases.size(); i++) {
            list.add(cases.get(i));
        }
        return Collections.unmodifiableList(list);
    }
}

// The base of every instruction: the opcode and how many bytes it takes.
abstract class AbstractInstruction implements Instruction {

    private final Opcode op;
    private final int size;

    AbstractInstruction(Opcode op, int size) {
        this.op = op;
        this.size = size;
    }

    public Opcode opcode() {
        return this.op;
    }

    public int sizeInBytes() {
        return this.size;
    }

    public String toString() {
        return this.op.name();
    }
}

final class NopImpl extends AbstractInstruction implements NopInstruction {

    NopImpl(Opcode op, int size) {
        super(op, size);
    }
}

final class ThrowImpl extends AbstractInstruction implements ThrowInstruction {

    ThrowImpl(Opcode op, int size) {
        super(op, size);
    }
}

final class MonitorImpl extends AbstractInstruction implements MonitorInstruction {

    MonitorImpl(Opcode op, int size) {
        super(op, size);
    }
}

final class StackImpl extends AbstractInstruction implements StackInstruction {

    StackImpl(Opcode op, int size) {
        super(op, size);
    }
}

final class OperatorImpl extends AbstractInstruction implements OperatorInstruction {

    OperatorImpl(Opcode op, int size) {
        super(op, size);
    }

    public TypeKind typeKind() {
        return Instructions.typeKindOf(opcode());
    }
}

final class ConvertImpl extends AbstractInstruction implements ConvertInstruction {

    ConvertImpl(Opcode op, int size) {
        super(op, size);
    }

    public TypeKind fromType() {
        return Instructions.fromTypeOf(opcode());
    }

    public TypeKind toType() {
        return Instructions.toTypeOf(opcode());
    }
}

final class ReturnImpl extends AbstractInstruction implements ReturnInstruction {

    ReturnImpl(Opcode op, int size) {
        super(op, size);
    }

    public TypeKind typeKind() {
        return Instructions.typeKindOf(opcode());
    }
}

final class ArrayLoadImpl extends AbstractInstruction implements ArrayLoadInstruction {

    ArrayLoadImpl(Opcode op, int size) {
        super(op, size);
    }

    public TypeKind typeKind() {
        return Instructions.typeKindOf(opcode());
    }
}

final class ArrayStoreImpl extends AbstractInstruction implements ArrayStoreInstruction {

    ArrayStoreImpl(Opcode op, int size) {
        super(op, size);
    }

    public TypeKind typeKind() {
        return Instructions.typeKindOf(opcode());
    }
}

final class LoadImpl extends AbstractInstruction implements LoadInstruction {

    private final int slot;

    LoadImpl(Opcode op, int size, int slot) {
        super(op, size);
        this.slot = slot;
    }

    public int slot() {
        return this.slot;
    }

    public TypeKind typeKind() {
        return Instructions.typeKindOf(opcode());
    }

    public String toString() {
        return opcode().name() + " #" + this.slot;
    }
}

final class StoreImpl extends AbstractInstruction implements StoreInstruction {

    private final int slot;

    StoreImpl(Opcode op, int size, int slot) {
        super(op, size);
        this.slot = slot;
    }

    public int slot() {
        return this.slot;
    }

    public TypeKind typeKind() {
        return Instructions.typeKindOf(opcode());
    }

    public String toString() {
        return opcode().name() + " #" + this.slot;
    }
}

final class IncrementImpl extends AbstractInstruction implements IncrementInstruction {

    private final int slot;
    private final int constant;

    IncrementImpl(Opcode op, int size, int slot, int constant) {
        super(op, size);
        this.slot = slot;
        this.constant = constant;
    }

    public int slot() {
        return this.slot;
    }

    public int constant() {
        return this.constant;
    }

    public String toString() {
        return opcode().name() + " #" + this.slot + " += " + this.constant;
    }
}

final class BranchImpl extends AbstractInstruction implements BranchInstruction {

    private final Label target;

    BranchImpl(Opcode op, int size, Label target) {
        super(op, size);
        this.target = target;
    }

    public Label target() {
        return this.target;
    }

    public String toString() {
        return opcode().name() + " -> " + this.target;
    }
}

final class JsrImpl extends AbstractInstruction implements JsrInstruction {

    private final Label target;

    JsrImpl(Opcode op, int size, Label target) {
        super(op, size);
        this.target = target;
    }

    public Label target() {
        return this.target;
    }

    public String toString() {
        return opcode().name() + " -> " + this.target;
    }
}

final class RetImpl extends AbstractInstruction implements RetInstruction {

    private final int slot;

    RetImpl(Opcode op, int size, int slot) {
        super(op, size);
        this.slot = slot;
    }

    public int slot() {
        return this.slot;
    }

    public String toString() {
        return opcode().name() + " #" + this.slot;
    }
}

final class FieldImpl extends AbstractInstruction implements FieldInstruction {

    private final FieldRefEntry field;

    FieldImpl(Opcode op, int size, FieldRefEntry field) {
        super(op, size);
        this.field = field;
    }

    public FieldRefEntry field() {
        return this.field;
    }

    public String toString() {
        return opcode().name() + " " + this.field.owner().asInternalName() + "."
                + this.field.nameAndType().name().stringValue();
    }
}

final class InvokeImpl extends AbstractInstruction implements InvokeInstruction {

    private final MemberRefEntry method;
    private final boolean interfaceRef;
    private final int count;

    InvokeImpl(Opcode op, int size, MemberRefEntry method, boolean interfaceRef, int count) {
        super(op, size);
        this.method = method;
        this.interfaceRef = interfaceRef;
        this.count = count;
    }

    public MemberRefEntry method() {
        return this.method;
    }

    public boolean isInterface() {
        return this.interfaceRef;
    }

    public int count() {
        return this.count;
    }

    public String toString() {
        return opcode().name() + " " + this.method.owner().asInternalName() + "."
                + this.method.nameAndType().name().stringValue();
    }
}

final class InvokeDynamicImpl extends AbstractInstruction implements InvokeDynamicInstruction {

    private final InvokeDynamicEntry entry;

    InvokeDynamicImpl(Opcode op, int size, InvokeDynamicEntry entry) {
        super(op, size);
        this.entry = entry;
    }

    public InvokeDynamicEntry invokedynamic() {
        return this.entry;
    }

    public String toString() {
        return "INVOKEDYNAMIC " + this.entry.nameAndType().name().stringValue();
    }
}

final class NewObjectImpl extends AbstractInstruction implements NewObjectInstruction {

    private final ClassEntry classEntry;

    NewObjectImpl(Opcode op, int size, ClassEntry classEntry) {
        super(op, size);
        this.classEntry = classEntry;
    }

    public ClassEntry className() {
        return this.classEntry;
    }

    public String toString() {
        return "NEW " + this.classEntry.asInternalName();
    }
}

final class NewPrimitiveArrayImpl extends AbstractInstruction implements NewPrimitiveArrayInstruction {

    private final TypeKind typeKind;

    NewPrimitiveArrayImpl(Opcode op, int size, TypeKind typeKind) {
        super(op, size);
        this.typeKind = typeKind;
    }

    public TypeKind typeKind() {
        return this.typeKind;
    }

    public String toString() {
        return "NEWARRAY " + this.typeKind;
    }
}

final class NewReferenceArrayImpl extends AbstractInstruction implements NewReferenceArrayInstruction {

    private final ClassEntry componentType;

    NewReferenceArrayImpl(Opcode op, int size, ClassEntry componentType) {
        super(op, size);
        this.componentType = componentType;
    }

    public ClassEntry componentType() {
        return this.componentType;
    }

    public String toString() {
        return "ANEWARRAY " + this.componentType.asInternalName();
    }
}

final class NewMultiArrayImpl extends AbstractInstruction implements NewMultiArrayInstruction {

    private final ClassEntry typeKind;
    private final int dimensions;

    NewMultiArrayImpl(Opcode op, int size, ClassEntry typeKind, int dimensions) {
        super(op, size);
        this.typeKind = typeKind;
        this.dimensions = dimensions;
    }

    public ClassEntry arrayType() {
        return this.typeKind;
    }

    public int dimensions() {
        return this.dimensions;
    }

    public String toString() {
        return "MULTIANEWARRAY " + this.typeKind.asInternalName() + " x" + this.dimensions;
    }
}

final class TypeCheckImpl extends AbstractInstruction implements TypeCheckInstruction {

    private final ClassEntry typeKind;

    TypeCheckImpl(Opcode op, int size, ClassEntry typeKind) {
        super(op, size);
        this.typeKind = typeKind;
    }

    public ClassEntry type() {
        return this.typeKind;
    }

    public String toString() {
        return opcode().name() + " " + this.typeKind.asInternalName();
    }
}

final class SwitchCaseImpl implements SwitchCase {

    private final int value;
    private final Label target;

    SwitchCaseImpl(int value, Label target) {
        this.value = value;
        this.target = target;
    }

    public int caseValue() {
        return this.value;
    }

    public Label target() {
        return this.target;
    }

    public String toString() {
        return this.value + " -> " + this.target;
    }
}

final class TableSwitchImpl extends AbstractInstruction implements TableSwitchInstruction {

    private final int low;
    private final int high;
    private final Label defaultTarget;
    private final List<SwitchCase> cases;

    TableSwitchImpl(Opcode op, int size, int low, int high, Label defaultTarget,
            List<SwitchCase> cases) {
        super(op, size);
        this.low = low;
        this.high = high;
        this.defaultTarget = defaultTarget;
        this.cases = cases;
    }

    public int lowValue() {
        return this.low;
    }

    public int highValue() {
        return this.high;
    }

    public Label defaultTarget() {
        return this.defaultTarget;
    }

    public List<SwitchCase> cases() {
        return this.cases;
    }

    public String toString() {
        return "TABLESWITCH " + this.low + ".." + this.high;
    }
}

final class LookupSwitchImpl extends AbstractInstruction implements LookupSwitchInstruction {

    private final Label defaultTarget;
    private final List<SwitchCase> cases;

    LookupSwitchImpl(Opcode op, int size, Label defaultTarget, List<SwitchCase> cases) {
        super(op, size);
        this.defaultTarget = defaultTarget;
        this.cases = cases;
    }

    public Label defaultTarget() {
        return this.defaultTarget;
    }

    public List<SwitchCase> cases() {
        return this.cases;
    }

    public String toString() {
        return "LOOKUPSWITCH x" + this.cases.size();
    }
}

final class IntrinsicConstantImpl extends AbstractInstruction implements IntrinsicConstantInstruction {

    private final ConstantDesc value;

    IntrinsicConstantImpl(Opcode op, int size, ConstantDesc value) {
        super(op, size);
        this.value = value;
    }

    public ConstantDesc constantValue() {
        return this.value;
    }
}

final class ArgumentConstantImpl extends AbstractInstruction implements ArgumentConstantInstruction {

    private final Integer value;

    ArgumentConstantImpl(Opcode op, int size, Integer value) {
        super(op, size);
        this.value = value;
    }

    public Integer constantValue() {
        return this.value;
    }

    public String toString() {
        return opcode().name() + " " + this.value;
    }
}

final class LoadConstantImpl extends AbstractInstruction implements LoadConstantInstruction {

    private final LoadableConstantEntry entry;

    LoadConstantImpl(Opcode op, int size, LoadableConstantEntry entry) {
        super(op, size);
        this.entry = entry;
    }

    public LoadableConstantEntry constantEntry() {
        return this.entry;
    }

    public ConstantDesc constantValue() {
        return this.entry.constantValue();
    }

    public String toString() {
        return opcode().name() + " " + this.entry;
    }
}

final class LabelTargetImpl implements LabelTarget {

    private final Label label;

    LabelTargetImpl(Label label) {
        this.label = label;
    }

    public Label label() {
        return this.label;
    }

    public String toString() {
        return "LabelTarget[" + this.label + "]";
    }
}

final class LineNumberImpl implements LineNumber {

    private final int n;

    LineNumberImpl(int n) {
        this.n = n;
    }

    public int line() {
        return this.n;
    }

    public String toString() {
        return "LineNumber[" + this.n + "]";
    }
}

final class LocalVariableImpl implements LocalVariable {

    private final int slot;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private final Label start;
    private final Label end;

    LocalVariableImpl(int slot, Utf8Entry name, Utf8Entry descriptor, Label start, Label end) {
        this.slot = slot;
        this.name = name;
        this.descriptor = descriptor;
        this.start = start;
        this.end = end;
    }

    public int slot() {
        return this.slot;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public Utf8Entry type() {
        return this.descriptor;
    }

    public Label startScope() {
        return this.start;
    }

    public Label endScope() {
        return this.end;
    }

    public String toString() {
        return "LocalVariable[#" + this.slot + " " + this.name.stringValue() + " "
                + this.descriptor.stringValue() + "]";
    }
}

final class LocalVariableTypeImpl implements LocalVariableType {

    private final int slot;
    private final Utf8Entry name;
    private final Utf8Entry signature;
    private final Label start;
    private final Label end;

    LocalVariableTypeImpl(int slot, Utf8Entry name, Utf8Entry signature, Label start, Label end) {
        this.slot = slot;
        this.name = name;
        this.signature = signature;
        this.start = start;
        this.end = end;
    }

    public int slot() {
        return this.slot;
    }

    public Utf8Entry name() {
        return this.name;
    }

    public Utf8Entry signature() {
        return this.signature;
    }

    public Label startScope() {
        return this.start;
    }

    public Label endScope() {
        return this.end;
    }

    public String toString() {
        return "LocalVariableType[#" + this.slot + " " + this.name.stringValue() + " "
                + this.signature.stringValue() + "]";
    }
}

final class CharacterRangeImpl implements CharacterRange {

    private final Label start;
    private final Label end;
    private final int from;
    private final int to;
    private final int flags;

    CharacterRangeImpl(Label start, Label end, int from, int to, int flags) {
        this.start = start;
        this.end = end;
        this.from = from;
        this.to = to;
        this.flags = flags;
    }

    public Label startScope() {
        return this.start;
    }

    public Label endScope() {
        return this.end;
    }

    public int characterRangeStart() {
        return this.from;
    }

    public int characterRangeEnd() {
        return this.to;
    }

    public int flags() {
        return this.flags;
    }

    public String toString() {
        return "CharacterRange[" + this.from + ".." + this.to + "]";
    }
}
