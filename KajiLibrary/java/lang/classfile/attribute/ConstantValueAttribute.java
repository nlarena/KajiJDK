package java.lang.classfile.attribute;

import java.lang.classfile.Attribute;
import java.lang.classfile.FieldElement;
import java.lang.classfile.constantpool.ConstantValueEntry;
import java.lang.constant.ConstantDesc;
import jdk.internal.classfile.impl.TypedAttributes;

// `ConstantValue` (JVMS §4.7.2): el valor de un campo `static final` de tipo primitivo o `String`.
// La JVM lo asigna al inicializar la clase, ANTES de correr `<clinit>`, y por eso un campo con este
// atributo se puede leer aunque el inicializador estático todavía no haya corrido.
public interface ConstantValueAttribute extends Attribute<ConstantValueAttribute>, FieldElement {

    /** La entrada del pool con el valor. */
    ConstantValueEntry constant();

    /** El atributo con este valor. */
    public static ConstantValueAttribute of(ConstantValueEntry value) {
        return TypedAttributes.constantValue(value);
    }

    /** El atributo con este valor. */
    public static ConstantValueAttribute of(ConstantDesc value) {
        return TypedAttributes.constantValue(TypedAttributes.constantValueEntry(value));
    }
}
