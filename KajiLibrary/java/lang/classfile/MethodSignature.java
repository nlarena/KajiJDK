package java.lang.classfile;

import java.lang.classfile.Signature.ThrowableSig;
import java.lang.classfile.Signature.TypeParam;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import jdk.internal.classfile.impl.Signatures;

// A method's generic signature (JVMS §4.7.9.1, `MethodSignature`): type parameters, argument types,
// result type and the signatures of what it declares it throws.
public interface MethodSignature {

    /** The declared type parameters, empty if the method is not generic. */
    List<TypeParam> typeParameters();

    /** The arguments' signatures, in order. */
    List<Signature> arguments();

    /** The result's signature; `V` if the method returns nothing. */
    Signature result();

    /** The `throws` signatures, empty if the signature wrote none. */
    List<ThrowableSig> throwableSignatures();

    /** The signature's text, just as it would go into the attribute's `Utf8`. */
    String signatureString();

    /** `descriptor`'s signature with no generics. */
    public static MethodSignature of(MethodTypeDesc descriptor) {
        return Signatures.methodSignatureOf(descriptor);
    }

    /** A signature with no type parameters and no `throws`. */
    public static MethodSignature of(Signature result, Signature... arguments) {
        return Signatures.methodSignature(null, null, result, arguments);
    }

    /** A complete signature. */
    public static MethodSignature of(List<TypeParam> typeParameters,
            List<ThrowableSig> exceptions, Signature result, Signature... arguments) {
        return Signatures.methodSignature(typeParameters, exceptions, result, arguments);
    }

    /** It parses a method signature. It throws `IllegalArgumentException` if it is not one. */
    public static MethodSignature parseFrom(String methodSignature) {
        return Signatures.parseMethodSignature(methodSignature);
    }
}
