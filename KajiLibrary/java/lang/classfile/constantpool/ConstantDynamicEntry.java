package java.lang.classfile.constantpool;

import java.lang.classfile.BootstrapMethodEntry;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.util.List;

// `CONSTANT_Dynamic_info` (JVMS §4.4.10): a constant the VM works out the first time it is loaded.
// Its descriptor is a *field* one -- the type of what it produces -- unlike
// `CONSTANT_InvokeDynamic`'s, which is a method one.
public interface ConstantDynamicEntry extends DynamicConstantPoolEntry, LoadableConstantEntry {

    /** The constant's type. */
    default ClassDesc typeSymbol() {
        return ClassDesc.ofDescriptor(type().stringValue());
    }

    /** The dynamic constant's nominal descriptor, with its static arguments. */
    default DynamicConstantDesc<?> asSymbol() {
        BootstrapMethodEntry bsm = bootstrap();
        DirectMethodHandleDesc handle = bsm.bootstrapMethod().asSymbol();
        List<LoadableConstantEntry> args = bsm.arguments();
        ConstantDesc[] staticArgs = new ConstantDesc[args.size()];
        for (int i = 0; i < args.size(); i++) {
            staticArgs[i] = args.get(i).constantValue();
        }
        return DynamicConstantDesc.ofNamed(handle, name().stringValue(), typeSymbol(), staticArgs);
    }

    default ConstantDesc constantValue() {
        return asSymbol();
    }

    /** The type of the value `ldc`/`ldc2_w` leaves on the stack, derived from the descriptor. */
    default TypeKind typeKind() {
        return TypeKind.fromDescriptor(type());
    }
}
