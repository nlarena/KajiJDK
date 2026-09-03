package java.lang.classfile;

import java.lang.classfile.AttributeMapper.AttributeStability;
import jdk.internal.classfile.impl.AttributeMapperImpl;
import jdk.internal.classfile.impl.RawAttribute;

// El registro de los atributos que el JVMS define (§4.7): su nombre y el mapeador que los lee y los
// escribe. Un lector busca acá por nombre; lo que no encuentra es un atributo a medida.
//
// ALCANCE, y es la divergencia más grande de este paquete: en el JDK cada mapeador produce un
// atributo TIPADO —`CodeAttribute`, `SourceFileAttribute`, …— del paquete
// `java.lang.classfile.attribute`, que KajiLibrary no tiene. Acá todos producen un
// {@link jdk.internal.classfile.impl.RawAttribute}: el nombre correcto, la entrada de pool correcta,
// el cuerpo en bytes, y nada más. Es un subconjunto —no se puede preguntar por el `sourceFile()` de
// un `SourceFile`— pero no es una mentira: el atributo que devuelve ES ese atributo, con sus bytes.
// Por eso los métodos de acá devuelven `AttributeMapper<RawAttribute>` en vez del tipo del JDK; el
// tipo borrado, que es lo que compara el medidor, es el mismo.
//
// `writeAttribute` de estos mapeadores sí funciona: escribe nombre, largo y cuerpo tal cual se
// leyeron, que es lo que hace falta para copiar un atributo de un archivo a otro sin entenderlo.
public final class Attributes {

    private Attributes() {
    }

    /** El nombre del atributo `AnnotationDefault`. */
    public static final String NAME_ANNOTATION_DEFAULT = "AnnotationDefault";

    /** El nombre del atributo `BootstrapMethods`. */
    public static final String NAME_BOOTSTRAP_METHODS = "BootstrapMethods";

    /** El nombre del atributo `CharacterRangeTable`. */
    public static final String NAME_CHARACTER_RANGE_TABLE = "CharacterRangeTable";

    /** El nombre del atributo `Code`. */
    public static final String NAME_CODE = "Code";

    /** El nombre del atributo `CompilationID`. */
    public static final String NAME_COMPILATION_ID = "CompilationID";

    /** El nombre del atributo `ConstantValue`. */
    public static final String NAME_CONSTANT_VALUE = "ConstantValue";

    /** El nombre del atributo `Deprecated`. */
    public static final String NAME_DEPRECATED = "Deprecated";

    /** El nombre del atributo `EnclosingMethod`. */
    public static final String NAME_ENCLOSING_METHOD = "EnclosingMethod";

    /** El nombre del atributo `Exceptions`. */
    public static final String NAME_EXCEPTIONS = "Exceptions";

    /** El nombre del atributo `InnerClasses`. */
    public static final String NAME_INNER_CLASSES = "InnerClasses";

    /** El nombre del atributo `LineNumberTable`. */
    public static final String NAME_LINE_NUMBER_TABLE = "LineNumberTable";

    /** El nombre del atributo `LocalVariableTable`. */
    public static final String NAME_LOCAL_VARIABLE_TABLE = "LocalVariableTable";

    /** El nombre del atributo `LocalVariableTypeTable`. */
    public static final String NAME_LOCAL_VARIABLE_TYPE_TABLE = "LocalVariableTypeTable";

    /** El nombre del atributo `MethodParameters`. */
    public static final String NAME_METHOD_PARAMETERS = "MethodParameters";

    /** El nombre del atributo `Module`. */
    public static final String NAME_MODULE = "Module";

    /** El nombre del atributo `ModuleHashes`. */
    public static final String NAME_MODULE_HASHES = "ModuleHashes";

    /** El nombre del atributo `ModuleMainClass`. */
    public static final String NAME_MODULE_MAIN_CLASS = "ModuleMainClass";

    /** El nombre del atributo `ModulePackages`. */
    public static final String NAME_MODULE_PACKAGES = "ModulePackages";

    /** El nombre del atributo `ModuleResolution`. */
    public static final String NAME_MODULE_RESOLUTION = "ModuleResolution";

    /** El nombre del atributo `ModuleTarget`. */
    public static final String NAME_MODULE_TARGET = "ModuleTarget";

    /** El nombre del atributo `NestHost`. */
    public static final String NAME_NEST_HOST = "NestHost";

    /** El nombre del atributo `NestMembers`. */
    public static final String NAME_NEST_MEMBERS = "NestMembers";

    /** El nombre del atributo `PermittedSubclasses`. */
    public static final String NAME_PERMITTED_SUBCLASSES = "PermittedSubclasses";

    /** El nombre del atributo `Record`. */
    public static final String NAME_RECORD = "Record";

    /** El nombre del atributo `RuntimeInvisibleAnnotations`. */
    public static final String NAME_RUNTIME_INVISIBLE_ANNOTATIONS = "RuntimeInvisibleAnnotations";

    /** El nombre del atributo `RuntimeInvisibleParameterAnnotations`. */
    public static final String NAME_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS = "RuntimeInvisibleParameterAnnotations";

    /** El nombre del atributo `RuntimeInvisibleTypeAnnotations`. */
    public static final String NAME_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS = "RuntimeInvisibleTypeAnnotations";

    /** El nombre del atributo `RuntimeVisibleAnnotations`. */
    public static final String NAME_RUNTIME_VISIBLE_ANNOTATIONS = "RuntimeVisibleAnnotations";

    /** El nombre del atributo `RuntimeVisibleParameterAnnotations`. */
    public static final String NAME_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS = "RuntimeVisibleParameterAnnotations";

    /** El nombre del atributo `RuntimeVisibleTypeAnnotations`. */
    public static final String NAME_RUNTIME_VISIBLE_TYPE_ANNOTATIONS = "RuntimeVisibleTypeAnnotations";

    /** El nombre del atributo `Signature`. */
    public static final String NAME_SIGNATURE = "Signature";

    /** El nombre del atributo `SourceDebugExtension`. */
    public static final String NAME_SOURCE_DEBUG_EXTENSION = "SourceDebugExtension";

    /** El nombre del atributo `SourceFile`. */
    public static final String NAME_SOURCE_FILE = "SourceFile";

    /** El nombre del atributo `SourceID`. */
    public static final String NAME_SOURCE_ID = "SourceID";

    /** El nombre del atributo `StackMapTable`. */
    public static final String NAME_STACK_MAP_TABLE = "StackMapTable";

    /** El nombre del atributo `Synthetic`. */
    public static final String NAME_SYNTHETIC = "Synthetic";


    /** El mapeador de `AnnotationDefault`. */
    public static AttributeMapper<RawAttribute> annotationDefault() {
        return M_ANNOTATION_DEFAULT;
    }

    /** El mapeador de `BootstrapMethods`. */
    public static AttributeMapper<RawAttribute> bootstrapMethods() {
        return M_BOOTSTRAP_METHODS;
    }

    /** El mapeador de `CharacterRangeTable`. */
    public static AttributeMapper<RawAttribute> characterRangeTable() {
        return M_CHARACTER_RANGE_TABLE;
    }

    /** El mapeador de `Code`. */
    public static AttributeMapper<RawAttribute> code() {
        return M_CODE;
    }

    /** El mapeador de `CompilationID`. */
    public static AttributeMapper<RawAttribute> compilationId() {
        return M_COMPILATION_ID;
    }

    /** El mapeador de `ConstantValue`. */
    public static AttributeMapper<RawAttribute> constantValue() {
        return M_CONSTANT_VALUE;
    }

    /** El mapeador de `Deprecated`. */
    public static AttributeMapper<RawAttribute> deprecated() {
        return M_DEPRECATED;
    }

    /** El mapeador de `EnclosingMethod`. */
    public static AttributeMapper<RawAttribute> enclosingMethod() {
        return M_ENCLOSING_METHOD;
    }

    /** El mapeador de `Exceptions`. */
    public static AttributeMapper<RawAttribute> exceptions() {
        return M_EXCEPTIONS;
    }

    /** El mapeador de `InnerClasses`. */
    public static AttributeMapper<RawAttribute> innerClasses() {
        return M_INNER_CLASSES;
    }

    /** El mapeador de `LineNumberTable`. */
    public static AttributeMapper<RawAttribute> lineNumberTable() {
        return M_LINE_NUMBER_TABLE;
    }

    /** El mapeador de `LocalVariableTable`. */
    public static AttributeMapper<RawAttribute> localVariableTable() {
        return M_LOCAL_VARIABLE_TABLE;
    }

    /** El mapeador de `LocalVariableTypeTable`. */
    public static AttributeMapper<RawAttribute> localVariableTypeTable() {
        return M_LOCAL_VARIABLE_TYPE_TABLE;
    }

    /** El mapeador de `MethodParameters`. */
    public static AttributeMapper<RawAttribute> methodParameters() {
        return M_METHOD_PARAMETERS;
    }

    /** El mapeador de `Module`. */
    public static AttributeMapper<RawAttribute> module() {
        return M_MODULE;
    }

    /** El mapeador de `ModuleHashes`. */
    public static AttributeMapper<RawAttribute> moduleHashes() {
        return M_MODULE_HASHES;
    }

    /** El mapeador de `ModuleMainClass`. */
    public static AttributeMapper<RawAttribute> moduleMainClass() {
        return M_MODULE_MAIN_CLASS;
    }

    /** El mapeador de `ModulePackages`. */
    public static AttributeMapper<RawAttribute> modulePackages() {
        return M_MODULE_PACKAGES;
    }

    /** El mapeador de `ModuleResolution`. */
    public static AttributeMapper<RawAttribute> moduleResolution() {
        return M_MODULE_RESOLUTION;
    }

    /** El mapeador de `ModuleTarget`. */
    public static AttributeMapper<RawAttribute> moduleTarget() {
        return M_MODULE_TARGET;
    }

    /** El mapeador de `NestHost`. */
    public static AttributeMapper<RawAttribute> nestHost() {
        return M_NEST_HOST;
    }

    /** El mapeador de `NestMembers`. */
    public static AttributeMapper<RawAttribute> nestMembers() {
        return M_NEST_MEMBERS;
    }

    /** El mapeador de `PermittedSubclasses`. */
    public static AttributeMapper<RawAttribute> permittedSubclasses() {
        return M_PERMITTED_SUBCLASSES;
    }

    /** El mapeador de `Record`. */
    public static AttributeMapper<RawAttribute> record() {
        return M_RECORD;
    }

    /** El mapeador de `RuntimeInvisibleAnnotations`. */
    public static AttributeMapper<RawAttribute> runtimeInvisibleAnnotations() {
        return M_RUNTIME_INVISIBLE_ANNOTATIONS;
    }

    /** El mapeador de `RuntimeInvisibleParameterAnnotations`. */
    public static AttributeMapper<RawAttribute> runtimeInvisibleParameterAnnotations() {
        return M_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS;
    }

    /** El mapeador de `RuntimeInvisibleTypeAnnotations`. */
    public static AttributeMapper<RawAttribute> runtimeInvisibleTypeAnnotations() {
        return M_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS;
    }

    /** El mapeador de `RuntimeVisibleAnnotations`. */
    public static AttributeMapper<RawAttribute> runtimeVisibleAnnotations() {
        return M_RUNTIME_VISIBLE_ANNOTATIONS;
    }

    /** El mapeador de `RuntimeVisibleParameterAnnotations`. */
    public static AttributeMapper<RawAttribute> runtimeVisibleParameterAnnotations() {
        return M_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS;
    }

    /** El mapeador de `RuntimeVisibleTypeAnnotations`. */
    public static AttributeMapper<RawAttribute> runtimeVisibleTypeAnnotations() {
        return M_RUNTIME_VISIBLE_TYPE_ANNOTATIONS;
    }

    /** El mapeador de `Signature`. */
    public static AttributeMapper<RawAttribute> signature() {
        return M_SIGNATURE;
    }

    /** El mapeador de `SourceDebugExtension`. */
    public static AttributeMapper<RawAttribute> sourceDebugExtension() {
        return M_SOURCE_DEBUG_EXTENSION;
    }

    /** El mapeador de `SourceFile`. */
    public static AttributeMapper<RawAttribute> sourceFile() {
        return M_SOURCE_FILE;
    }

    /** El mapeador de `SourceID`. */
    public static AttributeMapper<RawAttribute> sourceId() {
        return M_SOURCE_ID;
    }

    /** El mapeador de `StackMapTable`. */
    public static AttributeMapper<RawAttribute> stackMapTable() {
        return M_STACK_MAP_TABLE;
    }

    /** El mapeador de `Synthetic`. */
    public static AttributeMapper<RawAttribute> synthetic() {
        return M_SYNTHETIC;
    }

    // Los mapeadores, uno por atributo. Son constantes: la identidad de cada uno es lo que
    // `findAttribute` compara, así que tienen que ser siempre el mismo objeto.
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
