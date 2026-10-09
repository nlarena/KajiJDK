package jdk.internal.classfile.impl;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantDynamicEntry;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.lang.classfile.constantpool.ConstantPoolException;
import java.lang.classfile.constantpool.DoubleEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.FloatEntry;
import java.lang.classfile.constantpool.IntegerEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.InvokeDynamicEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.LongEntry;
import java.lang.classfile.constantpool.MemberRefEntry;
import java.lang.classfile.constantpool.MethodHandleEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.classfile.constantpool.MethodTypeEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.StringEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The writing pool. It keeps the entries in a list indexed like the file --slot 0 does not exist,
// and a `long` or a `double` takes two-- and a deduplication map by textual key, which is what
// makes asking twice for the same `Utf8` return the same entry and the same index.
//
// Two decisions worth naming:
//
//   1. `of(ClassModel)` COPIES the model's entries instead of sharing them, and puts them at the
//      same indices. Sharing them would be cheaper, but then `entry.constantPool()` of an entry of
//      this pool would return the reader's pool, which is not this one: an entry that lies about
//      which pool it belongs to breaks any code that uses that answer to decide whether it can
//      write the index as it stands. 2. Every entry that comes from outside is ADOPTED: if its
//      `constantPool()` is not this pool, an equivalent one is rebuilt here. Accepting it as it is
//      would keep an index of the foreign pool.
//
// The validation is the same as the reader's and for the same reason: a pool that accepts a
// `reference_kind` outside 1..9, or a field handle pointing at a method, produces a `.class` the
// JVM rejects on loading it, and the error shows up very far from where it was made.
public final class ConstantPoolBuilderImpl implements ConstantPoolBuilder {

    // Local copies of the tags of PoolEntry: the frozen javac's bytecode generator does not fold a
    // constant of another compilation unit into a `case` label (finding #461). The values are those
    // of JVMS §4.4.
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

    // The highest index a `u2` of the format can name.
    private static final int MAX_INDEX = 65535;

    // Indexed by pool index. Position 0 and the second slot of a long/double are left null.
    private final List<PoolEntry> entries = new ArrayList<PoolEntry>();
    private final Map<String, PoolEntry> byKey = new HashMap<String, PoolEntry>();
    private final List<BootstrapMethodEntry> bsms = new ArrayList<BootstrapMethodEntry>();
    private final Map<String, BootstrapMethodEntry> bsmByKey =
            new HashMap<String, BootstrapMethodEntry>();

    public ConstantPoolBuilderImpl(ClassModel model) {
        this.entries.add(null);
        if (model != null) {
            importPool(model.constantPool());
        }
    }

    // --- ConstantPool ---

    public int size() {
        return this.entries.size();
    }

    public PoolEntry entryByIndex(int index) {
        if (index < 1 || index >= this.entries.size()) {
            throw new ConstantPoolException("index " + index + " is not in the pool");
        }
        PoolEntry e = this.entries.get(index);
        if (e == null) {
            throw new ConstantPoolException("index " + index + " is not a pool entry");
        }
        return e;
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
        if (index < 0 || index >= this.bsms.size()) {
            throw new ConstantPoolException(
                    "index " + index + " is not in the BootstrapMethods table");
        }
        return this.bsms.get(index);
    }

    public int bootstrapMethodCount() {
        return this.bsms.size();
    }

    // --- ConstantPoolBuilder: the primitive forms ---

    // See the scope note in `ConstantPoolBuilder`: only this very pool.
    public boolean canWriteDirect(ConstantPool constantPool) {
        return constantPool == this;
    }

    public Utf8Entry utf8Entry(String s) {
        if (s == null) {
            throw new NullPointerException("utf8Entry(null)");
        }
        String key = "u:" + s;
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (Utf8Entry) existing;
        }
        Utf8EntryImpl e = new Utf8EntryImpl(this, nextIndex(), s);
        return (Utf8Entry) addEntry(key, e, 1);
    }

    public ClassEntry classEntry(Utf8Entry ne) {
        Utf8Entry n = adopt(ne);
        String key = "c:" + n.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (ClassEntry) existing;
        }
        ClassEntryImpl e = new ClassEntryImpl(this, nextIndex(), n);
        return (ClassEntry) addEntry(key, e, 1);
    }

    public PackageEntry packageEntry(Utf8Entry nameEntry) {
        Utf8Entry n = adopt(nameEntry);
        String key = "p:" + n.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (PackageEntry) existing;
        }
        PackageEntryImpl e = new PackageEntryImpl(this, nextIndex(), n);
        return (PackageEntry) addEntry(key, e, 1);
    }

    public ModuleEntry moduleEntry(Utf8Entry moduleName) {
        Utf8Entry n = adopt(moduleName);
        String key = "m:" + n.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (ModuleEntry) existing;
        }
        ModuleEntryImpl e = new ModuleEntryImpl(this, nextIndex(), n);
        return (ModuleEntry) addEntry(key, e, 1);
    }

    public NameAndTypeEntry nameAndTypeEntry(Utf8Entry nameEntry, Utf8Entry typeEntry) {
        Utf8Entry n = adopt(nameEntry);
        Utf8Entry t = adopt(typeEntry);
        String key = "n:" + n.index() + ":" + t.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (NameAndTypeEntry) existing;
        }
        NameAndTypeEntryImpl e = new NameAndTypeEntryImpl(this, nextIndex(), n, t);
        return (NameAndTypeEntry) addEntry(key, e, 1);
    }

    public FieldRefEntry fieldRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        ClassEntry o = adopt(owner);
        NameAndTypeEntry nt = adopt(nameAndType);
        String key = "F:" + o.index() + ":" + nt.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (FieldRefEntry) existing;
        }
        FieldRefEntryImpl e = new FieldRefEntryImpl(this, nextIndex(), o, nt);
        return (FieldRefEntry) addEntry(key, e, 1);
    }

    public MethodRefEntry methodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        ClassEntry o = adopt(owner);
        NameAndTypeEntry nt = adopt(nameAndType);
        String key = "M:" + o.index() + ":" + nt.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (MethodRefEntry) existing;
        }
        MethodRefEntryImpl e = new MethodRefEntryImpl(this, nextIndex(), o, nt);
        return (MethodRefEntry) addEntry(key, e, 1);
    }

    public InterfaceMethodRefEntry interfaceMethodRefEntry(ClassEntry owner,
            NameAndTypeEntry nameAndType) {
        ClassEntry o = adopt(owner);
        NameAndTypeEntry nt = adopt(nameAndType);
        String key = "I:" + o.index() + ":" + nt.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (InterfaceMethodRefEntry) existing;
        }
        InterfaceMethodRefEntryImpl e =
                new InterfaceMethodRefEntryImpl(this, nextIndex(), o, nt);
        return (InterfaceMethodRefEntry) addEntry(key, e, 1);
    }

    public MethodTypeEntry methodTypeEntry(MethodTypeDesc descriptor) {
        return methodTypeEntry(utf8Entry(descriptor.descriptorString()));
    }

    public MethodTypeEntry methodTypeEntry(Utf8Entry descriptor) {
        Utf8Entry d = adopt(descriptor);
        String key = "t:" + d.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (MethodTypeEntry) existing;
        }
        MethodTypeEntryImpl e = new MethodTypeEntryImpl(this, nextIndex(), d);
        return (MethodTypeEntry) addEntry(key, e, 1);
    }

    public MethodHandleEntry methodHandleEntry(int refKind, MemberRefEntry reference) {
        if (refKind < 1 || refKind > 9) {
            throw new IllegalArgumentException("reference_kind " + refKind + " outside 1..9");
        }
        MemberRefEntry r = adopt(reference);
        requireHandleConsistency(refKind, r);
        String key = "h:" + refKind + ":" + r.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (MethodHandleEntry) existing;
        }
        MethodHandleEntryImpl e = new MethodHandleEntryImpl(this, nextIndex(), refKind, r);
        return (MethodHandleEntry) addEntry(key, e, 1);
    }

    public InvokeDynamicEntry invokeDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType) {
        int bsm = adopt(bootstrapMethodEntry).bsmIndex();
        NameAndTypeEntry nt = adopt(nameAndType);
        String key = "y:" + bsm + ":" + nt.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (InvokeDynamicEntry) existing;
        }
        InvokeDynamicEntryImpl e = new InvokeDynamicEntryImpl(this, nextIndex(), bsm, nt);
        return (InvokeDynamicEntry) addEntry(key, e, 1);
    }

    public ConstantDynamicEntry constantDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry,
            NameAndTypeEntry nameAndType) {
        int bsm = adopt(bootstrapMethodEntry).bsmIndex();
        NameAndTypeEntry nt = adopt(nameAndType);
        String key = "D:" + bsm + ":" + nt.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (ConstantDynamicEntry) existing;
        }
        ConstantDynamicEntryImpl e = new ConstantDynamicEntryImpl(this, nextIndex(), bsm, nt);
        return (ConstantDynamicEntry) addEntry(key, e, 1);
    }

    public IntegerEntry intEntry(int value) {
        String key = "i:" + value;
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (IntegerEntry) existing;
        }
        IntegerEntryImpl e = new IntegerEntryImpl(this, nextIndex(), value);
        return (IntegerEntry) addEntry(key, e, 1);
    }

    // The key goes by bits and not by value: `0.0f` and `-0.0f` are different pool entries, and two
    // `NaN`s with the same representation are the same one. `==` on float would say the opposite in
    // both.
    public FloatEntry floatEntry(float value) {
        String key = "f:" + Float.floatToRawIntBits(value);
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (FloatEntry) existing;
        }
        FloatEntryImpl e = new FloatEntryImpl(this, nextIndex(), value);
        return (FloatEntry) addEntry(key, e, 1);
    }

    public LongEntry longEntry(long value) {
        String key = "l:" + value;
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (LongEntry) existing;
        }
        LongEntryImpl e = new LongEntryImpl(this, nextIndex(), value);
        return (LongEntry) addEntry(key, e, 2);
    }

    public DoubleEntry doubleEntry(double value) {
        String key = "d:" + Double.doubleToRawLongBits(value);
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (DoubleEntry) existing;
        }
        DoubleEntryImpl e = new DoubleEntryImpl(this, nextIndex(), value);
        return (DoubleEntry) addEntry(key, e, 2);
    }

    public StringEntry stringEntry(Utf8Entry utf8) {
        Utf8Entry u = adopt(utf8);
        String key = "s:" + u.index();
        PoolEntry existing = this.byKey.get(key);
        if (existing != null) {
            return (StringEntry) existing;
        }
        StringEntryImpl e = new StringEntryImpl(this, nextIndex(), u);
        return (StringEntry) addEntry(key, e, 1);
    }

    public BootstrapMethodEntry bsmEntry(MethodHandleEntry methodReference,
            List<LoadableConstantEntry> arguments) {
        MethodHandleEntry h = adopt(methodReference);
        List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
        StringBuilder key = new StringBuilder("B:").append(h.index());
        for (int i = 0; i < arguments.size(); i++) {
            LoadableConstantEntry a = adoptLoadable(arguments.get(i));
            args.add(a);
            key.append(':').append(a.index());
        }
        String k = key.toString();
        BootstrapMethodEntry existing = this.bsmByKey.get(k);
        if (existing != null) {
            return existing;
        }
        BootstrapMethodEntryImpl e =
                new BootstrapMethodEntryImpl(this, this.bsms.size(), h, args);
        this.bsms.add(e);
        this.bsmByKey.put(k, e);
        return e;
    }

    // --- Internal ---

    private int nextIndex() {
        int i = this.entries.size();
        if (i > MAX_INDEX) {
            throw new ConstantPoolException(
                    "the pool went past " + MAX_INDEX + " entries and no longer fits in a u2");
        }
        return i;
    }

    private PoolEntry addEntry(String key, PoolEntry e, int width) {
        this.entries.add(e);
        if (width == 2) {
            // The second slot of a long/double is unusable (JVMS §4.4.5).
            this.entries.add(null);
        }
        this.byKey.put(key, e);
        return e;
    }

    // §4.4.8: kinds 1..4 name a field; 5..9, a method, and only 9 can be of an interface.
    private static void requireHandleConsistency(int refKind, MemberRefEntry ref) {
        boolean isField = ref.tag() == TAG_FIELDREF;
        boolean isInterface = ref.tag() == TAG_INTERFACE_METHODREF;
        if (refKind <= 4) {
            if (!isField) {
                throw new IllegalArgumentException(
                        "reference_kind " + refKind + " requires a CONSTANT_Fieldref");
            }
            return;
        }
        if (isField) {
            throw new IllegalArgumentException(
                    "reference_kind " + refKind + " does not admit a CONSTANT_Fieldref");
        }
        if (refKind == 9 && !isInterface) {
            throw new IllegalArgumentException(
                    "reference_kind 9 requires a CONSTANT_InterfaceMethodref");
        }
        if (refKind != 9 && isInterface && refKind != 6 && refKind != 7) {
            throw new IllegalArgumentException("reference_kind " + refKind
                    + " does not admit a CONSTANT_InterfaceMethodref");
        }
    }

    // --- Adoption: an entry of another pool is rebuilt here ---

    /**
     * That entry, brought into **this** pool if it came from another.
     *
     * <p>It is what makes transforming a class possible: the elements of the original model carry
     * entries of the original pool, and their index means nothing in the new pool. Without adopting
     * them, the `.class` that comes out has indices pointing at anything -- and the worst of it is
     * that **the file comes out well formed**, so it does not fail on writing but much later, on
     * reading it.
     *
     * <p>If the entry is already of this pool it is returned as it is: adopting is idempotent and
     * cheap.
     */
    public PoolEntry adoptEntry(PoolEntry e) {
        if (e == null || e.constantPool() == this) {
            return e;
        }
        int tag = e.tag();
        if (tag == TAG_UTF8) {
            return utf8Entry(((Utf8Entry) e).stringValue());
        }
        if (tag == TAG_INTEGER) {
            return intEntry(((IntegerEntry) e).intValue());
        }
        if (tag == TAG_FLOAT) {
            return floatEntry(((FloatEntry) e).floatValue());
        }
        if (tag == TAG_LONG) {
            return longEntry(((LongEntry) e).longValue());
        }
        if (tag == TAG_DOUBLE) {
            return doubleEntry(((DoubleEntry) e).doubleValue());
        }
        if (tag == TAG_CLASS) {
            return adopt((ClassEntry) e);
        }
        if (tag == TAG_STRING) {
            return stringEntry(utf8Entry(((StringEntry) e).stringValue()));
        }
        if (tag == TAG_NAME_AND_TYPE) {
            return adopt((NameAndTypeEntry) e);
        }
        if (tag == TAG_FIELDREF || tag == TAG_METHODREF || tag == TAG_INTERFACE_METHODREF) {
            return adopt((MemberRefEntry) e);
        }
        if (tag == TAG_METHOD_HANDLE) {
            MethodHandleEntry mh = (MethodHandleEntry) e;
            return methodHandleEntry(mh.kind(), adopt(mh.reference()));
        }
        if (tag == TAG_METHOD_TYPE) {
            return methodTypeEntry(utf8Entry(((MethodTypeEntry) e).descriptor().stringValue()));
        }
        if (tag == TAG_MODULE) {
            return moduleEntry(utf8Entry(((ModuleEntry) e).name().stringValue()));
        }
        if (tag == TAG_PACKAGE) {
            return packageEntry(utf8Entry(((PackageEntry) e).name().stringValue()));
        }
        if (tag == TAG_DYNAMIC || tag == TAG_INVOKE_DYNAMIC) {
            DynamicConstantPoolEntry d = (DynamicConstantPoolEntry) e;
            BootstrapMethodEntry b = d.bootstrap();
            MethodHandleEntry mh = (MethodHandleEntry) adoptEntry(b.bootstrapMethod());
            List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
            List<LoadableConstantEntry> src = b.arguments();
            for (int i = 0; i < src.size(); i++) {
                args.add((LoadableConstantEntry) adoptEntry(src.get(i)));
            }
            BootstrapMethodEntry nb = bsmEntry(mh, args);
            NameAndTypeEntry nt = adopt(d.nameAndType());
            if (tag == TAG_DYNAMIC) {
                return constantDynamicEntry(nb, nt);
            }
            return invokeDynamicEntry(nb, nt);
        }
        throw new IllegalArgumentException("cannot adopt an entry with tag " + tag);
    }

    private Utf8Entry adopt(Utf8Entry e) {
        return e.constantPool() == this ? e : utf8Entry(e.stringValue());
    }

    private ClassEntry adopt(ClassEntry e) {
        return e.constantPool() == this ? e : classEntry(utf8Entry(e.asInternalName()));
    }

    private NameAndTypeEntry adopt(NameAndTypeEntry e) {
        return e.constantPool() == this
                ? e
                : nameAndTypeEntry(utf8Entry(e.name().stringValue()),
                        utf8Entry(e.type().stringValue()));
    }

    private MemberRefEntry adopt(MemberRefEntry e) {
        if (e.constantPool() == this) {
            return e;
        }
        ClassEntry o = adopt(e.owner());
        NameAndTypeEntry nt = adopt(e.nameAndType());
        int tag = e.tag();
        if (tag == TAG_FIELDREF) {
            return fieldRefEntry(o, nt);
        }
        if (tag == TAG_INTERFACE_METHODREF) {
            return interfaceMethodRefEntry(o, nt);
        }
        return methodRefEntry(o, nt);
    }

    private MethodHandleEntry adopt(MethodHandleEntry e) {
        return e.constantPool() == this ? e : methodHandleEntry(e.kind(), e.reference());
    }

    private BootstrapMethodEntry adopt(BootstrapMethodEntry e) {
        return e.constantPool() == this ? e : bsmEntry(e.bootstrapMethod(), e.arguments());
    }

    private LoadableConstantEntry adoptLoadable(LoadableConstantEntry e) {
        if (e.constantPool() == this) {
            return e;
        }
        int tag = e.tag();
        switch (tag) {
            case TAG_UTF8:
                // A `Utf8` is not loadable with `ldc`; if it gets here the argument was wrong.
                throw new IllegalArgumentException("a CONSTANT_Utf8 is not a loadable argument");
            case TAG_INTEGER:
                return intEntry(((IntegerEntry) e).intValue());
            case TAG_FLOAT:
                return floatEntry(((FloatEntry) e).floatValue());
            case TAG_LONG:
                return longEntry(((LongEntry) e).longValue());
            case TAG_DOUBLE:
                return doubleEntry(((DoubleEntry) e).doubleValue());
            case TAG_CLASS:
                return classEntry(utf8Entry(((ClassEntry) e).asInternalName()));
            case TAG_STRING:
                return stringEntry(utf8Entry(((StringEntry) e).stringValue()));
            case TAG_METHOD_TYPE:
                return methodTypeEntry(
                        utf8Entry(((MethodTypeEntry) e).descriptor().stringValue()));
            case TAG_METHOD_HANDLE:
                return adopt((MethodHandleEntry) e);
            case TAG_DYNAMIC: {
                ConstantDynamicEntry d = (ConstantDynamicEntry) e;
                BootstrapMethodEntry b = adopt(d.bootstrap());
                return constantDynamicEntry(b, adopt(d.nameAndType()));
            }
            default:
                throw new IllegalArgumentException(
                        "tag " + tag + " is not a loadable constant");
        }
    }

    // --- Import of a complete pool, keeping the indices ---

    private void importPool(ConstantPool source) {
        int n = source.size();
        while (this.entries.size() < n) {
            this.entries.add(null);
        }
        for (int i = 1; i < n; i++) {
            copyEntry(source, i);
        }
        for (int i = 0; i < source.bootstrapMethodCount(); i++) {
            copyBsm(source.bootstrapMethodEntry(i));
        }
    }

    // Recursive on purpose: a `CONSTANT_Class` at index 3 may point at the `Utf8` at 40, which has
    // not been copied yet. On coming back, the copy of 40 is already in its place and with its
    // original index.
    private PoolEntry copyEntry(ConstantPool source, int i) {
        PoolEntry existing = this.entries.get(i);
        if (existing != null) {
            return existing;
        }
        PoolEntry o;
        try {
            o = source.entryByIndex(i);
        } catch (ConstantPoolException notAnEntry) {
            // The dead slot that follows a long/double: it is left null, just as in the file.
            return null;
        }
        PoolEntry created = buildCopy(source, o, i);
        this.entries.set(i, created);
        this.byKey.put(keyOf(created), created);
        return created;
    }

    private PoolEntry buildCopy(ConstantPool source, PoolEntry o, int i) {
        switch (o.tag()) {
            case TAG_UTF8:
                return new Utf8EntryImpl(this, i, ((Utf8Entry) o).stringValue());
            case TAG_INTEGER:
                return new IntegerEntryImpl(this, i, ((IntegerEntry) o).intValue());
            case TAG_FLOAT:
                return new FloatEntryImpl(this, i, ((FloatEntry) o).floatValue());
            case TAG_LONG:
                return new LongEntryImpl(this, i, ((LongEntry) o).longValue());
            case TAG_DOUBLE:
                return new DoubleEntryImpl(this, i, ((DoubleEntry) o).doubleValue());
            case TAG_CLASS:
                return new ClassEntryImpl(this, i, copiedUtf8(source, ((ClassEntry) o).name()));
            case TAG_STRING:
                return new StringEntryImpl(this, i, copiedUtf8(source, ((StringEntry) o).utf8()));
            case TAG_METHOD_TYPE:
                return new MethodTypeEntryImpl(this, i,
                        copiedUtf8(source, ((MethodTypeEntry) o).descriptor()));
            case TAG_MODULE:
                return new ModuleEntryImpl(this, i, copiedUtf8(source, ((ModuleEntry) o).name()));
            case TAG_PACKAGE:
                return new PackageEntryImpl(this, i,
                        copiedUtf8(source, ((PackageEntry) o).name()));
            case TAG_NAME_AND_TYPE: {
                NameAndTypeEntry nt = (NameAndTypeEntry) o;
                return new NameAndTypeEntryImpl(this, i, copiedUtf8(source, nt.name()),
                        copiedUtf8(source, nt.type()));
            }
            case TAG_FIELDREF: {
                MemberRefEntry m = (MemberRefEntry) o;
                return new FieldRefEntryImpl(this, i, copiedClass(source, m.owner()),
                        copiedNat(source, m.nameAndType()));
            }
            case TAG_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) o;
                return new MethodRefEntryImpl(this, i, copiedClass(source, m.owner()),
                        copiedNat(source, m.nameAndType()));
            }
            case TAG_INTERFACE_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) o;
                return new InterfaceMethodRefEntryImpl(this, i, copiedClass(source, m.owner()),
                        copiedNat(source, m.nameAndType()));
            }
            case TAG_METHOD_HANDLE: {
                MethodHandleEntry h = (MethodHandleEntry) o;
                PoolEntry r = copyEntry(source, h.reference().index());
                return new MethodHandleEntryImpl(this, i, h.kind(), (MemberRefEntry) r);
            }
            case TAG_DYNAMIC: {
                ConstantDynamicEntry d = (ConstantDynamicEntry) o;
                return new ConstantDynamicEntryImpl(this, i, d.bootstrapMethodIndex(),
                        copiedNat(source, d.nameAndType()));
            }
            case TAG_INVOKE_DYNAMIC: {
                InvokeDynamicEntry d = (InvokeDynamicEntry) o;
                return new InvokeDynamicEntryImpl(this, i, d.bootstrapMethodIndex(),
                        copiedNat(source, d.nameAndType()));
            }
            default:
                throw new ConstantPoolException(
                        "tag " + o.tag() + " unknown at index " + i);
        }
    }

    private Utf8Entry copiedUtf8(ConstantPool source, Utf8Entry o) {
        PoolEntry e = copyEntry(source, o.index());
        return (Utf8Entry) e;
    }

    private ClassEntry copiedClass(ConstantPool source, ClassEntry o) {
        PoolEntry e = copyEntry(source, o.index());
        return (ClassEntry) e;
    }

    private NameAndTypeEntry copiedNat(ConstantPool source, NameAndTypeEntry o) {
        PoolEntry e = copyEntry(source, o.index());
        return (NameAndTypeEntry) e;
    }

    private void copyBsm(BootstrapMethodEntry o) {
        MethodHandleEntry h = (MethodHandleEntry) this.entries.get(o.bootstrapMethod().index());
        List<LoadableConstantEntry> args = new ArrayList<LoadableConstantEntry>();
        List<LoadableConstantEntry> orig = o.arguments();
        StringBuilder key = new StringBuilder("B:").append(h.index());
        for (int i = 0; i < orig.size(); i++) {
            LoadableConstantEntry a =
                    (LoadableConstantEntry) this.entries.get(orig.get(i).index());
            args.add(a);
            key.append(':').append(a.index());
        }
        BootstrapMethodEntryImpl e =
                new BootstrapMethodEntryImpl(this, this.bsms.size(), h, args);
        this.bsms.add(e);
        this.bsmByKey.put(key.toString(), e);
    }

    // The same key the `xxxEntry` methods build, so that an imported entry is reused instead of
    // duplicated when somebody asks for it again by value.
    private static String keyOf(PoolEntry e) {
        switch (e.tag()) {
            case TAG_UTF8:
                return "u:" + ((Utf8Entry) e).stringValue();
            case TAG_INTEGER:
                return "i:" + ((IntegerEntry) e).intValue();
            case TAG_FLOAT:
                return "f:" + Float.floatToRawIntBits(((FloatEntry) e).floatValue());
            case TAG_LONG:
                return "l:" + ((LongEntry) e).longValue();
            case TAG_DOUBLE:
                return "d:" + Double.doubleToRawLongBits(((DoubleEntry) e).doubleValue());
            case TAG_CLASS:
                return "c:" + ((ClassEntry) e).name().index();
            case TAG_STRING:
                return "s:" + ((StringEntry) e).utf8().index();
            case TAG_METHOD_TYPE:
                return "t:" + ((MethodTypeEntry) e).descriptor().index();
            case TAG_MODULE:
                return "m:" + ((ModuleEntry) e).name().index();
            case TAG_PACKAGE:
                return "p:" + ((PackageEntry) e).name().index();
            case TAG_NAME_AND_TYPE: {
                NameAndTypeEntry nt = (NameAndTypeEntry) e;
                return "n:" + nt.name().index() + ":" + nt.type().index();
            }
            case TAG_FIELDREF: {
                MemberRefEntry m = (MemberRefEntry) e;
                return "F:" + m.owner().index() + ":" + m.nameAndType().index();
            }
            case TAG_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) e;
                return "M:" + m.owner().index() + ":" + m.nameAndType().index();
            }
            case TAG_INTERFACE_METHODREF: {
                MemberRefEntry m = (MemberRefEntry) e;
                return "I:" + m.owner().index() + ":" + m.nameAndType().index();
            }
            case TAG_METHOD_HANDLE: {
                MethodHandleEntry h = (MethodHandleEntry) e;
                return "h:" + h.kind() + ":" + h.reference().index();
            }
            case TAG_DYNAMIC: {
                ConstantDynamicEntry d = (ConstantDynamicEntry) e;
                return "D:" + d.bootstrapMethodIndex() + ":" + d.nameAndType().index();
            }
            default: {
                InvokeDynamicEntry d = (InvokeDynamicEntry) e;
                return "y:" + d.bootstrapMethodIndex() + ":" + d.nameAndType().index();
            }
        }
    }

    public String toString() {
        return "ConstantPoolBuilder[" + (this.entries.size() - 1) + " slots, "
                + this.bsms.size() + " bsm]";
    }
}
