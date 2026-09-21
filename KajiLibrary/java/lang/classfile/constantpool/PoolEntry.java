package java.lang.classfile.constantpool;

// The root of the constant pool's entries (JVMS §4.4). An entry is a tagged tuple living at an index
// of a concrete class's pool: `tag()` says which structure it is, `index()` what position it is at,
// and `constantPool()` which pool it belongs to -- two equal entries from different pools are NOT the
// same entry, and that is why the pool's identity is part of the contract.
//
// `width()` is 2 for `CONSTANT_Long` and `CONSTANT_Double` and 1 for all the rest: it is §4.4.5's
// historical oddity, where those two take two slots of the pool and the following one is declared
// unusable. Whoever walks the pool by index has to add `width()`, not 1.
//
// In KajiJDK this hierarchy is exactly the JDK's, with a single deliberate difference: the interfaces
// are NOT declared `sealed`. The JDK seals them towards `jdk.internal.classfile.impl`; sealing them
// here would force naming the internal implementations from the public package, and a seal that is
// relaxed is a permission too many, not a contract that is broken.
public interface PoolEntry {

    /** `CONSTANT_Utf8_info`'s tag (JVMS §4.4.7). */
    public static final int TAG_UTF8 = 1;
    /** `CONSTANT_Integer_info`'s tag (§4.4.4). */
    public static final int TAG_INTEGER = 3;
    /** `CONSTANT_Float_info`'s tag (§4.4.4). */
    public static final int TAG_FLOAT = 4;
    /** `CONSTANT_Long_info`'s tag (§4.4.5). */
    public static final int TAG_LONG = 5;
    /** `CONSTANT_Double_info`'s tag (§4.4.5). */
    public static final int TAG_DOUBLE = 6;
    /** `CONSTANT_Class_info`'s tag (§4.4.1). */
    public static final int TAG_CLASS = 7;
    /** `CONSTANT_String_info`'s tag (§4.4.3). */
    public static final int TAG_STRING = 8;
    /** `CONSTANT_Fieldref_info`'s tag (§4.4.2). */
    public static final int TAG_FIELDREF = 9;
    /** `CONSTANT_Methodref_info`'s tag (§4.4.2). */
    public static final int TAG_METHODREF = 10;
    /** `CONSTANT_InterfaceMethodref_info`'s tag (§4.4.2). */
    public static final int TAG_INTERFACE_METHODREF = 11;
    /** `CONSTANT_NameAndType_info`'s tag (§4.4.6). */
    public static final int TAG_NAME_AND_TYPE = 12;
    /** `CONSTANT_MethodHandle_info`'s tag (§4.4.8). */
    public static final int TAG_METHOD_HANDLE = 15;
    /** `CONSTANT_MethodType_info`'s tag (§4.4.9). */
    public static final int TAG_METHOD_TYPE = 16;
    /** `CONSTANT_Dynamic_info`'s tag (§4.4.10). */
    public static final int TAG_DYNAMIC = 17;
    /** `CONSTANT_InvokeDynamic_info`'s tag (§4.4.10). */
    public static final int TAG_INVOKE_DYNAMIC = 18;
    /** `CONSTANT_Module_info`'s tag (§4.4.11). */
    public static final int TAG_MODULE = 19;
    /** `CONSTANT_Package_info`'s tag (§4.4.12). */
    public static final int TAG_PACKAGE = 20;

    /** The pool this entry belongs to. */
    ConstantPool constantPool();

    /** The structure's `tag`, one of the `TAG_*`. */
    int tag();

    /** This entry's index within its pool; always &ge; 1. */
    int index();

    /** How many pool slots it takes: 2 for `long` and `double`, 1 for the rest. */
    int width();
}
