package java.lang.classfile;

import java.util.List;
import jdk.internal.classfile.impl.Annotations;

// A type annotation (JVMS §4.7.20, `type_annotation`). It is an {@link Annotation} plus two pieces of
// data saying which type of the program it sticks to: the `target_info`, naming the site (the third
// formal parameter, the second bound of the first type parameter, the `instanceof` at bci 27), and
// the `target_path`, which goes down inside that type (the array component's type argument).
//
// The format's awkward part, and the reason `TargetInfo` is a hierarchy and not a tuple, is that
// `target_info` is a union discriminated by `target_type`: each value of the tag changes the size and
// the meaning of what follows. Reading it as though it were fixed throws the rest of the attribute out
// of place.
public interface TypeAnnotation {

    /** Where the annotation sticks. */
    TargetInfo targetInfo();

    /** The path inside the type, in file order; empty if it sticks to the whole type. */
    List<TypePathComponent> targetPath();

    /** The annotation itself. */
    Annotation annotation();

    /** A type annotation with these three parts. */
    public static TypeAnnotation of(TargetInfo targetInfo, List<TypePathComponent> targetPath,
            Annotation annotation) {
        return Annotations.typeAnnotationOf(targetInfo, targetPath, annotation);
    }

    /**
     * The `target_info`: which site of the program is annotated. Each subtype corresponds to one of
     * the ten forms the format defines, and `targetType()` says which of the twenty-two tags chose it
     * -- two different tags can share a form (`CLASS_TYPE_PARAMETER` and `METHOD_TYPE_PARAMETER` are
     * both a `TypeParameterTarget`).
     */
    public interface TargetInfo {

        /** A class's `type_parameter_target` (§4.7.20.1). */
        public static final int TARGET_CLASS_TYPE_PARAMETER = 0x00;
        /** A method's `type_parameter_target`. */
        public static final int TARGET_METHOD_TYPE_PARAMETER = 0x01;
        /** `supertype_target`. */
        public static final int TARGET_CLASS_EXTENDS = 0x10;
        /** A class's `type_parameter_bound_target`. */
        public static final int TARGET_CLASS_TYPE_PARAMETER_BOUND = 0x11;
        /** A method's `type_parameter_bound_target`. */
        public static final int TARGET_METHOD_TYPE_PARAMETER_BOUND = 0x12;
        /** `empty_target` on a field's type. */
        public static final int TARGET_FIELD = 0x13;
        /** `empty_target` on the return type. */
        public static final int TARGET_METHOD_RETURN = 0x14;
        /** `empty_target` on the receiver. */
        public static final int TARGET_METHOD_RECEIVER = 0x15;
        /** `formal_parameter_target`. */
        public static final int TARGET_METHOD_FORMAL_PARAMETER = 0x16;
        /** `throws_target`. */
        public static final int TARGET_THROWS = 0x17;
        /** A local variable's `localvar_target`. */
        public static final int TARGET_LOCAL_VARIABLE = 0x40;
        /** A `try` resource's `localvar_target`. */
        public static final int TARGET_RESOURCE_VARIABLE = 0x41;
        /** `catch_target`. */
        public static final int TARGET_EXCEPTION_PARAMETER = 0x42;
        /** An `instanceof`'s `offset_target`. */
        public static final int TARGET_INSTANCEOF = 0x43;
        /** A `new`'s `offset_target`. */
        public static final int TARGET_NEW = 0x44;
        /** A constructor reference's `offset_target`. */
        public static final int TARGET_CONSTRUCTOR_REFERENCE = 0x45;
        /** A method reference's `offset_target`. */
        public static final int TARGET_METHOD_REFERENCE = 0x46;
        /** A cast's `type_argument_target`. */
        public static final int TARGET_CAST = 0x47;
        /** A constructor invocation's `type_argument_target`. */
        public static final int TARGET_CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;
        /** A method invocation's `type_argument_target`. */
        public static final int TARGET_METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;
        /** A constructor reference's `type_argument_target`. */
        public static final int TARGET_CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;
        /** A method reference's `type_argument_target`. */
        public static final int TARGET_METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

        /** The tag that chose this form. */
        TargetType targetType();

        /** How many bytes it takes, counting the `target_type` byte. */
        default int size() {
            return targetType().sizeIfFixed() + 1;
        }

        /** A `type_parameter_target` with this tag. */
        public static TypeParameterTarget ofTypeParameter(TargetType targetType,
                int typeParameterIndex) {
            return Annotations.typeParameterTarget(targetType, typeParameterIndex);
        }

        /** The class's type parameter number `typeParameterIndex`. */
        public static TypeParameterTarget ofClassTypeParameter(int typeParameterIndex) {
            return ofTypeParameter(TargetType.CLASS_TYPE_PARAMETER, typeParameterIndex);
        }

        /** The method's type parameter number `typeParameterIndex`. */
        public static TypeParameterTarget ofMethodTypeParameter(int typeParameterIndex) {
            return ofTypeParameter(TargetType.METHOD_TYPE_PARAMETER, typeParameterIndex);
        }

        /** Supertype number `supertypeIndex`; 65535 is the superclass. */
        public static SupertypeTarget ofClassExtends(int supertypeIndex) {
            return Annotations.supertypeTarget(supertypeIndex);
        }

        /** A `type_parameter_bound_target` with this tag. */
        public static TypeParameterBoundTarget ofTypeParameterBound(TargetType targetType,
                int typeParameterIndex, int boundIndex) {
            return Annotations.typeParameterBoundTarget(targetType, typeParameterIndex, boundIndex);
        }

        /** Bound `boundIndex` of the class's parameter `typeParameterIndex`. */
        public static TypeParameterBoundTarget ofClassTypeParameterBound(int typeParameterIndex,
                int boundIndex) {
            return ofTypeParameterBound(TargetType.CLASS_TYPE_PARAMETER_BOUND, typeParameterIndex,
                    boundIndex);
        }

        /** Bound `boundIndex` of the method's parameter `typeParameterIndex`. */
        public static TypeParameterBoundTarget ofMethodTypeParameterBound(int typeParameterIndex,
                int boundIndex) {
            return ofTypeParameterBound(TargetType.METHOD_TYPE_PARAMETER_BOUND, typeParameterIndex,
                    boundIndex);
        }

        /** An `empty_target` with this tag. */
        public static EmptyTarget of(TargetType targetType) {
            return Annotations.emptyTarget(targetType);
        }

        /** A field's type. */
        public static EmptyTarget ofField() {
            return of(TargetType.FIELD);
        }

        /** A method's return type. */
        public static EmptyTarget ofMethodReturn() {
            return of(TargetType.METHOD_RETURN);
        }

        /** A method's receiver. */
        public static EmptyTarget ofMethodReceiver() {
            return of(TargetType.METHOD_RECEIVER);
        }

        /** Formal parameter number `formalParameterIndex`. */
        public static FormalParameterTarget ofMethodFormalParameter(int formalParameterIndex) {
            return Annotations.formalParameterTarget(formalParameterIndex);
        }

        /** Exception number `throwsTargetIndex` of the `throws` clause. */
        public static ThrowsTarget ofThrows(int throwsTargetIndex) {
            return Annotations.throwsTarget(throwsTargetIndex);
        }

        /** A `localvar_target` with this tag. */
        public static LocalVarTarget ofVariable(TargetType targetType,
                List<LocalVarTargetInfo> table) {
            return Annotations.localVarTarget(targetType, table);
        }

        /** A local variable with this live range. */
        public static LocalVarTarget ofLocalVariable(List<LocalVarTargetInfo> table) {
            return ofVariable(TargetType.LOCAL_VARIABLE, table);
        }

        /** A `try` resource with this live range. */
        public static LocalVarTarget ofResourceVariable(List<LocalVarTargetInfo> table) {
            return ofVariable(TargetType.RESOURCE_VARIABLE, table);
        }

        /** Handler number `exceptionTableIndex` of the `exception_table`. */
        public static CatchTarget ofExceptionParameter(int exceptionTableIndex) {
            return Annotations.catchTarget(exceptionTableIndex);
        }

        /** An `offset_target` with this tag. */
        public static OffsetTarget ofOffset(TargetType targetType, Label target) {
            return Annotations.offsetTarget(targetType, target);
        }

        /** The `instanceof` sitting at `target`. */
        public static OffsetTarget ofInstanceofExpr(Label target) {
            return ofOffset(TargetType.INSTANCEOF, target);
        }

        /** The `new` sitting at `target`. */
        public static OffsetTarget ofNewExpr(Label target) {
            return ofOffset(TargetType.NEW, target);
        }

        /** The constructor reference sitting at `target`. */
        public static OffsetTarget ofConstructorReference(Label target) {
            return ofOffset(TargetType.CONSTRUCTOR_REFERENCE, target);
        }

        /** The method reference sitting at `target`. */
        public static OffsetTarget ofMethodReference(Label target) {
            return ofOffset(TargetType.METHOD_REFERENCE, target);
        }

        /** A `type_argument_target` with this tag. */
        public static TypeArgumentTarget ofTypeArgument(TargetType targetType, Label target,
                int typeArgumentIndex) {
            return Annotations.typeArgumentTarget(targetType, target, typeArgumentIndex);
        }

        /** Type argument `typeArgumentIndex` of the cast sitting at `target`. */
        public static TypeArgumentTarget ofCastExpr(Label target, int typeArgumentIndex) {
            return ofTypeArgument(TargetType.CAST, target, typeArgumentIndex);
        }

        /** The type argument of the constructor invocation sitting at `target`. */
        public static TypeArgumentTarget ofConstructorInvocationTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }

        /** The type argument of the method invocation sitting at `target`. */
        public static TypeArgumentTarget ofMethodInvocationTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.METHOD_INVOCATION_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }

        /** The type argument of the constructor reference sitting at `target`. */
        public static TypeArgumentTarget ofConstructorReferenceTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }

        /** The type argument of the method reference sitting at `target`. */
        public static TypeArgumentTarget ofMethodReferenceTypeArgument(Label target,
                int typeArgumentIndex) {
            return ofTypeArgument(TargetType.METHOD_REFERENCE_TYPE_ARGUMENT, target,
                    typeArgumentIndex);
        }
    }

    /**
     * `target_type`'s twenty-two tags. `sizeIfFixed()` does NOT count the tag's byte and is -1 on the
     * two local variable forms, whose length depends on the number of ranges.
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

        private final int value;
        private final int size;

        private TargetType(int value, int size) {
            this.value = value;
            this.size = size;
        }

        /** The `target_type` byte. */
        public int targetTypeValue() {
            return this.value;
        }

        /** The `target_info`'s length without the tag, or -1 if it depends on the contents. */
        public int sizeIfFixed() {
            return this.size;
        }
    }

    /** A `type_parameter_target`. */
    public interface TypeParameterTarget extends TargetInfo {

        /** The type parameter's index. */
        int typeParameterIndex();
    }

    /** A `supertype_target`: 65535 names the superclass, and the rest name an interface. */
    public interface SupertypeTarget extends TargetInfo {

        /** The supertype's index. */
        int supertypeIndex();
    }

    /** A `type_parameter_bound_target`. */
    public interface TypeParameterBoundTarget extends TargetInfo {

        /** The type parameter's index. */
        int typeParameterIndex();

        /** The bound's index within that parameter. */
        int boundIndex();
    }

    /** An `empty_target`: the tag says it all and there are no more bytes. */
    public interface EmptyTarget extends TargetInfo {
    }

    /** A `formal_parameter_target`. */
    public interface FormalParameterTarget extends TargetInfo {

        /** The formal parameter's index, counting from 0 and leaving out the receiver. */
        int formalParameterIndex();
    }

    /** A `throws_target`. */
    public interface ThrowsTarget extends TargetInfo {

        /** The index within the `Exceptions` attribute's table. */
        int throwsTargetIndex();
    }

    /** A `localvar_target`: a variable can have several disjoint live ranges. */
    public interface LocalVarTarget extends TargetInfo {

        /** The ranges, in file order. */
        List<LocalVarTargetInfo> table();

        /** Three header bytes and six per range. */
        default int size() {
            return 3 + table().size() * 6;
        }
    }

    /** A row of a `localvar_target`'s table. */
    public interface LocalVarTargetInfo {

        /** Where the live range starts. */
        Label startLabel();

        /** Where it ends, exclusive. */
        Label endLabel();

        /** The local variable slot. */
        int index();

        /** A row with these values. */
        public static LocalVarTargetInfo of(Label startLabel, Label endLabel, int index) {
            return Annotations.localVarTargetInfo(startLabel, endLabel, index);
        }
    }

    /** A `catch_target`. */
    public interface CatchTarget extends TargetInfo {

        /** The index within the `Code` attribute's `exception_table`. */
        int exceptionTableIndex();
    }

    /** An `offset_target`: it points at a bci of the `code` array. */
    public interface OffsetTarget extends TargetInfo {

        /** The annotated instruction's bci. */
        Label target();
    }

    /** A `type_argument_target`. */
    public interface TypeArgumentTarget extends TargetInfo {

        /** The annotated instruction's bci. */
        Label target();

        /** Which of that expression's type arguments. */
        int typeArgumentIndex();
    }

    /**
     * A step of the `target_path` (§4.7.20.2). Each step goes down one level inside the type: to an
     * array's component, to the inner type, to a wildcard's bound, or to a type argument.
     */
    public interface TypePathComponent {

        /** Going down to an array's component. */
        public static final TypePathComponent ARRAY = of(Kind.ARRAY, 0);
        /** Going down to the nested type. */
        public static final TypePathComponent INNER_TYPE = of(Kind.INNER_TYPE, 0);
        /** Going down to a wildcard's bound. */
        public static final TypePathComponent WILDCARD = of(Kind.WILDCARD, 0);

        /** What kind of step it is. */
        Kind typePathKind();

        /** Which type argument, if the step is `TYPE_ARGUMENT`; 0 on the other three. */
        int typeArgumentIndex();

        /** A step with this kind and this index. */
        public static TypePathComponent of(Kind typePathKind, int typeArgumentIndex) {
            return Annotations.typePathComponent(typePathKind, typeArgumentIndex);
        }

        /** The four kinds of step, with the `type_path_kind` the format gives them. */
        public enum Kind {

            ARRAY(0),
            INNER_TYPE(1),
            WILDCARD(2),
            TYPE_ARGUMENT(3);

            private final int tag;

            private Kind(int tag) {
                this.tag = tag;
            }

            /** The `type_path_kind`. */
            public int tag() {
                return this.tag;
            }
        }
    }
}
