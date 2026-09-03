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
 * Donde se escribe el cuerpo de un metodo.
 *
 * <p>Es la interfaz mas grande del paquete --mas de doscientos miembros-- y toda esa superficie es
 * una sola idea repetida: por cada opcode del JVMS hay un metodo con su nombre, y su cuerpo es
 * `with(...)` del elemento que le corresponde. `aload(3)` es `with(Instructions.load(ALOAD, 3))`.
 * Nada mas.
 *
 * <p>Lo que si tiene logica propia son seis: {@link #block}, {@link #ifThen}, {@link #ifThenElse},
 * {@link #trying}, {@link #transforming} y {@link #loadConstant}. Los cinco primeros arman
 * etiquetas y las atan solas --que es lo que uno no quiere hacer a mano-- y el ultimo elige el
 * opcode mas corto que sirva para la constante que se le da.
 *
 * <h2>Las etiquetas</h2>
 *
 * <p>Una {@link Label} es una incognita hasta que se la ata: {@link #newLabel} la crea y
 * {@link #labelBinding} dice donde cae. Se puede saltar a una etiqueta antes de atarla --es lo
 * normal en un salto hacia adelante-- y el escritor resuelve los offsets al cerrar el metodo.
 * {@link #newBoundLabel} es el atajo para "una etiqueta aca mismo".
 *
 * <h2>Los slots</h2>
 *
 * <p>{@link #receiverSlot}, {@link #parameterSlot} y {@link #allocateLocal} existen porque el
 * numero de slot de una variable **no** es su posicion entre los parametros: un `long` y un `double`
 * ocupan dos. Calcularlo a mano es la forma mas facil de escribir un metodo que no verifica.
 */
public interface CodeBuilder extends ClassFileBuilder<CodeElement, CodeBuilder> {

    // ---- etiquetas y slots ---------------------------------------------------------------------

    /** Una etiqueta nueva, todavia sin atar. */
    Label newLabel();

    /** La etiqueta del principio del metodo. */
    Label startLabel();

    /** La etiqueta del final del metodo. */
    Label endLabel();

    /** El slot del receptor (`this`). Tira si el metodo es estatico: ahi no hay receptor. */
    int receiverSlot();

    /** El slot del parametro numero `i`, contando desde cero y sin el receptor. */
    int parameterSlot(int paramNo);

    /** Reserva un slot nuevo para una variable de ese tipo y lo devuelve. */
    int allocateLocal(TypeKind typeKind);

    /** Una etiqueta atada en este punto. */
    default Label newBoundLabel() {
        Label l = this.newLabel();
        this.labelBinding(l);
        return l;
    }

    /** Ata esa etiqueta a este punto. */
    default CodeBuilder labelBinding(Label label) {
        return this.with(Instructions.labelTarget(label));
    }

    // ---- instrucciones sin operando ------------------------------------------------------
    //
    // Uno por opcode, todos con el mismo cuerpo. Estan generados a partir de la tabla de
    // opcodes, no escritos a mano, y el motivo es que a mano son ciento sesenta
    // oportunidades de equivocarse en uno solo.

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

    // ---- con un operando inmediato -------------------------------------------------------

    /** `aload` de ese slot. */
    default CodeBuilder aload(int slot) {
        return this.with(Instructions.load(Opcode.ALOAD, slot));
    }

    /** `dload` de ese slot. */
    default CodeBuilder dload(int slot) {
        return this.with(Instructions.load(Opcode.DLOAD, slot));
    }

    /** `fload` de ese slot. */
    default CodeBuilder fload(int slot) {
        return this.with(Instructions.load(Opcode.FLOAD, slot));
    }

    /** `iload` de ese slot. */
    default CodeBuilder iload(int slot) {
        return this.with(Instructions.load(Opcode.ILOAD, slot));
    }

    /** `lload` de ese slot. */
    default CodeBuilder lload(int slot) {
        return this.with(Instructions.load(Opcode.LLOAD, slot));
    }

    /** `astore` en ese slot. */
    default CodeBuilder astore(int slot) {
        return this.with(Instructions.store(Opcode.ASTORE, slot));
    }

    /** `dstore` en ese slot. */
    default CodeBuilder dstore(int slot) {
        return this.with(Instructions.store(Opcode.DSTORE, slot));
    }

    /** `fstore` en ese slot. */
    default CodeBuilder fstore(int slot) {
        return this.with(Instructions.store(Opcode.FSTORE, slot));
    }

    /** `istore` en ese slot. */
    default CodeBuilder istore(int slot) {
        return this.with(Instructions.store(Opcode.ISTORE, slot));
    }

    /** `lstore` en ese slot. */
    default CodeBuilder lstore(int slot) {
        return this.with(Instructions.store(Opcode.LSTORE, slot));
    }

    /** `bipush` de ese byte. */
    default CodeBuilder bipush(int b) {
        return this.with(Instructions.argumentConstant(Opcode.BIPUSH, b));
    }

    /** `sipush` de ese short. */
    default CodeBuilder sipush(int s) {
        return this.with(Instructions.argumentConstant(Opcode.SIPUSH, s));
    }

    /** `iinc` de ese slot por esa constante. */
    default CodeBuilder iinc(int slot, int val) {
        return this.with(Instructions.increment(slot, val));
    }

    // ---- saltos ---------------------------------------------------------------------------

    /** `goto` a esa etiqueta. */
    default CodeBuilder goto_(Label target) {
        return this.with(Instructions.branch(Opcode.GOTO, target));
    }

    /** `goto_w` a esa etiqueta. */
    default CodeBuilder goto_w(Label target) {
        return this.with(Instructions.branch(Opcode.GOTO_W, target));
    }

    /** `if_acmpeq` a esa etiqueta. */
    default CodeBuilder if_acmpeq(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ACMPEQ, target));
    }

    /** `if_acmpne` a esa etiqueta. */
    default CodeBuilder if_acmpne(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ACMPNE, target));
    }

    /** `if_icmpeq` a esa etiqueta. */
    default CodeBuilder if_icmpeq(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPEQ, target));
    }

    /** `if_icmpge` a esa etiqueta. */
    default CodeBuilder if_icmpge(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPGE, target));
    }

    /** `if_icmpgt` a esa etiqueta. */
    default CodeBuilder if_icmpgt(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPGT, target));
    }

    /** `if_icmple` a esa etiqueta. */
    default CodeBuilder if_icmple(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPLE, target));
    }

    /** `if_icmplt` a esa etiqueta. */
    default CodeBuilder if_icmplt(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPLT, target));
    }

    /** `if_icmpne` a esa etiqueta. */
    default CodeBuilder if_icmpne(Label target) {
        return this.with(Instructions.branch(Opcode.IF_ICMPNE, target));
    }

    /** `ifeq` a esa etiqueta. */
    default CodeBuilder ifeq(Label target) {
        return this.with(Instructions.branch(Opcode.IFEQ, target));
    }

    /** `ifge` a esa etiqueta. */
    default CodeBuilder ifge(Label target) {
        return this.with(Instructions.branch(Opcode.IFGE, target));
    }

    /** `ifgt` a esa etiqueta. */
    default CodeBuilder ifgt(Label target) {
        return this.with(Instructions.branch(Opcode.IFGT, target));
    }

    /** `ifle` a esa etiqueta. */
    default CodeBuilder ifle(Label target) {
        return this.with(Instructions.branch(Opcode.IFLE, target));
    }

    /** `iflt` a esa etiqueta. */
    default CodeBuilder iflt(Label target) {
        return this.with(Instructions.branch(Opcode.IFLT, target));
    }

    /** `ifne` a esa etiqueta. */
    default CodeBuilder ifne(Label target) {
        return this.with(Instructions.branch(Opcode.IFNE, target));
    }

    /** `ifnonnull` a esa etiqueta. */
    default CodeBuilder ifnonnull(Label target) {
        return this.with(Instructions.branch(Opcode.IFNONNULL, target));
    }

    /** `ifnull` a esa etiqueta. */
    default CodeBuilder ifnull(Label target) {
        return this.with(Instructions.branch(Opcode.IFNULL, target));
    }

    /** El salto de ese opcode a esa etiqueta. */
    default CodeBuilder branch(Opcode op, Label target) {
        return this.with(Instructions.branch(op, target));
    }

    // ---- acceso a campos y llamadas -----------------------------------------------------------

    /** El acceso de ese opcode a ese campo. */
    default CodeBuilder fieldAccess(Opcode opcode, FieldRefEntry ref) {
        return this.with(Instructions.field(opcode, ref));
    }

    /** El acceso de ese opcode al campo nombrado por su dueño, su nombre y su tipo. */
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

    /** La llamada de ese opcode a ese método. */
    default CodeBuilder invoke(Opcode opcode, MemberRefEntry ref) {
        return this.with(Instructions.invoke(opcode, ref));
    }

    /**
     * La llamada de ese opcode al método nombrado por su dueño, su nombre y su tipo.
     *
     * <p>`isInterface` **no** es redundante con el opcode, y es el error clásico de esta API: un
     * `invokestatic` y un `invokespecial` a un método de una interfaz llevan un
     * `InterfaceMethodref` y no un `Methodref`, y la JVM rechaza la clase si se le pone el otro. El
     * opcode no alcanza para decidirlo; por eso el parámetro está.
     */
    default CodeBuilder invoke(Opcode opcode, ClassDesc owner, String name, MethodTypeDesc type,
            boolean isInterface) {
        // Con `if` y no con un ternario: las dos ramas dan tipos distintos --`InterfaceMethodRefEntry`
        // y `MethodRefEntry`-- y el supertipo comun lo calcula el compilador; nuestro javac no lo
        // hace, y con el local escrito no hay nada que calcular.
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

    /** `invokespecial` a un método de clase. */
    default CodeBuilder invokespecial(MethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESPECIAL, ref);
    }

    /** `invokespecial` a un método de interfaz. */
    default CodeBuilder invokespecial(InterfaceMethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESPECIAL, ref);
    }

    /** `invokespecial` a un método de clase. */
    default CodeBuilder invokespecial(ClassDesc owner, String name, MethodTypeDesc type) {
        return this.invoke(Opcode.INVOKESPECIAL, owner, name, type, false);
    }

    /** `invokespecial`, diciendo si el dueño es una interfaz. Ver la nota de {@link #invoke}. */
    default CodeBuilder invokespecial(ClassDesc owner, String name, MethodTypeDesc type,
            boolean isInterface) {
        return this.invoke(Opcode.INVOKESPECIAL, owner, name, type, isInterface);
    }

    /** `invokestatic` a un método de clase. */
    default CodeBuilder invokestatic(MethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESTATIC, ref);
    }

    /** `invokestatic` a un método de interfaz. */
    default CodeBuilder invokestatic(InterfaceMethodRefEntry ref) {
        return this.invoke(Opcode.INVOKESTATIC, ref);
    }

    /** `invokestatic` a un método de clase. */
    default CodeBuilder invokestatic(ClassDesc owner, String name, MethodTypeDesc type) {
        return this.invoke(Opcode.INVOKESTATIC, owner, name, type, false);
    }

    /** `invokestatic`, diciendo si el dueño es una interfaz. Ver la nota de {@link #invoke}. */
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

    /** `invokedynamic` desde el descriptor del call site. */
    default CodeBuilder invokedynamic(DynamicCallSiteDesc desc) {
        return this.invokedynamic(this.constantPool().invokeDynamicEntry(desc));
    }

    // ---- creación y chequeo de tipos ----------------------------------------------------------

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

    /** `newarray` de ese tipo primitivo. */
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

    /** `instanceof`. Se llama así y no `instanceof` porque esa palabra está reservada. */
    default CodeBuilder instanceOf(ClassEntry type) {
        return this.with(Instructions.typeCheck(Opcode.INSTANCEOF, type));
    }

    /** `instanceof`. */
    default CodeBuilder instanceOf(ClassDesc type) {
        return this.instanceOf(this.constantPool().classEntry(type));
    }

    // ---- constantes -----------------------------------------------------------------------------

    /** `ldc` de esa entrada. */
    default CodeBuilder ldc(LoadableConstantEntry entry) {
        return this.with(Instructions.loadConstant(
                entry.typeKind().slotSize() == 2 ? Opcode.LDC2_W : Opcode.LDC, entry));
    }

    /** `ldc` de esa constante. */
    default CodeBuilder ldc(ConstantDesc value) {
        return this.ldc(this.constantPool().loadableConstantEntry(value));
    }

    /**
     * La constante, con el opcode **más corto** que la puede cargar.
     *
     * <p>Es la diferencia con {@link #ldc}, y no es cosmética: un `iconst_1` ocupa un byte y no toca
     * el pool; un `ldc` ocupa dos y le mete una entrada. Para los valores chicos --que son casi
     * todos-- la diferencia se multiplica por cada aparición.
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

    /** La constante, con el opcode más corto. */
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
     * La constante, con el opcode más corto.
     *
     * <p>La comparación es por **bits** y no con `==` porque `0.0f == -0.0f` es cierto y las dos no
     * son la misma constante: emitir `fconst_0` para un `-0.0f` cambiaría el signo del valor. Con
     * `NaN` pasa lo inverso --nunca es igual a nada, ni a sí mismo-- y por bits sí se lo puede
     * comparar.
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

    /** La constante, con el opcode más corto. Ver la nota de la versión `float` sobre los bits. */
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
     * La constante, con el opcode más corto, sea del tipo que sea.
     *
     * <p>`null` se carga con `aconst_null`: es el único valor que esta forma acepta y que no es un
     * `ConstantDesc` de verdad.
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

    // El `iconst_*` de un valor entre -1 y 5. Estático y no un `switch` en el cuerpo de arriba
    // porque una interfaz no puede tener campos que no sean constantes, y una tabla sería lo único
    // más corto.
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

    // ---- por tipo, en vez de por opcode ---------------------------------------------------------
    //
    // Los cuatro de abajo eligen el opcode a partir de un `TypeKind`. Sirven para el código que se
    // genera a partir de una firma --donde el tipo se sabe y el opcode no-- y evitan la tabla de
    // cuatro ramas que si no hay que escribir en cada sitio.

    /** La carga de una variable de ese tipo desde ese slot. */
    default CodeBuilder loadLocal(TypeKind tk, int slot) {
        return this.with(Instructions.load(tk, slot));
    }

    /** El guardado de una variable de ese tipo en ese slot. */
    default CodeBuilder storeLocal(TypeKind tk, int slot) {
        return this.with(Instructions.store(tk, slot));
    }

    /** La carga desde un arreglo de ese tipo. */
    default CodeBuilder arrayLoad(TypeKind tk) {
        return this.with(Instructions.arrayLoad(CodeBuilder.arrayLoadOpcode(tk)));
    }

    /** El guardado en un arreglo de ese tipo. */
    default CodeBuilder arrayStore(TypeKind tk) {
        return this.with(Instructions.arrayStore(CodeBuilder.arrayStoreOpcode(tk)));
    }

    /** El retorno de ese tipo. */
    default CodeBuilder return_(TypeKind tk) {
        return this.with(Instructions.returnInstruction(tk));
    }

    /** La conversión de un tipo primitivo a otro. */
    default CodeBuilder conversion(TypeKind from, TypeKind to) {
        return this.with(Instructions.convert(from, to));
    }

    // `boolean`, `byte`, `char` y `short` tienen su propio opcode de arreglo --y ahí sí importa la
    // diferencia, porque el ancho del elemento cambia-- aunque en la pila los cuatro sean `int`.
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
        throw new IllegalArgumentException("no hay carga de arreglo para " + tk);
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
        throw new IllegalArgumentException("no hay guardado de arreglo para " + tk);
    }

    // ---- switches --------------------------------------------------------------------------------

    /** `tableswitch` con ese rango. */
    default CodeBuilder tableswitch(int low, int high, Label defaultTarget, List<SwitchCase> cases) {
        return this.with(Instructions.tableSwitch(low, high, defaultTarget, cases));
    }

    /**
     * `tableswitch` con el rango deducido de los casos.
     *
     * <p>El rango es del menor al mayor de los valores dados. Con casos dispersos eso llena la tabla
     * de huecos que apuntan al destino por omisión, y ahí `lookupswitch` es más chico -- esta forma
     * no elige por uno: hace el `tableswitch` que se le pidió.
     */
    default CodeBuilder tableswitch(Label defaultTarget, List<SwitchCase> cases) {
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("un tableswitch sin casos no tiene rango");
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

    // ---- pseudo-instrucciones ---------------------------------------------------------------------

    /** Un número de línea para lo que siga. */
    default CodeBuilder lineNumber(int line) {
        return this.with(Instructions.lineNumber(line));
    }

    /** Una variable local con nombre, para el depurador. */
    default CodeBuilder localVariable(int slot, Utf8Entry name, Utf8Entry descriptor,
            Label startScope, Label endScope) {
        return this.with(Instructions.localVariable(slot, name, descriptor, startScope, endScope));
    }

    /** Una variable local con nombre. */
    default CodeBuilder localVariable(int slot, String name, ClassDesc descriptor, Label startScope,
            Label endScope) {
        return this.with(Instructions.localVariable(slot, name, descriptor, startScope, endScope));
    }

    /** El tipo genérico de una variable local. */
    default CodeBuilder localVariableType(int slot, Utf8Entry name, Utf8Entry signature,
            Label startScope, Label endScope) {
        return this.with(
                Instructions.localVariableType(slot, name, signature, startScope, endScope));
    }

    /** El tipo genérico de una variable local. */
    default CodeBuilder localVariableType(int slot, String name, Signature signature,
            Label startScope, Label endScope) {
        return this.with(
                Instructions.localVariableType(slot, name, signature, startScope, endScope));
    }

    /** Un rango de caracteres del fuente, para las herramientas que los usan. */
    default CodeBuilder characterRange(Label startScope, Label endScope, int characterRangeStart,
            int characterRangeEnd, int flags) {
        return this.with(Instructions.characterRange(startScope, endScope, characterRangeStart,
                characterRangeEnd, flags));
    }

    /** Un manejador de excepciones para ese rango. */
    default CodeBuilder exceptionCatch(Label start, Label end, Label handler, ClassEntry catchType) {
        return this.with(new ExceptionCatchImpl(handler, start, end, Optional.of(catchType)));
    }

    /** Un manejador de excepciones para ese rango. */
    default CodeBuilder exceptionCatch(Label start, Label end, Label handler, ClassDesc catchType) {
        return this.exceptionCatch(start, end, handler, this.constantPool().classEntry(catchType));
    }

    /** Un manejador de excepciones; sin tipo, atrapa todo. */
    default CodeBuilder exceptionCatch(Label start, Label end, Label handler,
            Optional<ClassEntry> catchType) {
        return this.with(new ExceptionCatchImpl(handler, start, end, catchType));
    }

    /** Un manejador que atrapa todo, incluido lo que no es `Exception`. */
    default CodeBuilder exceptionCatchAll(Label start, Label end, Label handler) {
        return this.with(new ExceptionCatchImpl(handler, start, end, Optional.<ClassEntry>empty()));
    }

    // ---- las formas que arman etiquetas solas -----------------------------------------------------
    //
    // Las cinco de abajo son el motivo por el que esta interfaz es agradable de usar. Todas hacen lo
    // mismo por dentro --piden etiquetas, escriben el cuerpo, atan las etiquetas donde va-- y lo que
    // aportan es que uno no tiene que acordarse de atar ninguna. Una etiqueta sin atar es un error
    // que no se ve hasta que la clase no verifica.

    /** El cuerpo, entre una etiqueta de inicio y una de fin. */
    default CodeBuilder block(Consumer<CodeBuilder> handler) {
        Label end = this.newLabel();
        handler.accept(this);
        this.labelBinding(end);
        return this;
    }

    /**
     * `if (cond) { ... }`, saltando con ese opcode.
     *
     * <p>Ojo con el sentido: el opcode que se pasa es el de **entrar** al cuerpo, así que lo que se
     * emite es su salto contrario hacia el final. `ifThen(IFEQ, ...)` corre el cuerpo cuando el
     * valor de la pila es cero.
     */
    default CodeBuilder ifThen(Opcode opcode, Consumer<CodeBuilder> thenHandler) {
        Label end = this.newLabel();
        this.branch(CodeBuilder.opposite(opcode), end);
        thenHandler.accept(this);
        this.labelBinding(end);
        return this;
    }

    /** `if (x != 0) { ... }`, que es el caso común. */
    default CodeBuilder ifThen(Consumer<CodeBuilder> thenHandler) {
        return this.ifThen(Opcode.IFNE, thenHandler);
    }

    /** `if (cond) { ... } else { ... }`, saltando con ese opcode. */
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
     * Un `try` con sus `catch`.
     *
     * <p>El `catchesHandler` recibe un {@link CodeBuilder.CatchBuilder}, con el que declara un
     * manejador por tipo. Las etiquetas del rango protegido y los saltos al final los pone esta
     * forma.
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

    /** El constructor de `catch` que usa {@link #trying}. */
    CatchBuilder catchBuilder(Label tryStart, Label tryEnd, Label end);

    /** Escribe el cuerpo pasándolo por esa transformación. */
    default CodeBuilder transforming(CodeTransform transform, Consumer<CodeBuilder> handler) {
        CodeBuilder inner = this.transformingBuilder(transform);
        transform.atStart(inner);
        handler.accept(inner);
        transform.atEnd(inner);
        return this;
    }

    /** El constructor intermedio que usa {@link #transforming}. */
    CodeBuilder transformingBuilder(CodeTransform transform);

    /**
     * El salto contrario a ése.
     *
     * <p>Lo necesitan `ifThen` e `ifThenElse`: uno declara la condición para **entrar** y el
     * bytecode salta cuando **no** se cumple.
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
        throw new IllegalArgumentException(op + " no es un salto condicional");
    }

    /**
     * Los `catch` de un {@link #trying}.
     *
     * <p>Cada llamada a {@link #catching} agrega un manejador para ese tipo; {@link #catchingAll} el
     * que atrapa todo, y tiene que ir último — un manejador que atrapa todo después del cual hubiera
     * otro dejaría a ése inalcanzable.
     */
    public interface CatchBuilder {

        /** Un manejador para ese tipo. */
        CatchBuilder catching(ClassDesc exceptionType, Consumer<CodeBuilder> catchHandler);

        /** Un manejador para varios tipos, con el mismo cuerpo. */
        CatchBuilder catchingMulti(List<ClassDesc> exceptionTypes,
                Consumer<CodeBuilder> catchHandler);

        /** El manejador que atrapa todo. Va último. */
        void catchingAll(Consumer<CodeBuilder> catchAllHandler);
    }
}
