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
// The `BSM_*` handles below were omitted for a long time, and the note that explained why said
// they needed `DirectMethodHandleDesc.Kind.STATIC` -- a static member of a nested type, which our
// javac could not name from another file (#101). #101 is closed, so they are here. Worth saying
// out loud because it is the second time this session: a comment that explains why something is
// MISSING outlives the reason as easily as any other comment, and nothing recompiles it.
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

    // ---- the bootstrap handles ----
    //
    // A dynamic constant is not a value in the class file: it is a **recipe**. The entry names a
    // bootstrap method and its arguments, and the VM runs it the first time the constant is read.
    // These are the descriptors of the standard recipes, and the reason they all look alike is
    // that the shape is fixed by the JVM: a bootstrap always takes `(Lookup, String, Class)` and
    // then whatever else it was given, which is what `ofConstantBootstrap` prepends.

    /** The recipe for a primitive type's mirror -- `int.class` as a constant. */
    public static final DirectMethodHandleDesc BSM_PRIMITIVE_CLASS =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "primitiveClass", CD_Class);

    /** The recipe for an enum constant, looked up by name in its own type. */
    public static final DirectMethodHandleDesc BSM_ENUM_CONSTANT =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "enumConstant", CD_Enum);

    /** The recipe for reading a {@code static final} field. */
    public static final DirectMethodHandleDesc BSM_GET_STATIC_FINAL =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "getStaticFinal", CD_Object,
                    CD_Class);

    /**
     * The recipe for {@code null}.
     *
     * <p>A constant pool cannot hold a null, which is why this exists at all: the entry names a
     * bootstrap that returns one.
     */
    public static final DirectMethodHandleDesc BSM_NULL_CONSTANT =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "nullConstant", CD_Object);

    /** The recipe for a {@code VarHandle} onto an instance field. */
    public static final DirectMethodHandleDesc BSM_VARHANDLE_FIELD =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "fieldVarHandle",
                    CD_VarHandle, CD_Class, CD_Class);

    /** The recipe for a {@code VarHandle} onto a static field. */
    public static final DirectMethodHandleDesc BSM_VARHANDLE_STATIC_FIELD =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "staticFieldVarHandle",
                    CD_VarHandle, CD_Class, CD_Class);

    /** The recipe for a {@code VarHandle} onto an array's elements. */
    public static final DirectMethodHandleDesc BSM_VARHANDLE_ARRAY =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "arrayVarHandle",
                    CD_VarHandle, CD_Class);

    /**
     * The recipe that invokes a method handle with the remaining static arguments.
     *
     * <p>The general one: any constant that can be computed by calling something is expressible
     * through this, which is why a language that needs a new kind of constant rarely needs a new
     * bootstrap.
     */
    public static final DirectMethodHandleDesc BSM_INVOKE =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "invoke", CD_Object,
                    CD_MethodHandle, CD_Object.arrayType());

    /**
     * The recipe that casts a constant to a type.
     *
     * <p>This is how a {@code short} or a {@code byte} becomes a constant: the class file holds
     * an {@code int}, and the cast is what narrows it. That is why {@code Short.describeConstable}
     * is a dynamic constant while {@code Integer.describeConstable} is not.
     */
    public static final DirectMethodHandleDesc BSM_EXPLICIT_CAST =
            ConstantDescs.ofConstantBootstrap(CD_ConstantBootstraps, "explicitCast", CD_Object,
                    CD_Object);

    /** The recipe for the class data a hidden class was defined with. */
    public static final DirectMethodHandleDesc BSM_CLASS_DATA =
            ConstantDescs.ofConstantBootstrap(CD_MethodHandles, "classData", CD_Object);

    /** The recipe for one element of that class data. */
    public static final DirectMethodHandleDesc BSM_CLASS_DATA_AT =
            ConstantDescs.ofConstantBootstrap(CD_MethodHandles, "classDataAt", CD_Object, CD_int);

    // The three constants built from the recipes above. Declared after them and not before, and
    // that is load-bearing rather than tidy: static initialisers run in TEXTUAL order, so a
    // forward reference here would read a null and store it forever.

    /** The {@code null} constant. */
    public static final ConstantDesc NULL =
            DynamicConstantDesc.ofNamed(ConstantDescs.BSM_NULL_CONSTANT, DEFAULT_NAME, CD_Object);

    /** The constant {@code Boolean.TRUE}. */
    public static final DynamicConstantDesc<Boolean> TRUE =
            DynamicConstantDesc.ofNamed(ConstantDescs.BSM_GET_STATIC_FINAL, "TRUE", CD_Boolean,
                    CD_Boolean);

    /** The constant {@code Boolean.FALSE}. */
    public static final DynamicConstantDesc<Boolean> FALSE =
            DynamicConstantDesc.ofNamed(ConstantDescs.BSM_GET_STATIC_FINAL, "FALSE", CD_Boolean,
                    CD_Boolean);

    // ---- the two factories ----

    /**
     * The descriptor of a <em>constant</em> bootstrap: one that computes a dynamic constant.
     *
     * <p>The three prepended parameters are not a convention this method invented -- the JVM
     * passes them to every bootstrap, so a descriptor that omitted them would name a method the
     * VM could never call.
     *
     * @param owner the class declaring the bootstrap
     * @param name its name
     * @param returnType what it computes
     * @param paramTypes the static argument types, beyond the three the VM always passes
     */
    public static DirectMethodHandleDesc ofConstantBootstrap(ClassDesc owner, String name,
            ClassDesc returnType, ClassDesc... paramTypes) {
        ClassDesc[] full = new ClassDesc[paramTypes.length + 3];
        full[0] = CD_MethodHandles_Lookup;
        full[1] = CD_String;
        full[2] = CD_Class;
        System.arraycopy(paramTypes, 0, full, 3, paramTypes.length);
        return MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, owner, name,
                MethodTypeDesc.of(returnType, full));
    }

    /**
     * The descriptor of a <em>call site</em> bootstrap: one that links an {@code invokedynamic}.
     *
     * <p>Same shape as {@link #ofConstantBootstrap}, and the third prepended parameter is the
     * difference: a constant bootstrap is told the TYPE of the constant it must produce, a call
     * site bootstrap is told the SIGNATURE the call site expects.
     *
     * @param owner the class declaring the bootstrap
     * @param name its name
     * @param returnType what it returns, normally a {@code CallSite}
     * @param paramTypes the static argument types, beyond the three the VM always passes
     */
    public static DirectMethodHandleDesc ofCallsiteBootstrap(ClassDesc owner, String name,
            ClassDesc returnType, ClassDesc... paramTypes) {
        ClassDesc[] full = new ClassDesc[paramTypes.length + 3];
        full[0] = CD_MethodHandles_Lookup;
        full[1] = CD_String;
        full[2] = CD_MethodType;
        System.arraycopy(paramTypes, 0, full, 3, paramTypes.length);
        return MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, owner, name,
                MethodTypeDesc.of(returnType, full));
    }

}
