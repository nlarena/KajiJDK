package java.lang.classfile;

import java.lang.classfile.Signature.ClassTypeSig;
import java.lang.classfile.Signature.TypeParam;
import java.util.List;
import jdk.internal.classfile.impl.Signatures;

// La firma genérica de una clase (JVMS §4.7.9.1, `ClassSignature`): sus parámetros de tipo, la firma
// de su superclase y las de sus interfaces. Es lo que lleva el atributo `Signature` de una clase.
public interface ClassSignature {

    /** Los parámetros de tipo declarados, vacíos si la clase no es genérica. */
    List<TypeParam> typeParameters();

    /** La firma de la superclase. */
    ClassTypeSig superclassSignature();

    /** Las firmas de las interfaces directas, en orden. */
    List<ClassTypeSig> superinterfaceSignatures();

    /** El texto de la firma, tal como iría en el `Utf8` del atributo. */
    String signatureString();

    /** Una firma sin parámetros de tipo. */
    public static ClassSignature of(ClassTypeSig superclassSignature,
            ClassTypeSig... superinterfaceSignatures) {
        return Signatures.classSignature(null, superclassSignature, superinterfaceSignatures);
    }

    /** Una firma con estos parámetros de tipo. */
    public static ClassSignature of(List<TypeParam> typeParameters,
            ClassTypeSig superclassSignature, ClassTypeSig... superinterfaceSignatures) {
        return Signatures.classSignature(typeParameters, superclassSignature, superinterfaceSignatures);
    }

    /** Parsea una firma de clase. Tira `IllegalArgumentException` si no es una. */
    public static ClassSignature parseFrom(String classSignature) {
        return Signatures.parseClassSignature(classSignature);
    }
}
