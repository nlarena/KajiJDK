package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

// `CONSTANT_InvokeDynamic_info` (JVMS §4.4.10): el operando de `invokedynamic`. Su descriptor es de
// *método* — el tipo del sitio de llamada.
public interface InvokeDynamicEntry extends DynamicConstantPoolEntry {

    /** El tipo del sitio de llamada. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }

    /** El descriptor nominal del sitio de llamada, con sus argumentos estáticos. */
    default DynamicCallSiteDesc asSymbol() {
        BootstrapMethodEntry bsm = bootstrap();
        DirectMethodHandleDesc handle = bsm.bootstrapMethod().asSymbol();
        List<LoadableConstantEntry> args = bsm.arguments();
        ConstantDesc[] estaticos = new ConstantDesc[args.size()];
        for (int i = 0; i < args.size(); i++) {
            estaticos[i] = args.get(i).constantValue();
        }
        return DynamicCallSiteDesc.of(handle, name().stringValue(), typeSymbol(), estaticos);
    }
}
