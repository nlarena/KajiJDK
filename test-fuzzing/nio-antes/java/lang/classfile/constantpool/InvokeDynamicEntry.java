package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

// `CONSTANT_InvokeDynamic_info` (JVMS §4.4.10): `invokedynamic`'s operand. Its descriptor is a
// *method* one -- the call site's type.
public interface InvokeDynamicEntry extends DynamicConstantPoolEntry {

    /** The call site's type. */
    default MethodTypeDesc typeSymbol() {
        return MethodTypeDesc.ofDescriptor(type().stringValue());
    }

    /** The call site's nominal descriptor, with its static arguments. */
    default DynamicCallSiteDesc asSymbol() {
        BootstrapMethodEntry bsm = bootstrap();
        DirectMethodHandleDesc handle = bsm.bootstrapMethod().asSymbol();
        List<LoadableConstantEntry> args = bsm.arguments();
        ConstantDesc[] staticArgs = new ConstantDesc[args.size()];
        for (int i = 0; i < args.size(); i++) {
            staticArgs[i] = args.get(i).constantValue();
        }
        return DynamicCallSiteDesc.of(handle, name().stringValue(), typeSymbol(), staticArgs);
    }
}
