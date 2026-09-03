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

// `Signature` (JVMS §4.7.9): el tipo GENÉRICO de una clase, un campo o un método, que el descriptor
// no puede expresar porque los genéricos se borran. Los tres casos comparten atributo pero no
// gramática, y por eso hay tres formas de analizarlo: quien lo lee tiene que saber de dónde salió.
// Pedir la equivocada tira `IllegalArgumentException` al analizar, que es lo correcto — no hay forma
// de adivinar el contexto desde el texto.
public interface SignatureAttribute
        extends Attribute<SignatureAttribute>, ClassElement, MethodElement, FieldElement {

    /** La firma, sin analizar. */
    Utf8Entry signature();

    /** La firma leída como firma de clase. */
    default ClassSignature asClassSignature() {
        return ClassSignature.parseFrom(signature().stringValue());
    }

    /** La firma leída como firma de método. */
    default MethodSignature asMethodSignature() {
        return MethodSignature.parseFrom(signature().stringValue());
    }

    /** La firma leída como firma de tipo, que es el caso de un campo. */
    default Signature asTypeSignature() {
        return Signature.parseFrom(signature().stringValue());
    }

    /** El atributo con esta firma de clase. */
    public static SignatureAttribute of(ClassSignature classSignature) {
        return TypedAttributes.signature(
                TypedAttributes.utf8(classSignature.signatureString()));
    }

    /** El atributo con esta firma de método. */
    public static SignatureAttribute of(MethodSignature methodSignature) {
        return TypedAttributes.signature(
                TypedAttributes.utf8(methodSignature.signatureString()));
    }

    /** El atributo con esta firma de tipo. */
    public static SignatureAttribute of(Signature signature) {
        return TypedAttributes.signature(TypedAttributes.utf8(signature.signatureString()));
    }

    /** El atributo con esta firma. */
    public static SignatureAttribute of(Utf8Entry signature) {
        return TypedAttributes.signature(signature);
    }
}
