package java.lang.classfile;

import java.lang.classfile.AttributeMapper.AttributeStability;
import jdk.internal.classfile.impl.AttributeMapperImpl;
import jdk.internal.classfile.impl.RawAttribute;

// The register of the attributes the JVMS defines (§4.7): their name and the mapper reading and
// writing them. A reader looks here by name; what it does not find is a custom attribute.
//
// SCOPE, and it is this package's largest divergence: in the JDK each mapper produces a TYPED
// attribute --`CodeAttribute`, `SourceFileAttribute`, ...-- of the `java.lang.classfile.attribute`
// package. Those interfaces do exist in KajiLibrary, but nothing here builds them: every mapper
// produces a {@link jdk.internal.classfile.impl.RawAttribute} instead -- the right name, the right
// pool entry, the body in bytes, and nothing else. It is a subset --a `SourceFile`'s `sourceFile()`
// cannot be asked for-- but it is not a lie: the attribute it returns IS that attribute, with its
// bytes. That is why the methods here return `AttributeMapper<RawAttribute>` instead of the JDK's
// type; the erased type, which is what the meter compares, is the same.
//
// These mappers' `writeAttribute` does work: it writes name, length and body just as they were read,
// which is what is needed to copy an attribute from one file to another without understanding it.
public final class Attributes {

    private Attributes() {
    }

    /** The `AnnotationDefault` attribute's name. */
    public static final String NAME_ANNOTATION_DEFAULT = "AnnotationDefault";

    /** The `BootstrapMethods` attribute's name. */
    public static final String NAME_BOOTSTRAP_METHODS = "BootstrapMethods";

    /** The `CharacterRangeTable` attribute's name. */
    public static final String NAME_CHARACTER_RANGE_TABLE = "CharacterRangeTable";

    /** The `Code` attribute's name. */
    public static final String NAME_CODE = "Code";

    /** The `CompilationID` attribute's name. */
    public static final String NAME_COMPILATION_ID = "CompilationID";

    /** The `ConstantValue` attribute's name. */
    public static final String NAME_CONSTANT_VALUE = "ConstantValue";

    /** The `Deprecated` attribute's name. */
    public static final String NAME_DEPRECATED = "Deprecated";

    /** The `EnclosingMethod` attribute's name. */
    public static final String NAME_ENCLOSING_METHOD = "EnclosingMethod";

    /** The `Exceptions` attribute's name. */
    public static final String NAME_EXCEPTIONS = "Exceptions";

    /** The `InnerClasses` attribute's name. */
    public static final String NAME_INNER_CLASSES = "InnerClasses";

    /** The `LineNumberTable` attribute's name. */
    public static final String NAME_LINE_NUMBER_TABLE = "LineNumberTable";

    /** The `LocalVariableTable` attribute's name. */
    public static final String NAME_LOCAL_VARIABLE_TABLE = "LocalVariableTable";

    /** The `LocalVariableTypeTable` attribute's name. */
    public static final String NAME_LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable";

    /** The `MethodParameters` attribute's name. */
    public static final String NAME_METHOD_PARAMETERS = "MethodParameters";

    /** The `Module` attribute's name. */
    public static final String NAME_MODULE = "Module";

    /** The `ModuleHashes` attribute's name. */
    public static final String NAME_MODULE_HASHES = "ModuleHashes";

    /** The `ModuleMainClass` attribute's name. */
    public static final String NAME_MODULE_MAIN_CLASS = "ModuleMainClass";

    /** The `ModulePackages` attribute's name. */
    public static final String NAME_MODULE_PACKAGES = "ModulePackages";

    /** The `ModuleResolution` attribute's name. */
    public static final String NAME_MODULE_RESOLUTION = "ModuleResolution";

    /** The `ModuleTarget` attribute's name. */
    public static final String NAME_MODULE_TARGET = "ModuleTarget";

    /** The `NestHost` attribute's name. */
    public static final String NAME_NEST_HOST = "NestHost";

    /** The `NestMembers` attribute's name. */
    public static final String NAME_NEST_MEMBERS = "NestMembers";

    /** The `PermittedSubclasses` attribute's name. */
    public static final String NAME_PERMITTED_SUBCLASSES = "PermittedSubclasses";

    /** The `Record` attribute's name. */
    public static final String NAME_RECORD = "Record";

    /** The `RuntimeInvisibleAnnotations` attribute's name. */
    public static final String NAME_RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";

    /** The `RuntimeInvisibleParameterAnnotations` attribute's name. */
    public static final String NAME_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations";

    /** The `RuntimeInvisibleTypeAnnotations` attribute's name. */
    public static final String NAME_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations";

    /** The `RuntimeVisibleAnnotations` attribute's name. */
    public static final String NAME_RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";

    /** The `RuntimeVisibleParameterAnnotations` attribute's name. */
    public static final String NAME_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations";

    /** The `RuntimeVisibleTypeAnnotations` attribute's name. */
    public static final String NAME_RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations";

    /** The `Signature` attribute's name. */
    public static final String NAME_SIGNATURE = "Signature";

    /** The `SourceDebugExtension` attribute's name. */
    public static final String NAME_SOURCE_DEBUG_EXTENSION = "SourceDebugExtension";

    /** The `SourceFile` attribute's name. */
    public static final String NAME_SOURCE_FILE = "SourceFile";

    /** The `SourceID` attribute's name. */
    public static final String NAME_SOURCE_ID = "SourceID";

    /** The `StackMapTable` attribute's name. */
    public static final String NAME_STACK_MAP_TABLE = "StackMapTable";

    /** The `Synthetic` attribute's name. */
    public static final String NAME_SYNTHETIC = "Synthetic";


    /** `AnnotationDefault`'s mapper. */
    public static AttributeMapper<RawAttribute> annotationDefault() {
        return M_ANNOTATION_DEFAULT;
    }

    /** `BootstrapMethods`'s mapper. */
    public static AttributeMapper<RawAttribute> bootstrapMethods() {
        return M_BOOTSTRAP_METHODS;
    }

    /** `CharacterRangeTable`'s mapper. */
    public static AttributeMapper<RawAttribute> characterRangeTable() {
        return M_CHARACTER_RANGE_TABLE;
    }

    /** `Code`'s mapper. */
    public static AttributeMapper<RawAttribute> code() {
        return M_CODE;
    }

    /** `CompilationID`'s mapper. */
    public static AttributeMapper<RawAttribute> compilationId() {
        return M_COMPILATION_ID;
    }

    /** `ConstantValue`'s mapper. */
    public static AttributeMapper<RawAttribute> constantValue() {
        return M_CONSTANT_VALUE;
    }

    /** `Deprecated`'s mapper. */
    public static AttributeMapper<RawAttribute> deprecated() {
        return M_DEPRECATED;
    }

    /** `EnclosingMethod`'s mapper. */
    public static AttributeMapper<RawAttribute> enclosingMethod() {
        return M_ENCLOSING_METHOD;
    }

    /** `Exceptions`'s mapper. */
    public static AttributeMapper<RawAttribute> exceptions() {
        return M_EXCEPTIONS;
    }

    /** `InnerClasses`'s mapper. */
    public static AttributeMapper<RawAttribute> innerClasses() {
        return M_INNER_CLASSES;
    }

    /** `LineNumberTable`'s mapper. */
    public static AttributeMapper<RawAttribute> lineNumberTable() {
        return M_LINE_NUMBER_TABLE;
    }

    /** `LocalVariableTable`'s mapper. */
    public static AttributeMapper<RawAttribute> localVariableTable() {
        return M_LOCAL_VARIABLE_TABLE;
    }

    /** `LocalVariableTypeTable`'s mapper. */
    public static AttributeMapper<RawAttribute> localVariableTypeTable() {
        return M_LOCAL_VARIABLE_TYPE_TABLE;
    }

    /** `MethodParameters`'s mapper. */
    public static AttributeMapper<RawAttribute> methodParameters() {
        return M_METHOD_PARAMETERS;
    }

    /** `Module`'s mapper. */
    public static AttributeMapper<RawAttribute> module() {
        return M_MODULE;
    }

    /** `ModuleHashes`'s mapper. */
    public static AttributeMapper<RawAttribute> moduleHashes() {
        return M_MODULE_HASHES;
    }

    /** `ModuleMainClass`'s mapper. */
    public static AttributeMapper<RawAttribute> moduleMainClass() {
        return M_MODULE_MAIN_CLASS;
    }

    /** `ModulePackages`'s mapper. */
    public static AttributeMapper<RawAttribute> modulePackages() {
        return M_MODULE_PACKAGES;
    }

    /** `ModuleResolution`'s mapper. */
    public static AttributeMapper<RawAttribute> moduleResolution() {
        return M_MODULE_RESOLUTION;
    }

    /** `ModuleTarget`'s mapper. */
    public static AttributeMapper<RawAttribute> moduleTarget() {
        return M_MODULE_TARGET;
    }

    /** `NestHost`'s mapper. */
    public static AttributeMapper<RawAttribute> nestHost() {
        return M_NEST_HOST;
    }

    /** `NestMembers`'s mapper. */
    public static AttributeMapper<RawAttribute> nestMembers() {
        return M_NEST_MEMBERS;
    }

    /** `PermittedSubclasses`'s mapper. */
    public static AttributeMapper<RawAttribute> permittedSubclasses() {
        return M_PERMITTED_SUBCLASSES;
    }

    /** `Record`'s mapper. */
    public static AttributeMapper<RawAttribute> record() {
        return M_RECORD;
    }

    /** `RuntimeInvisibleAnnotations`'s mapper. */
    public static AttributeMapper<RawAttribute> runtimeInvisibleAnnotations() {
        return M_RUNTIME_INVISIBLE_ANNOTATIONS;
    }

    /** `RuntimeInvisibleParameterAnnotations`'s mapper. */
    public static AttributeMapper<RawAttribute> runtimeInvisibleParameterAnnotations() {
        return M_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS;
    }

    /** `RuntimeInvisibleTypeAnnotations`'s mapper. */
    public static AttributeMapper<RawAttribute> runtimeInvisibleTypeAnnotations() {
        return M_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS;
    }

    /** `RuntimeVisibleAnnotations`'s mapper. */
    public static AttributeMapper<RawAttribute> runtimeVisibleAnnotations() {
        return M_RUNTIME_VISIBLE_ANNOTATIONS;
    }

    /** `RuntimeVisibleParameterAnnotations`'s mapper. */
    public static AttributeMapper<RawAttribute> runtimeVisibleParameterAnnotations() {
        return M_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS;
    }

    /** `RuntimeVisibleTypeAnnotations`'s mapper. */
    public static AttributeMapper<RawAttribute> runtimeVisibleTypeAnnotations() {
        return M_RUNTIME_VISIBLE_TYPE_ANNOTATIONS;
    }

    /** `Signature`'s mapper. */
    public static AttributeMapper<RawAttribute> signature() {
        return M_SIGNATURE;
    }

    /** `SourceDebugExtension`'s mapper. */
    public static AttributeMapper<RawAttribute> sourceDebugExtension() {
        return M_SOURCE_DEBUG_EXTENSION;
    }

    /** `SourceFile`'s mapper. */
    public static AttributeMapper<RawAttribute> sourceFile() {
        return M_SOURCE_FILE;
    }

    /** `SourceID`'s mapper. */
    public static AttributeMapper<RawAttribute> sourceId() {
        return M_SOURCE_ID;
    }

    /** `StackMapTable`'s mapper. */
    public static AttributeMapper<RawAttribute> stackMapTable() {
        return M_STACK_MAP_TABLE;
    }

    /** `Synthetic`'s mapper. */
    public static AttributeMapper<RawAttribute> synthetic() {
        return M_SYNTHETIC;
    }

    // The mappers, one per attribute. They are constants: each one's identity is what
    // `findAttribute` compares, so they have to be the same object every time.
    private static final AttributeMapper<RawAttribute> M_ANNOTATION_DEFAULT =
            new AttributeMapperImpl(NAME_ANNOTATION_DEFAULT, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_BOOTSTRAP_METHODS =
            new AttributeMapperImpl(NAME_BOOTSTRAP_METHODS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_CHARACTER_RANGE_TABLE =
            new AttributeMapperImpl(NAME_CHARACTER_RANGE_TABLE, AttributeStability.LABELS, true);
    private static final AttributeMapper<RawAttribute> M_CODE =
            new AttributeMapperImpl(NAME_CODE, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_COMPILATION_ID =
            new AttributeMapperImpl(NAME_COMPILATION_ID, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_CONSTANT_VALUE =
            new AttributeMapperImpl(NAME_CONSTANT_VALUE, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_DEPRECATED =
            new AttributeMapperImpl(NAME_DEPRECATED, AttributeStability.STATELESS, true);
    private static final AttributeMapper<RawAttribute> M_ENCLOSING_METHOD =
            new AttributeMapperImpl(NAME_ENCLOSING_METHOD, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_EXCEPTIONS =
            new AttributeMapperImpl(NAME_EXCEPTIONS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_INNER_CLASSES =
            new AttributeMapperImpl(NAME_INNER_CLASSES, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_LINE_NUMBER_TABLE =
            new AttributeMapperImpl(NAME_LINE_NUMBER_TABLE, AttributeStability.LABELS, true);
    private static final AttributeMapper<RawAttribute> M_LOCAL_VARIABLE_TABLE =
            new AttributeMapperImpl(NAME_LOCAL_VARIABLE_TABLE, AttributeStability.LABELS, true);
    private static final AttributeMapper<RawAttribute> M_LOCAL_VARIABLE_TYPE_TABLE =
            new AttributeMapperImpl(NAME_LOCAL_VARIABLE_TYPE_TABLE, AttributeStability.LABELS, true);
    private static final AttributeMapper<RawAttribute> M_METHOD_PARAMETERS =
            new AttributeMapperImpl(NAME_METHOD_PARAMETERS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_MODULE =
            new AttributeMapperImpl(NAME_MODULE, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_MODULE_HASHES =
            new AttributeMapperImpl(NAME_MODULE_HASHES, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_MODULE_MAIN_CLASS =
            new AttributeMapperImpl(NAME_MODULE_MAIN_CLASS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_MODULE_PACKAGES =
            new AttributeMapperImpl(NAME_MODULE_PACKAGES, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_MODULE_RESOLUTION =
            new AttributeMapperImpl(NAME_MODULE_RESOLUTION, AttributeStability.STATELESS, false);
    private static final AttributeMapper<RawAttribute> M_MODULE_TARGET =
            new AttributeMapperImpl(NAME_MODULE_TARGET, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_NEST_HOST =
            new AttributeMapperImpl(NAME_NEST_HOST, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_NEST_MEMBERS =
            new AttributeMapperImpl(NAME_NEST_MEMBERS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_PERMITTED_SUBCLASSES =
            new AttributeMapperImpl(NAME_PERMITTED_SUBCLASSES, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_RECORD =
            new AttributeMapperImpl(NAME_RECORD, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_RUNTIME_INVISIBLE_ANNOTATIONS =
            new AttributeMapperImpl(NAME_RUNTIME_INVISIBLE_ANNOTATIONS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS =
            new AttributeMapperImpl(NAME_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS =
            new AttributeMapperImpl(NAME_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS, AttributeStability.UNSTABLE, false);
    private static final AttributeMapper<RawAttribute> M_RUNTIME_VISIBLE_ANNOTATIONS =
            new AttributeMapperImpl(NAME_RUNTIME_VISIBLE_ANNOTATIONS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS =
            new AttributeMapperImpl(NAME_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_RUNTIME_VISIBLE_TYPE_ANNOTATIONS =
            new AttributeMapperImpl(NAME_RUNTIME_VISIBLE_TYPE_ANNOTATIONS, AttributeStability.UNSTABLE, false);
    private static final AttributeMapper<RawAttribute> M_SIGNATURE =
            new AttributeMapperImpl(NAME_SIGNATURE, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_SOURCE_DEBUG_EXTENSION =
            new AttributeMapperImpl(NAME_SOURCE_DEBUG_EXTENSION, AttributeStability.STATELESS, false);
    private static final AttributeMapper<RawAttribute> M_SOURCE_FILE =
            new AttributeMapperImpl(NAME_SOURCE_FILE, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_SOURCE_ID =
            new AttributeMapperImpl(NAME_SOURCE_ID, AttributeStability.CP_REFS, false);
    private static final AttributeMapper<RawAttribute> M_STACK_MAP_TABLE =
            new AttributeMapperImpl(NAME_STACK_MAP_TABLE, AttributeStability.LABELS, false);
    private static final AttributeMapper<RawAttribute> M_SYNTHETIC =
            new AttributeMapperImpl(NAME_SYNTHETIC, AttributeStability.STATELESS, true);
}
