package java.lang.classfile;

import java.lang.classfile.Signature.ClassTypeSig;
import java.lang.classfile.Signature.TypeParam;
import java.util.List;
import jdk.internal.classfile.impl.Signatures;

// A class's generic signature (JVMS §4.7.9.1, `ClassSignature`): its type parameters, its
// superclass's signature and those of its interfaces. It is what a class's `Signature` attribute
// carries.
public interface ClassSignature {

    /** The declared type parameters, empty if the class is not generic. */
    List<TypeParam> typeParameters();

    /** The superclass's signature. */
    ClassTypeSig superclassSignature();

    /** The direct interfaces' signatures, in order. */
    List<ClassTypeSig> superinterfaceSignatures();

    /** The signature's text, just as it would go into the attribute's `Utf8`. */
    String signatureString();

    /** A signature with no type parameters. */
    public static ClassSignature of(ClassTypeSig superclassSignature,
            ClassTypeSig... superinterfaceSignatures) {
        return Signatures.classSignature(null, superclassSignature, superinterfaceSignatures);
    }

    /** A signature with these type parameters. */
    public static ClassSignature of(List<TypeParam> typeParameters,
            ClassTypeSig superclassSignature, ClassTypeSig... superinterfaceSignatures) {
        return Signatures.classSignature(typeParameters, superclassSignature, superinterfaceSignatures);
    }

    /** It parses a class signature. It throws `IllegalArgumentException` if it is not one. */
    public static ClassSignature parseFrom(String classSignature) {
        return Signatures.parseClassSignature(classSignature);
    }
}
