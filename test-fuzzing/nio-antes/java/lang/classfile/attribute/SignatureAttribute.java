package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassSignature;
import java.lang.classfile.FieldElement;
import java.lang.classfile.MethodElement;
import java.lang.classfile.MethodSignature;
import java.lang.classfile.Signature;
import java.lang.classfile.constantpool.Utf8Entry;
import jdk.internal.classfile.impl.TypedAttributes;

// `Signature` (JVMS §4.7.9): the GENERIC type of a class, a field or a method, which the descriptor
// cannot express because generics are erased. The three cases share the attribute but not the grammar,
// and that is why there are three ways of parsing it: whoever reads it has to know where it came from.
// Asking for the wrong one throws `IllegalArgumentException` while parsing, which is the right answer
// -- there is no way of guessing the context from the text.
public interface SignatureAttribute
        extends Attribute<SignatureAttribute>, ClassElement, MethodElement, FieldElement {

    /** The signature, unparsed. */
    Utf8Entry signature();

    /** The signature read as a class signature. */
    default ClassSignature asClassSignature() {
        return ClassSignature.parseFrom(signature().stringValue());
    }

    /** The signature read as a method signature. */
    default MethodSignature asMethodSignature() {
        return MethodSignature.parseFrom(signature().stringValue());
    }

    /** The signature read as a type signature, which is a field's case. */
    default Signature asTypeSignature() {
        return Signature.parseFrom(signature().stringValue());
    }

    /** The attribute with this class signature. */
    public static SignatureAttribute of(ClassSignature classSignature) {
        return TypedAttributes.signature(
                TypedAttributes.utf8(classSignature.signatureString()));
    }

    /** The attribute with this method signature. */
    public static SignatureAttribute of(MethodSignature methodSignature) {
        return TypedAttributes.signature(
                TypedAttributes.utf8(methodSignature.signatureString()));
    }

    /** The attribute with this type signature. */
    public static SignatureAttribute of(Signature signature) {
        return TypedAttributes.signature(TypedAttributes.utf8(signature.signatureString()));
    }

    /** The attribute with this signature. */
    public static SignatureAttribute of(Utf8Entry signature) {
        return TypedAttributes.signature(signature);
    }
}
