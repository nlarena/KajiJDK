package java.lang.constant;

// The well-known descriptors, so that callers do not spell `ClassDesc.of("java.lang.String")` a
// thousand times. Nothing here is computed at run time — every field is a name, resolved into a
// nominal descriptor once at class initialisation.
//
// Note that `CD_MethodHandles_Lookup` and friends name types KajiLibrary does not implement. That
// is not a problem and is in fact the point of the whole package: a nominal descriptor NAMES a
// class, it does not load one, so describing `java.lang.invoke.MethodHandles$Lookup` costs
// nothing even where the class is absent.
//
// OMITTED (subset): the `BSM_*` bootstrap handles, `MHD_METHODHANDLE_ASTYPE`, the `NULL`/`TRUE`/
// `FALSE` constants that are built from them, and the two `of*Bootstrap` factories. Every one of
// them needs a `DirectMethodHandleDesc.Kind` VALUE (`Kind.STATIC`), and a static member of a
// nested type cannot be named from outside its declaring file — see the note in
// `MethodHandleDesc.ofConstructor` for the three spellings that were tried. They return with #101.
public final class ConstantDescs {

    private ConstantDescs() {
    }

    public static final String DEFAULT_NAME = "_";
    public static final String INIT_NAME = "<init>";
    public static final String CLASS_INIT_NAME = "<clinit>";

    public static final ClassDesc CD_Object = ClassDesc.of("java.lang.Object");
    public static final ClassDesc CD_String = ClassDesc.of("java.lang.String");
    public static final ClassDesc CD_Class = ClassDesc.of("java.lang.Class");
    public static final ClassDesc CD_Number = ClassDesc.of("java.lang.Number");
    public static final ClassDesc CD_Integer = ClassDesc.of("java.lang.Integer");
    public static final ClassDesc CD_Long = ClassDesc.of("java.lang.Long");
    public static final ClassDesc CD_Float = ClassDesc.of("java.lang.Float");
    public static final ClassDesc CD_Double = ClassDesc.of("java.lang.Double");
    public static final ClassDesc CD_Short = ClassDesc.of("java.lang.Short");
    public static final ClassDesc CD_Byte = ClassDesc.of("java.lang.Byte");
    public static final ClassDesc CD_Character = ClassDesc.of("java.lang.Character");
    public static final ClassDesc CD_Boolean = ClassDesc.of("java.lang.Boolean");
    public static final ClassDesc CD_Void = ClassDesc.of("java.lang.Void");
    public static final ClassDesc CD_Throwable = ClassDesc.of("java.lang.Throwable");
    public static final ClassDesc CD_Exception = ClassDesc.of("java.lang.Exception");
    public static final ClassDesc CD_Enum = ClassDesc.of("java.lang.Enum");

    public static final ClassDesc CD_VarHandle = ClassDesc.of("java.lang.invoke.VarHandle");
    public static final ClassDesc CD_MethodHandles = ClassDesc.of("java.lang.invoke.MethodHandles");
    public static final ClassDesc CD_MethodHandles_Lookup = ClassDesc.of("java.lang.invoke.MethodHandles$Lookup");
    public static final ClassDesc CD_MethodHandle = ClassDesc.of("java.lang.invoke.MethodHandle");
    public static final ClassDesc CD_MethodType = ClassDesc.of("java.lang.invoke.MethodType");
    public static final ClassDesc CD_CallSite = ClassDesc.of("java.lang.invoke.CallSite");

    public static final ClassDesc CD_Collection = ClassDesc.of("java.util.Collection");
    public static final ClassDesc CD_List = ClassDesc.of("java.util.List");
    public static final ClassDesc CD_Set = ClassDesc.of("java.util.Set");
    public static final ClassDesc CD_Map = ClassDesc.of("java.util.Map");

    public static final ClassDesc CD_ConstantDesc = ClassDesc.of("java.lang.constant.ConstantDesc");
    public static final ClassDesc CD_ClassDesc = ClassDesc.of("java.lang.constant.ClassDesc");
    public static final ClassDesc CD_EnumDesc = ClassDesc.of("java.lang.Enum$EnumDesc");
    public static final ClassDesc CD_MethodTypeDesc = ClassDesc.of("java.lang.constant.MethodTypeDesc");
    public static final ClassDesc CD_MethodHandleDesc = ClassDesc.of("java.lang.constant.MethodHandleDesc");
    public static final ClassDesc CD_DirectMethodHandleDesc = ClassDesc.of("java.lang.constant.DirectMethodHandleDesc");
    public static final ClassDesc CD_VarHandleDesc = ClassDesc.of("java.lang.invoke.VarHandle$VarHandleDesc");
    public static final ClassDesc CD_MethodHandleDesc_Kind = ClassDesc.of("java.lang.constant.DirectMethodHandleDesc$Kind");
    public static final ClassDesc CD_DynamicConstantDesc = ClassDesc.of("java.lang.constant.DynamicConstantDesc");
    public static final ClassDesc CD_DynamicCallSiteDesc = ClassDesc.of("java.lang.constant.DynamicCallSiteDesc");
    public static final ClassDesc CD_ConstantBootstraps = ClassDesc.of("java.lang.invoke.ConstantBootstraps");

    // The primitives, which have no binary name at all — only a one-character descriptor.
    public static final ClassDesc CD_int = ClassDesc.ofDescriptor("I");
    public static final ClassDesc CD_long = ClassDesc.ofDescriptor("J");
    public static final ClassDesc CD_float = ClassDesc.ofDescriptor("F");
    public static final ClassDesc CD_double = ClassDesc.ofDescriptor("D");
    public static final ClassDesc CD_short = ClassDesc.ofDescriptor("S");
    public static final ClassDesc CD_byte = ClassDesc.ofDescriptor("B");
    public static final ClassDesc CD_char = ClassDesc.ofDescriptor("C");
    public static final ClassDesc CD_boolean = ClassDesc.ofDescriptor("Z");
    public static final ClassDesc CD_void = ClassDesc.ofDescriptor("V");

    public static final MethodTypeDesc MTD_void = MethodTypeDesc.of(CD_void);
}
