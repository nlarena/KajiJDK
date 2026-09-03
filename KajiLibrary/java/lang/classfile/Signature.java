package java.lang.classfile;

import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.Optional;
import jdk.internal.classfile.impl.Signatures;

// Una firma genérica (JVMS §4.7.9.1): lo que dice el atributo `Signature` y que el descriptor no
// puede decir, porque el descriptor no tiene genéricos. `Ljava/util/List<Ljava/lang/String;>;` es
// una firma; `Ljava/util/List;` es su descriptor.
//
// El árbol es cerrado por construcción: toda firma es un `BaseTypeSig`, un `ClassTypeSig`, un
// `ArrayTypeSig` o un `TypeVarSig`. Las interfaces NO se declaran `sealed` —el JDK sí las sella—
// por la misma razón que en `PoolEntry`: sellar hacia un paquete que no exporta nada no agrega
// garantía y sí agrega una forma de no compilar.
public interface Signature {

    /** El texto de la firma, tal como iría en el `Utf8` del atributo. */
    String signatureString();

    /** Parsea una firma de tipo. Tira `IllegalArgumentException` si no es una. */
    public static Signature parseFrom(String signature) {
        return Signatures.parseTipo(signature);
    }

    /** La firma de un tipo sin genéricos. */
    public static Signature of(ClassDesc classDesc) {
        return Signatures.ofDescriptor(classDesc);
    }

    /** Una firma que denota un tipo de referencia: clase, arreglo o variable de tipo. */
    public interface RefTypeSig extends Signature {
    }

    /** Una firma que puede aparecer detrás de un `^` en un `throws`. */
    public interface ThrowableSig extends Signature {
    }

    /** Un tipo primitivo, o `void` en la posición de resultado. */
    public interface BaseTypeSig extends Signature {

        /** La letra del descriptor: `B`, `C`, `D`, `F`, `I`, `J`, `S`, `Z` o `V`. */
        char baseType();

        /** La firma del primitivo que describe `classDesc`. */
        public static BaseTypeSig of(ClassDesc classDesc) {
            if (classDesc == null) {
                throw new NullPointerException("classDesc");
            }
            if (!classDesc.isPrimitive()) {
                throw new IllegalArgumentException("no es primitivo: " + classDesc.descriptorString());
            }
            return Signatures.baseTypeSig(classDesc.descriptorString().charAt(0));
        }

        /** La firma del primitivo cuya letra de descriptor es `baseType`. */
        public static BaseTypeSig of(char baseType) {
            return Signatures.baseTypeSig(baseType);
        }
    }

    /** Una clase o interfaz, con sus argumentos de tipo y su tipo externo si es anidada. */
    public interface ClassTypeSig extends RefTypeSig, ThrowableSig {

        /** El tipo externo, si esta firma escribió el anidamiento con un punto. */
        Optional<ClassTypeSig> outerType();

        /**
         * El nombre. Si `outerType()` está, es sólo el nombre simple de la clase anidada; si no, es
         * el nombre interno completo. Es la misma división que hace el formato: `Lp/Outer<*>.Inner;`
         * parte el nombre en dos y `Lp/Outer$Inner;` no.
         */
        String className();

        /** Los argumentos de tipo, vacíos si la firma no tiene `<...>`. */
        List<TypeArg> typeArgs();

        /** El tipo sin genéricos, con el anidamiento resuelto a `$`. */
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

        /** La firma de `classDesc`, con estos argumentos de tipo. */
        public static ClassTypeSig of(ClassDesc classDesc, TypeArg... typeArgs) {
            return of(null, classDesc, typeArgs);
        }

        /** Como la anterior, anidada dentro de `outerType`. */
        public static ClassTypeSig of(ClassTypeSig outerType, ClassDesc classDesc,
                TypeArg... typeArgs) {
            if (classDesc == null) {
                throw new NullPointerException("classDesc");
            }
            if (!classDesc.isClassOrInterface()) {
                throw new IllegalArgumentException(
                        "no es clase ni interfaz: " + classDesc.descriptorString());
            }
            String d = classDesc.descriptorString();
            return of(outerType, d.substring(1, d.length() - 1), typeArgs);
        }

        /** La firma de la clase de nombre interno `className`, con estos argumentos de tipo. */
        public static ClassTypeSig of(String className, TypeArg... typeArgs) {
            return of(null, className, typeArgs);
        }

        /** Como la anterior, anidada dentro de `outerType`. */
        public static ClassTypeSig of(ClassTypeSig outerType, String className,
                TypeArg... typeArgs) {
            return Signatures.classTypeSig(outerType, className, typeArgs);
        }
    }

    /** Un arreglo. */
    public interface ArrayTypeSig extends RefTypeSig {

        /** La firma del componente. */
        Signature componentSignature();

        /** Un arreglo de `componentSignature`. */
        public static ArrayTypeSig of(Signature componentSignature) {
            return of(1, componentSignature);
        }

        /** Un arreglo de `dims` dimensiones sobre `componentSignature`. */
        public static ArrayTypeSig of(int dims, Signature componentSignature) {
            return Signatures.arrayTypeSig(dims, componentSignature);
        }
    }

    /** Una variable de tipo, o sea una `T` declarada por una clase o un método. */
    public interface TypeVarSig extends RefTypeSig, ThrowableSig {

        /** El nombre de la variable. */
        String identifier();

        /** La firma de la variable de tipo `identifier`. */
        public static TypeVarSig of(String identifier) {
            return Signatures.typeVarSig(identifier);
        }
    }

    /** La declaración de una variable de tipo: su nombre y sus cotas. */
    public interface TypeParam {

        /** El nombre de la variable. */
        String identifier();

        /** La cota de clase; vacía si la declaración escribió `T::…`. */
        Optional<RefTypeSig> classBound();

        /** Las cotas de interfaz, en orden. */
        List<RefTypeSig> interfaceBounds();

        /** Una declaración con esta cota de clase y estas cotas de interfaz. */
        public static TypeParam of(String identifier, RefTypeSig classBound,
                RefTypeSig... interfaceBounds) {
            return Signatures.typeParam(identifier, Optional.ofNullable(classBound), interfaceBounds);
        }

        /** Como la anterior, con la cota de clase ya envuelta. */
        public static TypeParam of(String identifier, Optional<RefTypeSig> classBound,
                RefTypeSig... interfaceBounds) {
            return Signatures.typeParam(identifier, classBound, interfaceBounds);
        }
    }

    /** Un argumento de tipo: un tipo, un comodín acotado, o `*`. */
    public interface TypeArg {

        /** El argumento exacto `refTypeSig`. */
        public static Bounded of(RefTypeSig refTypeSig) {
            return bounded(Bounded.WildcardIndicator.NONE, refTypeSig);
        }

        /** El argumento `*`. */
        public static Unbounded unbounded() {
            return Signatures.unbounded();
        }

        /** El argumento `? extends refTypeSig`. */
        public static Bounded extendsOf(RefTypeSig refTypeSig) {
            return bounded(Bounded.WildcardIndicator.EXTENDS, refTypeSig);
        }

        /** El argumento `? super refTypeSig`. */
        public static Bounded superOf(RefTypeSig refTypeSig) {
            return bounded(Bounded.WildcardIndicator.SUPER, refTypeSig);
        }

        /** El argumento con este comodín sobre este tipo. */
        public static Bounded bounded(Bounded.WildcardIndicator wildcardIndicator,
                RefTypeSig boundType) {
            return Signatures.bounded(wildcardIndicator, boundType);
        }

        /** Un argumento que nombra un tipo, con o sin comodín. */
        public interface Bounded extends TypeArg {

            /** Qué comodín lleva. */
            WildcardIndicator wildcardIndicator();

            /** El tipo acotado. */
            RefTypeSig boundType();

            /** El comodín de un argumento acotado. */
            public enum WildcardIndicator {

                /** Sin comodín: el argumento es el tipo mismo. */
                NONE,
                /** `? extends`, que en la firma se escribe `+`. */
                EXTENDS,
                /** `? super`, que en la firma se escribe `-`. */
                SUPER
            }
        }

        /** El argumento `*`, o sea `?` a secas. */
        public interface Unbounded extends TypeArg {
        }
    }
}
