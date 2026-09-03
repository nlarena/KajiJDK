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
 * Las implementaciones de `java.lang.classfile.attribute` y las fábricas que las arman.
 *
 * <p>Todas las interfaces de ese paquete son declaraciones puras: sus `of(...)` delegan acá. Está
 * separado en dos por una razón concreta y no por gusto — un atributo que el usuario **construye**
 * y uno que sale de **leer** un `.class` no son la misma cosa:
 *
 * <ul>
 * <li>El construido tiene sus componentes ya en la mano. No hay archivo, no hay offsets, no hay
 *     pool de destino: es un objeto de valor y nada más. Eso es lo que hay acá.</li>
 * <li>El leído sale de {@link Mappers} por su nombre, y lo que devuelve hoy es un
 *     {@link RawAttribute} — el nombre, la entrada de pool y el cuerpo en bytes. Ver la nota de
 *     alcance de {@link java.lang.classfile.Attributes}.</li>
 * </ul>
 *
 * <p><strong>Las listas se copian y se congelan al entrar.</strong> No es defensa por defensa: un
 * atributo describe algo que ya pasó —las excepciones que este método declara, las clases anidadas
 * que esta clase tiene— y si la lista que se pasó siguiera viva, cambiarla después cambiaría lo que
 * el atributo dice sin que nadie lo haya vuelto a construir. `Collections.unmodifiableList` sobre
 * una copia es lo único que hace que `exceptions()` devuelva siempre lo mismo.
 *
 * <p><strong>Los `Optional` se guardan como el valor o `null`</strong> y se envuelven al salir. Un
 * campo `Optional` es un objeto más por atributo y por componente, y acá hay tablas con miles de
 * entradas (`LineNumberTable` de un método grande). El contrato hacia afuera es idéntico.
 */
public final class TypedAttributes {

    private TypedAttributes() {
    }

    // Los nombres, una vez por atributo y no uno por objeto construido. El pool deduplica igual,
    // pero con la constante ni siquiera se lo consulta.
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


    // ---- conversiones que las fábricas de la API piden por nombre ------------------------------
    //
    // Estas existen porque una interfaz de `java.lang.classfile.attribute` no puede llamar a
    // `TemporaryConstantPool` (es interna y el `of` está en el paquete público), así que pasa por
    // acá. Son de una línea a propósito: si alguna creciera, la lógica estaría en el lugar
    // equivocado.

    /** Un `CONSTANT_Utf8` suelto. */
    public static Utf8Entry utf8(String s) {
        return TemporaryConstantPool.utf8(s);
    }

    /**
     * Igual que {@link #utf8}, pero `null` pasa como `null` en vez de romper.
     *
     * <p>Lo pide `ModuleRequireInfo`: la versión de un `requires` es opcional en el formato, y quien
     * llama la tiene como un `String` que puede faltar. Sin esto, cada llamador repetiría el mismo
     * `if`.
     */
    public static Utf8Entry utf8OrNull(String s) {
        return s == null ? null : TemporaryConstantPool.utf8(s);
    }

    /** Un `CONSTANT_Class` suelto. */
    public static ClassEntry classEntry(ClassDesc d) {
        return TemporaryConstantPool.classEntry(d);
    }

    /** Un `CONSTANT_Package` suelto. */
    public static PackageEntry packageEntry(PackageDesc d) {
        return TemporaryConstantPool.pool().packageEntry(d);
    }

    /** Un `CONSTANT_Module` suelto. */
    public static ModuleEntry moduleEntry(ModuleDesc d) {
        return TemporaryConstantPool.pool().moduleEntry(d);
    }

    /** La entrada de valor constante de `c` (`Integer`, `Long`, `Float`, `Double` o `String`). */
    public static ConstantValueEntry constantValueEntry(ConstantDesc c) {
        return TemporaryConstantPool.pool().constantValueEntry(c);
    }

    /** Las entradas `CONSTANT_Class` de esos descriptores. */
    public static List<ClassEntry> classEntries(List<ClassDesc> descs) {
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < descs.size(); i++) {
            out.add(TemporaryConstantPool.classEntry(descs.get(i)));
        }
        return out;
    }

    /** Las entradas `CONSTANT_Class` de esos descriptores. */
    public static List<ClassEntry> classEntries(ClassDesc[] descs) {
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < descs.length; i++) {
            out.add(TemporaryConstantPool.classEntry(descs[i]));
        }
        return out;
    }

    /** Las entradas `CONSTANT_Package` de esos descriptores. */
    public static List<PackageEntry> packageEntries(List<PackageDesc> descs) {
        List<PackageEntry> out = new ArrayList<PackageEntry>();
        for (int i = 0; i < descs.size(); i++) {
            out.add(TemporaryConstantPool.pool().packageEntry(descs.get(i)));
        }
        return out;
    }

    /** Las entradas `CONSTANT_Package` de esos descriptores. */
    public static List<PackageEntry> packageEntries(PackageDesc[] descs) {
        List<PackageEntry> out = new ArrayList<PackageEntry>();
        for (int i = 0; i < descs.length; i++) {
            out.add(TemporaryConstantPool.pool().packageEntry(descs[i]));
        }
        return out;
    }

    /** Las entradas `CONSTANT_Module` de esos descriptores. */
    public static List<ModuleEntry> moduleEntries(List<ModuleDesc> descs) {
        List<ModuleEntry> out = new ArrayList<ModuleEntry>();
        for (int i = 0; i < descs.size(); i++) {
            out.add(TemporaryConstantPool.pool().moduleEntry(descs.get(i)));
        }
        return out;
    }

    /** Las entradas `CONSTANT_Module` de esos descriptores. */
    public static List<ModuleEntry> moduleEntries(ModuleDesc[] descs) {
        List<ModuleEntry> out = new ArrayList<ModuleEntry>();
        for (int i = 0; i < descs.length; i++) {
            out.add(TemporaryConstantPool.pool().moduleEntry(descs[i]));
        }
        return out;
    }

    /**
     * Un arreglo variádico como lista.
     *
     * <p>Hay además `listOfClasses`, `listOfModules`, `listOfAttributes`, `listOfAnnotations` y
     * `listOfTypeAnnotations`, que hacen exactamente esto para un tipo fijo. No son redundancia:
     * nuestro javac no siempre infiere `T` cuando el resultado va derecho como argumento de otra
     * llamada genérica, y con el nombre concreto no hay nada que inferir.
     */
    public static <T> List<T> listOf(T[] items) {
        List<T> out = new ArrayList<T>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** Ver {@link #listOf}. */
    public static List<ClassEntry> listOfClasses(ClassEntry[] items) {
        List<ClassEntry> out = new ArrayList<ClassEntry>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** Ver {@link #listOf}. */
    public static List<ModuleEntry> listOfModules(ModuleEntry[] items) {
        List<ModuleEntry> out = new ArrayList<ModuleEntry>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** Ver {@link #listOf}. */
    public static List<Attribute<?>> listOfAttributes(Attribute<?>[] items) {
        List<Attribute<?>> out = new ArrayList<Attribute<?>>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** Ver {@link #listOf}. */
    public static List<Annotation> listOfAnnotations(Annotation[] items) {
        List<Annotation> out = new ArrayList<Annotation>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** Ver {@link #listOf}. */
    public static List<TypeAnnotation> listOfTypeAnnotations(TypeAnnotation[] items) {
        List<TypeAnnotation> out = new ArrayList<TypeAnnotation>();
        for (int i = 0; i < items.length; i++) {
            out.add(items[i]);
        }
        return out;
    }

    /** La máscara de bits de esas banderas. */
    public static int mask(AccessFlag[] flags) {
        int m = 0;
        for (int i = 0; i < flags.length; i++) {
            m = m | flags[i].mask();
        }
        return m;
    }

    /** La máscara de bits de esas banderas. */
    public static int mask(Collection<AccessFlag> flags) {
        int m = 0;
        for (AccessFlag f : flags) {
            m = m | f.mask();
        }
        return m;
    }

    // Copia congelada: ver la nota de la clase sobre por qué no se guarda la lista viva.
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

    // ---- fábricas -----------------------------------------------------------------------------

    /** El atributo `AnnotationDefault` con ese valor. */
    public static AnnotationDefaultAttribute annotationDefault(AnnotationValue v) {
        return new AnnotationDefaultImpl(v);
    }

    /** Un rango del `CharacterRangeTable`. */
    public static CharacterRangeInfo characterRangeInfo(int startPc, int endPc, int rangeStart,
            int rangeEnd, int flags) {
        return new CharacterRangeInfoImpl(startPc, endPc, rangeStart, rangeEnd, flags);
    }

    /** El atributo `CharacterRangeTable` con esos rangos. */
    public static CharacterRangeTableAttribute characterRangeTable(List<CharacterRangeInfo> r) {
        return new CharacterRangeTableImpl(frozen(r));
    }

    /** El atributo `CompilationID`. */
    public static CompilationIDAttribute compilationId(Utf8Entry id) {
        return new CompilationIDImpl(id);
    }

    /** El atributo `ConstantValue`. */
    public static ConstantValueAttribute constantValue(ConstantValueEntry v) {
        return new ConstantValueImpl(v);
    }

    /**
     * El atributo `Deprecated`.
     *
     * <p>No tiene cuerpo: existir **es** todo lo que dice. Por eso hay una sola instancia y no una
     * nueva por llamada — dos `Deprecated` no se distinguen en nada.
     */
    public static DeprecatedAttribute deprecated() {
        return DeprecatedImpl.INSTANCE;
    }

    /** El atributo `Synthetic`. Sin cuerpo, como `Deprecated`. */
    public static SyntheticAttribute synthetic() {
        return SyntheticImpl.INSTANCE;
    }

    /** El atributo `EnclosingMethod`. */
    public static EnclosingMethodAttribute enclosingMethod(ClassEntry owner,
            Optional<NameAndTypeEntry> method) {
        return new EnclosingMethodImpl(owner, method.isPresent() ? method.get() : null);
    }

    /**
     * El atributo `EnclosingMethod`, nombrando la clase y el método por sus descriptores.
     *
     * <p>El nombre y el tipo van juntos o no va ninguno: el formato guarda **un** índice a un
     * `NameAndType`, no dos campos sueltos. Pedir uno solo describiría un `.class` que no existe, y
     * por eso es `IllegalArgumentException` y no una interpretación amable.
     */
    public static EnclosingMethodAttribute enclosingMethod(ClassDesc owner,
            Optional<String> methodName, Optional<MethodTypeDesc> methodType) {
        if (methodName.isPresent() != methodType.isPresent()) {
            throw new IllegalArgumentException(
                    "EnclosingMethod lleva el nombre y el tipo juntos, o ninguno de los dos");
        }
        NameAndTypeEntry nat = null;
        if (methodName.isPresent()) {
            nat = TemporaryConstantPool.nameAndType(
                    TemporaryConstantPool.utf8(methodName.get()),
                    TemporaryConstantPool.utf8(methodType.get().descriptorString()));
        }
        return new EnclosingMethodImpl(TemporaryConstantPool.classEntry(owner), nat);
    }

    /** El atributo `Exceptions`. */
    public static ExceptionsAttribute exceptions(List<ClassEntry> exceptions) {
        return new ExceptionsImpl(frozen(exceptions));
    }

    /** Una entrada del `InnerClasses`. */
    public static InnerClassInfo innerClassInfo(ClassEntry inner, Optional<ClassEntry> outer,
            Optional<Utf8Entry> innerName, int flags) {
        return new InnerClassInfoImpl(inner, outer.isPresent() ? outer.get() : null,
                innerName.isPresent() ? innerName.get() : null, flags);
    }

    /** Una entrada del `InnerClasses`, por descriptores. */
    public static InnerClassInfo innerClassInfo(ClassDesc inner, Optional<ClassDesc> outer,
            Optional<String> innerName, int flags) {
        return new InnerClassInfoImpl(TemporaryConstantPool.classEntry(inner),
                outer.isPresent() ? TemporaryConstantPool.classEntry(outer.get()) : null,
                innerName.isPresent() ? TemporaryConstantPool.utf8(innerName.get()) : null,
                flags);
    }

    /** El atributo `InnerClasses`. */
    public static InnerClassesAttribute innerClasses(List<InnerClassInfo> classes) {
        return new InnerClassesImpl(frozen(classes));
    }

    /** Una entrada del `LineNumberTable`. */
    public static LineNumberInfo lineNumberInfo(int startPc, int lineNumber) {
        return new LineNumberInfoImpl(startPc, lineNumber);
    }

    /** El atributo `LineNumberTable`. */
    public static LineNumberTableAttribute lineNumberTable(List<LineNumberInfo> lines) {
        return new LineNumberTableImpl(frozen(lines));
    }

    /** El atributo `LocalVariableTable`. */
    public static LocalVariableTableAttribute localVariableTable(List<LocalVariableInfo> vars) {
        return new LocalVariableTableImpl(frozen(vars));
    }

    /** El atributo `LocalVariableTypeTable`. */
    public static LocalVariableTypeTableAttribute localVariableTypeTable(
            List<LocalVariableTypeInfo> vars) {
        return new LocalVariableTypeTableImpl(frozen(vars));
    }

    /** Una entrada del `MethodParameters`. */
    public static MethodParameterInfo methodParameterInfo(Optional<Utf8Entry> name, int flags) {
        return new MethodParameterInfoImpl(name.isPresent() ? name.get() : null, flags);
    }

    /** Una entrada del `MethodParameters`, con el nombre como texto. */
    public static MethodParameterInfo methodParameterInfoOfNames(Optional<String> name, int flags) {
        return new MethodParameterInfoImpl(
                name.isPresent() ? TemporaryConstantPool.utf8(name.get()) : null, flags);
    }

    /** El atributo `MethodParameters`. */
    public static MethodParametersAttribute methodParameters(List<MethodParameterInfo> ps) {
        return new MethodParametersImpl(frozen(ps));
    }

    /** El atributo `Module`. */
    public static ModuleAttribute module(ModuleEntry name, int flags, Utf8Entry version,
            Collection<ModuleRequireInfo> requires, Collection<ModuleExportInfo> exports,
            Collection<ModuleOpenInfo> opens, Collection<ClassEntry> uses,
            Collection<ModuleProvideInfo> provides) {
        return new ModuleImpl(name, flags, version, frozen(requires), frozen(exports),
                frozen(opens), frozen(uses), frozen(provides));
    }

    /**
     * El atributo `Module` armado por un constructor paso a paso.
     *
     * <p>El `handler` recibe un constructor mutable y le va agregando directivas; lo que se
     * devuelve es un atributo ya congelado. Después de esta llamada el constructor no se vuelve a
     * usar, así que lo que el `handler` se haya guardado no puede cambiar el atributo.
     */
    public static ModuleAttribute buildModule(ModuleEntry name,
            Consumer<ModuleAttributeBuilder> handler) {
        ModuleBuilderImpl b = new ModuleBuilderImpl(name);
        handler.accept(b);
        return b.build();
    }

    /** Una directiva `exports`. */
    public static ModuleExportInfo moduleExportInfo(PackageEntry pkg, int flags,
            List<ModuleEntry> to) {
        return new ModuleExportInfoImpl(pkg, flags, frozen(to));
    }

    /** Una directiva `opens`. */
    public static ModuleOpenInfo moduleOpenInfo(PackageEntry pkg, int flags,
            List<ModuleEntry> to) {
        return new ModuleOpenInfoImpl(pkg, flags, frozen(to));
    }

    /** Una directiva `provides`. */
    public static ModuleProvideInfo moduleProvideInfo(ClassEntry service,
            List<ClassEntry> impls) {
        return new ModuleProvideInfoImpl(service, frozen(impls));
    }

    /** Una directiva `requires`. */
    public static ModuleRequireInfo moduleRequireInfo(ModuleEntry module, int flags,
            Utf8Entry version) {
        return new ModuleRequireInfoImpl(module, flags, version);
    }

    /** Una entrada del `ModuleHashes`. */
    public static ModuleHashInfo moduleHashInfo(ModuleEntry module, byte[] hash) {
        return new ModuleHashInfoImpl(module, copy(hash));
    }

    /** El atributo `ModuleHashes`. */
    public static ModuleHashesAttribute moduleHashes(Utf8Entry algorithm,
            List<ModuleHashInfo> hashes) {
        return new ModuleHashesImpl(algorithm, frozen(hashes));
    }

    /** El atributo `ModuleMainClass`. */
    public static ModuleMainClassAttribute moduleMainClass(ClassEntry mainClass) {
        return new ModuleMainClassImpl(mainClass);
    }

    /** El atributo `ModulePackages`. */
    public static ModulePackagesAttribute modulePackages(List<PackageEntry> packages) {
        return new ModulePackagesImpl(frozen(packages));
    }

    /** El atributo `ModuleResolution`. */
    public static ModuleResolutionAttribute moduleResolution(int flags) {
        return new ModuleResolutionImpl(flags);
    }

    /** El atributo `ModuleTarget`. */
    public static ModuleTargetAttribute moduleTarget(Utf8Entry platform) {
        return new ModuleTargetImpl(platform);
    }

    /** El atributo `NestHost`. */
    public static NestHostAttribute nestHost(ClassEntry host) {
        return new NestHostImpl(host);
    }

    /** El atributo `NestMembers`. */
    public static NestMembersAttribute nestMembers(List<ClassEntry> members) {
        return new NestMembersImpl(frozen(members));
    }

    /** El atributo `PermittedSubclasses`. */
    public static PermittedSubclassesAttribute permittedSubclasses(List<ClassEntry> subs) {
        return new PermittedSubclassesImpl(frozen(subs));
    }

    /** El atributo `Record`. */
    public static RecordAttribute record(List<RecordComponentInfo> components) {
        return new RecordImpl(frozen(components));
    }

    /** Un componente de un `record`. */
    public static RecordComponentInfo recordComponentInfo(Utf8Entry name, Utf8Entry descriptor,
            List<Attribute<?>> attributes) {
        return new RecordComponentInfoImpl(name, descriptor, frozen(attributes));
    }

    /** El atributo `RuntimeVisibleAnnotations`. */
    public static RuntimeVisibleAnnotationsAttribute runtimeVisibleAnnotations(
            List<Annotation> annotations) {
        return new RuntimeVisibleAnnotationsImpl(frozen(annotations));
    }

    /** El atributo `RuntimeInvisibleAnnotations`. */
    public static RuntimeInvisibleAnnotationsAttribute runtimeInvisibleAnnotations(
            List<Annotation> annotations) {
        return new RuntimeInvisibleAnnotationsImpl(frozen(annotations));
    }

    /** El atributo `RuntimeVisibleParameterAnnotations`. */
    public static RuntimeVisibleParameterAnnotationsAttribute runtimeVisibleParameterAnnotations(
            List<List<Annotation>> byParameter) {
        return new RuntimeVisibleParameterAnnotationsImpl(frozenNested(byParameter));
    }

    /** El atributo `RuntimeInvisibleParameterAnnotations`. */
    public static RuntimeInvisibleParameterAnnotationsAttribute
            runtimeInvisibleParameterAnnotations(List<List<Annotation>> byParameter) {
        return new RuntimeInvisibleParameterAnnotationsImpl(frozenNested(byParameter));
    }

    /** El atributo `RuntimeVisibleTypeAnnotations`. */
    public static RuntimeVisibleTypeAnnotationsAttribute runtimeVisibleTypeAnnotations(
            List<TypeAnnotation> annotations) {
        return new RuntimeVisibleTypeAnnotationsImpl(frozen(annotations));
    }

    /** El atributo `RuntimeInvisibleTypeAnnotations`. */
    public static RuntimeInvisibleTypeAnnotationsAttribute runtimeInvisibleTypeAnnotations(
            List<TypeAnnotation> annotations) {
        return new RuntimeInvisibleTypeAnnotationsImpl(frozen(annotations));
    }

    // La lista de listas de las anotaciones por parámetro: se congelan las dos capas. Congelar sólo
    // la de afuera dejaría mutable la de cada parámetro, que es justo la que alguien va a tener a
    // mano después de construirla.
    private static List<List<Annotation>> frozenNested(List<List<Annotation>> src) {
        List<List<Annotation>> out = new ArrayList<List<Annotation>>();
        for (int i = 0; i < src.size(); i++) {
            out.add(frozen(src.get(i)));
        }
        return Collections.unmodifiableList(out);
    }

    /** El atributo `Signature`. */
    public static SignatureAttribute signature(Utf8Entry signature) {
        return new SignatureImpl(signature);
    }

    /** El atributo `SourceDebugExtension`. */
    public static SourceDebugExtensionAttribute sourceDebugExtension(byte[] contents) {
        return new SourceDebugExtensionImpl(copy(contents));
    }

    /** El atributo `SourceFile`. */
    public static SourceFileAttribute sourceFile(Utf8Entry sourceFile) {
        return new SourceFileImpl(sourceFile);
    }

    /** El atributo `SourceID`. */
    public static SourceIDAttribute sourceId(Utf8Entry sourceId) {
        return new SourceIDImpl(sourceId);
    }

    /** Un frame del `StackMapTable`. */
    public static StackMapFrameInfo stackMapFrame(Label target, List<VerificationTypeInfo> locals,
            List<VerificationTypeInfo> stack) {
        return new StackMapFrameImpl(target, frozen(locals), frozen(stack));
    }

    /** El atributo `StackMapTable`. */
    public static StackMapTableAttribute stackMapTable(List<StackMapFrameInfo> entries) {
        return new StackMapTableImpl(frozen(entries));
    }

    /** El tipo de verificación de una referencia a esa clase. */
    public static ObjectVerificationTypeInfo objectVerificationType(ClassEntry className) {
        return new ObjectVerificationTypeImpl(className);
    }

    /** El tipo de verificación del objeto creado por el `new` de esa etiqueta. */
    public static UninitializedVerificationTypeInfo uninitializedVerificationType(Label target) {
        return new UninitializedVerificationTypeImpl(target);
    }

    // ---- implementaciones ---------------------------------------------------------------------
    //
    // Cada una guarda sus componentes y contesta. Todas comparten la misma forma, así que las que
    // siguen no llevan comentario propio: lo que hay que saber está arriba. Lo que sí se comenta es
    // lo que se aparta de la forma.

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

    // El constructor paso a paso de `Module`. Es lo único mutable de este archivo, y vive lo que
    // dura la llamada a `buildModule`: acumula y se lo tira.
    //
    // `moduleName` se puede volver a fijar porque la API lo permite (`ModuleAttributeBuilder`
    // declara el método), no porque tenga sentido llamarlo dos veces.
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

        // Se copia al salir además de al entrar: un `byte[]` que se devuelve tal cual es una puerta
        // abierta para que quien lo reciba cambie el hash de un atributo ya construido.
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
         * Siempre 255, `full_frame`.
         *
         * <p>No es una simplificación: el `frame_type` es una **codificación**, no un dato. Las
         * formas comprimidas (`same_frame`, `chop`, `append`…) sólo se pueden elegir sabiendo qué
         * frame vino antes, y un frame construido suelto no tiene anterior. `full_frame` describe
         * cualquier estado y no necesita contexto, así que es la única respuesta correcta acá. Un
         * frame que sale de **leer** un `.class` conserva el que tenía.
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
