package jdk.internal.classfile.impl;

import java.lang.classfile.Attribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.AttributeMapper.AttributeStability;
import java.lang.classfile.AttributedElement;
import java.lang.classfile.BufWriter;
import java.lang.classfile.ClassReader;
import java.lang.classfile.attribute.AnnotationDefaultAttribute;
import java.lang.classfile.attribute.CharacterRangeTableAttribute;
import java.lang.classfile.attribute.CompilationIDAttribute;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.EnclosingMethodAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.attribute.LineNumberTableAttribute;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.classfile.attribute.LocalVariableTypeTableAttribute;
import java.lang.classfile.attribute.MethodParametersAttribute;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleHashesAttribute;
import java.lang.classfile.attribute.ModuleMainClassAttribute;
import java.lang.classfile.attribute.ModulePackagesAttribute;
import java.lang.classfile.attribute.ModuleResolutionAttribute;
import java.lang.classfile.attribute.ModuleTargetAttribute;
import java.lang.classfile.attribute.NestHostAttribute;
import java.lang.classfile.attribute.NestMembersAttribute;
import java.lang.classfile.attribute.PermittedSubclassesAttribute;
import java.lang.classfile.attribute.RecordAttribute;
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
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.attribute.UnknownAttribute;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// The TYPED mappers: the ones that read a JVMS attribute and return the
// `java.lang.classfile.attribute` object that corresponds to it, instead of the `RawAttribute` that
// the ones of `java.lang.classfile.Attributes` return.
//
// There are the thirty-four that can be read on their own, plus the generic one for an unknown
// name. `Code` and `BootstrapMethods` are NOT here, and the reason is concrete: neither of the two
// is a value that can be read by itself. The labels of a `Code` are offsets within the method that
// contains it and it carries its own attributes inside, so `ClassModelImpl` builds it while walking
// the method; `BootstrapMethods` describes the whole file and is consulted by index from the pool.
// Both are still read that way and with the raw mapper of `Attributes` -- what there is not is a
// loose typed mapper pretending to be able to do it without that context.
//
// Each mapper is a CONSTANT and has to be: `AttributedElement.findAttribute` compares mappers by
// identity, so two different mappers of the same attribute would make
// `findAttribute(Attributes.code())` not find the `Code` that was just read.
//
// The read and write dispatch goes through an `int` and not through inheritance --one mapper
// subclass per attribute-- because that way all the attributes share a single mapper class and the
// code that builds them lives together in {@link AttributeReader} and {@link AttributeWriter},
// which is where one can be compared with another. (The note said "the fifty attributes"; there are
// thirty-seven dispatch codes, the thirty-six of the JVMS plus the unknown one.)
public final class AttributeMappers {

    /** The dispatch code of each attribute. It only makes sense within this package. */
    public static final int C_ANNOTATION_DEFAULT = 1;
    public static final int C_BOOTSTRAP_METHODS = 2;
    public static final int C_CHARACTER_RANGE_TABLE = 3;
    public static final int C_CODE = 4;
    public static final int C_COMPILATION_ID = 5;
    public static final int C_CONSTANT_VALUE = 6;
    public static final int C_DEPRECATED = 7;
    public static final int C_ENCLOSING_METHOD = 8;
    public static final int C_EXCEPTIONS = 9;
    public static final int C_INNER_CLASSES = 10;
    public static final int C_LINE_NUMBER_TABLE = 11;
    public static final int C_LOCAL_VARIABLE_TABLE = 12;
    public static final int C_LOCAL_VARIABLE_TYPE_TABLE = 13;
    public static final int C_METHOD_PARAMETERS = 14;
    public static final int C_MODULE = 15;
    public static final int C_MODULE_HASHES = 16;
    public static final int C_MODULE_MAIN_CLASS = 17;
    public static final int C_MODULE_PACKAGES = 18;
    public static final int C_MODULE_RESOLUTION = 19;
    public static final int C_MODULE_TARGET = 20;
    public static final int C_NEST_HOST = 21;
    public static final int C_NEST_MEMBERS = 22;
    public static final int C_PERMITTED_SUBCLASSES = 23;
    public static final int C_RECORD = 24;
    public static final int C_RUNTIME_INVISIBLE_ANNOTATIONS = 25;
    public static final int C_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = 26;
    public static final int C_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = 27;
    public static final int C_RUNTIME_VISIBLE_ANNOTATIONS = 28;
    public static final int C_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = 29;
    public static final int C_RUNTIME_VISIBLE_TYPE_ANNOTATIONS = 30;
    public static final int C_SIGNATURE = 31;
    public static final int C_SOURCE_DEBUG_EXTENSION = 32;
    public static final int C_SOURCE_FILE = 33;
    public static final int C_SOURCE_ID = 34;
    public static final int C_STACK_MAP_TABLE = 35;
    public static final int C_SYNTHETIC = 36;
    public static final int C_UNKNOWN = 37;

    public static final AttributeMapper<AnnotationDefaultAttribute> ANNOTATION_DEFAULT =
            new TypedAttributeMapper<AnnotationDefaultAttribute>("AnnotationDefault",
                    C_ANNOTATION_DEFAULT, AttributeStability.CP_REFS, false);
    public static final AttributeMapper<CharacterRangeTableAttribute> CHARACTER_RANGE_TABLE =
            new TypedAttributeMapper<CharacterRangeTableAttribute>("CharacterRangeTable",
                    C_CHARACTER_RANGE_TABLE, AttributeStability.LABELS, true);
    public static final AttributeMapper<CompilationIDAttribute> COMPILATION_ID =
            new TypedAttributeMapper<CompilationIDAttribute>("CompilationID", C_COMPILATION_ID,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ConstantValueAttribute> CONSTANT_VALUE =
            new TypedAttributeMapper<ConstantValueAttribute>("ConstantValue", C_CONSTANT_VALUE,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<DeprecatedAttribute> DEPRECATED =
            new TypedAttributeMapper<DeprecatedAttribute>("Deprecated", C_DEPRECATED,
                    AttributeStability.STATELESS, true);
    public static final AttributeMapper<EnclosingMethodAttribute> ENCLOSING_METHOD =
            new TypedAttributeMapper<EnclosingMethodAttribute>("EnclosingMethod", C_ENCLOSING_METHOD,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ExceptionsAttribute> EXCEPTIONS =
            new TypedAttributeMapper<ExceptionsAttribute>("Exceptions", C_EXCEPTIONS,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<InnerClassesAttribute> INNER_CLASSES =
            new TypedAttributeMapper<InnerClassesAttribute>("InnerClasses", C_INNER_CLASSES,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<LineNumberTableAttribute> LINE_NUMBER_TABLE =
            new TypedAttributeMapper<LineNumberTableAttribute>("LineNumberTable",
                    C_LINE_NUMBER_TABLE, AttributeStability.LABELS, true);
    public static final AttributeMapper<LocalVariableTableAttribute> LOCAL_VARIABLE_TABLE =
            new TypedAttributeMapper<LocalVariableTableAttribute>("LocalVariableTable",
                    C_LOCAL_VARIABLE_TABLE, AttributeStability.LABELS, true);
    public static final AttributeMapper<LocalVariableTypeTableAttribute>
            LOCAL_VARIABLE_TYPE_TABLE =
            new TypedAttributeMapper<LocalVariableTypeTableAttribute>("LocalVariableTypeTable",
                    C_LOCAL_VARIABLE_TYPE_TABLE, AttributeStability.LABELS, true);
    public static final AttributeMapper<MethodParametersAttribute> METHOD_PARAMETERS =
            new TypedAttributeMapper<MethodParametersAttribute>("MethodParameters",
                    C_METHOD_PARAMETERS, AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ModuleAttribute> MODULE =
            new TypedAttributeMapper<ModuleAttribute>("Module", C_MODULE,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ModuleHashesAttribute> MODULE_HASHES =
            new TypedAttributeMapper<ModuleHashesAttribute>("ModuleHashes", C_MODULE_HASHES,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ModuleMainClassAttribute> MODULE_MAIN_CLASS =
            new TypedAttributeMapper<ModuleMainClassAttribute>("ModuleMainClass",
                    C_MODULE_MAIN_CLASS, AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ModulePackagesAttribute> MODULE_PACKAGES =
            new TypedAttributeMapper<ModulePackagesAttribute>("ModulePackages", C_MODULE_PACKAGES,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<ModuleResolutionAttribute> MODULE_RESOLUTION =
            new TypedAttributeMapper<ModuleResolutionAttribute>("ModuleResolution",
                    C_MODULE_RESOLUTION, AttributeStability.STATELESS, false);
    public static final AttributeMapper<ModuleTargetAttribute> MODULE_TARGET =
            new TypedAttributeMapper<ModuleTargetAttribute>("ModuleTarget", C_MODULE_TARGET,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<NestHostAttribute> NEST_HOST =
            new TypedAttributeMapper<NestHostAttribute>("NestHost", C_NEST_HOST,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<NestMembersAttribute> NEST_MEMBERS =
            new TypedAttributeMapper<NestMembersAttribute>("NestMembers", C_NEST_MEMBERS,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<PermittedSubclassesAttribute> PERMITTED_SUBCLASSES =
            new TypedAttributeMapper<PermittedSubclassesAttribute>("PermittedSubclasses",
                    C_PERMITTED_SUBCLASSES, AttributeStability.CP_REFS, false);
    public static final AttributeMapper<RecordAttribute> RECORD =
            new TypedAttributeMapper<RecordAttribute>("Record", C_RECORD,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<RuntimeInvisibleAnnotationsAttribute>
            RUNTIME_INVISIBLE_ANNOTATIONS =
            new TypedAttributeMapper<RuntimeInvisibleAnnotationsAttribute>(
                    "RuntimeInvisibleAnnotations", C_RUNTIME_INVISIBLE_ANNOTATIONS,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<RuntimeInvisibleParameterAnnotationsAttribute>
            RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS =
            new TypedAttributeMapper<RuntimeInvisibleParameterAnnotationsAttribute>(
                    "RuntimeInvisibleParameterAnnotations",
                    C_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS, AttributeStability.CP_REFS, false);
    public static final AttributeMapper<RuntimeInvisibleTypeAnnotationsAttribute>
            RUNTIME_INVISIBLE_TYPE_ANNOTATIONS =
            new TypedAttributeMapper<RuntimeInvisibleTypeAnnotationsAttribute>(
                    "RuntimeInvisibleTypeAnnotations", C_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS,
                    AttributeStability.UNSTABLE, false);
    public static final AttributeMapper<RuntimeVisibleAnnotationsAttribute>
            RUNTIME_VISIBLE_ANNOTATIONS =
            new TypedAttributeMapper<RuntimeVisibleAnnotationsAttribute>(
                    "RuntimeVisibleAnnotations", C_RUNTIME_VISIBLE_ANNOTATIONS,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<RuntimeVisibleParameterAnnotationsAttribute>
            RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS =
            new TypedAttributeMapper<RuntimeVisibleParameterAnnotationsAttribute>(
                    "RuntimeVisibleParameterAnnotations", C_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<RuntimeVisibleTypeAnnotationsAttribute>
            RUNTIME_VISIBLE_TYPE_ANNOTATIONS =
            new TypedAttributeMapper<RuntimeVisibleTypeAnnotationsAttribute>(
                    "RuntimeVisibleTypeAnnotations", C_RUNTIME_VISIBLE_TYPE_ANNOTATIONS,
                    AttributeStability.UNSTABLE, false);
    public static final AttributeMapper<SignatureAttribute> SIGNATURE =
            new TypedAttributeMapper<SignatureAttribute>("Signature", C_SIGNATURE,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<SourceDebugExtensionAttribute> SOURCE_DEBUG_EXTENSION =
            new TypedAttributeMapper<SourceDebugExtensionAttribute>("SourceDebugExtension",
                    C_SOURCE_DEBUG_EXTENSION, AttributeStability.STATELESS, false);
    public static final AttributeMapper<SourceFileAttribute> SOURCE_FILE =
            new TypedAttributeMapper<SourceFileAttribute>("SourceFile", C_SOURCE_FILE,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<SourceIDAttribute> SOURCE_ID =
            new TypedAttributeMapper<SourceIDAttribute>("SourceID", C_SOURCE_ID,
                    AttributeStability.CP_REFS, false);
    public static final AttributeMapper<StackMapTableAttribute> STACK_MAP_TABLE =
            new TypedAttributeMapper<StackMapTableAttribute>("StackMapTable", C_STACK_MAP_TABLE,
                    AttributeStability.LABELS, false);
    public static final AttributeMapper<SyntheticAttribute> SYNTHETIC =
            new TypedAttributeMapper<SyntheticAttribute>("Synthetic", C_SYNTHETIC,
                    AttributeStability.STATELESS, true);

    private static final Map<String, AttributeMapper<?>> KNOWN = build();
    private static final Map<String, AttributeMapper<UnknownAttribute>> UNKNOWN =
            new HashMap<String, AttributeMapper<UnknownAttribute>>();

    private AttributeMappers() {
    }

    private static Map<String, AttributeMapper<?>> build() {
        Map<String, AttributeMapper<?>> m = new HashMap<String, AttributeMapper<?>>();
        AttributeMapper<?>[] all = new AttributeMapper<?>[] {
            ANNOTATION_DEFAULT, CHARACTER_RANGE_TABLE, COMPILATION_ID,
            CONSTANT_VALUE, DEPRECATED, ENCLOSING_METHOD, EXCEPTIONS, INNER_CLASSES,
            LINE_NUMBER_TABLE, LOCAL_VARIABLE_TABLE, LOCAL_VARIABLE_TYPE_TABLE, METHOD_PARAMETERS,
            MODULE, MODULE_HASHES, MODULE_MAIN_CLASS, MODULE_PACKAGES, MODULE_RESOLUTION,
            MODULE_TARGET, NEST_HOST, NEST_MEMBERS, PERMITTED_SUBCLASSES, RECORD,
            RUNTIME_INVISIBLE_ANNOTATIONS, RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS,
            RUNTIME_INVISIBLE_TYPE_ANNOTATIONS, RUNTIME_VISIBLE_ANNOTATIONS,
            RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS, RUNTIME_VISIBLE_TYPE_ANNOTATIONS, SIGNATURE,
            SOURCE_DEBUG_EXTENSION, SOURCE_FILE, SOURCE_ID, STACK_MAP_TABLE, SYNTHETIC };
        for (int i = 0; i < all.length; i++) {
            m.put(all[i].name(), all[i]);
        }
        return m;
    }

    /** The mapper of `name`, or the generic unknown-name one if it is not one of the known ones. */
    public static AttributeMapper<?> find(String name) {
        AttributeMapper<?> m = KNOWN.get(name);
        if (m != null) {
            return m;
        }
        synchronized (UNKNOWN) {
            AttributeMapper<UnknownAttribute> u = UNKNOWN.get(name);
            if (u == null) {
                u = new TypedAttributeMapper<UnknownAttribute>(name, C_UNKNOWN,
                        AttributeStability.UNKNOWN, true);
                UNKNOWN.put(name, u);
            }
            return u;
        }
    }
}

// The mapper of an attribute. A single one for the thirty-seven cases: what changes is the name and
// the dispatch code.
final class TypedAttributeMapper<A extends Attribute<A>> implements AttributeMapper<A> {

    private final String name;
    private final int code;
    private final AttributeStability stability;
    private final boolean allowMultiple;

    TypedAttributeMapper(String name, int code, AttributeStability stability,
            boolean allowMultiple) {
        this.name = name;
        this.code = code;
        this.stability = stability;
        this.allowMultiple = allowMultiple;
    }

    public String name() {
        return this.name;
    }

    /** The dispatch code of this attribute. */
    int code() {
        return this.code;
    }

    // `pos` is the offset of the first byte of the body: the length is in the four bytes before it
    // and the name in the two before those, which is where the format puts them (§4.7).
    public A readAttribute(AttributedElement enclosing, ClassReader cf, int pos) {
        int length = cf.readInt(pos - 4);
        if (length < 0 || pos + length > cf.classfileLength()) {
            throw new IllegalArgumentException(
                    "attribute " + this.name + " claims length " + length + " and does not fit");
        }
        Utf8Entry nameEntry = cf.readEntryOrNull(pos - 6, Utf8Entry.class);
        Attribute<?> a = AttributeReader.read(this.code, this, nameEntry, enclosing, cf, pos,
                length);
        return (A) a;
    }

    public void writeAttribute(BufWriter buf, A attr) {
        AttributeWriter.write(buf, attr);
    }

    public boolean allowMultiple() {
        return this.allowMultiple;
    }

    public AttributeStability stability() {
        return this.stability;
    }

    public String toString() {
        return "AttributeMapper[" + this.name + "]";
    }
}

// The custom-mapper function of a reader that has none registered. Registering them requires the
// `ClassFile.Option`s, which KajiLibrary does not implement; always returning `null` says exactly
// that, and there is no way for a custom attribute to be lost silently: without a mapper of its own
// it falls into the unknown-name mapper, which keeps the name and the bytes.
final class NoCustomAttributes implements Function<Utf8Entry, AttributeMapper<?>> {

    static final NoCustomAttributes INSTANCE = new NoCustomAttributes();

    public AttributeMapper<?> apply(Utf8Entry name) {
        return null;
    }
}
