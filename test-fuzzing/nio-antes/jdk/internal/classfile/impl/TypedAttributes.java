package jdk.internal.classfile.impl;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.Label;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.AnnotationDefaultAttribute;
import java.lang.classfile.attribute.CharacterRangeInfo;
import java.lang.classfile.attribute.CharacterRangeTableAttribute;
import java.lang.classfile.attribute.CompilationIDAttribute;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.EnclosingMethodAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.LineNumberInfo;
import java.lang.classfile.attribute.LineNumberTableAttribute;
import java.lang.classfile.attribute.LocalVariableInfo;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.classfile.attribute.LocalVariableTypeInfo;
import java.lang.classfile.attribute.LocalVariableTypeTableAttribute;
import java.lang.classfile.attribute.MethodParameterInfo;
import java.lang.classfile.attribute.MethodParametersAttribute;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleAttribute.ModuleAttributeBuilder;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleHashInfo;
import java.lang.classfile.attribute.ModuleHashesAttribute;
import java.lang.classfile.attribute.ModuleMainClassAttribute;
import java.lang.classfile.attribute.ModuleOpenInfo;
import java.lang.classfile.attribute.ModulePackagesAttribute;
import java.lang.classfile.attribute.ModuleProvideInfo;
import java.lang.classfile.attribute.ModuleRequireInfo;
import java.lang.classfile.attribute.ModuleResolutionAttribute;
import java.lang.classfile.attribute.ModuleTargetAttribute;
import java.lang.classfile.attribute.NestHostAttribute;
import java.lang.classfile.attribute.NestMembersAttribute;
import java.lang.classfile.attribute.PermittedSubclassesAttribute;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.attribute.SourceDebugExtensionAttribute;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.classfile.attribute.SourceIDAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.ObjectVerificationTypeInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.UninitializedVerificationTypeInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.VerificationTypeInfo;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ConstantValueEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.constant.ModuleDesc;
import java.lang.constant.PackageDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The implementations of `java.lang.classfile.attribute` and the factories that build them.
 *
 * <p>All the interfaces of that package are pure declarations: their `of(...)` delegate here. It is
 * split in two for a concrete reason and not for taste -- an attribute the user **builds** and one
 * that comes out of **reading** a `.class` are not the same thing:
 *
 * <ul>
 * <li>The built one has its components already in hand. There is no file, there are no offsets,
 *     there is no destination pool: it is a value object and nothing more. That is what is
 *     here.</li>
 * <li>The read one comes out of {@link Mappers} by its name, and what it returns today is a
 *     {@link RawAttribute} -- the name, the pool entry and the body in bytes. See the scope note of
 *     {@link java.lang.classfile.Attributes}.</li>
 * </ul>
 *
 * <p><strong>The lists are copied and frozen on the way in.</strong> It is not defence for
 * defence's sake: an attribute describes something that already happened --the exceptions this
 * method declares, the nested classes this class has-- and if the list passed stayed alive,
 * changing it afterwards would change what the attribute says without anybody having rebuilt it.
 * `Collections.unmodifiableList` over a copy is the only thing that makes `exceptions()` always
 * return the same.
 *
 * <p><strong>The `Optional`s are kept as the value or `null`</strong> and wrapped on the way out.
 * An `Optional` field is one more object per attribute and per component, and here there are tables
 * with thousands of entries (`LineNumberTable` of a big method). The contract outwards is
 * identical.
 */
public final class TypedAttributes {

    private TypedAttributes() {
    }

    // The names, once per attribute and not one per built object. The pool deduplicates anyway, but
    // with the constant it is not even consulted.
    static final Utf8Entry N_ANNOTATION_DEFAULT = TemporaryConstantPool.utf8("AnnotationDefault");
    static final Utf8Entry N_CHARACTER_RANGE_TABLE = TemporaryConstantPool.utf8("CharacterRangeTable");
    static final Utf8Entry N_COMPILATION_ID = TemporaryConstantPool.utf8("CompilationID");
    static final Utf8Entry N_CONSTANT_VALUE = TemporaryConstantPool.utf8("ConstantValue");
    static final Utf8Entry N_DEPRECATED = TemporaryConstantPool.utf8("Deprecated");
    static final Utf8Entry N_ENCLOSING_METHOD = TemporaryConstantPool.utf8("EnclosingMethod");
    static final Utf8Entry N_EXCEPTIONS = TemporaryConstantPool.utf8("Exceptions");
    static final Utf8Entry N_INNER_CLASSES = TemporaryConstantPool.utf8("InnerClasses");
    static final Utf8Entry N_LINE_NUMBER_TABLE = TemporaryConstantPool.utf8("LineNumberTable");
    static final Utf8Entry N_LOCAL_VARIABLE_TABLE = TemporaryConstantPool.utf8("LocalVariableTable");
    static final Utf8Entry N_LOCAL_VARIABLE_TYPE_TABLE = TemporaryConstantPool.utf8("LocalVariableTypeTable");
    static final Utf8Entry N_METHOD_PARAMETERS = TemporaryConstantPool.utf8("MethodParameters");
    static final Utf8Entry N_MODULE = TemporaryConstantPool.utf8("Module");
    static final Utf8Entry N_MODULE_HASHES = TemporaryConstantPool.utf8("ModuleHashes");
    static final Utf8Entry N_MODULE_MAIN_CLASS = TemporaryConstantPool.utf8("ModuleMainClass");
    static final Utf8Entry N_MODULE_PACKAGES = TemporaryConstantPool.utf8("ModulePackages");
    static final Utf8Entry N_MODULE_RESOLUTION = TemporaryConstantPool.utf8("ModuleResolution");
    static final Utf8Entry N_MODULE_TARGET = TemporaryConstantPool.utf8("ModuleTarget");
    static final Utf8Entry N_NEST_HOST = TemporaryConstantPool.utf8("NestHost");
    static final Utf8Entry N_NEST_MEMBERS = TemporaryConstantPool.utf8("NestMembers");
    static final Utf8Entry N_PERMITTED_SUBCLASSES = TemporaryConstantPool.utf8("PermittedSubclasses");
    static final Utf8Entry N_RECORD = TemporaryConstantPool.utf8("Record");
    static final Utf8Entry N_RUNTIME_INVISIBLE_ANNOTATIONS = TemporaryConstantPool.utf8("RuntimeInvisibleAnnotations");
    static final Utf8Entry N_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = TemporaryConstantPool.utf8("RuntimeInvisibleParameterAnnotations");
    static final Utf8Entry N_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = TemporaryConstantPool.utf8("RuntimeInvisibleTypeAnnotations");
    static final Utf8Entry N_RUNTIME_VISIBLE_ANNOTATIONS = TemporaryConstantPool.utf8("RuntimeVisibleAnnotations");
    static final Utf8Entry N_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = TemporaryConstantPool.utf8("RuntimeVisibleParameterAnnotations");
    static final Utf8Entry N_RUNTIME_VISIBLE_TYPE_ANNOTATIONS = TemporaryConstantPool.utf8("RuntimeVisibleTypeAnnotations");
    static final Utf8Entry N_SIGNATURE = TemporaryConstantPool.utf8("Signature");
    static final Utf8Entry N_SOURCE_DEBUG_EXTENSION = TemporaryConstantPool.utf8("SourceDebugExtension");
    static final Utf8Entry N_SOURCE_FILE = TemporaryConstantPool.utf8("SourceFile");
    static final Utf8Entry N_SOURCE_ID = TemporaryConstantPool.utf8("SourceID");
    static final Utf8Entry N_STACK_MAP_TABLE = TemporaryConstantPool.utf8("StackMapTable");
    static final Utf8Entry N_SYNTHETIC = TemporaryConstantPool.utf8("Synthetic");


    // ---- conversions the API factories ask for by name ------------------------------------------
    //
    // These exist because an interface of `java.lang.classfile.attribute` cannot call
    // `TemporaryConstantPool` (it is internal and the `of` is in the public package), so it goes
    // through here. They are one-liners on purpose: if one grew, the logic would be in the wrong
    // place.

    /** A loose `CONSTANT_Utf8`. */
    public static Utf8Entry utf8(String s) {
        return TemporaryConstantPool.utf8(s);
    }

    /**
     * Like {@link #utf8}, but `null` passes as `null` instead of breaking.
     *
     * <p>`ModuleRequireInfo` asks for it: the version of a `requires` is optional in the format,
     * and the caller has it as a `String` that may be missing. Without this, each caller would
     * repeat the same `if`.
     */
    public static Utf8Entry utf8OrNull(String s) {
        return s == null ? null : TemporaryConstantPool.utf8(s);
    }

    /** A loose `CONSTANT_Class`. */
    public static ClassEntry classEntry(ClassDesc d) {
        return TemporaryConstantPool.classEntry(d);
    }

    /** A loose `CONSTANT_Package`. */
    public static PackageEntry packageEntry(PackageDesc d) {
        return TemporaryConstantPool.pool().packageEntry(d);
    }

    /** A loose `CONSTANT_Module`. */
    public static ModuleEntry moduleEntry(ModuleDesc d) {
        return TemporaryConstantPool.pool().moduleEntry(d);
    }

    /** The constant-value entry of `c` (`Integer`, `Long`, `Float`, `Double` or `String`). */
    public static ConstantValueEntry constantValueEntry(ConstantDesc c) {
        return TemporaryConstantPool.pool().constantValueEntry(c);
    }

    /** The `CONSTANT_Class` entries of those descriptors. */
    public static List<ClassEntry> classEntries(List<ClassDesc> descs) {
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < descs.size(); i++) {
            out.add(TemporaryConstantPool.classEntry(descs.get(i)));
        }
        return out;
    }

    /** The `CONSTANT_Class` entries of those descriptors. */
    public static List<ClassEntry> classEntries(ClassDesc[] descs) {
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < descs.length; i++) {
            out.add(TemporaryConstantPool.classEntry(descs[i]));
        }
        return out;
    }

    /** The `CONSTANT_Package` entries of those descriptors. */
    public static List<PackageEntry> packageEntries(List<PackageDesc> descs) {
        List<PackageEntry> out = new ArrayList<PackageEntry>();
        for (int i = 0; i < descs.size(); i++) {
            out.add(TemporaryConstantPool.pool().packageEntry(descs.get(i)));
        }
        return out;
    }

    /** The `CONSTANT_Package` entries of those descriptors. */
    public static List<PackageEntry> packageEntries(PackageDesc[] descs) {
        List<PackageEntry> out = new ArrayList<PackageEntry>();
        for (int i = 0; i < descs.length; i++) {
            out.add(TemporaryConstantPool.pool().packageEntry(descs[i]));
        }
        return out;
    }

    /** The `CONSTANT_Module` entries of those descriptors. */
    public static List<ModuleEntry> moduleEntries(List<ModuleDesc> descs) {
        List<ModuleEntry> out = new ArrayList<ModuleEntry>();
        for (int i = 0; i < descs.size(); i++) {
            out.add(TemporaryConstantPool.pool().moduleEntry(descs.get(i)));
        }
        return out;
    }

    /** The `CONSTANT_Module` entries of those descriptors. */
    public static List<ModuleEntry> moduleEntries(ModuleDesc[] descs) {
        List<ModuleEntry> out = new ArrayList<ModuleEntry>();
        for (int i = 0; i < descs.length; i++) {
            out.add(TemporaryConstantPool.pool().moduleEntry(descs[i]));
        }
        return out;
    }

    /**
     * A varargs array as a list.
     *
     * <p>There are also `listOfClasses`, `listOfModules`, `listOfAttributes`, `listOfAnnotations`
     * and `listOfTypeAnnotations`, which do exactly this for a fixed type. They are not redundancy:
     * our javac does not always infer `T` when the result goes straight in as an argument of
     * another generic call, and with the concrete name there is nothing to infer.
     */
    public static <T> List<T> listOf(T[] items) {
        List<T> out = new ArrayList<T>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** See {@link #listOf}. */
    public static List<ClassEntry> listOfClasses(ClassEntry[] items) {
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** See {@link #listOf}. */
    public static List<ModuleEntry> listOfModules(ModuleEntry[] items) {
        List<ModuleEntry> out = new ArrayList<ModuleEntry>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** See {@link #listOf}. */
    public static List<Attribute<?>> listOfAttributes(Attribute<?>[] items) {
        List<Attribute<?>> out = new ArrayList<Attribute<?>>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** See {@link #listOf}. */
    public static List<Annotation> listOfAnnotations(Annotation[] items) {
        List<Annotation> out = new ArrayList<Annotation>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** See {@link #listOf}. */
    public static List<TypeAnnotation> listOfTypeAnnotations(TypeAnnotation[] items) {
        List<TypeAnnotation> out = new ArrayList<TypeAnnotation>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** The bit mask of those flags. */
    public static int mask(AccessFlag[] flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return m;
    }

    /** The bit mask of those flags. */
    public static int mask(Collection<AccessFlag> flags) {
        int m = 0;
        for (AccessFlag f : flags) {
            m = m | f.mask();
        }
        return m;
    }

    // Frozen copy: see the class note on why the live list is not kept.
    private static <T> List<T> frozen(List<T> src) {
        return Collections.unmodifiableList(new ArrayList<T>(src));
    }

    private static <T> List<T> frozen(Collection<T> src) {
        return Collections.unmodifiableList(new ArrayList<T>(src));
    }

    private static byte[] copy(byte[] src) {
        byte[] out = new byte[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    // ---- factories -----------------------------------------------------------------------------

    /** The `AnnotationDefault` attribute with that value. */
    public static AnnotationDefaultAttribute annotationDefault(AnnotationValue v) {
        return new AnnotationDefaultImpl(v);
    }

    /** A range of the `CharacterRangeTable`. */
    public static CharacterRangeInfo characterRangeInfo(int startPc, int endPc, int rangeStart,
            int rangeEnd, int flags) {
        return new CharacterRangeInfoImpl(startPc, endPc, rangeStart, rangeEnd, flags);
    }

    /** The `CharacterRangeTable` attribute with those ranges. */
    public static CharacterRangeTableAttribute characterRangeTable(List<CharacterRangeInfo> r) {
        return new CharacterRangeTableImpl(frozen(r));
    }

    /** The `CompilationID` attribute. */
    public static CompilationIDAttribute compilationId(Utf8Entry id) {
        return new CompilationIDImpl(id);
    }

    /** The `ConstantValue` attribute. */
    public static ConstantValueAttribute constantValue(ConstantValueEntry v) {
        return new ConstantValueImpl(v);
    }

    /**
     * The `Deprecated` attribute.
     *
     * <p>It has no body: existing **is** all it says. That is why there is a single instance and
     * not a new one per call -- two `Deprecated`s differ in nothing.
     */
    public static DeprecatedAttribute deprecated() {
        return DeprecatedImpl.INSTANCE;
    }

    /** The `Synthetic` attribute. Without body, like `Deprecated`. */
    public static SyntheticAttribute synthetic() {
        return SyntheticImpl.INSTANCE;
    }

    /** The `EnclosingMethod` attribute. */
    public static EnclosingMethodAttribute enclosingMethod(ClassEntry owner,
            Optional<NameAndTypeEntry> method) {
        return new EnclosingMethodImpl(owner, method.isPresent() ? method.get() : null);
    }

    /**
     * The `EnclosingMethod` attribute, naming the class and the method by their descriptors.
     *
     * <p>The name and the type go together or neither goes: the format keeps **one** index to a
     * `NameAndType`, not two loose fields. Asking for only one would describe a `.class` that does
     * not exist, and that is why it is `IllegalArgumentException` and not a kind interpretation.
     */
    public static EnclosingMethodAttribute enclosingMethod(ClassDesc owner,
            Optional<String> methodName, Optional<MethodTypeDesc> methodType) {
        if (methodName.isPresent() != methodType.isPresent()) {
            throw new IllegalArgumentException(
                    "EnclosingMethod carries the name and the type together, or neither");
        }
        NameAndTypeEntry nat = null;
        if (methodName.isPresent()) {
            nat = TemporaryConstantPool.nameAndType(
                    TemporaryConstantPool.utf8(methodName.get()),
                    TemporaryConstantPool.utf8(methodType.get().descriptorString()));
        }
        return new EnclosingMethodImpl(TemporaryConstantPool.classEntry(owner), nat);
    }

    /** The `Exceptions` attribute. */
    public static ExceptionsAttribute exceptions(List<ClassEntry> exceptions) {
        return new ExceptionsImpl(frozen(exceptions));
    }

    /** An entry of the `InnerClasses`. */
    public static InnerClassInfo innerClassInfo(ClassEntry inner, Optional<ClassEntry> outer,
            Optional<Utf8Entry> innerName, int flags) {
        return new InnerClassInfoImpl(inner, outer.isPresent() ? outer.get() : null,
                innerName.isPresent() ? innerName.get() : null, flags);
    }

    /** An entry of the `InnerClasses`, by descriptors. */
    public static InnerClassInfo innerClassInfo(ClassDesc inner, Optional<ClassDesc> outer,
            Optional<String> innerName, int flags) {
        return new InnerClassInfoImpl(TemporaryConstantPool.classEntry(inner),
                outer.isPresent() ? TemporaryConstantPool.classEntry(outer.get()) : null,
                innerName.isPresent() ? TemporaryConstantPool.utf8(innerName.get()) : null,
                flags);
    }

    /** The `InnerClasses` attribute. */
    public static InnerClassesAttribute innerClasses(List<InnerClassInfo> classes) {
        return new InnerClassesImpl(frozen(classes));
    }

    /** An entry of the `LineNumberTable`. */
    public static LineNumberInfo lineNumberInfo(int startPc, int lineNumber) {
        return new LineNumberInfoImpl(startPc, lineNumber);
    }

    /** The `LineNumberTable` attribute. */
    public static LineNumberTableAttribute lineNumberTable(List<LineNumberInfo> lines) {
        return new LineNumberTableImpl(frozen(lines));
    }

    /** The `LocalVariableTable` attribute. */
    public static LocalVariableTableAttribute localVariableTable(List<LocalVariableInfo> vars) {
        return new LocalVariableTableImpl(frozen(vars));
    }

    /** The `LocalVariableTypeTable` attribute. */
    public static LocalVariableTypeTableAttribute localVariableTypeTable(
            List<LocalVariableTypeInfo> vars) {
        return new LocalVariableTypeTableImpl(frozen(vars));
    }

    /** An entry of the `MethodParameters`. */
    public static MethodParameterInfo methodParameterInfo(Optional<Utf8Entry> name, int flags) {
        return new MethodParameterInfoImpl(name.isPresent() ? name.get() : null, flags);
    }

    /** An entry of the `MethodParameters`, with the name as text. */
    public static MethodParameterInfo methodParameterInfoOfNames(Optional<String> name, int flags) {
        return new MethodParameterInfoImpl(
                name.isPresent() ? TemporaryConstantPool.utf8(name.get()) : null, flags);
    }

    /** The `MethodParameters` attribute. */
    public static MethodParametersAttribute methodParameters(List<MethodParameterInfo> ps) {
        return new MethodParametersImpl(frozen(ps));
    }

    /** The `Module` attribute. */
    public static ModuleAttribute module(ModuleEntry name, int flags, Utf8Entry version,
            Collection<ModuleRequireInfo> requires, Collection<ModuleExportInfo> exports,
            Collection<ModuleOpenInfo> opens, Collection<ClassEntry> uses,
            Collection<ModuleProvideInfo> provides) {
        return new ModuleImpl(name, flags, version, frozen(requires), frozen(exports),
                frozen(opens), frozen(uses), frozen(provides));
    }

    /**
     * The `Module` attribute put together by a step-by-step builder.
     *
     * <p>The `handler` receives a mutable builder and keeps adding directives to it; what is
     * returned is an attribute already frozen. After this call the builder is not used again, so
     * whatever the `handler` kept cannot change the attribute.
     */
    public static ModuleAttribute buildModule(ModuleEntry name,
            Consumer<ModuleAttributeBuilder> handler) {
        ModuleBuilderImpl b = new ModuleBuilderImpl(name);
        handler.accept(b);
        return b.build();
    }

    /** An `exports` directive. */
    public static ModuleExportInfo moduleExportInfo(PackageEntry pkg, int flags,
            List<ModuleEntry> to) {
        return new ModuleExportInfoImpl(pkg, flags, frozen(to));
    }

    /** An `opens` directive. */
    public static ModuleOpenInfo moduleOpenInfo(PackageEntry pkg, int flags,
            List<ModuleEntry> to) {
        return new ModuleOpenInfoImpl(pkg, flags, frozen(to));
    }

    /** A `provides` directive. */
    public static ModuleProvideInfo moduleProvideInfo(ClassEntry service,
            List<ClassEntry> impls) {
        return new ModuleProvideInfoImpl(service, frozen(impls));
    }

    /** A `requires` directive. */
    public static ModuleRequireInfo moduleRequireInfo(ModuleEntry module, int flags,
            Utf8Entry version) {
        return new ModuleRequireInfoImpl(module, flags, version);
    }

    /** An entry of the `ModuleHashes`. */
    public static ModuleHashInfo moduleHashInfo(ModuleEntry module, byte[] hash) {
        return new ModuleHashInfoImpl(module, copy(hash));
    }

    /** The `ModuleHashes` attribute. */
    public static ModuleHashesAttribute moduleHashes(Utf8Entry algorithm,
            List<ModuleHashInfo> hashes) {
        return new ModuleHashesImpl(algorithm, frozen(hashes));
    }

    /** The `ModuleMainClass` attribute. */
    public static ModuleMainClassAttribute moduleMainClass(ClassEntry mainClass) {
        return new ModuleMainClassImpl(mainClass);
    }

    /** The `ModulePackages` attribute. */
    public static ModulePackagesAttribute modulePackages(List<PackageEntry> packages) {
        return new ModulePackagesImpl(frozen(packages));
    }

    /** The `ModuleResolution` attribute. */
    public static ModuleResolutionAttribute moduleResolution(int flags) {
        return new ModuleResolutionImpl(flags);
    }

    /** The `ModuleTarget` attribute. */
    public static ModuleTargetAttribute moduleTarget(Utf8Entry platform) {
        return new ModuleTargetImpl(platform);
    }

    /** The `NestHost` attribute. */
    public static NestHostAttribute nestHost(ClassEntry host) {
        return new NestHostImpl(host);
    }

    /** The `NestMembers` attribute. */
    public static NestMembersAttribute nestMembers(List<ClassEntry> members) {
        return new NestMembersImpl(frozen(members));
    }

    /** The `PermittedSubclasses` attribute. */
    public static PermittedSubclassesAttribute permittedSubclasses(List<ClassEntry> subs) {
        return new PermittedSubclassesImpl(frozen(subs));
    }

    /** The `Record` attribute. */
    public static RecordAttribute record(List<RecordComponentInfo> components) {
        return new RecordImpl(frozen(components));
    }

    /** A component of a `record`. */
    public static RecordComponentInfo recordComponentInfo(Utf8Entry name, Utf8Entry descriptor,
            List<Attribute<?>> attributes) {
        return new RecordComponentInfoImpl(name, descriptor, frozen(attributes));
    }

    /** The `RuntimeVisibleAnnotations` attribute. */
    public static RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotations(
            List<Annotation> annotations) {
        return new RuntimeVisibleAnnotationsImpl(frozen(annotations));
    }

    /** The `RuntimeInvisibleAnnotations` attribute. */
    public static RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotations(
            List<Annotation> annotations) {
        return new RuntimeInvisibleAnnotationsImpl(frozen(annotations));
    }

    /** The `RuntimeVisibleParameterAnnotations` attribute. */
    public static RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotations(
            List<List<Annotation>> byParameter) {
        return new RuntimeVisibleParameterAnnotationsImpl(frozenNested(byParameter));
    }

    /** The `RuntimeInvisibleParameterAnnotations` attribute. */
    public static RuntimeInvisibleParameterAnnotationsAttribute
            runtimeInvisibleParameterAnnotations(List<List<Annotation>> byParameter) {
        return new RuntimeInvisibleParameterAnnotationsImpl(frozenNested(byParameter));
    }

    /** The `RuntimeVisibleTypeAnnotations` attribute. */
    public static RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotations(
            List<TypeAnnotation> annotations) {
        return new RuntimeVisibleTypeAnnotationsImpl(frozen(annotations));
    }

    /** The `RuntimeInvisibleTypeAnnotations` attribute. */
    public static RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotations(
            List<TypeAnnotation> annotations) {
        return new RuntimeInvisibleTypeAnnotationsImpl(frozen(annotations));
    }

    // The list of lists of the per-parameter annotations: both layers are frozen. Freezing only the
    // outer one would leave each parameter's mutable, which is exactly the one somebody is going to
    // have at hand after building it.
    private static List<List<Annotation>> frozenNested(List<List<Annotation>> src) {
        List<List<Annotation>> out = new ArrayList<List<Annotation>>();
        for (int i = 0; i < src.size(); i++) {
            out.add(frozen(src.get(i)));
        }
        return Collections.unmodifiableList(out);
    }

    /** The `Signature` attribute. */
    public static SignatureAttribute signature(Utf8Entry signature) {
        return new SignatureImpl(signature);
    }

    /** The `SourceDebugExtension` attribute. */
    public static SourceDebugExtensionAttribute sourceDebugExtension(byte[] contents) {
        return new SourceDebugExtensionImpl(copy(contents));
    }

    /** The `SourceFile` attribute. */
    public static SourceFileAttribute sourceFile(Utf8Entry sourceFile) {
        return new SourceFileImpl(sourceFile);
    }

    /** The `SourceID` attribute. */
    public static SourceIDAttribute sourceId(Utf8Entry sourceId) {
        return new SourceIDImpl(sourceId);
    }

    /** A frame of the `StackMapTable`. */
    public static StackMapFrameInfo stackMapFrame(Label target, List<VerificationTypeInfo> locals,
            List<VerificationTypeInfo> stack) {
        return new StackMapFrameImpl(target, frozen(locals), frozen(stack));
    }

    /** The `StackMapTable` attribute. */
    public static StackMapTableAttribute stackMapTable(List<StackMapFrameInfo> entries) {
        return new StackMapTableImpl(frozen(entries));
    }

    /** The verification type of a reference to that class. */
    public static ObjectVerificationTypeInfo objectVerificationType(ClassEntry className) {
        return new ObjectVerificationTypeImpl(className);
    }

    /** The verification type of the object created by the `new` at that label. */
    public static UninitializedVerificationTypeInfo uninitializedVerificationType(Label target) {
        return new UninitializedVerificationTypeImpl(target);
    }

    // ---- implementations -----------------------------------------------------------------------
    //
    // Each one keeps its components and answers. They all share the same shape, so the ones that
    // follow carry no comment of their own: what has to be known is above. What is commented is
    // what departs from the shape.

    private static final class AnnotationDefaultImpl implements AnnotationDefaultAttribute {

        private final AnnotationValue value;

        AnnotationDefaultImpl(AnnotationValue value) {
            this.value = value;
        }

        public AnnotationValue defaultValue() {
            return this.value;
        }

        public Utf8Entry attributeName() {
            return N_ANNOTATION_DEFAULT;
        }

        public AttributeMapper<AnnotationDefaultAttribute> attributeMapper() {
            return AttributeMappers.ANNOTATION_DEFAULT;
        }

        public String toString() {
            return "AnnotationDefault[" + this.value + "]";
        }
    }

    private static final class CharacterRangeInfoImpl implements CharacterRangeInfo {

        private final int startPc;
        private final int endPc;
        private final int rangeStart;
        private final int rangeEnd;
        private final int flags;

        CharacterRangeInfoImpl(int startPc, int endPc, int rangeStart, int rangeEnd, int flags) {
            this.startPc = startPc;
            this.endPc = endPc;
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
            this.flags = flags;
        }

        public int startPc() {
            return this.startPc;
        }

        public int endPc() {
            return this.endPc;
        }

        public int characterRangeStart() {
            return this.rangeStart;
        }

        public int characterRangeEnd() {
            return this.rangeEnd;
        }

        public int flags() {
            return this.flags;
        }
    }

    private static final class CharacterRangeTableImpl implements CharacterRangeTableAttribute {

        private final List<CharacterRangeInfo> ranges;

        CharacterRangeTableImpl(List<CharacterRangeInfo> ranges) {
            this.ranges = ranges;
        }

        public List<CharacterRangeInfo> characterRangeTable() {
            return this.ranges;
        }

        public Utf8Entry attributeName() {
            return N_CHARACTER_RANGE_TABLE;
        }

        public AttributeMapper<CharacterRangeTableAttribute> attributeMapper() {
            return AttributeMappers.CHARACTER_RANGE_TABLE;
        }
    }

    private static final class CompilationIDImpl implements CompilationIDAttribute {

        private final Utf8Entry id;

        CompilationIDImpl(Utf8Entry id) {
            this.id = id;
        }

        public Utf8Entry compilationId() {
            return this.id;
        }

        public Utf8Entry attributeName() {
            return N_COMPILATION_ID;
        }

        public AttributeMapper<CompilationIDAttribute> attributeMapper() {
            return AttributeMappers.COMPILATION_ID;
        }
    }

    private static final class ConstantValueImpl implements ConstantValueAttribute {

        private final ConstantValueEntry value;

        ConstantValueImpl(ConstantValueEntry value) {
            this.value = value;
        }

        public ConstantValueEntry constant() {
            return this.value;
        }

        public Utf8Entry attributeName() {
            return N_CONSTANT_VALUE;
        }

        public AttributeMapper<ConstantValueAttribute> attributeMapper() {
            return AttributeMappers.CONSTANT_VALUE;
        }

        public String toString() {
            return "ConstantValue[" + this.value + "]";
        }
    }

    private static final class DeprecatedImpl implements DeprecatedAttribute {

        static final DeprecatedAttribute INSTANCE = new DeprecatedImpl();

        private DeprecatedImpl() {
        }

        public Utf8Entry attributeName() {
            return N_DEPRECATED;
        }

        public AttributeMapper<DeprecatedAttribute> attributeMapper() {
            return AttributeMappers.DEPRECATED;
        }

        public String toString() {
            return "Deprecated[]";
        }
    }

    private static final class SyntheticImpl implements SyntheticAttribute {

        static final SyntheticAttribute INSTANCE = new SyntheticImpl();

        private SyntheticImpl() {
        }

        public Utf8Entry attributeName() {
            return N_SYNTHETIC;
        }

        public AttributeMapper<SyntheticAttribute> attributeMapper() {
            return AttributeMappers.SYNTHETIC;
        }

        public String toString() {
            return "Synthetic[]";
        }
    }

    private static final class EnclosingMethodImpl implements EnclosingMethodAttribute {

        private final ClassEntry owner;
        private final NameAndTypeEntry method;

        EnclosingMethodImpl(ClassEntry owner, NameAndTypeEntry method) {
            this.owner = owner;
            this.method = method;
        }

        public ClassEntry enclosingClass() {
            return this.owner;
        }

        public Optional<NameAndTypeEntry> enclosingMethod() {
            return Optional.ofNullable(this.method);
        }

        public Utf8Entry attributeName() {
            return N_ENCLOSING_METHOD;
        }

        public AttributeMapper<EnclosingMethodAttribute> attributeMapper() {
            return AttributeMappers.ENCLOSING_METHOD;
        }
    }

    private static final class ExceptionsImpl implements ExceptionsAttribute {

        private final List<ClassEntry> exceptions;

        ExceptionsImpl(List<ClassEntry> exceptions) {
            this.exceptions = exceptions;
        }

        public List<ClassEntry> exceptions() {
            return this.exceptions;
        }

        public Utf8Entry attributeName() {
            return N_EXCEPTIONS;
        }

        public AttributeMapper<ExceptionsAttribute> attributeMapper() {
            return AttributeMappers.EXCEPTIONS;
        }

        public String toString() {
            return "Exceptions" + this.exceptions;
        }
    }

    private static final class InnerClassInfoImpl implements InnerClassInfo {

        private final ClassEntry inner;
        private final ClassEntry outer;
        private final Utf8Entry innerName;
        private final int flags;

        InnerClassInfoImpl(ClassEntry inner, ClassEntry outer, Utf8Entry innerName, int flags) {
            this.inner = inner;
            this.outer = outer;
            this.innerName = innerName;
            this.flags = flags;
        }

        public ClassEntry innerClass() {
            return this.inner;
        }

        public Optional<ClassEntry> outerClass() {
            return Optional.ofNullable(this.outer);
        }

        public Optional<Utf8Entry> innerName() {
            return Optional.ofNullable(this.innerName);
        }

        public int flagsMask() {
            return this.flags;
        }
    }

    private static final class InnerClassesImpl implements InnerClassesAttribute {

        private final List<InnerClassInfo> classes;

        InnerClassesImpl(List<InnerClassInfo> classes) {
            this.classes = classes;
        }

        public List<InnerClassInfo> classes() {
            return this.classes;
        }

        public Utf8Entry attributeName() {
            return N_INNER_CLASSES;
        }

        public AttributeMapper<InnerClassesAttribute> attributeMapper() {
            return AttributeMappers.INNER_CLASSES;
        }
    }

    private static final class LineNumberInfoImpl implements LineNumberInfo {

        private final int startPc;
        private final int lineNumber;

        LineNumberInfoImpl(int startPc, int lineNumber) {
            this.startPc = startPc;
            this.lineNumber = lineNumber;
        }

        public int startPc() {
            return this.startPc;
        }

        public int lineNumber() {
            return this.lineNumber;
        }

        public String toString() {
            return "LineNumber[" + this.startPc + " -> " + this.lineNumber + "]";
        }
    }

    private static final class LineNumberTableImpl implements LineNumberTableAttribute {

        private final List<LineNumberInfo> lines;

        LineNumberTableImpl(List<LineNumberInfo> lines) {
            this.lines = lines;
        }

        public List<LineNumberInfo> lineNumbers() {
            return this.lines;
        }

        public Utf8Entry attributeName() {
            return N_LINE_NUMBER_TABLE;
        }

        public AttributeMapper<LineNumberTableAttribute> attributeMapper() {
            return AttributeMappers.LINE_NUMBER_TABLE;
        }
    }

    private static final class LocalVariableTableImpl implements LocalVariableTableAttribute {

        private final List<LocalVariableInfo> vars;

        LocalVariableTableImpl(List<LocalVariableInfo> vars) {
            this.vars = vars;
        }

        public List<LocalVariableInfo> localVariables() {
            return this.vars;
        }

        public Utf8Entry attributeName() {
            return N_LOCAL_VARIABLE_TABLE;
        }

        public AttributeMapper<LocalVariableTableAttribute> attributeMapper() {
            return AttributeMappers.LOCAL_VARIABLE_TABLE;
        }
    }

    private static final class LocalVariableTypeTableImpl
            implements LocalVariableTypeTableAttribute {

        private final List<LocalVariableTypeInfo> vars;

        LocalVariableTypeTableImpl(List<LocalVariableTypeInfo> vars) {
            this.vars = vars;
        }

        public List<LocalVariableTypeInfo> localVariableTypes() {
            return this.vars;
        }

        public Utf8Entry attributeName() {
            return N_LOCAL_VARIABLE_TYPE_TABLE;
        }

        public AttributeMapper<LocalVariableTypeTableAttribute> attributeMapper() {
            return AttributeMappers.LOCAL_VARIABLE_TYPE_TABLE;
        }
    }

    private static final class MethodParameterInfoImpl implements MethodParameterInfo {

        private final Utf8Entry name;
        private final int flags;

        MethodParameterInfoImpl(Utf8Entry name, int flags) {
            this.name = name;
            this.flags = flags;
        }

        public Optional<Utf8Entry> name() {
            return Optional.ofNullable(this.name);
        }

        public int flagsMask() {
            return this.flags;
        }
    }

    private static final class MethodParametersImpl implements MethodParametersAttribute {

        private final List<MethodParameterInfo> parameters;

        MethodParametersImpl(List<MethodParameterInfo> parameters) {
            this.parameters = parameters;
        }

        public List<MethodParameterInfo> parameters() {
            return this.parameters;
        }

        public Utf8Entry attributeName() {
            return N_METHOD_PARAMETERS;
        }

        public AttributeMapper<MethodParametersAttribute> attributeMapper() {
            return AttributeMappers.METHOD_PARAMETERS;
        }
    }

    private static final class ModuleImpl implements ModuleAttribute {

        private final ModuleEntry name;
        private final int flags;
        private final Utf8Entry version;
        private final List<ModuleRequireInfo> requires;
        private final List<ModuleExportInfo> exports;
        private final List<ModuleOpenInfo> opens;
        private final List<ClassEntry> uses;
        private final List<ModuleProvideInfo> provides;

        ModuleImpl(ModuleEntry name, int flags, Utf8Entry version,
                List<ModuleRequireInfo> requires, List<ModuleExportInfo> exports,
                List<ModuleOpenInfo> opens, List<ClassEntry> uses,
                List<ModuleProvideInfo> provides) {
            this.name = name;
            this.flags = flags;
            this.version = version;
            this.requires = requires;
            this.exports = exports;
            this.opens = opens;
            this.uses = uses;
            this.provides = provides;
        }

        public ModuleEntry moduleName() {
            return this.name;
        }

        public int moduleFlagsMask() {
            return this.flags;
        }

        public Optional<Utf8Entry> moduleVersion() {
            return Optional.ofNullable(this.version);
        }

        public List<ModuleRequireInfo> requires() {
            return this.requires;
        }

        public List<ModuleExportInfo> exports() {
            return this.exports;
        }

        public List<ModuleOpenInfo> opens() {
            return this.opens;
        }

        public List<ClassEntry> uses() {
            return this.uses;
        }

        public List<ModuleProvideInfo> provides() {
            return this.provides;
        }

        public Utf8Entry attributeName() {
            return N_MODULE;
        }

        public AttributeMapper<ModuleAttribute> attributeMapper() {
            return AttributeMappers.MODULE;
        }

        public String toString() {
            return "Module[" + this.name.name().stringValue() + "]";
        }
    }

    // The step-by-step builder of `Module`. It is the only mutable thing in this file, and it lives
    // as long as the call to `buildModule`: it accumulates and is thrown away.
    //
    // `moduleName` can be set again because the API allows it (`ModuleAttributeBuilder` declares
    // the method), not because calling it twice makes sense.
    private static final class ModuleBuilderImpl implements ModuleAttributeBuilder {

        private ModuleEntry name;
        private int flags;
        private Utf8Entry version;
        private final List<ModuleRequireInfo> requires = new ArrayList<ModuleRequireInfo>();
        private final List<ModuleExportInfo> exports = new ArrayList<ModuleExportInfo>();
        private final List<ModuleOpenInfo> opens = new ArrayList<ModuleOpenInfo>();
        private final List<ClassEntry> uses = new ArrayList<ClassEntry>();
        private final List<ModuleProvideInfo> provides = new ArrayList<ModuleProvideInfo>();

        ModuleBuilderImpl(ModuleEntry name) {
            this.name = name;
        }

        public ModuleAttributeBuilder moduleName(ModuleDesc moduleName) {
            this.name = TemporaryConstantPool.pool().moduleEntry(moduleName);
            return this;
        }

        public ModuleAttributeBuilder moduleFlags(int flagsMask) {
            this.flags = flagsMask;
            return this;
        }

        public ModuleAttributeBuilder moduleVersion(String version) {
            this.version = version == null ? null : TemporaryConstantPool.utf8(version);
            return this;
        }

        public ModuleAttributeBuilder requires(ModuleDesc module, int requiresFlagsMask,
                String version) {
            return this.requires(TypedAttributes.moduleRequireInfo(
                    TemporaryConstantPool.pool().moduleEntry(module), requiresFlagsMask,
                    TypedAttributes.utf8OrNull(version)));
        }

        public ModuleAttributeBuilder requires(ModuleRequireInfo requires) {
            this.requires.add(requires);
            return this;
        }

        public ModuleAttributeBuilder exports(PackageDesc pkge, int flagsMask,
                ModuleDesc[] exportsToModules) {
            return this.exports(TypedAttributes.moduleExportInfo(
                    TemporaryConstantPool.pool().packageEntry(pkge), flagsMask,
                    TypedAttributes.moduleEntries(exportsToModules)));
        }

        public ModuleAttributeBuilder exports(ModuleExportInfo exports) {
            this.exports.add(exports);
            return this;
        }

        public ModuleAttributeBuilder opens(PackageDesc pkge, int flagsMask,
                ModuleDesc[] opensToModules) {
            return this.opens(TypedAttributes.moduleOpenInfo(
                    TemporaryConstantPool.pool().packageEntry(pkge), flagsMask,
                    TypedAttributes.moduleEntries(opensToModules)));
        }

        public ModuleAttributeBuilder opens(ModuleOpenInfo opens) {
            this.opens.add(opens);
            return this;
        }

        public ModuleAttributeBuilder uses(ClassDesc service) {
            return this.uses(TemporaryConstantPool.classEntry(service));
        }

        public ModuleAttributeBuilder uses(ClassEntry uses) {
            this.uses.add(uses);
            return this;
        }

        public ModuleAttributeBuilder provides(ClassDesc service, ClassDesc[] implClasses) {
            return this.provides(TypedAttributes.moduleProvideInfo(
                    TemporaryConstantPool.classEntry(service),
                    TypedAttributes.classEntries(implClasses)));
        }

        public ModuleAttributeBuilder provides(ModuleProvideInfo provides) {
            this.provides.add(provides);
            return this;
        }

        ModuleAttribute build() {
            return new ModuleImpl(this.name, this.flags, this.version, frozen(this.requires),
                    frozen(this.exports), frozen(this.opens), frozen(this.uses),
                    frozen(this.provides));
        }
    }

    private static final class ModuleExportInfoImpl implements ModuleExportInfo {

        private final PackageEntry pkg;
        private final int flags;
        private final List<ModuleEntry> to;

        ModuleExportInfoImpl(PackageEntry pkg, int flags, List<ModuleEntry> to) {
            this.pkg = pkg;
            this.flags = flags;
            this.to = to;
        }

        public PackageEntry exportedPackage() {
            return this.pkg;
        }

        public int exportsFlagsMask() {
            return this.flags;
        }

        public List<ModuleEntry> exportsTo() {
            return this.to;
        }
    }

    private static final class ModuleOpenInfoImpl implements ModuleOpenInfo {

        private final PackageEntry pkg;
        private final int flags;
        private final List<ModuleEntry> to;

        ModuleOpenInfoImpl(PackageEntry pkg, int flags, List<ModuleEntry> to) {
            this.pkg = pkg;
            this.flags = flags;
            this.to = to;
        }

        public PackageEntry openedPackage() {
            return this.pkg;
        }

        public int opensFlagsMask() {
            return this.flags;
        }

        public List<ModuleEntry> opensTo() {
            return this.to;
        }
    }

    private static final class ModuleProvideInfoImpl implements ModuleProvideInfo {

        private final ClassEntry service;
        private final List<ClassEntry> impls;

        ModuleProvideInfoImpl(ClassEntry service, List<ClassEntry> impls) {
            this.service = service;
            this.impls = impls;
        }

        public ClassEntry provides() {
            return this.service;
        }

        public List<ClassEntry> providesWith() {
            return this.impls;
        }
    }

    private static final class ModuleRequireInfoImpl implements ModuleRequireInfo {

        private final ModuleEntry module;
        private final int flags;
        private final Utf8Entry version;

        ModuleRequireInfoImpl(ModuleEntry module, int flags, Utf8Entry version) {
            this.module = module;
            this.flags = flags;
            this.version = version;
        }

        public ModuleEntry requires() {
            return this.module;
        }

        public int requiresFlagsMask() {
            return this.flags;
        }

        public Optional<Utf8Entry> requiresVersion() {
            return Optional.ofNullable(this.version);
        }
    }

    private static final class ModuleHashInfoImpl implements ModuleHashInfo {

        private final ModuleEntry module;
        private final byte[] hash;

        ModuleHashInfoImpl(ModuleEntry module, byte[] hash) {
            this.module = module;
            this.hash = hash;
        }

        public ModuleEntry moduleName() {
            return this.module;
        }

        // It is copied on the way out as well as on the way in: a `byte[]` returned as it is is an
        // open door for whoever receives it to change the hash of an already built attribute.
        public byte[] hash() {
            return copy(this.hash);
        }
    }

    private static final class ModuleHashesImpl implements ModuleHashesAttribute {

        private final Utf8Entry algorithm;
        private final List<ModuleHashInfo> hashes;

        ModuleHashesImpl(Utf8Entry algorithm, List<ModuleHashInfo> hashes) {
            this.algorithm = algorithm;
            this.hashes = hashes;
        }

        public Utf8Entry algorithm() {
            return this.algorithm;
        }

        public List<ModuleHashInfo> hashes() {
            return this.hashes;
        }

        public Utf8Entry attributeName() {
            return N_MODULE_HASHES;
        }

        public AttributeMapper<ModuleHashesAttribute> attributeMapper() {
            return AttributeMappers.MODULE_HASHES;
        }
    }

    private static final class ModuleMainClassImpl implements ModuleMainClassAttribute {

        private final ClassEntry mainClass;

        ModuleMainClassImpl(ClassEntry mainClass) {
            this.mainClass = mainClass;
        }

        public ClassEntry mainClass() {
            return this.mainClass;
        }

        public Utf8Entry attributeName() {
            return N_MODULE_MAIN_CLASS;
        }

        public AttributeMapper<ModuleMainClassAttribute> attributeMapper() {
            return AttributeMappers.MODULE_MAIN_CLASS;
        }
    }

    private static final class ModulePackagesImpl implements ModulePackagesAttribute {

        private final List<PackageEntry> packages;

        ModulePackagesImpl(List<PackageEntry> packages) {
            this.packages = packages;
        }

        public List<PackageEntry> packages() {
            return this.packages;
        }

        public Utf8Entry attributeName() {
            return N_MODULE_PACKAGES;
        }

        public AttributeMapper<ModulePackagesAttribute> attributeMapper() {
            return AttributeMappers.MODULE_PACKAGES;
        }
    }

    private static final class ModuleResolutionImpl implements ModuleResolutionAttribute {

        private final int flags;

        ModuleResolutionImpl(int flags) {
            this.flags = flags;
        }

        public int resolutionFlags() {
            return this.flags;
        }

        public Utf8Entry attributeName() {
            return N_MODULE_RESOLUTION;
        }

        public AttributeMapper<ModuleResolutionAttribute> attributeMapper() {
            return AttributeMappers.MODULE_RESOLUTION;
        }
    }

    private static final class ModuleTargetImpl implements ModuleTargetAttribute {

        private final Utf8Entry platform;

        ModuleTargetImpl(Utf8Entry platform) {
            this.platform = platform;
        }

        public Utf8Entry targetPlatform() {
            return this.platform;
        }

        public Utf8Entry attributeName() {
            return N_MODULE_TARGET;
        }

        public AttributeMapper<ModuleTargetAttribute> attributeMapper() {
            return AttributeMappers.MODULE_TARGET;
        }
    }

    private static final class NestHostImpl implements NestHostAttribute {

        private final ClassEntry host;

        NestHostImpl(ClassEntry host) {
            this.host = host;
        }

        public ClassEntry nestHost() {
            return this.host;
        }

        public Utf8Entry attributeName() {
            return N_NEST_HOST;
        }

        public AttributeMapper<NestHostAttribute> attributeMapper() {
            return AttributeMappers.NEST_HOST;
        }
    }

    private static final class NestMembersImpl implements NestMembersAttribute {

        private final List<ClassEntry> members;

        NestMembersImpl(List<ClassEntry> members) {
            this.members = members;
        }

        public List<ClassEntry> nestMembers() {
            return this.members;
        }

        public Utf8Entry attributeName() {
            return N_NEST_MEMBERS;
        }

        public AttributeMapper<NestMembersAttribute> attributeMapper() {
            return AttributeMappers.NEST_MEMBERS;
        }
    }

    private static final class PermittedSubclassesImpl implements PermittedSubclassesAttribute {

        private final List<ClassEntry> subclasses;

        PermittedSubclassesImpl(List<ClassEntry> subclasses) {
            this.subclasses = subclasses;
        }

        public List<ClassEntry> permittedSubclasses() {
            return this.subclasses;
        }

        public Utf8Entry attributeName() {
            return N_PERMITTED_SUBCLASSES;
        }

        public AttributeMapper<PermittedSubclassesAttribute> attributeMapper() {
            return AttributeMappers.PERMITTED_SUBCLASSES;
        }
    }

    private static final class RecordImpl implements RecordAttribute {

        private final List<RecordComponentInfo> components;

        RecordImpl(List<RecordComponentInfo> components) {
            this.components = components;
        }

        public List<RecordComponentInfo> components() {
            return this.components;
        }

        public Utf8Entry attributeName() {
            return N_RECORD;
        }

        public AttributeMapper<RecordAttribute> attributeMapper() {
            return AttributeMappers.RECORD;
        }
    }

    private static final class RecordComponentInfoImpl implements RecordComponentInfo {

        private final Utf8Entry name;
        private final Utf8Entry descriptor;
        private final List<Attribute<?>> attributes;

        RecordComponentInfoImpl(Utf8Entry name, Utf8Entry descriptor,
                List<Attribute<?>> attributes) {
            this.name = name;
            this.descriptor = descriptor;
            this.attributes = attributes;
        }

        public Utf8Entry name() {
            return this.name;
        }

        public Utf8Entry descriptor() {
            return this.descriptor;
        }

        public List<Attribute<?>> attributes() {
            return this.attributes;
        }

        public String toString() {
            return "RecordComponent[" + this.name.stringValue() + " "
                    + this.descriptor.stringValue() + "]";
        }
    }

    private static final class RuntimeVisibleAnnotationsImpl
            implements RuntimeVisibleAnnotationsAttribute {

        private final List<Annotation> annotations;

        RuntimeVisibleAnnotationsImpl(List<Annotation> annotations) {
            this.annotations = annotations;
        }

        public List<Annotation> annotations() {
            return this.annotations;
        }

        public Utf8Entry attributeName() {
            return N_RUNTIME_VISIBLE_ANNOTATIONS;
        }

        public AttributeMapper<RuntimeVisibleAnnotationsAttribute> attributeMapper() {
            return AttributeMappers.RUNTIME_VISIBLE_ANNOTATIONS;
        }
    }

    private static final class RuntimeInvisibleAnnotationsImpl
            implements RuntimeInvisibleAnnotationsAttribute {

        private final List<Annotation> annotations;

        RuntimeInvisibleAnnotationsImpl(List<Annotation> annotations) {
            this.annotations = annotations;
        }

        public List<Annotation> annotations() {
            return this.annotations;
        }

        public Utf8Entry attributeName() {
            return N_RUNTIME_INVISIBLE_ANNOTATIONS;
        }

        public AttributeMapper<RuntimeInvisibleAnnotationsAttribute> attributeMapper() {
            return AttributeMappers.RUNTIME_INVISIBLE_ANNOTATIONS;
        }
    }

    private static final class RuntimeVisibleParameterAnnotationsImpl
            implements RuntimeVisibleParameterAnnotationsAttribute {

        private final List<List<Annotation>> byParameter;

        RuntimeVisibleParameterAnnotationsImpl(List<List<Annotation>> byParameter) {
            this.byParameter = byParameter;
        }

        public List<List<Annotation>> parameterAnnotations() {
            return this.byParameter;
        }

        public Utf8Entry attributeName() {
            return N_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS;
        }

        public AttributeMapper<RuntimeVisibleParameterAnnotationsAttribute> attributeMapper() {
            return AttributeMappers.RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS;
        }
    }

    private static final class RuntimeInvisibleParameterAnnotationsImpl
            implements RuntimeInvisibleParameterAnnotationsAttribute {

        private final List<List<Annotation>> byParameter;

        RuntimeInvisibleParameterAnnotationsImpl(List<List<Annotation>> byParameter) {
            this.byParameter = byParameter;
        }

        public List<List<Annotation>> parameterAnnotations() {
            return this.byParameter;
        }

        public Utf8Entry attributeName() {
            return N_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS;
        }

        public AttributeMapper<RuntimeInvisibleParameterAnnotationsAttribute> attributeMapper() {
            return AttributeMappers.RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS;
        }
    }

    private static final class RuntimeVisibleTypeAnnotationsImpl
            implements RuntimeVisibleTypeAnnotationsAttribute {

        private final List<TypeAnnotation> annotations;

        RuntimeVisibleTypeAnnotationsImpl(List<TypeAnnotation> annotations) {
            this.annotations = annotations;
        }

        public List<TypeAnnotation> annotations() {
            return this.annotations;
        }

        public Utf8Entry attributeName() {
            return N_RUNTIME_VISIBLE_TYPE_ANNOTATIONS;
        }

        public AttributeMapper<RuntimeVisibleTypeAnnotationsAttribute> attributeMapper() {
            return AttributeMappers.RUNTIME_VISIBLE_TYPE_ANNOTATIONS;
        }
    }

    private static final class RuntimeInvisibleTypeAnnotationsImpl
            implements RuntimeInvisibleTypeAnnotationsAttribute {

        private final List<TypeAnnotation> annotations;

        RuntimeInvisibleTypeAnnotationsImpl(List<TypeAnnotation> annotations) {
            this.annotations = annotations;
        }

        public List<TypeAnnotation> annotations() {
            return this.annotations;
        }

        public Utf8Entry attributeName() {
            return N_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS;
        }

        public AttributeMapper<RuntimeInvisibleTypeAnnotationsAttribute> attributeMapper() {
            return AttributeMappers.RUNTIME_INVISIBLE_TYPE_ANNOTATIONS;
        }
    }

    private static final class SignatureImpl implements SignatureAttribute {

        private final Utf8Entry signature;

        SignatureImpl(Utf8Entry signature) {
            this.signature = signature;
        }

        public Utf8Entry signature() {
            return this.signature;
        }

        public Utf8Entry attributeName() {
            return N_SIGNATURE;
        }

        public AttributeMapper<SignatureAttribute> attributeMapper() {
            return AttributeMappers.SIGNATURE;
        }

        public String toString() {
            return "Signature[" + this.signature.stringValue() + "]";
        }
    }

    private static final class SourceDebugExtensionImpl implements SourceDebugExtensionAttribute {

        private final byte[] contents;

        SourceDebugExtensionImpl(byte[] contents) {
            this.contents = contents;
        }

        public byte[] contents() {
            return copy(this.contents);
        }

        public Utf8Entry attributeName() {
            return N_SOURCE_DEBUG_EXTENSION;
        }

        public AttributeMapper<SourceDebugExtensionAttribute> attributeMapper() {
            return AttributeMappers.SOURCE_DEBUG_EXTENSION;
        }
    }

    private static final class SourceFileImpl implements SourceFileAttribute {

        private final Utf8Entry sourceFile;

        SourceFileImpl(Utf8Entry sourceFile) {
            this.sourceFile = sourceFile;
        }

        public Utf8Entry sourceFile() {
            return this.sourceFile;
        }

        public Utf8Entry attributeName() {
            return N_SOURCE_FILE;
        }

        public AttributeMapper<SourceFileAttribute> attributeMapper() {
            return AttributeMappers.SOURCE_FILE;
        }

        public String toString() {
            return "SourceFile[" + this.sourceFile.stringValue() + "]";
        }
    }

    private static final class SourceIDImpl implements SourceIDAttribute {

        private final Utf8Entry sourceId;

        SourceIDImpl(Utf8Entry sourceId) {
            this.sourceId = sourceId;
        }

        public Utf8Entry sourceId() {
            return this.sourceId;
        }

        public Utf8Entry attributeName() {
            return N_SOURCE_ID;
        }

        public AttributeMapper<SourceIDAttribute> attributeMapper() {
            return AttributeMappers.SOURCE_ID;
        }
    }

    private static final class StackMapFrameImpl implements StackMapFrameInfo {

        private final Label target;
        private final List<VerificationTypeInfo> locals;
        private final List<VerificationTypeInfo> stack;

        StackMapFrameImpl(Label target, List<VerificationTypeInfo> locals,
                List<VerificationTypeInfo> stack) {
            this.target = target;
            this.locals = locals;
            this.stack = stack;
        }

        /**
         * Always 255, `full_frame`.
         *
         * <p>It is not a simplification: the `frame_type` is an **encoding**, not a datum. The
         * compressed forms (`same_frame`, `chop`, `append`...) can only be chosen knowing which
         * frame came before, and a frame built loose has no previous one. `full_frame` describes
         * any state and needs no context, so it is the only right answer here. The note said a
         * frame that comes from **reading** a `.class` keeps the one it had; it does not -- the
         * reader builds its frames through this same class, so they report 255 too, where the JDK
         * reports the frame type read from the file.
         */
        public int frameType() {
            return 255;
        }

        public Label target() {
            return this.target;
        }

        public List<VerificationTypeInfo> locals() {
            return this.locals;
        }

        public List<VerificationTypeInfo> stack() {
            return this.stack;
        }
    }

    private static final class StackMapTableImpl implements StackMapTableAttribute {

        private final List<StackMapFrameInfo> entries;

        StackMapTableImpl(List<StackMapFrameInfo> entries) {
            this.entries = entries;
        }

        public List<StackMapFrameInfo> entries() {
            return this.entries;
        }

        public Utf8Entry attributeName() {
            return N_STACK_MAP_TABLE;
        }

        public AttributeMapper<StackMapTableAttribute> attributeMapper() {
            return AttributeMappers.STACK_MAP_TABLE;
        }
    }

    private static final class ObjectVerificationTypeImpl implements ObjectVerificationTypeInfo {

        private final ClassEntry className;

        ObjectVerificationTypeImpl(ClassEntry className) {
            this.className = className;
        }

        public int tag() {
            return VerificationTypeInfo.ITEM_OBJECT;
        }

        public ClassEntry className() {
            return this.className;
        }

        public String toString() {
            return this.className.asInternalName();
        }
    }

    private static final class UninitializedVerificationTypeImpl
            implements UninitializedVerificationTypeInfo {

        private final Label newTarget;

        UninitializedVerificationTypeImpl(Label newTarget) {
            this.newTarget = newTarget;
        }

        public int tag() {
            return VerificationTypeInfo.ITEM_UNINITIALIZED;
        }

        public Label newTarget() {
            return this.newTarget;
        }

        public String toString() {
            return "uninitialized@" + this.newTarget;
        }
    }
}
