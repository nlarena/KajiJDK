package java.lang.classfile;

import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.SwitchCase;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import jdk.internal.classfile.impl.ExceptionCatchImpl;
import jdk.internal.classfile.impl.Instructions;

/**
 * Where a method's body gets written.
 *
 * <p>It is the package's largest interface --more than two hundred members-- and all that surface is
 * a single idea repeated: for each opcode of the JVMS there is a method with its name, and its body
 * is `with(...)` of the element that corresponds to it. `aload(3)` is
 * `with(Instructions.load(ALOAD, 3))`. Nothing else.
 *
 * <p>What does have logic of its own is six of them: {@link #block}, {@link #ifThen},
 * {@link #ifThenElse}, {@link #trying}, {@link #transforming} and {@link #loadConstant}. The first
 * five build labels and bind them by themselves --which is what one does not want to do by hand--
 * and the last picks the shortest opcode that will serve for the constant it is given.
 *
 * <h2>The labels</h2>
 *
 * <p>A {@link Label} is an unknown until it is bound: {@link #newLabel} creates it and
 * {@link #labelBinding} says where it falls. A label can be jumped to before being bound --that is
 * the normal thing in a forward jump-- and the writer resolves the offsets when closing the method.
 * {@link #newBoundLabel} is the shortcut for "a label right here".
 *
 * <h2>The slots</h2>
 *
 * <p>{@link #receiverSlot}, {@link #parameterSlot} and {@link #allocateLocal} exist because a
 * variable's slot number is **not** its position among the parameters: a `long` and a `double` take
 * two. Computing it by hand is the easiest way of writing a method that does not verify.
 */
public interface CodeBuilder extends ClassFileBuilder<CodeElement, CodeBuilder> {

    // ---- labels and slots ----------------------------------------------------------------------

    /** A new label, not bound yet. */
    Label newLabel();

    /** The label of the method's start. */
    Label startLabel();

    /** The label of the method's end. */
    Label endLabel();

    /** The receiver's slot (`this`). It throws if the method is static: there is no receiver
     * there. */
    int receiverSlot();

    /** The slot of parameter number `i`, counting from zero and leaving out the receiver. */
    int parameterSlot(int paramNo);

    /** It reserves a new slot for a variable of that type and returns it. */
    int allocateLocal(TypeKind typeKind);

    /** A label bound at this point. */
    default Label newBoundLabel() {
        Label l = this.newLabel();
        this.labelBinding(l);
        return l;
    }

    /** It binds that label to this point. */
    default CodeBuilder labelBinding(Label label) {
        return this.with(Instructions.labelTarget(label));
    }

    // ---- instructions with no operand ----------------------------------------------------
    //
    // One per opcode, all with the same body. They are generated from the opcode table, not
    // written by hand, and the reason is that by hand they are a hundred and sixty chances of
    // getting a single one wrong.

    /** `nop`. */
    default CodeBuilder nop() {
        return this.with(Instructions.nop());
    }

    /** `athrow`. */
    default CodeBuilder athrow() {
        return this.with(Instructions.throwInstruction());
    }

    /** `arraylength`. */
    default CodeBuilder arraylength() {
        return this.with(Instructions.operator(Opcode.ARRAYLENGTH));
    }

    /** `aaload`. */
    default CodeBuilder aaload() {
        return this.with(Instructions.arrayLoad(Opcode.AALOAD));
    }

    /** `baload`. */
    default CodeBuilder baload() {
        return this.with(Instructions.arrayLoad(Opcode.BALOAD));
    }

    /** `caload`. */
    default CodeBuilder caload() {
        return this.with(Instructions.arrayLoad(Opcode.CALOAD));
    }

    /** `daload`. */
    default CodeBuilder daload() {
        return this.with(Instructions.arrayLoad(Opcode.DALOAD));
    }

    /** `faload`. */
    default CodeBuilder faload() {
        return this.with(Instructions.arrayLoad(Opcode.FALOAD));
    }

    /** `iaload`. */
    default CodeBuilder iaload() {
        return this.with(Instructions.arrayLoad(Opcode.IALOAD));
    }

    /** `laload`. */
    default CodeBuilder laload() {
        return this.with(Instructions.arrayLoad(Opcode.LALOAD));
    }

    /** `saload`. */
    default CodeBuilder saload() {
        return this.with(Instructions.arrayLoad(Opcode.SALOAD));
    }

    /** `aastore`. */
    default CodeBuilder aastore() {
        return this.with(Instructions.arrayStore(Opcode.AASTORE));
    }

    /** `bastore`. */
    default CodeBuilder bastore() {
        return this.with(Instructions.arrayStore(Opcode.BASTORE));
    }

    /** `castore`. */
    default CodeBuilder castore() {
        return this.with(Instructions.arrayStore(Opcode.CASTORE));
    }

    /** `dastore`. */
    default CodeBuilder dastore() {
        return this.with(Instructions.arrayStore(Opcode.DASTORE));
    }

    /** `fastore`. */
    default CodeBuilder fastore() {
        return this.with(Instructions.arrayStore(Opcode.FASTORE));
    }

    /** `iastore`. */
    default CodeBuilder iastore() {
        return this.with(Instructions.arrayStore(Opcode.IASTORE));
    }

    /** `lastore`. */
    default CodeBuilder lastore() {
        return this.with(Instructions.arrayStore(Opcode.LASTORE));
    }

    /** `sastore`. */
    default CodeBuilder sastore() {
        return this.with(Instructions.arrayStore(Opcode.SASTORE));
    }

    /** `dup`. */
    default CodeBuilder dup() {
        return this.with(Instructions.stack(Opcode.DUP));
    }

    /** `dup2`. */
    default CodeBuilder dup2() {
        return this.with(Instructions.stack(Opcode.DUP2));
    }

    /** `dup2_x1`. */
    default CodeBuilder dup2_x1() {
        return this.with(Instructions.stack(Opcode.DUP2_X1));
    }

    /** `dup2_x2`. */
    default CodeBuilder dup2_x2() {
        return this.with(Instructions.stack(Opcode.DUP2_X2));
    }

    /** `dup_x1`. */
    default CodeBuilder dup_x1() {
        return this.with(Instructions.stack(Opcode.DUP_X1));
    }

    /** `dup_x2`. */
    default CodeBuilder dup_x2() {
        return this.with(Instructions.stack(Opcode.DUP_X2));
    }

    /** `pop`. */
    default CodeBuilder pop() {
        return this.with(Instructions.stack(Opcode.POP));
    }

    /** `pop2`. */
    default CodeBuilder pop2() {
        return this.with(Instructions.stack(Opcode.POP2));
    }

    /** `swap`. */
    default CodeBuilder swap() {
        return this.with(Instructions.stack(Opcode.SWAP));
    }

    /** `d2f`. */
    default CodeBuilder d2f() {
        return this.with(Instructions.convert(Opcode.D2F));
    }

    /** `d2i`. */
    default CodeBuilder d2i() {
        return this.with(Instructions.convert(Opcode.D2I));
    }

    /** `d2l`. */
    default CodeBuilder d2l() {
        return this.with(Instructions.convert(Opcode.D2L));
    }

    /** `f2d`. */
    default CodeBuilder f2d() {
        return this.with(Instructions.convert(Opcode.F2D));
    }

    /** `f2i`. */
    default CodeBuilder f2i() {
        return this.with(Instructions.convert(Opcode.F2I));
    }

    /** `f2l`. */
    default CodeBuilder f2l() {
        return this.with(Instructions.convert(Opcode.F2L));
    }

    /** `i2b`. */
    default CodeBuilder i2b() {
        return this.with(Instructions.convert(Opcode.I2B));
    }

    /** `i2c`. */
    default CodeBuilder i2c() {
        return this.with(Instructions.convert(Opcode.I2C));
    }

    /** `i2d`. */
    default CodeBuilder i2d() {
        return this.with(Instructions.convert(Opcode.I2D));
    }

    /** `i2f`. */
    default CodeBuilder i2f() {
        return this.with(Instructions.convert(Opcode.I2F));
    }

    /** `i2l`. */
    default CodeBuilder i2l() {
        return this.with(Instructions.convert(Opcode.I2L));
    }

    /** `i2s`. */
    default CodeBuilder i2s() {
        return this.with(Instructions.convert(Opcode.I2S));
    }

    /** `l2d`. */
    default CodeBuilder l2d() {
        return this.with(Instructions.convert(Opcode.L2D));
    }

    /** `l2f`. */
    default CodeBuilder l2f() {
        return this.with(Instructions.convert(Opcode.L2F));
    }

    /** `l2i`. */
    default CodeBuilder l2i() {
        return this.with(Instructions.convert(Opcode.L2I));
    }

    /** `dadd`. */
    default CodeBuilder dadd() {
        return this.with(Instructions.operator(Opcode.DADD));
    }

    /** `dcmpg`. */
    default CodeBuilder dcmpg() {
        return this.with(Instructions.operator(Opcode.DCMPG));
    }

    /** `dcmpl`. */
    default CodeBuilder dcmpl() {
        return this.with(Instructions.operator(Opcode.DCMPL));
    }

    /** `ddiv`. */
    default CodeBuilder ddiv() {
        return this.with(Instructions.operator(Opcode.DDIV));
    }

    /** `dmul`. */
    default CodeBuilder dmul() {
        return this.with(Instructions.operator(Opcode.DMUL));
    }

    /** `dneg`. */
    default CodeBuilder dneg() {
        return this.with(Instructions.operator(Opcode.DNEG));
    }

    /** `drem`. */
    default CodeBuilder drem() {
        return this.with(Instructions.operator(Opcode.DREM));
    }

    /** `dsub`. */
    default CodeBuilder dsub() {
        return this.with(Instructions.operator(Opcode.DSUB));
    }

    /** `fadd`. */
    default CodeBuilder fadd() {
        return this.with(Instructions.operator(Opcode.FADD));
    }

    /** `fcmpg`. */
    default CodeBuilder fcmpg() {
        return this.with(Instructions.operator(Opcode.FCMPG));
    }

    /** `fcmpl`. */
    default CodeBuilder fcmpl() {
        return this.with(Instructions.operator(Opcode.FCMPL));
    }

    /** `fdiv`. */
    default CodeBuilder fdiv() {
        return this.with(Instructions.operator(Opcode.FDIV));
    }

    /** `fmul`. */
    default CodeBuilder fmul() {
        return this.with(Instructions.operator(Opcode.FMUL));
    }

    /** `fneg`. */
    default CodeBuilder fneg() {
        return this.with(Instructions.operator(Opcode.FNEG));
    }

    /** `frem`. */
    default CodeBuilder frem() {
        return this.with(Instructions.operator(Opcode.FREM));
    }

    /** `fsub`. */
    default CodeBuilder fsub() {
        return this.with(Instructions.operator(Opcode.FSUB));
    }

    /** `iadd`. */
    default CodeBuilder iadd() {
        return this.with(Instructions.operator(Opcode.IADD));
    }

    /** `iand`. */
    default CodeBuilder iand() {
        return this.with(Instructions.operator(Opcode.IAND));
    }

    /** `idiv`. */
    default CodeBuilder idiv() {
        return this.with(Instructions.operator(Opcode.IDIV));
    }

    /** `imul`. */
    default CodeBuilder imul() {
        return this.with(Instructions.operator(Opcode.IMUL));
    }

    /** `ineg`. */
    default CodeBuilder ineg() {
        return this.with(Instructions.operator(Opcode.INEG));
    }

    /** `ior`. */
    default CodeBuilder ior() {
        return this.with(Instructions.operator(Opcode.IOR));
    }

    /** `irem`. */
    default CodeBuilder irem() {
        return this.with(Instructions.operator(Opcode.IREM));
    }

    /** `ishl`. */
    default CodeBuilder ishl() {
        return this.with(Instructions.operator(Opcode.ISHL));
    }

    /** `ishr`. */
    default CodeBuilder ishr() {
        return this.with(Instructions.operator(Opcode.ISHR));
    }

    /** `isub`. */
    default CodeBuilder isub() {
        return this.with(Instructions.operator(Opcode.ISUB));
    }

    /** `iushr`. */
    default CodeBuilder iushr() {
        return this.with(Instructions.operator(Opcode.IUSHR));
    }

    /** `ixor`. */
    default CodeBuilder ixor() {
        return this.with(Instructions.operator(Opcode.IXOR));
    }

    /** `ladd`. */
    default CodeBuilder ladd() {
        return this.with(Instructions.operator(Opcode.LADD));
    }

    /** `land`. */
    default CodeBuilder land() {
        return this.with(Instructions.operator(Opcode.LAND));
    }

    /** `lcmp`. */
    default CodeBuilder lcmp() {
        return this.with(Instructions.operator(Opcode.LCMP));
    }

    /** `ldiv`. */
    default CodeBuilder ldiv() {
        return this.with(Instructions.operator(Opcode.LDIV));
    }

    /** `lmul`. */
    default CodeBuilder lmul() {
        return this.with(Instructions.operator(Opcode.LMUL));
    }

    /** `lneg`. */
    default CodeBuilder lneg() {
        return this.with(Instructions.operator(Opcode.LNEG));
    }

    /** `lor`. */
    default CodeBuilder lor() {
        return this.with(Instructions.operator(Opcode.LOR));
    }

    /** `lrem`. */
    default CodeBuilder lrem() {
        return this.with(Instructions.operator(Opcode.LREM));
    }

    /** `lshl`. */
    default CodeBuilder lshl() {
        return this.with(Instructions.operator(Opcode.LSHL));
    }

    /** `lshr`. */
    default CodeBuilder lshr() {
        return this.with(Instructions.operator(Opcode.LSHR));
    }

    /** `lsub`. */
    default CodeBuilder lsub() {
        return this.with(Instructions.operator(Opcode.LSUB));
    }

    /** `lushr`. */
    default CodeBuilder lushr() {
        return this.with(Instructions.operator(Opcode.LUSHR));
    }

    /** `lxor`. */
    default CodeBuilder lxor() {
        return this.with(Instructions.operator(Opcode.LXOR));
    }

    /** `monitorenter`. */
    default CodeBuilder monitorenter() {
        return this.with(Instructions.monitor(Opcode.MONITORENTER));
    }

    /** `monitorexit`. */
    default CodeBuilder monitorexit() {
        return this.with(Instructions.monitor(Opcode.MONITOREXIT));
    }

    /** `areturn`. */
    default CodeBuilder areturn() {
        return this.with(Instructions.returnInstruction(Opcode.ARETURN));
    }

    /** `dreturn`. */
    default CodeBuilder dreturn() {
        return this.with(Instructions.returnInstruction(Opcode.DRETURN));
    }

    /** `freturn`. */
    default CodeBuilder freturn() {
        return this.with(Instructions.returnInstruction(Opcode.FRETURN));
    }

    /** `ireturn`. */
    default CodeBuilder ireturn() {
        return this.with(Instructions.returnInstruction(Opcode.IRETURN));
    }

    /** `lreturn`. */
    default CodeBuilder lreturn() {
        return this.with(Instructions.returnInstruction(Opcode.LRETURN));
    }

    /** `aconst_null`. */
    default CodeBuilder aconst_null() {
        return this.with(Instructions.intrinsicConstant(Opcode.ACONST_NULL));
    }

    /** `dconst_0`. */
    default CodeBuilder dconst_0() {
        return this.with(Instructions.intrinsicConstant(Opcode.DCONST_0));
    }

    /** `dconst_1`. */
    default CodeBuilder dconst_1() {
        return this.with(Instructions.intrinsicConstant(Opcode.DCONST_1));
    }

    /** `fconst_0`. */
    default CodeBuilder fconst_0() {
        return this.with(Instructions.intrinsicConstant(Opcode.FCONST_0));
    }

    /** `fconst_1`. */
    default CodeBuilder fconst_1() {
        return this.with(Instructions.intrinsicConstant(Opcode.FCONST_1));
    }

    /** `fconst_2`. */
    default CodeBuilder fconst_2() {
        return this.with(Instructions.intrinsicConstant(Opcode.FCONST_2));
    }

    /** `iconst_0`. */
    default CodeBuilder iconst_0() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_0));
    }

    /** `iconst_1`. */
    default CodeBuilder iconst_1() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_1));
    }

    /** `iconst_2`. */
    default CodeBuilder iconst_2() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_2));
    }

    /** `iconst_3`. */
    default CodeBuilder iconst_3() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_3));
    }

    /** `iconst_4`. */
    default CodeBuilder iconst_4() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_4));
    }

    /** `iconst_5`. */
    default CodeBuilder iconst_5() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_5));
    }

    /** `iconst_m1`. */
    default CodeBuilder iconst_m1() {
        return this.with(Instructions.intrinsicConstant(Opcode.ICONST_M1));
    }

    /** `lconst_0`. */
    default CodeBuilder lconst_0() {
        return this.with(Instructions.intrinsicConstant(Opcode.LCONST_0));
    }

    /** `lconst_1`. */
    default CodeBuilder lconst_1() {
        return this.with(Instructions.intrinsicConstant(Opcode.LCONST_1));
    }

    /** `return`. */
    default CodeBuilder return_() {
        return this.with(Instructions.returnInstruction(Opcode.RETURN));
    }

    // ---- with one immediate operand ------------------------------------------------------

    /** `aload` of that slot. */
    default CodeBuilder aload(int slot) {
        return this.with(Instructions.load(Opcode.ALOAD, slot));
    }

    /** `dload` of that slot. */
    default CodeBuilder dload(int slot) {
        return this.with(Instructions.load(Opcode.DLOAD, slot));
    }

    /** `fload` of that slot. */
    default CodeBuilder fload(int slot) {
        return this.with(Instructions.load(Opcode.FLOAD, slot));
    }

    /** `iload` of that slot. */
    default CodeBuilder iload(int slot) {
        return this.with(Instructions.load(Opcode.ILOAD, slot));
    }

    /** `lload` of that slot. */
    default CodeBuilder lload(int slot) {
        return this.with(Instructions.load(Opcode.LLOAD, slot));
    }

    /** `astore` into that slot. */
    default CodeBuilder astore(int slot) {
        return this.with(Instructions.store(Opcode.ASTORE, slot));
    }

    /** `dstore` into that slot. */
    default CodeBuilder dstore(int slot) {
        return this.with(Instructions.store(Opcode.DSTORE, slot));
    }

    /** `fstore` into that slot. */
    default CodeBuilder fstore(int slot) {
        return this.with(Instructions.store(Opcode.FSTORE, slot));
    }

    /** `istore` into that slot. */
    default CodeBuilder istore(int slot) {
        return this.with(Instructions.store(Opcode.ISTORE, slot));
    }

    /** `lstore` into that slot. */
    default CodeBuilder lstore(int slot) {
        return this.with(Instructions.store(Opcode.LSTORE, slot));
    }

    /** `bipush` of that byte. */
    default CodeBuilder bipush(int b) {
        return this.with(Instructions.argumentConstant(Opcode.BIPUSH, b));
    }

    /** `sipush` of that short. */
    default CodeBuilder sipush(int s) {
        return this.with(Instructions.argumentConstant(Opcode.SIPUSH, s));
    }

    /** `iinc` of that slot by that constant. */
    default CodeBuilder iinc(int slot, int val) {
        return this.with(Instructions.increment(slot, val));
    }

    // ---- jumps ----------------------------------------------------------------------------

    /** `goto` to that label. */
    default CodeBuilder goto_(Label target) {
        return this.with(Instructions.branch(Opcode.GOTO, target));
    }

    /** `goto_w` to that label. */
    default CodeBuilder goto_w(Label target) {
        return this.with(Instructions.branch(Opcode.GOTO_W, target));
    }

    /** `if_acmpeq` to that label. */
    default CodeBuilder if_acmpeq(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ACMPEQ, target));
    }

    /** `if_acmpne` to that label. */
    default CodeBuilder if_acmpne(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ACMPNE, target));
    }

    /** `if_icmpeq` to that label. */
    default CodeBuilder if_icmpeq(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPEQ, target));
    }

    /** `if_icmpge` to that label. */
    default CodeBuilder if_icmpge(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPGE, target));
    }

    /** `if_icmpgt` to that label. */
    default CodeBuilder if_icmpgt(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPGT, target));
    }

    /** `if_icmple` to that label. */
    default CodeBuilder if_icmple(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPLE, target));
    }

    /** `if_icmplt` to that label. */
    default CodeBuilder if_icmplt(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPLT, target));
    }

    /** `if_icmpne` to that label. */
    default CodeBuilder if_icmpne(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPNE, target));
    }

    /** `ifeq` to that label. */
    default CodeBuilder ifeq(Label target) {
        return this.with(Instructions.branch(Opcode.IFEQ, target));
    }

    /** `ifge` to that label. */
    default CodeBuilder ifge(Label target) {
        return this.with(Instructions.branch(Opcode.IFGE, target));
    }

    /** `ifgt` to that label. */
    default CodeBuilder ifgt(Label target) {
        return this.with(Instructions.branch(Opcode.IFGT, target));
    }

    /** `ifle` to that label. */
    default CodeBuilder ifle(Label target) {
        return this.with(Instructions.branch(Opcode.IFLE, target));
    }

    /** `iflt` to that label. */
    default CodeBuilder iflt(Label target) {
        return this.with(Instructions.branch(Opcode.IFLT, target));
    }

    /** `ifne` to that label. */
    default CodeBuilder ifne(Label target) {
        return this.with(Instructions.branch(Opcode.IFNE, target));
    }

    /** `ifnonnull` to that label. */
    default CodeBuilder ifnonnull(Label target) {
        return this.with(Instructions.branch(Opcode.IFNONNULL, target));
    }

    /** `ifnull` to that label. */
    default CodeBuilder ifnull(Label target) {
        return this.with(Instructions.branch(Opcode.IFNULL, target));
    }

    /** That opcode's jump to that label. */
    default CodeBuilder branch(Opcode op, Label target) {
        return this.with(Instructions.branch(op, target));
    }

    // ---- field access and calls ---------------------------------------------------------------

    /** That opcode's access to that field. */
    default CodeBuilder fieldAccess(Opcode opcode, FieldRefEntry ref) {
        return this.with(Instructions.field(opcode, ref));
    }

    /** That opcode's access to the field named by its owner, its name and its type. */
    default CodeBuilder fieldAccess(Opcode opcode, ClassDesc owner, String name, ClassDesc type) {
        return this.fieldAccess(opcode, this.constantPool().fieldRefEntry(owner, name, type));
    }

    /** `getfield`. */
    default CodeBuilder getfield(FieldRefEntry ref) {
        return this.fieldAccess(Opcode.GETFIELD, ref);
    }

    /** `getfield`. */
    default CodeBuilder getfield(ClassDesc owner, String name, ClassDesc type) {
        return this.fieldAccess(Opcode.GETFIELD, owner, name, type);
    }

    /** `getstatic`. */
    default CodeBuilder getstatic(FieldRefEntry ref) {
        return this.fieldAccess(Opcode.GETSTATIC, ref);
    }

    /** `getstatic`. */
    default CodeBuilder getstatic(ClassDesc owner, String name, ClassDesc type) {
        return this.fieldAccess(Opcode.GETSTATIC, owner, name, type);
    }

    /** `putfield`. */
    default CodeBuilder putfield(FieldRefEntry ref) {
        return this.fieldAccess(Opcode.PUTFIELD, ref);
    }

    /** `putfield`. */
    default CodeBuilder putfield(ClassDesc owner, String name, ClassDesc type) {
        return this.fieldAccess(Opcode.PUTFIELD, owner, name, type);
    }

    /** `putstatic`. */
    default CodeBuilder putstatic(FieldRefEntry ref) {
        return this.fieldAccess(Opcode.PUTSTATIC, ref);
    }

    /** `putstatic`. */
    default CodeBuilder putstatic(ClassDesc owner, String name, ClassDesc type) {
        return this.fieldAccess(Opcode.PUTSTATIC, owner, name, type);
    }

    /** That opcode's call to that method. */
    default CodeBuilder invoke(Opcode opcode, MemberRefEntry ref) {
        return this.with(Instructions.invoke(opcode, ref));
    }

    /**
     * That opcode's call to the method named by its owner, its name and its type.
     *
     * <p>`isInterface` is **not** redundant with the opcode, and it is this API's classic mistake: an
     * `invokestatic` and an `invokespecial` to an interface's method carry an `InterfaceMethodref`
     * and not a `Methodref`, and the JVM rejects the class if it is given the other one. The opcode
     * is not enough to decide it; that is why the parameter is there.
     */
    default CodeBuilder invoke(Opcode opcode, ClassDesc owner, String name, MethodTypeDesc type,
            boolean isInterface) {
        // With an `if` and not with a ternary: the two branches give different types
        // --`InterfaceMethodRefEntry` and `MethodRefEntry`-- and the common supertype is computed by
        // the compiler; our javac does not do it, and with the local written out there is nothing to
        // compute.
        MemberRefEntry ref;
        if (isInterface) {
            ref = this.constantPool().interfaceMethodRefEntry(owner, name, type);
        } else {
            ref = this.constantPool().methodRefEntry(owner, name, type);
        }
        return this.invoke(opcode, ref);
    }

    /** `invokevirtual`. */
    default CodeBuilder invokevirtual(MethodRefEntry ref) {
        return this.invoke(Opcode.INVOKEVIRTUAL, ref);
    }

    /** `invokevirtual`. */
    default CodeBuilder invokevirtual(ClassDesc owner, String name, MethodTypeDesc type) {
        return this.invoke(Opcode.INVOKEVIRTUAL, owner, name, type, false);
    }

    /** `invokespecial` to a class method. */
    default CodeBuilder invokespecial(MethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESPECIAL, ref);
    }

    /** `invokespecial` to an interface method. */
    default CodeBuilder invokespecial(InterfaceMethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESPECIAL, ref);
    }

    /** `invokespecial` to a class method. */
    default CodeBuilder invokespecial(ClassDesc owner, String name, MethodTypeDesc type) {
        return this.invoke(Opcode.INVOKESPECIAL, owner, name, type, false);
    }

    /** `invokespecial`, saying whether the owner is an interface. See the note on {@link #invoke}. */
    default CodeBuilder invokespecial(ClassDesc owner, String name, MethodTypeDesc type,
            boolean isInterface) {
        return this.invoke(Opcode.INVOKESPECIAL, owner, name, type, isInterface);
    }

    /** `invokestatic` to a class method. */
    default CodeBuilder invokestatic(MethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESTATIC, ref);
    }

    /** `invokestatic` to an interface method. */
    default CodeBuilder invokestatic(InterfaceMethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESTATIC, ref);
    }

    /** `invokestatic` to a class method. */
    default CodeBuilder invokestatic(ClassDesc owner, String name, MethodTypeDesc type) {
        return this.invoke(Opcode.INVOKESTATIC, owner, name, type, false);
    }

    /** `invokestatic`, saying whether the owner is an interface. See the note on {@link #invoke}. */
    default CodeBuilder invokestatic(ClassDesc owner, String name, MethodTypeDesc type,
            boolean isInterface) {
        return this.invoke(Opcode.INVOKESTATIC, owner, name, type, isInterface);
    }

    /** `invokeinterface`. */
    default CodeBuilder invokeinterface(InterfaceMethodRefEntry ref) {
        return this.invoke(Opcode.INVOKEINTERFACE, ref);
    }

    /** `invokeinterface`. */
    default CodeBuilder invokeinterface(ClassDesc owner, String name, MethodTypeDesc type) {
        return this.invoke(Opcode.INVOKEINTERFACE, owner, name, type, true);
    }

    /** `invokedynamic`. */
    default CodeBuilder invokedynamic(InvokeDynamicEntry ref) {
        return this.with(Instructions.invokeDynamic(ref));
    }

    /** `invokedynamic` from the call site's descriptor. */
    default CodeBuilder invokedynamic(DynamicCallSiteDesc desc) {
        return this.invokedynamic(this.constantPool().invokeDynamicEntry(desc));
    }

    // ---- creation and type checking -----------------------------------------------------------

    /** `new`. */
    default CodeBuilder new_(ClassEntry clazz) {
        return this.with(Instructions.newObject(clazz));
    }

    /** `new`. */
    default CodeBuilder new_(ClassDesc clazz) {
        return this.new_(this.constantPool().classEntry(clazz));
    }

    /** `anewarray`. */
    default CodeBuilder anewarray(ClassEntry clazz) {
        return this.with(Instructions.newReferenceArray(clazz));
    }

    /** `anewarray`. */
    default CodeBuilder anewarray(ClassDesc clazz) {
        return this.anewarray(this.constantPool().classEntry(clazz));
    }

    /** `newarray` of that primitive type. */
    default CodeBuilder newarray(TypeKind typeKind) {
        return this.with(Instructions.newPrimitiveArray(typeKind));
    }

    /** `multianewarray`. */
    default CodeBuilder multianewarray(ClassEntry array, int dims) {
        return this.with(Instructions.newMultiArray(array, dims));
    }

    /** `multianewarray`. */
    default CodeBuilder multianewarray(ClassDesc array, int dims) {
        return this.multianewarray(this.constantPool().classEntry(array), dims);
    }

    /** `checkcast`. */
    default CodeBuilder checkcast(ClassEntry type) {
        return this.with(Instructions.typeCheck(Opcode.CHECKCAST, type));
    }

    /** `checkcast`. */
    default CodeBuilder checkcast(ClassDesc type) {
        return this.checkcast(this.constantPool().classEntry(type));
    }

    /** `instanceof`. It is called this and not `instanceof` because that word is reserved. */
    default CodeBuilder instanceOf(ClassEntry type) {
        return this.with(Instructions.typeCheck(Opcode.INSTANCEOF, type));
    }

    /** `instanceof`. */
    default CodeBuilder instanceOf(ClassDesc type) {
        return this.instanceOf(this.constantPool().classEntry(type));
    }

    // ---- constants ------------------------------------------------------------------------------

    /** `ldc` of that entry. */
    default CodeBuilder ldc(LoadableConstantEntry entry) {
        return this.with(Instructions.loadConstant(
                entry.typeKind().slotSize() == 2 ? Opcode.LDC2_W : Opcode.LDC, entry));
    }

    /** `ldc` of that constant. */
    default CodeBuilder ldc(ConstantDesc value) {
        return this.ldc(this.constantPool().loadableConstantEntry(value));
    }

    /**
     * The constant, with the **shortest** opcode that can load it.
     *
     * <p>It is the difference from {@link #ldc}, and it is not cosmetic: an `iconst_1` takes one byte
     * and does not touch the pool; an `ldc` takes two and puts an entry into it. For small values
     * --which are nearly all of them-- the difference multiplies by each appearance.
     */
    default CodeBuilder loadConstant(int value) {
        if (value >= -1 && value <= 5) {
            return this.with(Instructions.intrinsicConstant(CodeBuilder.iconstOpcode(value)));
        }
        if (value >= -128 && value <= 127) {
            return this.bipush(value);
        }
        if (value >= -32768 && value <= 32767) {
            return this.sipush(value);
        }
        return this.ldc(Integer.valueOf(value));
    }

    /** The constant, with the shortest opcode. */
    default CodeBuilder loadConstant(long value) {
        if (value == 0L) {
            return this.lconst_0();
        }
        if (value == 1L) {
            return this.lconst_1();
        }
        return this.ldc(Long.valueOf(value));
    }

    /**
     * The constant, with the shortest opcode.
     *
     * <p>The comparison is by **bits** and not with `==` because `0.0f == -0.0f` is true and the two
     * are not the same constant: emitting `fconst_0` for a `-0.0f` would change the value's sign.
     * With `NaN` the reverse happens --it is never equal to anything, not even to itself-- and by
     * bits it can be compared.
     */
    default CodeBuilder loadConstant(float value) {
        int bits = Float.floatToRawIntBits(value);
        if (bits == Float.floatToRawIntBits(0.0f)) {
            return this.fconst_0();
        }
        if (bits == Float.floatToRawIntBits(1.0f)) {
            return this.fconst_1();
        }
        if (bits == Float.floatToRawIntBits(2.0f)) {
            return this.fconst_2();
        }
        return this.ldc(Float.valueOf(value));
    }

    /** The constant, with the shortest opcode. See the `float` version's note on the bits. */
    default CodeBuilder loadConstant(double value) {
        long bits = Double.doubleToRawLongBits(value);
        if (bits == Double.doubleToRawLongBits(0.0)) {
            return this.dconst_0();
        }
        if (bits == Double.doubleToRawLongBits(1.0)) {
            return this.dconst_1();
        }
        return this.ldc(Double.valueOf(value));
    }

    /**
     * The constant, with the shortest opcode, whatever its type.
     *
     * <p>`null` is loaded with `aconst_null`: it is the only value this form accepts that is not a
     * real `ConstantDesc`.
     */
    default CodeBuilder loadConstant(ConstantDesc value) {
        if (value == null) {
            return this.aconst_null();
        }
        if (value instanceof Integer) {
            return this.loadConstant(((Integer) value).intValue());
        }
        if (value instanceof Long) {
            return this.loadConstant(((Long) value).longValue());
        }
        if (value instanceof Float) {
            return this.loadConstant(((Float) value).floatValue());
        }
        if (value instanceof Double) {
            return this.loadConstant(((Double) value).doubleValue());
        }
        return this.ldc(value);
    }

    // The `iconst_*` of a value between -1 and 5. A static and not a `switch` in the body above
    // because an interface cannot have fields that are not constants, and a table would be the only
    // shorter thing.
    static Opcode iconstOpcode(int value) {
        if (value == -1) {
            return Opcode.ICONST_M1;
        }
        if (value == 0) {
            return Opcode.ICONST_0;
        }
        if (value == 1) {
            return Opcode.ICONST_1;
        }
        if (value == 2) {
            return Opcode.ICONST_2;
        }
        if (value == 3) {
            return Opcode.ICONST_3;
        }
        if (value == 4) {
            return Opcode.ICONST_4;
        }
        return Opcode.ICONST_5;
    }

    // ---- by type, instead of by opcode ----------------------------------------------------------
    //
    // The four below pick the opcode from a `TypeKind`. They are for code generated out of a
    // signature --where the type is known and the opcode is not-- and they avoid the four-branch
    // table one would otherwise have to write at each site.

    /** The load of a variable of that type from that slot. */
    default CodeBuilder loadLocal(TypeKind tk, int slot) {
        return this.with(Instructions.load(tk, slot));
    }

    /** The store of a variable of that type into that slot. */
    default CodeBuilder storeLocal(TypeKind tk, int slot) {
        return this.with(Instructions.store(tk, slot));
    }

    /** The load from an array of that type. */
    default CodeBuilder arrayLoad(TypeKind tk) {
        return this.with(Instructions.arrayLoad(CodeBuilder.arrayLoadOpcode(tk)));
    }

    /** The store into an array of that type. */
    default CodeBuilder arrayStore(TypeKind tk) {
        return this.with(Instructions.arrayStore(CodeBuilder.arrayStoreOpcode(tk)));
    }

    /** The return of that type. */
    default CodeBuilder return_(TypeKind tk) {
        return this.with(Instructions.returnInstruction(tk));
    }

    /** The conversion from one primitive type to another. */
    default CodeBuilder conversion(TypeKind from, TypeKind to) {
        return this.with(Instructions.convert(from, to));
    }

    // `boolean`, `byte`, `char` and `short` have their own array opcode --and there the difference
    // does matter, because the element's width changes-- even though on the stack all four are
    // `int`.
    static Opcode arrayLoadOpcode(TypeKind tk) {
        if (tk == TypeKind.BYTE || tk == TypeKind.BOOLEAN) {
            return Opcode.BALOAD;
        }
        if (tk == TypeKind.CHAR) {
            return Opcode.CALOAD;
        }
        if (tk == TypeKind.SHORT) {
            return Opcode.SALOAD;
        }
        if (tk == TypeKind.INT) {
            return Opcode.IALOAD;
        }
        if (tk == TypeKind.LONG) {
            return Opcode.LALOAD;
        }
        if (tk == TypeKind.FLOAT) {
            return Opcode.FALOAD;
        }
        if (tk == TypeKind.DOUBLE) {
            return Opcode.DALOAD;
        }
        if (tk == TypeKind.REFERENCE) {
            return Opcode.AALOAD;
        }
        throw new IllegalArgumentException("no array load for " + tk);
    }

    static Opcode arrayStoreOpcode(TypeKind tk) {
        if (tk == TypeKind.BYTE || tk == TypeKind.BOOLEAN) {
            return Opcode.BASTORE;
        }
        if (tk == TypeKind.CHAR) {
            return Opcode.CASTORE;
        }
        if (tk == TypeKind.SHORT) {
            return Opcode.SASTORE;
        }
        if (tk == TypeKind.INT) {
            return Opcode.IASTORE;
        }
        if (tk == TypeKind.LONG) {
            return Opcode.LASTORE;
        }
        if (tk == TypeKind.FLOAT) {
            return Opcode.FASTORE;
        }
        if (tk == TypeKind.DOUBLE) {
            return Opcode.DASTORE;
        }
        if (tk == TypeKind.REFERENCE) {
            return Opcode.AASTORE;
        }
        throw new IllegalArgumentException("no array store for " + tk);
    }

    // ---- switches --------------------------------------------------------------------------------

    /** `tableswitch` with that range. */
    default CodeBuilder tableswitch(int low, int high, Label defaultTarget, List<SwitchCase> cases) {
        return this.with(Instructions.tableSwitch(low, high, defaultTarget, cases));
    }

    /**
     * `tableswitch` with the range worked out from the cases.
     *
     * <p>The range goes from the least to the greatest of the values given. With sparse cases that
     * fills the table with holes pointing at the default target, and there `lookupswitch` is smaller
     * -- this form does not choose on one's behalf: it makes the `tableswitch` it was asked for.
     */
    default CodeBuilder tableswitch(Label defaultTarget, List<SwitchCase> cases) {
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("a tableswitch with no cases has no range");
        }
        int low = cases.get(0).caseValue();
        int high = low;
        for (int i = 1; i < cases.size(); i++) {
            int v = cases.get(i).caseValue();
            low = Math.min(low, v);
            high = Math.max(high, v);
        }
        return this.tableswitch(low, high, defaultTarget, cases);
    }

    /** `lookupswitch`. */
    default CodeBuilder lookupswitch(Label defaultTarget, List<SwitchCase> cases) {
        return this.with(Instructions.lookupSwitch(defaultTarget, cases));
    }

    // ---- pseudo-instructions --------------------------------------------------------------------

    /** A line number for what follows. */
    default CodeBuilder lineNumber(int line) {
        return this.with(Instructions.lineNumber(line));
    }

    /** A named local variable, for the debugger. */
    default CodeBuilder localVariable(int slot, Utf8Entry name, Utf8Entry descriptor,
            Label startScope, Label endScope) {
        return this.with(Instructions.localVariable(slot, name, descriptor, startScope, endScope));
    }

    /** A named local variable. */
    default CodeBuilder localVariable(int slot, String name, ClassDesc descriptor, Label startScope,
            Label endScope) {
        return this.with(Instructions.localVariable(slot, name, descriptor, startScope, endScope));
    }

    /** A local variable's generic type. */
    default CodeBuilder localVariableType(int slot, Utf8Entry name, Utf8Entry signature,
            Label startScope, Label endScope) {
        return this.with(
                Instructions.localVariableType(slot, name, signature, startScope, endScope));
    }

    /** A local variable's generic type. */
    default CodeBuilder localVariableType(int slot, String name, Signature signature,
            Label startScope, Label endScope) {
        return this.with(
                Instructions.localVariableType(slot, name, signature, startScope, endScope));
    }

    /** A range of source characters, for the tools that use them. */
    default CodeBuilder characterRange(Label startScope, Label endScope, int characterRangeStart,
            int characterRangeEnd, int flags) {
        return this.with(Instructions.characterRange(startScope, endScope, characterRangeStart,
                characterRangeEnd, flags));
    }

    /** An exception handler for that range. */
    default CodeBuilder exceptionCatch(Label start, Label end, Label handler, ClassEntry catchType) {
        return this.with(new ExceptionCatchImpl(handler, start, end, Optional.of(catchType)));
    }

    /** An exception handler for that range. */
    default CodeBuilder exceptionCatch(Label start, Label end, Label handler, ClassDesc catchType) {
        return this.exceptionCatch(start, end, handler, this.constantPool().classEntry(catchType));
    }

    /** An exception handler; with no type, it catches everything. */
    default CodeBuilder exceptionCatch(Label start, Label end, Label handler,
            Optional<ClassEntry> catchType) {
        return this.with(new ExceptionCatchImpl(handler, start, end, catchType));
    }

    /** A handler catching everything, including what is not an `Exception`. */
    default CodeBuilder exceptionCatchAll(Label start, Label end, Label handler) {
        return this.with(new ExceptionCatchImpl(handler, start, end, Optional.<ClassEntry>empty()));
    }

    // ---- the forms that build labels by themselves ----------------------------------------------
    //
    // The five below are the reason this interface is pleasant to use. They all do the same thing
    // inside --they ask for labels, write the body, bind the labels where they go-- and what they
    // contribute is that one does not have to remember to bind any. An unbound label is an error that
    // does not show until the class fails to verify.

    /** The body, between a start label and an end label. */
    default CodeBuilder block(Consumer<CodeBuilder> handler) {
        Label end = this.newLabel();
        handler.accept(this);
        this.labelBinding(end);
        return this;
    }

    /**
     * `if (cond) { ... }`, jumping with that opcode.
     *
     * <p>Mind the direction: the opcode handed in is the one for **entering** the body, so what gets
     * emitted is its opposite jump towards the end. `ifThen(IFEQ, ...)` runs the body when the
     * stack's value is zero.
     */
    default CodeBuilder ifThen(Opcode opcode, Consumer<CodeBuilder> thenHandler) {
        Label end = this.newLabel();
        this.branch(CodeBuilder.opposite(opcode), end);
        thenHandler.accept(this);
        this.labelBinding(end);
        return this;
    }

    /** `if (x != 0) { ... }`, which is the common case. */
    default CodeBuilder ifThen(Consumer<CodeBuilder> thenHandler) {
        return this.ifThen(Opcode.IFNE, thenHandler);
    }

    /** `if (cond) { ... } else { ... }`, jumping with that opcode. */
    default CodeBuilder ifThenElse(Opcode opcode, Consumer<CodeBuilder> thenHandler,
            Consumer<CodeBuilder> elseHandler) {
        Label otherwise = this.newLabel();
        Label end = this.newLabel();
        this.branch(CodeBuilder.opposite(opcode), otherwise);
        thenHandler.accept(this);
        this.goto_(end);
        this.labelBinding(otherwise);
        elseHandler.accept(this);
        this.labelBinding(end);
        return this;
    }

    /** `if (x != 0) { ... } else { ... }`. */
    default CodeBuilder ifThenElse(Consumer<CodeBuilder> thenHandler,
            Consumer<CodeBuilder> elseHandler) {
        return this.ifThenElse(Opcode.IFNE, thenHandler, elseHandler);
    }

    /**
     * A `try` with its `catch`es.
     *
     * <p>The `catchesHandler` gets a {@link CodeBuilder.CatchBuilder}, with which it declares one
     * handler per type. The protected range's labels and the jumps to the end are put in by this
     * form.
     */
    default CodeBuilder trying(Consumer<CodeBuilder> tryHandler,
            Consumer<CatchBuilder> catchesHandler) {
        Label tryStart = this.newBoundLabel();
        tryHandler.accept(this);
        Label tryEnd = this.newBoundLabel();
        Label end = this.newLabel();
        this.goto_(end);
        catchesHandler.accept(this.catchBuilder(tryStart, tryEnd, end));
        this.labelBinding(end);
        return this;
    }

    /** The `catch` builder {@link #trying} uses. */
    CatchBuilder catchBuilder(Label tryStart, Label tryEnd, Label end);

    /** It writes the body passing it through that transformation. */
    default CodeBuilder transforming(CodeTransform transform, Consumer<CodeBuilder> handler) {
        CodeBuilder inner = this.transformingBuilder(transform);
        transform.atStart(inner);
        handler.accept(inner);
        transform.atEnd(inner);
        return this;
    }

    /** The intermediate builder {@link #transforming} uses. */
    CodeBuilder transformingBuilder(CodeTransform transform);

    /**
     * The jump opposite to that one.
     *
     * <p>`ifThen` and `ifThenElse` need it: one declares the condition for **entering** and the
     * bytecode jumps when it does **not** hold.
     */
    static Opcode opposite(Opcode op) {
        if (op == Opcode.IFEQ) {
            return Opcode.IFNE;
        }
        if (op == Opcode.IFNE) {
            return Opcode.IFEQ;
        }
        if (op == Opcode.IFLT) {
            return Opcode.IFGE;
        }
        if (op == Opcode.IFGE) {
            return Opcode.IFLT;
        }
        if (op == Opcode.IFGT) {
            return Opcode.IFLE;
        }
        if (op == Opcode.IFLE) {
            return Opcode.IFGT;
        }
        if (op == Opcode.IFNULL) {
            return Opcode.IFNONNULL;
        }
        if (op == Opcode.IFNONNULL) {
            return Opcode.IFNULL;
        }
        if (op == Opcode.IF_ICMPEQ) {
            return Opcode.IF_ICMPNE;
        }
        if (op == Opcode.IF_ICMPNE) {
            return Opcode.IF_ICMPEQ;
        }
        if (op == Opcode.IF_ICMPLT) {
            return Opcode.IF_ICMPGE;
        }
        if (op == Opcode.IF_ICMPGE) {
            return Opcode.IF_ICMPLT;
        }
        if (op == Opcode.IF_ICMPGT) {
            return Opcode.IF_ICMPLE;
        }
        if (op == Opcode.IF_ICMPLE) {
            return Opcode.IF_ICMPGT;
        }
        if (op == Opcode.IF_ACMPEQ) {
            return Opcode.IF_ACMPNE;
        }
        if (op == Opcode.IF_ACMPNE) {
            return Opcode.IF_ACMPEQ;
        }
        throw new IllegalArgumentException(op + " is not a conditional jump");
    }

    /**
     * A {@link #trying}'s `catch`es.
     *
     * <p>Each call to {@link #catching} adds a handler for that type; {@link #catchingAll} the one
     * catching everything, and it has to go last -- a catch-all handler with another one after it
     * would leave that other one unreachable.
     */
    public interface CatchBuilder {

        /** A handler for that type. */
        CatchBuilder catching(ClassDesc exceptionType, Consumer<CodeBuilder> catchHandler);

        /** A handler for several types, with the same body. */
        CatchBuilder catchingMulti(List<ClassDesc> exceptionTypes,
                Consumer<CodeBuilder> catchHandler);

        /** The handler catching everything. It goes last. */
        void catchingAll(Consumer<CodeBuilder> catchAllHandler);
    }
}
