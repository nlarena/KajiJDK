package jdk.internal.classfile.impl;

import java.lang.classfile.AttributeMapper;
import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.BufWriter;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassReader;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantPoolException;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// The low-level reader: the bytes of the file, the pool already built and validated, and the access
// by offset that the attribute mappers use.
//
// The validation is strict on purpose, and that is the design point of the file. On construction:
//
//   1. it demands the magic, a `constant_pool_count` &ge; 1 and that the pool fit in the file; 2.
//   it walks the entries checking that each tag is one of the seventeen and that the body of each
//   one fits in what is left; 3. it materialises the seventeen forms, and in doing so checks that
//   each referenced index exists and has THE TAG that corresponds -- a `CONSTANT_Class` pointing at
//   a `CONSTANT_Integer` does not pass; 4. it demands that the slot following a `long` or a
//   `double` not be used.
//
// A file that does not meet all that throws `ConstantPoolException` here, on opening, and not later
// from some loose call. Accepting a malformed `.class` and returning a half-built model would be
// worse than having no reader.
public final class ClassReaderImpl implements ClassReader {

    // Local copies of the tags of PoolEntry. They are not a duplicate for the fun of it: the frozen
    // javac's bytecode generator does not fold a constant of another compilation unit into a `case`
    // label (finding #461), so a `case TAG_UTF8:` does not compile. The values are those of JVMS
    // §4.4. The note said a test compares them against PoolEntry; no such test was found in the
    // tree.
    private static final int TAG_UTF8 = 1;
    private static final int TAG_INTEGER = 3;
    private static final int TAG_FLOAT = 4;
    private static final int TAG_LONG = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_CLASS = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_FIELDREF = 9;
    private static final int TAG_METHODREF = 10;
    private static final int TAG_INTERFACE_METHODREF = 11;
    private static final int TAG_NAME_AND_TYPE = 12;
    private static final int TAG_METHOD_HANDLE = 15;
    private static final int TAG_METHOD_TYPE = 16;
    private static final int TAG_DYNAMIC = 17;
    private static final int TAG_INVOKE_DYNAMIC = 18;
    private static final int TAG_MODULE = 19;
    private static final int TAG_PACKAGE = 20;

    private final byte[] bytes;
    private final int poolCount;
    // By pool index: the tag and the offset of the first byte of the `info`. Index 0 and the dead
    // slots are left with tag 0.
    private final int[] labels;
    private final int[] offsets;
    private final PoolEntry[] entries;
    private final Function<Utf8Entry, AttributeMapper<?>> custom;

    // The offset right after the pool, where the `access_flags` starts.
    final int headerOffset;
    private final int accessFlags;
    private final ClassEntry thisClass;
    private final ClassEntry superClass;

    // The `BootstrapMethods` table, which is not in the pool but in an attribute of the class. It
    // is filled when the `ClassModel` finishes reading the attributes, because until then it is not
    // known where it is.
    private List<BootstrapMethodEntry> bsms;

    public ClassReaderImpl(byte[] bytes, Function<Utf8Entry, AttributeMapper<?>> custom) {
        this.bytes = bytes;
        this.custom = custom;
        if (bytes.length < 10) {
            throw new IllegalArgumentException(
                    "the file has " + bytes.length + " bytes: it does not even reach the header");
        }
        if (rawInt(0) != ClassFile.MAGIC_NUMBER) {
            throw new IllegalArgumentException("it does not start with 0xCAFEBABE");
        }
        this.poolCount = rawU2(8);
        if (this.poolCount < 1) {
            throw new ConstantPoolException("constant_pool_count = 0");
        }
        this.labels = new int[this.poolCount];
        this.offsets = new int[this.poolCount];
        this.entries = new PoolEntry[this.poolCount];
        this.headerOffset = walkPool();
        // Materialising everything forces each internal reference to be validated now and not
        // later.
        for (int i = 1; i < this.poolCount; i++) {
            if (this.labels[i] != 0) {
                materialize(i);
            }
        }
        require(this.headerOffset + 6 <= bytes.length, "the file is cut short before this_class");
        this.accessFlags = rawU2(this.headerOffset);
        this.thisClass = entryByIndex(rawU2(this.headerOffset + 2), ClassEntry.class);
        int idxSuper = rawU2(this.headerOffset + 4);
        this.superClass = idxSuper == 0 ? null : entryByIndex(idxSuper, ClassEntry.class);
    }

    // --- Step 1: the walk of the pool. It returns the offset of the `access_flags`. ---

    private int walkPool() {
        int p = 10;
        int i = 1;
        while (i < this.poolCount) {
            require(p < this.bytes.length, "the pool runs off the file at index " + i);
            int tag = this.bytes[p] & 0xFF;
            int body = bodyLength(tag, p, i);
            require(p + 1 + body <= this.bytes.length,
                    "entry " + i + " (tag " + tag + ") runs off the file");
            this.labels[i] = tag;
            this.offsets[i] = p + 1;
            p += 1 + body;
            if (tag == PoolEntry.TAG_LONG || tag == PoolEntry.TAG_DOUBLE) {
                // The following slot is unusable (JVMS §4.4.5) and is left with tag 0.
                require(i + 1 < this.poolCount,
                        "a long/double at index " + i + " leaves no room for its second slot");
                i += 2;
            } else {
                i += 1;
            }
        }
        return p;
    }

    private int bodyLength(int tag, int p, int i) {
        switch (tag) {
            case TAG_UTF8:
                require(p + 3 <= this.bytes.length, "a truncated CONSTANT_Utf8 at index " + i);
                return 2 + rawU2(p + 1);
            case TAG_INTEGER:
            case TAG_FLOAT:
                return 4;
            case TAG_LONG:
            case TAG_DOUBLE:
                return 8;
            case TAG_CLASS:
            case TAG_STRING:
            case TAG_METHOD_TYPE:
            case TAG_MODULE:
            case TAG_PACKAGE:
                return 2;
            case TAG_FIELDREF:
            case TAG_METHODREF:
            case TAG_INTERFACE_METHODREF:
            case TAG_NAME_AND_TYPE:
            case TAG_DYNAMIC:
            case TAG_INVOKE_DYNAMIC:
                return 4;
            case TAG_METHOD_HANDLE:
                return 3;
            default:
                throw new ConstantPoolException(
                        "unknown tag " + tag + " at index " + i);
        }
    }

    // --- Step 2: materialising an entry, validating what it references. ---

    private PoolEntry materialize(int i) {
        PoolEntry existing = this.entries[i];
        if (existing != null) {
            return existing;
        }
        int tag = this.labels[i];
        int p = this.offsets[i];
        PoolEntry e;
        switch (tag) {
            case TAG_UTF8:
                e = new Utf8EntryImpl(this, i, decodeUtf8(p + 2, rawU2(p)));
                break;
            case TAG_INTEGER:
                e = new IntegerEntryImpl(this, i, rawInt(p));
                break;
            case TAG_FLOAT:
                e = new FloatEntryImpl(this, i, Float.intBitsToFloat(rawInt(p)));
                break;
            case TAG_LONG:
                e = new LongEntryImpl(this, i, rawLong(p));
                break;
            case TAG_DOUBLE:
                e = new DoubleEntryImpl(this, i, Double.longBitsToDouble(rawLong(p)));
                break;
            case TAG_CLASS:
                e = new ClassEntryImpl(this, i, utf8At(rawU2(p), i));
                break;
            case TAG_STRING:
                e = new StringEntryImpl(this, i, utf8At(rawU2(p), i));
                break;
            case TAG_METHOD_TYPE:
                e = new MethodTypeEntryImpl(this, i, utf8At(rawU2(p), i));
                break;
            case TAG_MODULE:
                e = new ModuleEntryImpl(this, i, utf8At(rawU2(p), i));
                break;
            case TAG_PACKAGE:
                e = new PackageEntryImpl(this, i, utf8At(rawU2(p), i));
                break;
            case TAG_NAME_AND_TYPE:
                e = new NameAndTypeEntryImpl(this, i,
                        utf8At(rawU2(p), i),
                        utf8At(rawU2(p + 2), i));
                break;
            case TAG_FIELDREF:
                e = new FieldRefEntryImpl(this, i,
                        classAt(rawU2(p), i),
                        natAt(rawU2(p + 2), i));
                break;
            case TAG_METHODREF:
                e = new MethodRefEntryImpl(this, i,
                        classAt(rawU2(p), i),
                        natAt(rawU2(p + 2), i));
                break;
            case TAG_INTERFACE_METHODREF:
                e = new InterfaceMethodRefEntryImpl(this, i,
                        classAt(rawU2(p), i),
                        natAt(rawU2(p + 2), i));
                break;
            case TAG_METHOD_HANDLE: {
                int refKind = this.bytes[p] & 0xFF;
                if (refKind < 1 || refKind > 9) {
                    throw new ConstantPoolException(
                            "reference_kind " + refKind + " outside 1..9 at index " + i);
                }
                MemberRefEntry ref = memberAt(rawU2(p + 1), i);
                requireHandleConsistency(refKind, ref, i);
                e = new MethodHandleEntryImpl(this, i, refKind, ref);
                break;
            }
            case TAG_DYNAMIC:
                e = new ConstantDynamicEntryImpl(this, i, rawU2(p),
                        natAt(rawU2(p + 2), i));
                break;
            case TAG_INVOKE_DYNAMIC:
                e = new InvokeDynamicEntryImpl(this, i, rawU2(p),
                        natAt(rawU2(p + 2), i));
                break;
            default:
                throw new ConstantPoolException("tag " + tag + " at index " + i);
        }
        this.entries[i] = e;
        return e;
    }

    // What §4.4.8 demands of the kind/reference combination. It is the only pool rule that is not
    // deduced from the tags, and leaving it out would let impossible handles through.
    private void requireHandleConsistency(int refKind, MemberRefEntry ref, int i) {
        boolean isField = ref.tag() == PoolEntry.TAG_FIELDREF;
        if (refKind <= 4) {
            if (!isField) {
                throw new ConstantPoolException("reference_kind " + refKind
                        + " of index " + i + " requires a CONSTANT_Fieldref");
            }
        } else {
            if (isField) {
                throw new ConstantPoolException("reference_kind " + refKind
                        + " of index " + i + " does not admit a CONSTANT_Fieldref");
            }
            boolean isInit = ref.name().equalsString("<init>");
            if (refKind == 8 && !isInit) {
                throw new ConstantPoolException(
                        "a REF_newInvokeSpecial (index " + i + ") has to point at <init>");
            }
            if (refKind != 8 && isInit) {
                throw new ConstantPoolException("reference_kind " + refKind
                        + " of index " + i + " cannot point at <init>");
            }
        }
    }

    // The *modified* UTF-8 of §4.4.7: `NUL` travels in two bytes and the supplementary characters
    // in six (two surrogates of three bytes each). It is not UTF-8 and cannot be delegated to a
    // standard decoder, which would reject the first and collapse the second.
    private String decodeUtf8(int from, int length) {
        require(from + length <= this.bytes.length, "a CONSTANT_Utf8 runs off the file");
        StringBuilder sb = new StringBuilder(length);
        int p = from;
        int end = from + length;
        while (p < end) {
            int b1 = this.bytes[p] & 0xFF;
            if (b1 < 0x80) {
                if (b1 == 0) {
                    throw new ConstantPoolException("a raw 0x00 inside a CONSTANT_Utf8");
                }
                sb.append((char) b1);
                p += 1;
            } else if ((b1 & 0xE0) == 0xC0) {
                require(p + 1 < end, "a CONSTANT_Utf8 is cut short in the middle of a sequence");
                int b2 = this.bytes[p + 1] & 0xFF;
                require((b2 & 0xC0) == 0x80, "invalid continuation byte in a CONSTANT_Utf8");
                sb.append((char) (((b1 & 0x1F) << 6) | (b2 & 0x3F)));
                p += 2;
            } else if ((b1 & 0xF0) == 0xE0) {
                require(p + 2 < end, "a CONSTANT_Utf8 is cut short in the middle of a sequence");
                int b2 = this.bytes[p + 1] & 0xFF;
                int b3 = this.bytes[p + 2] & 0xFF;
                require((b2 & 0xC0) == 0x80 && (b3 & 0xC0) == 0x80,
                        "invalid continuation byte in a CONSTANT_Utf8");
                sb.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
                p += 3;
            } else {
                throw new ConstantPoolException(
                        "byte 0x" + Integer.toHexString(b1) + " invalid in a CONSTANT_Utf8");
            }
        }
        return sb.toString();
    }

    private void require(boolean cond, String message) {
        if (!cond) {
            throw new ConstantPoolException(message);
        }
    }

    // Non-generic wrappers of `requireType`. They exist because the compiler used to erase the type
    // parameter to its bound when the generic call went straight in as an argument of another call,
    // and then did not find the constructor. The frozen javac compiles the direct form now (checked
    // 2026-09-18), so they are no longer needed; they are harmless.
    private Utf8Entry utf8At(int index, int from) {
        return requireType(index, Utf8Entry.class, from);
    }

    private ClassEntry classAt(int index, int from) {
        return requireType(index, ClassEntry.class, from);
    }

    private NameAndTypeEntry natAt(int index, int from) {
        return requireType(index, NameAndTypeEntry.class, from);
    }

    private MemberRefEntry memberAt(int index, int from) {
        return requireType(index, MemberRefEntry.class, from);
    }

    private <T extends PoolEntry> T requireType(int index, Class<T> cls, int from) {
        if (index < 1 || index >= this.poolCount || this.labels[index] == 0) {
            throw new ConstantPoolException("index " + from + " references index "
                    + index + ", which is not a pool entry");
        }
        PoolEntry e = materialize(index);
        if (!cls.isInstance(e)) {
            throw new ConstantPoolException("index " + from + " references index " + index
                    + ", a " + e.getClass().getSimpleName() + " and not " + cls.getSimpleName());
        }
        return (T) e;
    }

    // --- ConstantPool ---

    public PoolEntry entryByIndex(int index) {
        if (index < 1 || index >= this.poolCount || this.labels[index] == 0) {
            throw new ConstantPoolException("invalid pool index: " + index);
        }
        return materialize(index);
    }

    public int size() {
        return this.poolCount;
    }

    public <T extends PoolEntry> T entryByIndex(int index, Class<T> cls) {
        PoolEntry e = entryByIndex(index);
        if (!cls.isInstance(e)) {
            throw new ConstantPoolException("index " + index + " is "
                    + e.getClass().getSimpleName() + " and not " + cls.getSimpleName());
        }
        return (T) e;
    }

    public BootstrapMethodEntry bootstrapMethodEntry(int index) {
        List<BootstrapMethodEntry> table = this.bsms;
        if (table == null) {
            throw new ConstantPoolException(
                    "the class has no BootstrapMethods attribute, and index " + index
                    + " needs it");
        }
        if (index < 0 || index >= table.size()) {
            throw new ConstantPoolException("BootstrapMethods index out of range: " + index);
        }
        return table.get(index);
    }

    public int bootstrapMethodCount() {
        return this.bsms == null ? 0 : this.bsms.size();
    }

    // `ClassModelImpl` calls it when it finds the attribute, which is the only moment at which it
    // can be known where the table is.
    void bootstrapTable(int bodyOffset) {
        int n = rawU2(bodyOffset);
        List<BootstrapMethodEntry> table = new ArrayList<BootstrapMethodEntry>();
        int p = bodyOffset + 2;
        for (int i = 0; i < n; i++) {
            require(p + 4 <= this.bytes.length, "truncated BootstrapMethods");
            MethodHandleEntry handle = entryByIndex(rawU2(p), MethodHandleEntry.class);
            int nargs = rawU2(p + 2);
            p += 4;
            List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
            for (int j = 0; j < nargs; j++) {
                require(p + 2 <= this.bytes.length, "truncated BootstrapMethods");
                LoadableConstantEntry arg =
                        entryByIndex(rawU2(p), LoadableConstantEntry.class);
                args.add(arg);
                p += 2;
            }
            table.add(new BootstrapMethodEntryImpl(this, i, handle, args));
        }
        this.bsms = table;
    }

    // --- ClassReader ---

    public Function<Utf8Entry, AttributeMapper<?>> customAttributes() {
        return this.custom;
    }

    public int flags() {
        return this.accessFlags;
    }

    public ClassEntry thisClassEntry() {
        return this.thisClass;
    }

    public Optional<ClassEntry> superclassEntry() {
        return Optional.ofNullable(this.superClass);
    }

    public int classfileLength() {
        return this.bytes.length;
    }

    public PoolEntry readEntry(int offset) {
        return entryByIndex(readU2(offset));
    }

    public <T extends PoolEntry> T readEntry(int offset, Class<T> cls) {
        return entryByIndex(readU2(offset), cls);
    }

    public PoolEntry readEntryOrNull(int offset) {
        int i = readU2(offset);
        return i == 0 ? null : entryByIndex(i);
    }

    public <T extends PoolEntry> T readEntryOrNull(int offset, Class<T> cls) {
        int i = readU2(offset);
        return i == 0 ? null : entryByIndex(i, cls);
    }

    public int readU1(int offset) {
        range(offset, 1);
        return this.bytes[offset] & 0xFF;
    }

    public int readU2(int offset) {
        range(offset, 2);
        return rawU2(offset);
    }

    public int readS1(int offset) {
        range(offset, 1);
        return this.bytes[offset];
    }

    public int readS2(int offset) {
        range(offset, 2);
        return (short) rawU2(offset);
    }

    public int readInt(int offset) {
        range(offset, 4);
        return rawInt(offset);
    }

    public long readLong(int offset) {
        range(offset, 8);
        return rawLong(offset);
    }

    public float readFloat(int offset) {
        return Float.intBitsToFloat(readInt(offset));
    }

    public double readDouble(int offset) {
        return Double.longBitsToDouble(readLong(offset));
    }

    public byte[] readBytes(int offset, int len) {
        range(offset, len);
        byte[] r = new byte[len];
        System.arraycopy(this.bytes, offset, r, 0, len);
        return r;
    }

    public void copyBytesTo(BufWriter buf, int offset, int len) {
        range(offset, len);
        buf.writeBytes(this.bytes, offset, len);
    }

    private void range(int offset, int len) {
        if (offset < 0 || len < 0 || offset + len > this.bytes.length) {
            throw new ConstantPoolException(
                    "read outside the file: offset " + offset + ", " + len + " bytes, file of "
                    + this.bytes.length);
        }
    }

    // Unchecked reads, for internal use where the range was already validated.
    int rawU2(int p) {
        return ((this.bytes[p] & 0xFF) << 8) | (this.bytes[p + 1] & 0xFF);
    }

    int rawInt(int p) {
        return ((this.bytes[p] & 0xFF) << 24) | ((this.bytes[p + 1] & 0xFF) << 16)
                | ((this.bytes[p + 2] & 0xFF) << 8) | (this.bytes[p + 3] & 0xFF);
    }

    long rawLong(int p) {
        return ((long) rawInt(p) << 32) | (rawInt(p + 4) & 0xFFFFFFFFL);
    }

    int fileLength() {
        return this.bytes.length;
    }
}
