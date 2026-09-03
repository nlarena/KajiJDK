package java.lang.classfile;

import java.lang.classfile.Signature.ThrowableSig;
import java.lang.classfile.Signature.TypeParam;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import jdk.internal.classfile.impl.Signatures;

// La firma genérica de un método (JVMS §4.7.9.1, `MethodSignature`): parámetros de tipo, tipos de
// los argumentos, tipo del resultado y firmas de lo que declara tirar.
public interface MethodSignature {

    /** Los parámetros de tipo declarados, vacíos si el método no es genérico. */
    List<TypeParam> typeParameters();

    /** Las firmas de los argumentos, en orden. */
    List<Signature> arguments();

    /** La firma del resultado; `V` si el método no devuelve nada. */
    Signature result();

    /** Las firmas de los `throws`, vacías si la firma no escribió ninguna. */
    List<ThrowableSig> throwableSignatures();

    /** El texto de la firma, tal como iría en el `Utf8` del atributo. */
    String signatureString();

    /** La firma sin genéricos de `descriptor`. */
    public static MethodSignature of(MethodTypeDesc descriptor) {
        return Signatures.methodSignatureOf(descriptor);
    }

    /** Una firma sin parámetros de tipo ni `throws`. */
    public static MethodSignature of(Signature result, Signature... arguments) {
        return Signatures.methodSignature(null, null, result, arguments);
    }

    /** Una firma completa. */
    public static MethodSignature of(List<TypeParam> typeParameters,
            List<ThrowableSig> exceptions, Signature result, Signature... arguments) {
        return Signatures.methodSignature(typeParameters, exceptions, result, arguments);
    }

    /** Parsea una firma de método. Tira `IllegalArgumentException` si no es una. */
    public static MethodSignature parseFrom(String methodSignature) {
        return Signatures.parseMethodSignature(methodSignature);
    }
}
