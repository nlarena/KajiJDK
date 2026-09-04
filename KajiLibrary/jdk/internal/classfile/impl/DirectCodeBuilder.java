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
 * El {@link CodeBuilder} que escribe bytes de verdad.
 *
 * <p>Junta los elementos que le dan y al cerrarse los serializa en un atributo `Code`. La escritura
 * es de una pasada con parches: cada salto deja su operando en blanco y se anota que hay que
 * completarlo; al terminar se conocen todos los offsets y se tapan los huecos. Es la unica forma de
 * escribir un salto hacia adelante sin recorrer el metodo dos veces.
 *
 * <h2>Lo que este escritor NO hace, y hay que saberlo</h2>
 *
 * <p><strong>No sintetiza un `StackMapTable`.</strong> Si el llamador agrega uno --y puede, con
 * {@link java.lang.classfile.attribute.StackMapTableAttribute}-- se escribe tal cual; si no, el
 * `Code` sale sin el. La consecuencia es concreta y conviene tenerla presente: una clase de version
 * 50 o mayor con saltos y sin `StackMapTable` **no pasa el verificador de una JVM**. El JDK lo
 * calcula solo; esto no, y calcularlo no es un detalle sino una inferencia de tipos sobre todo el
 * grafo de flujo, con el supertipo comun de cada union -- que es justamente para lo que existe
 * {@link java.lang.classfile.ClassHierarchyResolver}.
 *
 * <p>Lo que si hace, y hace bien: `max_stack` por recorrido del grafo (ver {@link StackCounter}),
 * `max_locals` por el mayor slot usado, la tabla de excepciones, y los atributos de depuracion.
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
     * @param descriptor el descriptor del metodo, para saber en que slot cae cada parametro
     * @param isStatic si no hay receptor
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
            int fin = desc.indexOf(';', j);
            return fin < 0 ? desc.length() : fin + 1;
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
            throw new IllegalStateException("un metodo estatico no tiene receptor");
        }
        return 0;
    }

    public int parameterSlot(int paramNo) {
        if (paramNo < 0 || paramNo >= this.paramSlots.length) {
            throw new IndexOutOfBoundsException("no hay parametro " + paramNo);
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

    // ---- serializacion --------------------------------------------------------------------------

    /** El atributo `Code` con todo lo que se acumulo. */
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

    // El resultado de recorrer los elementos: los bytes del codigo, donde cayo cada etiqueta, y todo
    // lo que va afuera del arreglo `code`.
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
                        "hay una etiqueta a la que nadie ato a ninguna posicion");
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
            throw new IllegalArgumentException("no se sabe escribir el elemento " + e);
        }
        em.offsets.put(this.end, Integer.valueOf(code.size()));

        byte[] bytes = code.toByteArray();
        DirectCodeBuilder.patch(bytes, fixups, em);
        em.code = bytes;
        em.maxLocals = maxSlot;
        em.maxStack = StackCounter.maxStack(instructions, em.handlers,
                StackCounter.indexLabels(raw));
        // Las etiquetas de los atributos de depuracion se resuelven recien acá, cuando ya se sabe
        // donde cayeron todas: un `LocalVariable` puede nombrar una etiqueta que se ata despues.
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

    // Cada parche es (posicion, ancho, etiqueta, base): el offset se escribe **relativo** a la
    // instruccion de salto, no absoluto, que es como el formato los guarda.
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

    // La codificacion de una instruccion. El reparto es por la **clase** del opcode y no por su
    // valor: el formato agrupa por forma --sin operando, con un indice, con un slot-- y esa es la
    // agrupacion que hace corto este metodo.
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
            // La tabla tiene una entrada por valor del rango, no una por caso: los huecos apuntan al
            // destino por omision. Es lo que distingue a un `tableswitch` de un `lookupswitch`.
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
                code.writeU1(op.bytecode()); // las formas `_0`..`_3`, que llevan el slot en el opcode
                return;
            }
            if (slot > 255) {
                // `wide`: el mismo opcode con el slot en dos bytes. Es la unica forma de nombrar un
                // slot alto, y un metodo generado con muchas variables llega ahi mas rapido de lo
                // que uno espera.
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
                // El `count` y el cero de relleno que solo lleva esta forma.
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
        // Los que no llevan operando: el opcode y nada mas.
        code.writeU1(op.bytecode());
    }

    private void emitConstant(BufWriterImpl code, Instruction ins, Opcode op) {
        if (op == Opcode.BIPUSH || op == Opcode.SIPUSH) {
            // Se pregunta por `ConstantInstruction` y se castea, en vez de por
            // `ArgumentConstantInstruction`, que estrecha el retorno a `Integer`: nuestro javac no
            // resuelve un metodo declarado en una interfaz ANIDADA que redefine al de la que la
            // encierra. Por el supertipo resuelve, y el valor es el mismo.
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
            // Con el local del tipo declarado en el medio: nuestro javac no siempre encadena a
            // traves de un metodo heredado del supertipo del retorno.
            java.lang.classfile.constantpool.LoadableConstantEntry entry =
                    Instructions.constantEntryOf(ins);
            // Por `indexOf` y no por `entry.index()`: la entrada puede venir del pool del
            // modelo original, y su indice ahi no significa nada aca.
            int index = code.indexOf(entry);
            // `ldc` nombra la entrada en UN byte. Una entrada de indice alto no entra, y ahi la
            // forma ancha no es una opcion sino la unica que existe -- por eso se corrige el opcode
            // en vez de fallar.
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
        // Los `iconst_*`, `aconst_null` y compania: el opcode lleva el valor.
        code.writeU1(op.bytecode());
    }

    // El relleno de un switch: su tabla arranca en el proximo multiplo de 4 **contado desde el
    // inicio del metodo**, no desde el opcode.
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

    // ---- las entradas de depuracion que esperan a que se resuelvan las etiquetas -----------------

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

// Una etiqueta que todavia no sabe donde cae. Se compara por identidad --es lo que manda el contrato
// de `Label`-- y por eso no lleva ningun estado: dos etiquetas distintas nunca son iguales aunque
// terminen en el mismo offset.
final class BuilderLabel implements Label {

    public String toString() {
        return "Label@" + System.identityHashCode(this);
    }
}

// El constructor de `catch` de `CodeBuilder.trying`. Cada `catching` escribe el cuerpo del manejador
// y agrega la entrada a la tabla de excepciones.
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
        List<java.lang.constant.ClassDesc> uno =
                new ArrayList<java.lang.constant.ClassDesc>();
        uno.add(exceptionType);
        return this.catchingMulti(uno, catchHandler);
    }

    public CodeBuilder.CatchBuilder catchingMulti(
            List<java.lang.constant.ClassDesc> exceptionTypes,
            java.util.function.Consumer<CodeBuilder> catchHandler) {
        if (this.closed) {
            throw new IllegalStateException(
                    "despues de catchingAll no puede haber otro manejador: seria inalcanzable");
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
            throw new IllegalStateException("ya hay un catchingAll");
        }
        this.closed = true;
        Label handler = this.builder.newBoundLabel();
        this.builder.exceptionCatchAll(this.tryStart, this.tryEnd, handler);
        catchAllHandler.accept(this.builder);
        this.builder.goto_(this.end);
    }
}
