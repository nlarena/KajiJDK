package jdk.internal.classfile.impl;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassFileVersion;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Interfaces;
import java.lang.classfile.Label;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Opcode;
import java.lang.classfile.Superclass;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolException;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.reflect.AccessFlag.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// The model of a read class. The file is walked whole in the constructor: header, interfaces,
// fields, methods and attributes. Nothing is left "for later" except the body of the methods, which
// is decoded the first time it is asked for.
public final class ClassModelImpl implements ClassModel {

    private final ClassReaderImpl reader;
    private final int major;
    private final int minor;
    private final AccessFlags flags;
    private final ClassEntry thisClass;
    private final ClassEntry superClass;
    private final List<ClassEntry> interfaces;
    private final List<FieldModel> fields;
    private final List<MethodModel> methods;
    private final List<Attribute<?>> attributes;

    public ClassModelImpl(ClassReaderImpl reader) {
        this.reader = reader;
        this.minor = reader.readU2(4);
        this.major = reader.readU2(6);
        this.flags = new AccessFlagsImpl(reader.flags(), Location.CLASS);
        this.thisClass = reader.thisClassEntry();
        this.superClass = reader.superclassEntry().orElse(null);

        int p = reader.headerOffset + 6;
        int nInterfaces = reader.readU2(p);
        p += 2;
        List<ClassEntry> ifs = new ArrayList<ClassEntry>();
        for (int i = 0; i < nInterfaces; i++) {
            ClassEntry ce = reader.readEntry(p, ClassEntry.class);
            ifs.add(ce);
            p += 2;
        }
        this.interfaces = Collections.unmodifiableList(ifs);

        int nFields = reader.readU2(p);
        p += 2;
        List<FieldModel> cs = new ArrayList<FieldModel>();
        for (int i = 0; i < nFields; i++) {
            FieldModelImpl f = new FieldModelImpl(this, reader, p);
            cs.add(f);
            p = f.end();
        }
        this.fields = Collections.unmodifiableList(cs);

        int nMethods = reader.readU2(p);
        p += 2;
        List<MethodModel> ms = new ArrayList<MethodModel>();
        for (int i = 0; i < nMethods; i++) {
            MethodModelImpl m = new MethodModelImpl(this, reader, p);
            ms.add(m);
            p = m.end();
        }
        this.methods = Collections.unmodifiableList(ms);

        AttributeList as = new AttributeList(reader, p, this);
        this.attributes = Collections.unmodifiableList(as.list);
        if (as.end != reader.classfileLength()) {
            throw new IllegalArgumentException("there are " + (reader.classfileLength() - as.end)
                    + " bytes left over after the class's last attribute");
        }
        // The bootstrap table has to be built before anybody resolves a dynamic pool entry, and
        // this is the first moment at which it is known where it is.
        int bsm = as.offsetOfAttribute(Attributes.NAME_BOOTSTRAP_METHODS);
        if (bsm >= 0) {
            reader.bootstrapTable(bsm);
        }
    }

    public ConstantPool constantPool() {
        return this.reader;
    }

    public AccessFlags flags() {
        return this.flags;
    }

    public ClassEntry thisClass() {
        return this.thisClass;
    }

    public int majorVersion() {
        return this.major;
    }

    public int minorVersion() {
        return this.minor;
    }

    public List<FieldModel> fields() {
        return this.fields;
    }

    public List<MethodModel> methods() {
        return this.methods;
    }

    public Optional<ClassEntry> superclass() {
        return Optional.ofNullable(this.superClass);
    }

    public List<ClassEntry> interfaces() {
        return this.interfaces;
    }

    public boolean isModuleInfo() {
        return (this.flags.flagsMask() & ClassFile.ACC_MODULE) != 0
                && this.thisClass.asInternalName().equals("module-info");
    }

    public List<Attribute<?>> attributes() {
        return this.attributes;
    }

    // The order is the file's, read from top to bottom: version, flags, superclass, interfaces,
    // fields, methods and attributes.
    public void forEach(Consumer<? super ClassElement> consumer) {
        consumer.accept(ClassFileVersion.of(this.major, this.minor));
        consumer.accept(this.flags);
        if (this.superClass != null) {
            consumer.accept(Superclass.of(this.superClass));
        }
        consumer.accept(Interfaces.of(this.interfaces));
        for (int i = 0; i < this.fields.size(); i++) {
            consumer.accept((ClassElement) this.fields.get(i));
        }
        for (int i = 0; i < this.methods.size(); i++) {
            consumer.accept((ClassElement) this.methods.get(i));
        }
        for (int i = 0; i < this.attributes.size(); i++) {
            consumer.accept((ClassElement) this.attributes.get(i));
        }
    }

    public String toString() {
        return "ClassModel[" + this.thisClass.asInternalName() + "]";
    }
}

// The attributes of a place in the file, read in one run. It also keeps the offset of each one's
// body: the model needs it to get back into `Code` and `BootstrapMethods`, which are the two
// attributes whose structure this reader does interpret.
final class AttributeList {

    final List<Attribute<?>> list = new ArrayList<Attribute<?>>();
    final List<String> names = new ArrayList<String>();
    final int[] offsets;
    final int end;

    AttributeList(ClassReaderImpl reader, int p, AttributedElement owner) {
        int n = reader.readU2(p);
        p += 2;
        this.offsets = new int[n];
        for (int i = 0; i < n; i++) {
            Utf8Entry name = reader.readEntry(p, Utf8Entry.class);
            int length = reader.readInt(p + 2);
            if (length < 0 || p + 6 + length > reader.classfileLength()) {
                throw new IllegalArgumentException("attribute " + name.stringValue()
                        + " claims length " + length + " bytes and does not fit in the file");
            }
            AttributeMapper<RawAttribute> mapper = Mappers.forName(name.stringValue());
            this.list.add(mapper.readAttribute(owner, reader, p + 6));
            this.names.add(name.stringValue());
            this.offsets[i] = p + 6;
            p += 6 + length;
        }
        this.end = p;
    }

    int offsetOfAttribute(String name) {
        for (int i = 0; i < this.names.size(); i++) {
            if (this.names.get(i).equals(name)) {
                return this.offsets[i];
            }
        }
        return -1;
    }
}

// A read field.
final class FieldModelImpl implements FieldModel {

    private final ClassModel owner;
    private final AccessFlags flags;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private final List<Attribute<?>> attributes;
    private final int end;

    FieldModelImpl(ClassModel owner, ClassReaderImpl reader, int p) {
        this.owner = owner;
        this.flags = new AccessFlagsImpl(reader.readU2(p), Location.FIELD);
        this.name = reader.readEntry(p + 2, Utf8Entry.class);
        this.descriptor = reader.readEntry(p + 4, Utf8Entry.class);
        AttributeList as = new AttributeList(reader, p + 6, this);
        this.attributes = Collections.unmodifiableList(as.list);
        this.end = as.end;
    }

    int end() {
        return this.end;
    }

    public AccessFlags flags() {
        return this.flags;
    }

    public Optional<ClassModel> parent() {
        return Optional.of(this.owner);
    }

    public Utf8Entry fieldName() {
        return this.name;
    }

    public Utf8Entry fieldType() {
        return this.descriptor;
    }

    public List<Attribute<?>> attributes() {
        return this.attributes;
    }

    public void forEach(Consumer<? super FieldElement> consumer) {
        consumer.accept(this.flags);
        for (int i = 0; i < this.attributes.size(); i++) {
            consumer.accept((FieldElement) this.attributes.get(i));
        }
    }

    public String toString() {
        return "FieldModel[" + this.name.stringValue() + " " + this.descriptor.stringValue() + "]";
    }
}

// A read method. The body is decoded the first time it is asked for.
final class MethodModelImpl implements MethodModel {

    private final ClassModel owner;
    private final ClassReaderImpl reader;
    private final AccessFlags flags;
    private final Utf8Entry name;
    private final Utf8Entry descriptor;
    private final List<Attribute<?>> attributes;
    private final int codeOffset;
    private final int end;
    private CodeModel body;

    MethodModelImpl(ClassModel owner, ClassReaderImpl reader, int p) {
        this.owner = owner;
        this.reader = reader;
        this.flags = new AccessFlagsImpl(reader.readU2(p), Location.METHOD);
        this.name = reader.readEntry(p + 2, Utf8Entry.class);
        this.descriptor = reader.readEntry(p + 4, Utf8Entry.class);
        AttributeList as = new AttributeList(reader, p + 6, this);
        this.attributes = Collections.unmodifiableList(as.list);
        this.codeOffset = as.offsetOfAttribute(Attributes.NAME_CODE);
        this.end = as.end;
    }

    int end() {
        return this.end;
    }

    public AccessFlags flags() {
        return this.flags;
    }

    public Optional<ClassModel> parent() {
        return Optional.of(this.owner);
    }

    public Utf8Entry methodName() {
        return this.name;
    }

    public Utf8Entry methodType() {
        return this.descriptor;
    }

    public List<Attribute<?>> attributes() {
        return this.attributes;
    }

    public Optional<CodeModel> code() {
        if (this.codeOffset < 0) {
            return Optional.empty();
        }
        if (this.body == null) {
            this.body = new CodeModelImpl(this, this.reader, this.codeOffset);
        }
        return Optional.of(this.body);
    }

    // Flags, body and attributes. The `Code` attribute is NOT emitted as an attribute: the
    // `CodeModel` is emitted, which is what the JDK does and what keeps the body from appearing
    // twice.
    public void forEach(Consumer<? super MethodElement> consumer) {
        consumer.accept(this.flags);
        Optional<CodeModel> c = code();
        if (c.isPresent()) {
            consumer.accept(c.get());
        }
        for (int i = 0; i < this.attributes.size(); i++) {
            Attribute<?> a = this.attributes.get(i);
            if (!a.attributeName().equalsString(Attributes.NAME_CODE)) {
                consumer.accept((MethodElement) a);
            }
        }
    }

    public String toString() {
        return "MethodModel[" + this.name.stringValue() + this.descriptor.stringValue() + "]";
    }
}

// The body of a method: the `Code` attribute (§4.7.3) decoded.
final class CodeModelImpl implements CodeModel {

    private final MethodModel owner;
    private final List<ExceptionCatch> handlers;
    private final List<Attribute<?>> attributes;
    private final List<Instruction> instructions;
    private final int maxStack;
    private final int maxLocals;

    CodeModelImpl(MethodModel owner, ClassReaderImpl reader, int p) {
        this.owner = owner;
        this.maxStack = reader.readU2(p);
        this.maxLocals = reader.readU2(p + 2);
        int codeLength = reader.readInt(p + 4);
        if (codeLength <= 0 || p + 8 + codeLength > reader.classfileLength()) {
            throw new IllegalArgumentException("invalid code_length: " + codeLength);
        }
        int start = p + 8;
        this.instructions = decode(reader, start, codeLength);

        int q = start + codeLength;
        int nHandlers = reader.readU2(q);
        q += 2;
        List<ExceptionCatch> hs = new ArrayList<ExceptionCatch>();
        for (int i = 0; i < nHandlers; i++) {
            int from = reader.readU2(q);
            int to = reader.readU2(q + 2);
            int handler = reader.readU2(q + 4);
            ClassEntry type = reader.readEntryOrNull(q + 6, ClassEntry.class);
            if (from > codeLength || to > codeLength || handler >= codeLength) {
                throw new IllegalArgumentException(
                        "an exception handler points outside the code array");
            }
            hs.add(new ExceptionCatchImpl(new LabelImpl(handler), new LabelImpl(from),
                    new LabelImpl(to), Optional.ofNullable(type)));
            q += 8;
        }
        this.handlers = Collections.unmodifiableList(hs);
        AttributeList as = new AttributeList(reader, q, this);
        this.attributes = Collections.unmodifiableList(as.list);
    }

    // The walk of the `code` array. The two cases that are not "add `sizeIfFixed()`" are the ones
    // that break a naive reader: `wide`, which changes the opcode space, and the two switches,
    // whose length depends on the padding up to the next multiple of 4 *from the start of the
    // method*.
    private static List<Instruction> decode(ClassReaderImpl reader, int start, int length) {
        List<Instruction> out = new ArrayList<Instruction>();
        int bci = 0;
        while (bci < length) {
            int b = reader.readU1(start + bci);
            Opcode op;
            int size;
            if (b == OpcodeTable.WIDE) {
                if (bci + 1 >= length) {
                    throw new IllegalArgumentException("a wide at the end of the code array");
                }
                op = OpcodeTable.wide(reader.readU1(start + bci + 1));
                if (op == null) {
                    throw new IllegalArgumentException(
                            "wide followed by an opcode that does not admit it, at bci " + bci);
                }
                size = op.sizeIfFixed();
            } else {
                op = OpcodeTable.simple(b);
                if (op == null) {
                    throw new IllegalArgumentException(
                            "unknown opcode 0x" + Integer.toHexString(b) + " at bci " + bci);
                }
                size = op.sizeIfFixed();
                if (size < 0) {
                    size = switchLength(reader, start, bci, op, length);
                }
            }
            if (bci + size > length) {
                throw new IllegalArgumentException(
                        "the instruction at bci " + bci + " runs off the code array");
            }
            out.add(Instructions.decode(reader, start, bci, op, size));
            bci += size;
        }
        return Collections.unmodifiableList(out);
    }

    private static int switchLength(ClassReaderImpl reader, int start, int bci, Opcode op,
            int length) {
        int p = bci + 1;
        while ((p & 3) != 0) {
            p++;
        }
        if (op == Opcode.TABLESWITCH) {
            if (start + p + 12 > reader.classfileLength()) {
                throw new IllegalArgumentException("a truncated tableswitch at bci " + bci);
            }
            int low = reader.readInt(start + p + 4);
            int high = reader.readInt(start + p + 8);
            if (high < low) {
                throw new IllegalArgumentException("tableswitch with high < low at bci " + bci);
            }
            long n = (long) high - (long) low + 1L;
            long size = (long) p + 12L + n * 4L - bci;
            if (size > length) {
                throw new IllegalArgumentException("a tableswitch that overruns the code array");
            }
            return (int) size;
        }
        if (start + p + 8 > reader.classfileLength()) {
            throw new IllegalArgumentException("a truncated lookupswitch at bci " + bci);
        }
        int n = reader.readInt(start + p + 4);
        if (n < 0) {
            throw new IllegalArgumentException("lookupswitch with negative npairs at bci " + bci);
        }
        long size = (long) p + 8L + (long) n * 8L - bci;
        if (size > length) {
            throw new IllegalArgumentException("a lookupswitch that overruns the code array");
        }
        return (int) size;
    }

    public Optional<MethodModel> parent() {
        return Optional.of(this.owner);
    }

    public List<ExceptionCatch> exceptionHandlers() {
        return this.handlers;
    }

    public List<Attribute<?>> attributes() {
        return this.attributes;
    }

    /**
     * The attribute's `max_stack`. It is not JDK API; it is here because the reader already has it.
     */
    public int maxStack() {
        return this.maxStack;
    }

    /** The attribute's `max_locals`. */
    public int maxLocals() {
        return this.maxLocals;
    }

    // Handlers, instructions and attributes of the code.
    //
    // What is NOT emitted: the debugging pseudo-instructions --`LabelTarget`, `LineNumber`,
    // `LocalVariable`, `LocalVariableType`, `CharacterRange`-- interleaved between the
    // instructions. The data is there: it is still there as the `LineNumberTable` attributes and
    // company, which this walk does emit. It is the same body the JDK produces with
    // `DebugElementsOption.DROP_DEBUG` and `DeadLabelsOption`, not a loss of information.
    public void forEach(Consumer<? super CodeElement> consumer) {
        for (int i = 0; i < this.handlers.size(); i++) {
            consumer.accept(this.handlers.get(i));
        }
        for (int i = 0; i < this.instructions.size(); i++) {
            consumer.accept(this.instructions.get(i));
        }
        for (int i = 0; i < this.attributes.size(); i++) {
            consumer.accept((CodeElement) this.attributes.get(i));
        }
    }

    public String toString() {
        return "CodeModel[" + this.instructions.size() + " instructions]";
    }
}
