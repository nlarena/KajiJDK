package java.lang.classfile;

import java.util.List;
import jdk.internal.classfile.impl.Annotations;

// Una anotación de tipo (JVMS §4.7.20, `type_annotation`). Es una {@link Annotation} más dos datos
// que dicen a qué tipo del programa se pega: el `target_info`, que nombra el sitio (el tercer
// parámetro formal, la segunda cota del primer parámetro de tipo, el `instanceof` del bci 27), y el
// `target_path`, que baja por dentro de ese tipo (el argumento de tipo del componente del arreglo).
//
// La parte incómoda del formato, y la razón de que `TargetInfo` sea una jerarquía y no una tupla, es
// que `target_info` es una unión discriminada por `target_type`: cada valor de la etiqueta cambia el
// tamaño y el significado de lo que sigue. Leerlo como si fuera fijo descoloca el resto del atributo.
public interface TypeAnnotation {

    /** Dónde se pega la anotación. */
    TargetInfo targetInfo();

    /** El camino por dentro del tipo, en el orden del archivo; vacío si se pega al tipo entero. */
    List<TypePathComponent> targetPath();

    /** La anotación en sí. */
    Annotation annotation();

    /** Una anotación de tipo con estas tres partes. */
    public static TypeAnnotation of(TargetInfo targetInfo, List<TypePathComponent> targetPath,
            Annotation annotation) {
        return Annotations.typeAnnotationOf(targetInfo, targetPath, annotation);
    }

    /**
     * El `target_info`: qué sitio del programa está anotado. Cada subtipo corresponde a una de las
     * diez formas que el formato define, y `targetType()` dice cuál de las veintidós etiquetas la
     * eligió — dos etiquetas distintas pueden compartir forma (`CLASS_TYPE_PARAMETER` y
     * `METHOD_TYPE_PARAMETER` son las dos un `TypeParameterTarget`).
     */
    public interface TargetInfo {

        /** `type_parameter_target` de una clase (§4.7.20.1). */
        public static final int TARGET_CLASS_TYPE_PARAMETER = 0x00;
        /** `type_parameter_target` de un método. */
        public static final int TARGET_METHOD_TYPE_PARAMETER = 0x01;
        /** `supertype_target`. */
        public static final int TARGET_CLASS_EXTENDS = 0x10;
        /** `type_parameter_bound_target` de una clase. */
        public static final int TARGET_CLASS_TYPE_PARAMETER_BOUND = 0x11;
        /** `type_parameter_bound_target` de un método. */
        public static final int TARGET_METHOD_TYPE_PARAMETER_BOUND = 0x12;
        /** `empty_target` en el tipo de un campo. */
        public static final int TARGET_FIELD = 0x13;
        /** `empty_target` en el tipo de retorno. */
        public static final int TARGET_METHOD_RETURN = 0x14;
        /** `empty_target` en el receptor. */
        public static final int TARGET_METHOD_RECEIVER = 0x15;
        /** `formal_parameter_target`. */
        public static final int TARGET_METHOD_FORMAL_PARAMETER = 0x16;
        /** `throws_target`. */
        public static final int TARGET_THROWS = 0x17;
        /** `localvar_target` de una variable local. */
        public static final int TARGET_LOCAL_VARIABLE = 0x40;
        /** `localvar_target` de un recurso de `try`. */
        public static final int TARGET_RESOURCE_VARIABLE = 0x41;
        /** `catch_target`. */
        public static final int TARGET_EXCEPTION_PARAMETER = 0x42;
        /** `offset_target` de un `instanceof`. */
        public static final int TARGET_INSTANCEOF = 0x43;
        /** `offset_target` de un `new`. */
        public static final int TARGET_NEW = 0x44;
        /** `offset_target` de una referencia a constructor. */
        public static final int TARGET_CONSTRUCTOR_REFERENCE = 0x45;
        /** `offset_target` de una referencia a método. */
        public static final int TARGET_METHOD_REFERENCE = 0x46;
        /** `type_argument_target` de un cast. */
        public static final int TARGET_CAST = 0x47;
        /** `type_argument_target` de una invocación de constructor. */
        public static final int TARGET_CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;
        /** `type_argument_target` de una invocación de método. */
        public static final int TARGET_METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;
        /** `type_argument_target` de una referencia a constructor. */
        public static final int TARGET_CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;
        /** `type_argument_target` de una referencia a método. */
        public static final int TARGET_METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

        /** La etiqueta que eligió esta forma. */
        TargetType targetType();

        /** Cuántos bytes ocupa, contando el byte de `target_type`. */
        default int size() {
            return targetType().sizeIfFixed() + 1;
        }

        /** Un `type_parameter_target` con esta etiqueta. */
        public static TypeParameterTarget ofTypeParameter(TargetType targetType,
                int typeParameterIndex) {
            return Annotations.typeParameterTarget(targetType, typeParameterIndex);
        }

        /** El parámetro de tipo número `typeParameterIndex` de la clase. */
        public static TypeParameterTarget ofClassTypeParameter(int typeParameterIndex) {
            return ofTypeParameter(TargetType.CLASS_TYPE_PARAMETER, typeParameterIndex);
        }

        /** El parámetro de tipo número `typeParameterIndex` del método. */
        public static TypeParameterTarget ofMethodTypeParameter(int typeParameterIndex) {
            return ofTypeParameter(TargetType.METHOD_TYPE_PARAMETER, typeParameterIndex);
        }

        /** El supertipo número `supertypeIndex`; 65535 es la superclase. */
        public static SupertypeTarget ofClassExtends(int supertypeIndex) {
            return Annotations.supertypeTarget(supertypeIndex);
        }

        /** Un `type_parameter_bound_target` con esta etiqueta. */
        public static TypeParameterBoundTarget ofTypeParameterBound(TargetType targetType,
                int typeParameterIndex, int boundIndex) {
            return Annotations.typeParameterBoundTarget(targetType, typeParameterIndex, boundIndex);
        }

        /** La cota `boundIndex` del parámetro `typeParameterIndex` de la clase. */
        public static TypeParameterBoundTarget ofClassTypeParameterBound(int typeParameterIndex,
                int boundIndex) {
            return ofTypeParameterBound(TargetType.CLASS_TYPE_PARAMETER_BOUND, typeParameterIndex,
                    boundIndex);
        }

        /** La cota `boundIndex` del parámetro `typeParameterIndex` del método. */
        public static TypeParameterBoundTarget ofMethodTypeParameterBound(int typeParameterIndex,
                int boundIndex) {
            return ofTypeParameterBound(TargetType.METHOD_TYPE_PARAMETER_BOUND, typeParameterIndex,
                    boundIndex);
        }

        /** Un `empty_target` con esta etiqueta. */
        public static EmptyTarget of(TargetType targetType) {
            return Annotations.emptyTarget(targetType);
        }

        /** El tipo de un campo. */
        public static EmptyTarget ofField() {
            return of(TargetType.FIELD);
        }

        /** El tipo de retorno de un método. */
        public static EmptyTarget ofMethodReturn() {
            return of(TargetType.METHOD_RETURN);
        }

        /** El receptor de un método. */
        public static EmptyTarget ofMethodReceiver() {
            return of(TargetType.METHOD_RECEIVER);
        }

        /** El parámetro formal número `formalParameterIndex`. */
        public static FormalParameterTarget ofMethodFormalParameter(int formalParameterIndex) {
            return Annotations.formalParameterTarget(formalParameterIndex);
        }

        /** La excepción número `throwsTargetIndex` de la cláusula `throws`. */
        public static ThrowsTarget ofThrows(int throwsTargetIndex) {
            return Annotations.throwsTarget(throwsTargetIndex);
        }

        /** Un `localvar_target` con esta etiqueta. */
        public static LocalVarTarget ofVariable(TargetType targetType,
                List<LocalVarTargetInfo> table) {
            return Annotations.localVarTarget(targetType, table);
        }

        /** Una variable local con este rango de vida. */
        public static LocalVarTarget ofLocalVariable(List<LocalVarTargetInfo> table) {
            return ofVariable(TargetType.LOCAL_VARIABLE, table);
        }

        /** Un recurso de `try` con este rango de vida. */
        public static LocalVarTarget ofResourceVariable(List<LocalVarTargetInfo> table) {
            return ofVariable(TargetType.RESOURCE_VARIABLE, table);
        }

        /** El manejador número `exceptionTableIndex` de la `exception_table`. */
        public static CatchTarget ofExceptionParameter(int exceptionTableIndex) {
            return Annotations.catchTarget(exceptionTableIndex);
        }

        /** Un `offset_target` con esta etiqueta. */
        public static OffsetTarget ofOffset(TargetType targetType, Label target) {
            return Annotations.offsetTarget(targetType, target);
        }

        /** El `instanceof` que está en `target`. */
        public static OffsetTarget ofInstanceofExpr(Label target) {
            return ofOffset(TargetType.INSTANCEOF, target);
        }

        /** El `new` que está en `target`. */
        public static OffsetTarget ofNewExpr(Label target) {
            return ofOffset(TargetType.NEW, target);
        }

        /** La referencia a constructor que está en `target`. */
        public static OffsetTarget ofConstructorReference(Label target) {
            return ofOffset(TargetType.CONSTRUCTOR_REFERENCE, target);
        }

        /** La referencia a método que está en `target`. */
        public static OffsetTarget ofMethodReference(Label target) {
            return ofOffset(TargetType.METHOD_REFERENCE, target);
        }

        /** Un `type_argument_target` con esta etiqueta. */
        public static TypeArgumentTarget ofTypeArgument(TargetType targetType, Label target,
                int typeArgumentIndex) {
            return Annotations.typeArgumentTarget(targetType, target, typeArgumentIndex);
        }

        /** El argumento de tipo `typeArgumentIndex` del cast que está en `target`. */
        public static TypeArgumentTarget ofCastExpr(Label target, int typeArgumentIndex) {
            return ofTypeArgument(TargetType.CAST, target, typeArgumentIndex);
        }

        /** El argumento de tipo de la invocación de constructor que está en `target`. */
        public static TypeArgumentTarget ofConstructorInvocationTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }

        /** El argumento de tipo de la invocación de método que está en `target`. */
        public static TypeArgumentTarget ofMethodInvocationTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.METHOD_INVOCATION_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }

        /** El argumento de tipo de la referencia a constructor que está en `target`. */
        public static TypeArgumentTarget ofConstructorReferenceTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }

        /** El argumento de tipo de la referencia a método que está en `target`. */
        public static TypeArgumentTarget ofMethodReferenceTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.METHOD_REFERENCE_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }
    }

    /**
     * Las veintidós etiquetas de `target_type`. `sizeIfFixed()` NO cuenta el byte de la etiqueta y
     * vale -1 en las dos formas de variable local, cuyo largo depende de la cantidad de rangos.
     */
    public enum TargetType {

        CLASS_TYPE_PARAMETER(0x00, 1),
        METHOD_TYPE_PARAMETER(0x01, 1),
        CLASS_EXTENDS(0x10, 2),
        CLASS_TYPE_PARAMETER_BOUND(0x11, 2),
        METHOD_TYPE_PARAMETER_BOUND(0x12, 2),
        FIELD(0x13, 0),
        METHOD_RETURN(0x14, 0),
        METHOD_RECEIVER(0x15, 0),
        METHOD_FORMAL_PARAMETER(0x16, 1),
        THROWS(0x17, 2),
        LOCAL_VARIABLE(0x40, -1),
        RESOURCE_VARIABLE(0x41, -1),
        EXCEPTION_PARAMETER(0x42, 2),
        INSTANCEOF(0x43, 2),
        NEW(0x44, 2),
        CONSTRUCTOR_REFERENCE(0x45, 2),
        METHOD_REFERENCE(0x46, 2),
        CAST(0x47, 3),
        CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT(0x48, 3),
        METHOD_INVOCATION_TYPE_ARGUMENT(0x49, 3),
        CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT(0x4A, 3),
        METHOD_REFERENCE_TYPE_ARGUMENT(0x4B, 3);

        private final int valor;
        private final int tamanio;

        private TargetType(int valor, int tamanio) {
            this.valor = valor;
            this.tamanio = tamanio;
        }

        /** El byte `target_type`. */
        public int targetTypeValue() {
            return this.valor;
        }

        /** El largo del `target_info` sin la etiqueta, o -1 si depende del contenido. */
        public int sizeIfFixed() {
            return this.tamanio;
        }
    }

    /** Un `type_parameter_target`. */
    public interface TypeParameterTarget extends TargetInfo {

        /** El índice del parámetro de tipo. */
        int typeParameterIndex();
    }

    /** Un `supertype_target`: 65535 nombra a la superclase, y el resto a una interfaz. */
    public interface SupertypeTarget extends TargetInfo {

        /** El índice del supertipo. */
        int supertypeIndex();
    }

    /** Un `type_parameter_bound_target`. */
    public interface TypeParameterBoundTarget extends TargetInfo {

        /** El índice del parámetro de tipo. */
        int typeParameterIndex();

        /** El índice de la cota dentro de ese parámetro. */
        int boundIndex();
    }

    /** Un `empty_target`: la etiqueta ya dice todo y no hay más bytes. */
    public interface EmptyTarget extends TargetInfo {
    }

    /** Un `formal_parameter_target`. */
    public interface FormalParameterTarget extends TargetInfo {

        /** El índice del parámetro formal, contando desde 0 y sin el receptor. */
        int formalParameterIndex();
    }

    /** Un `throws_target`. */
    public interface ThrowsTarget extends TargetInfo {

        /** El índice dentro de la tabla del atributo `Exceptions`. */
        int throwsTargetIndex();
    }

    /** Un `localvar_target`: una variable puede tener varios rangos de vida disjuntos. */
    public interface LocalVarTarget extends TargetInfo {

        /** Los rangos, en el orden del archivo. */
        List<LocalVarTargetInfo> table();

        /** Tres bytes de cabecera y seis por rango. */
        default int size() {
            return 3 + table().size() * 6;
        }
    }

    /** Una fila de la tabla de un `localvar_target`. */
    public interface LocalVarTargetInfo {

        /** Dónde empieza el rango de vida. */
        Label startLabel();

        /** Dónde termina, sin incluirlo. */
        Label endLabel();

        /** La ranura de variable local. */
        int index();

        /** Una fila con estos valores. */
        public static LocalVarTargetInfo of(Label startLabel, Label endLabel, int index) {
            return Annotations.localVarTargetInfo(startLabel, endLabel, index);
        }
    }

    /** Un `catch_target`. */
    public interface CatchTarget extends TargetInfo {

        /** El índice dentro de la `exception_table` del atributo `Code`. */
        int exceptionTableIndex();
    }

    /** Un `offset_target`: apunta a un bci del arreglo `code`. */
    public interface OffsetTarget extends TargetInfo {

        /** El bci de la instrucción anotada. */
        Label target();
    }

    /** Un `type_argument_target`. */
    public interface TypeArgumentTarget extends TargetInfo {

        /** El bci de la instrucción anotada. */
        Label target();

        /** Cuál de los argumentos de tipo de esa expresión. */
        int typeArgumentIndex();
    }

    /**
     * Un paso del `target_path` (§4.7.20.2). Cada paso baja un nivel dentro del tipo: al componente
     * de un arreglo, al tipo interno, a la cota de un comodín, o a un argumento de tipo.
     */
    public interface TypePathComponent {

        /** Bajar al componente de un arreglo. */
        public static final TypePathComponent ARRAY = of(Kind.ARRAY, 0);
        /** Bajar al tipo anidado. */
        public static final TypePathComponent INNER_TYPE = of(Kind.INNER_TYPE, 0);
        /** Bajar a la cota de un comodín. */
        public static final TypePathComponent WILDCARD = of(Kind.WILDCARD, 0);

        /** Qué clase de paso es. */
        Kind typePathKind();

        /** Cuál argumento de tipo, si el paso es `TYPE_ARGUMENT`; 0 en los otros tres. */
        int typeArgumentIndex();

        /** Un paso con esta clase y este índice. */
        public static TypePathComponent of(Kind typePathKind, int typeArgumentIndex) {
            return Annotations.typePathComponent(typePathKind, typeArgumentIndex);
        }

        /** Las cuatro clases de paso, con el `type_path_kind` que el formato les da. */
        public enum Kind {

            ARRAY(0),
            INNER_TYPE(1),
            WILDCARD(2),
            TYPE_ARGUMENT(3);

            private final int tag;

            private Kind(int tag) {
                this.tag = tag;
            }

            /** El `type_path_kind`. */
            public int tag() {
                return this.tag;
            }
        }
    }
}
