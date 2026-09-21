package java.lang.classfile;

import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.Optional;
import jdk.internal.classfile.impl.Signatures;

// A generic signature (JVMS §4.7.9.1): what the `Signature` attribute says and the descriptor cannot,
// because the descriptor has no generics. `Ljava/util/List<Ljava/lang/String;>;` is a signature;
// `Ljava/util/List;` is its descriptor.
//
// The tree is closed by construction: every signature is a `BaseTypeSig`, a `ClassTypeSig`, an
// `ArrayTypeSig` or a `TypeVarSig`. The interfaces are NOT declared `sealed` --the JDK does seal
// them-- for the same reason as in `PoolEntry`: sealing towards a package that exports nothing adds
// no guarantee and does add a way of failing to compile.
public interface Signature {

    /** The signature's text, just as it would go into the attribute's `Utf8`. */
    String signatureString();

    /** It parses a type signature. It throws `IllegalArgumentException` if it is not one. */
    public static Signature parseFrom(String signature) {
        return Signatures.parseType(signature);
    }

    /** The signature of a type with no generics. */
    public static Signature of(ClassDesc classDesc) {
        return Signatures.ofDescriptor(classDesc);
    }

    /** A signature denoting a reference type: class, array or type variable. */
    public interface RefTypeSig extends Signature {
    }

    /** A signature that can appear after a `^` in a `throws`. */
    public interface ThrowableSig extends Signature {
    }

    /** A primitive type, or `void` in the result position. */
    public interface BaseTypeSig extends Signature {

        /** The descriptor's letter: `B`, `C`, `D`, `F`, `I`, `J`, `S`, `Z` or `V`. */
        char baseType();

        /** The signature of the primitive `classDesc` describes. */
        public static BaseTypeSig of(ClassDesc classDesc) {
            if (classDesc == null) {
                throw new NullPointerException("classDesc");
            }
            if (!classDesc.isPrimitive()) {
                throw new IllegalArgumentException("not a primitive: " + classDesc.descriptorString());
            }
            return Signatures.baseTypeSig(classDesc.descriptorString().charAt(0));
        }

        /** The signature of the primitive whose descriptor letter is `baseType`. */
        public static BaseTypeSig of(char baseType) {
            return Signatures.baseTypeSig(baseType);
        }
    }

    /** A class or interface, with its type arguments and its outer type if it is nested. */
    public interface ClassTypeSig extends RefTypeSig, ThrowableSig {

        /** The outer type, if this signature wrote the nesting with a dot. */
        Optional<ClassTypeSig> outerType();

        /**
         * The name. If `outerType()` is there, it is only the nested class's simple name; if not, it
         * is the full internal name. It is the same split the format makes: `Lp/Outer<*>.Inner;`
         * breaks the name in two and `Lp/Outer$Inner;` does not.
         */
        String className();

        /** The type arguments, empty if the signature has no `<...>`. */
        List<TypeArg> typeArgs();

        /** The type with no generics, with the nesting resolved to `$`. */
        default ClassDesc classDesc() {
            Optional<ClassTypeSig> ext = outerType();
            if (ext.isPresent()) {
                String base = ext.get().classDesc().descriptorString();
                // `Lp/Outer;` + `Inner` -> `Lp/Outer$Inner;`
                return ClassDesc.ofDescriptor(
                        base.substring(0, base.length() - 1) + "$" + className() + ";");
            }
            return ClassDesc.ofDescriptor("L" + className() + ";");
        }

        /** `classDesc`'s signature, with these type arguments. */
        public static ClassTypeSig of(ClassDesc classDesc, TypeArg... typeArgs) {
            return of(null, classDesc, typeArgs);
        }

        /** Like the previous one, nested inside `outerType`. */
        public static ClassTypeSig of(ClassTypeSig outerType, ClassDesc classDesc,
                TypeArg... typeArgs) {
            if (classDesc == null) {
                throw new NullPointerException("classDesc");
            }
            if (!classDesc.isClassOrInterface()) {
                throw new IllegalArgumentException(
                        "neither class nor interface: " + classDesc.descriptorString());
            }
            String d = classDesc.descriptorString();
            return of(outerType, d.substring(1, d.length() - 1), typeArgs);
        }

        /** The signature of the class with internal name `className`, with these type arguments. */
        public static ClassTypeSig of(String className, TypeArg... typeArgs) {
            return of(null, className, typeArgs);
        }

        /** Like the previous one, nested inside `outerType`. */
        public static ClassTypeSig of(ClassTypeSig outerType, String className,
                TypeArg... typeArgs) {
            return Signatures.classTypeSig(outerType, className, typeArgs);
        }
    }

    /** An array. */
    public interface ArrayTypeSig extends RefTypeSig {

        /** The component's signature. */
        Signature componentSignature();

        /** An array of `componentSignature`. */
        public static ArrayTypeSig of(Signature componentSignature) {
            return of(1, componentSignature);
        }

        /** An array of `dims` dimensions over `componentSignature`. */
        public static ArrayTypeSig of(int dims, Signature componentSignature) {
            return Signatures.arrayTypeSig(dims, componentSignature);
        }
    }

    /** A type variable, that is, a `T` declared by a class or a method. */
    public interface TypeVarSig extends RefTypeSig, ThrowableSig {

        /** The variable's name. */
        String identifier();

        /** The signature of the type variable `identifier`. */
        public static TypeVarSig of(String identifier) {
            return Signatures.typeVarSig(identifier);
        }
    }

    /** A type variable's declaration: its name and its bounds. */
    public interface TypeParam {

        /** The variable's name. */
        String identifier();

        /** The class bound; empty if the declaration wrote `T::...`. */
        Optional<RefTypeSig> classBound();

        /** The interface bounds, in order. */
        List<RefTypeSig> interfaceBounds();

        /** A declaration with this class bound and these interface bounds. */
        public static TypeParam of(String identifier, RefTypeSig classBound,
                RefTypeSig... interfaceBounds) {
            return Signatures.typeParam(identifier, Optional.ofNullable(classBound), interfaceBounds);
        }

        /** Like the previous one, with the class bound already wrapped. */
        public static TypeParam of(String identifier, Optional<RefTypeSig> classBound,
                RefTypeSig... interfaceBounds) {
            return Signatures.typeParam(identifier, classBound, interfaceBounds);
        }
    }

    /** A type argument: a type, a bounded wildcard, or `*`. */
    public interface TypeArg {

        /** The exact argument `refTypeSig`. */
        public static Bounded of(RefTypeSig refTypeSig) {
            return bounded(Bounded.WildcardIndicator.NONE, refTypeSig);
        }

        /** The `*` argument. */
        public static Unbounded unbounded() {
            return Signatures.unbounded();
        }

        /** The `? extends refTypeSig` argument. */
        public static Bounded extendsOf(RefTypeSig refTypeSig) {
            return bounded(Bounded.WildcardIndicator.EXTENDS, refTypeSig);
        }

        /** The `? super refTypeSig` argument. */
        public static Bounded superOf(RefTypeSig refTypeSig) {
            return bounded(Bounded.WildcardIndicator.SUPER, refTypeSig);
        }

        /** The argument with this wildcard over this type. */
        public static Bounded bounded(Bounded.WildcardIndicator wildcardIndicator,
                RefTypeSig boundType) {
            return Signatures.bounded(wildcardIndicator, boundType);
        }

        /** An argument naming a type, with or without a wildcard. */
        public interface Bounded extends TypeArg {

            /** Which wildcard it carries. */
            WildcardIndicator wildcardIndicator();

            /** The bounded type. */
            RefTypeSig boundType();

            /** A bounded argument's wildcard. */
            public enum WildcardIndicator {

                /** No wildcard: the argument is the type itself. */
                NONE,
                /** `? extends`, written `+` in the signature. */
                EXTENDS,
                /** `? super`, written `-` in the signature. */
                SUPER
            }
        }

        /** The `*` argument, that is, a bare `?`. */
        public interface Unbounded extends TypeArg {
        }
    }
}
