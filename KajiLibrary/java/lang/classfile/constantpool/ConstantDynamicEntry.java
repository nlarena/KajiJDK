package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.util.List;

// `CONSTANT_Dynamic_info` (JVMS §4.4.10): una constante que la VM calcula la primera vez que se
// carga. Su descriptor es de *campo* — el tipo de lo que produce — a diferencia del de
// `CONSTANT_InvokeDynamic`, que es de método.
public interface ConstantDynamicEntry extends DynamicConstantPoolEntry, LoadableConstantEntry {

    /** El tipo de la constante. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** El descriptor nominal de la constante dinámica, con sus argumentos estáticos. */
    default DynamicConstantDesc<?> asSymbol() {
        BootstrapMethodEntry bsm = bootstrap();
        DirectMethodHandleDesc handle = bsm.bootstrapMethod().asSymbol();
        List<LoadableConstantEntry> args = bsm.arguments();
        ConstantDesc[] estaticos = new ConstantDesc[args.size()];
        for (int i = 0; i < args.size(); i++) {
            estaticos[i] = args.get(i).constantValue();
        }
        return DynamicConstantDesc.ofNamed(handle, name().stringValue(), typeSymbol(), estaticos);
    }

    default ConstantDesc constantValue() {
        return asSymbol();
    }

    /** El tipo del valor que `ldc`/`ldc2_w` deja en la pila, derivado del descriptor. */
    default TypeKind typeKind() {
        return TypeKind.fromDescriptor(type());
    }
}
